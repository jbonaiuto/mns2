package mns2.comp;

import mns2.motor.ArmHand;
import mns2.util.ParamsNode;
import sim.graphics.Point3d;
import sim.motor.Graspable;
import sim.util.VA;
import sim.util.Elib;

/**
 * Processes all visual information - Hand state calculation, hand/object working memory, via point snapshots.
 */
public class VisualProcessor extends sim.comp.VisualProcessor
{
    /**
     * status=0 on first call - calc init distance, don't collect input.
     */
    public int status;

    // the initial distance to target.
    private double initialdist;

    // Last wrist position.
    private Point3d lastpos;

    // Last remappingPoint position.
    private Point3d lastRemappingPointPos;

    // Working memory for the object.
    private WorkingMemory workingMemObject;

    // Working memory for the hand.
    private WorkingMemory workingMemHand;

    // Whether or not the object visual information is currently in working memory
    private boolean objInWM;

    // Whether or not the hand visual information is currently in working memory
    private boolean handInWM;

    // Trace of the old hand state parameter values
    private double[] old_param_values;

    // Whether or not to encode the derivative of each hand state parameter.
    public boolean encodeDerivative;

    public VisualProcessor(int numAngles)
    {
        super(numAngles);
        encodeDerivative=false;
    }

    public void reset(int numAngles)
    {
        status=0;
        handInWM=false;
        objInWM=false;
        workingMemObject=new WorkingMemory(2);
        workingMemHand=new WorkingMemory(7);
        lastRemappingPointPos=new Point3d(0,0,0);
        lastpos=new Point3d(0,0,0);
        old_param_values=new double[7];
        initialdist=0.0;
        super.reset(numAngles);
    }
    /**
     * Collects hand state and audio input.
     * @param handInfo - Hand info
     * @param recognizeNet - Network used for grasp recognition
     * @param obj - Object info
     * @param parnode - Hand State parameters
     */
    public void collectHandState(final ArmHand handInfo, final Network recognizeNet,
                                 final Graspable obj, ParamsNode parnode)
    {
        // Get the position of the remapping point
        String remappingPoint="forearm";
        if(recognizeNet!=null)
            remappingPoint=recognizeNet.remappingPoint;
        Point3d remappingPointPos = getRemappingPoint(remappingPoint, handInfo);

        // If first call - clear working mem
        if(status==0)
        {
            clearWorkingMem();
        }

        // Get object info
        Point3d objCenter=new Point3d(),lastOpposition=new Point3d();
        // If the object can be seen - update working memory
        if(obj.visible)
        {
            objCenter = new Point3d(obj.objectCenter.x, obj.objectCenter.y, obj.objectCenter.z);
            lastOpposition = new Point3d(obj.lastopposition.x, obj.lastopposition.y, obj.lastopposition.z);
            updateObjectWorkingMem(objCenter, lastOpposition,parnode.getSlot());
        }
        // If the object cannot be seen
        else if(objInWM)
        {
            // Decay working memory a bit
            workingMemObject.decay(parnode.getSlot(), null);

            // Get the object information from working memory
            objCenter = (Point3d)workingMemObject.get(0);
            lastOpposition = (Point3d)workingMemObject.get(1);
        }

        // Get hand info
        Double indexAperture= 0.0,thumbBeta= 0.0,thumbJointBeta= 0.0,ang1= 0.0,ang2= 0.0;
        Point3d wristx=new Point3d(),indexApertureCenter=new Point3d(),indexAperDir=new Point3d(),sideAperDir=new Point3d();
        Double axisDisp1= 0.0,axisDisp2= 0.0;
        // If the hand can be seen - update working memory
        if(handInfo.visible)
        {
            indexAperture = handInfo.indexAperture();
            thumbBeta = handInfo.thumb.beta;
            thumbJointBeta = handInfo.thumb.child[0].beta;
            ang1 = Math.pow((thumbBeta + Math.PI) / (2 * Math.PI), 2);
            ang2 = Math.pow((thumbJointBeta + Math.PI) / (2 * Math.PI), 2);
            wristx = new Point3d(handInfo.wristx.limb_pos.x, handInfo.wristx.limb_pos.y, handInfo.wristx.limb_pos.z);
            indexApertureCenter = new Point3d(handInfo.indexApertureCenter().x, handInfo.indexApertureCenter().y,
                                                      handInfo.indexApertureCenter().z);
            indexAperDir = new Point3d(handInfo.indexAperDir().x, handInfo.indexAperDir().y,
                                               handInfo.indexAperDir().z);
            sideAperDir = new Point3d(handInfo.sideAperDir().x, handInfo.sideAperDir().y, handInfo.sideAperDir().z);
            axisDisp1 = Elib.sqr(VA.inner(indexAperDir, lastOpposition));
            axisDisp2 = Elib.sqr(VA.inner(sideAperDir, lastOpposition));

            boolean dynamicRemapping=true;
            if(recognizeNet!=null)
                dynamicRemapping=recognizeNet.dynRemapping;
            updateHandWorkingMem(dynamicRemapping, indexAperture, ang1, ang2, wristx, indexApertureCenter, axisDisp1,
                    axisDisp2, parnode.getSlot());
        }
        // If the hand cannot be seen
        else if(handInWM)
        {
            // Calculate the displacement of the remapping point
            final Point3d displacement = VA.subtract(remappingPointPos, lastRemappingPointPos);

            // Decay working memory a bit and remap
            workingMemHand.decay(parnode.getSlot(), displacement);

            // Get the hand information from working memory
            indexAperture = (Double)workingMemHand.get(0);
            ang1 = (Double)workingMemHand.get(1);
            ang2 = (Double)workingMemHand.get(2);
            wristx = (Point3d)workingMemHand.get(3);
            indexApertureCenter = (Point3d)workingMemHand.get(4);
            axisDisp1 = (Double)workingMemHand.get(5);
            axisDisp2 = (Double)workingMemHand.get(6);
        }

        // Calculate hand state
        double aper1 = (indexAperture) /350.0;
        double distance =VA.dist(objCenter, indexApertureCenter)/initialdist;
        double speed = 5*VA.dist(lastpos,wristx)/initialdist;
        if((!obj.visible && !objInWM) || (!handInfo.visible && !handInWM))
        {
            distance=0.0;
            speed=0.0;
        }

        double[] handStateD = getHandStateDerivative(aper1, ang1, ang2, speed, distance, axisDisp1, axisDisp2);
        // First call - calc init distance
        if (status==0)
        {
            status=1;

            // Set last wrist position
            lastpos=wristx.duplicate();

            // Calculate initial dist from hand to target
            initialdist=VA.dist(objCenter, indexApertureCenter);

            // wait for next call - don't take input
            return;
        }

        encodeHandState(parnode, aper1, ang1, ang2, speed, distance, axisDisp1, axisDisp2);

        if(encodeDerivative)
        {
            encodeHandStateDerivative(parnode, handStateD);
        }

        captureViaPointSnapshot(speed, handInfo, parnode);

        // Set last wrist position
        lastpos=wristx.duplicate();

        // Set last remapping point position
        lastRemappingPointPos=remappingPointPos.duplicate();
    }

    private static void encodeHandStateDerivative(ParamsNode parnode, double[] handStateD)
    {
        parnode.put("d_aper1",(handStateD[0]/2)+.5);
        parnode.put("d_ang1", (handStateD[1]/2)+.5);
        parnode.put("d_ang2", (handStateD[2]/2)+.5);
        parnode.put("d_speed",(handStateD[3]/2)+.5);
        parnode.put("d_dist", (handStateD[4]/2)+.5);
        parnode.put("d_axisdisp1", (handStateD[5]/2)+.5);
        parnode.put("d_axisdisp2", (handStateD[6]/2)+.5);
    }

    private static void encodeHandState(ParamsNode parnode, double aper1, Double ang1, Double ang2, double speed,
                                        double distance, Double axisDisp1, Double axisDisp2)
    {
        parnode.put("aper1", aper1);
        parnode.put("ang1", ang1);
        parnode.put("ang2", ang2);
        parnode.put("speed",speed);
        parnode.put("dist", distance);
        parnode.put("axisdisp1", axisDisp1);
        parnode.put("axisdisp2", axisDisp2);
    }

    private double[] getHandStateDerivative(double aper1, double ang1, double ang2, double speed, double distance,
                                            double axisDisp1, double axisDisp2)
    {
        double[] handStateD = new double[7];

        handStateD[0]=aper1-old_param_values[0];
        old_param_values[0]=aper1;

        handStateD[1]=ang1-old_param_values[1];
        old_param_values[1]=ang1;

        handStateD[2]=ang2-old_param_values[2];
        old_param_values[2]=ang2;

        handStateD[3]=speed-old_param_values[3];
        old_param_values[3]=speed;
        if(Double.isInfinite(speed))
            old_param_values[3]=0.0;

        handStateD[4]=distance-old_param_values[4];
        old_param_values[4]=distance;
        if(Double.isInfinite(distance))
            old_param_values[4]=0.0;

        handStateD[5]=axisDisp1-old_param_values[5];
        old_param_values[5]=axisDisp1;

        handStateD[6]=axisDisp2-old_param_values[6];
        old_param_values[6]=axisDisp2;

        return handStateD;
    }

    private void captureViaPointSnapshot(double speed, ArmHand handInfo, ParamsNode parnode)
    {
        if(speed <= viaPointSpeedThresh && (viaPointIdx==0 || handInfo.rtime!=viaPointRatios[viaPointIdx-1]))
        {
            viaPointSnapshots[viaPointIdx]=handInfo.getJointAngleSnapshot(viaPointNoiseLevel);
            viaPointRatios[viaPointIdx++]=handInfo.rtime;
        }
        else if(parnode.getSlot() > 1 && parnode.getAll()[3][parnode.getSlot()-1]<parnode.getAll()[3][parnode.getSlot()-2] &&
                parnode.getAll()[3][parnode.getSlot()-1]<speed && (viaPointIdx==0 || handInfo.rtime!=viaPointRatios[viaPointIdx-1]))
        {
            viaPointSnapshots[viaPointIdx]=viaPointSnapshotTrace[1];
            viaPointRatios[viaPointIdx++]=viaPointRatiosTrace[1];
        }
        viaPointSnapshotTrace[0]=viaPointSnapshotTrace[1];
        viaPointSnapshotTrace[1]=handInfo.getJointAngleSnapshot(viaPointNoiseLevel);
        viaPointRatiosTrace[0]=viaPointRatiosTrace[1];
        viaPointRatiosTrace[1]=handInfo.rtime;
    }

    private static Point3d getRemappingPoint(String remappingPoint, ArmHand handInfo)
    {
        Point3d remappingPointPos = new Point3d();
        if(remappingPoint.equals("arm"))
        {
            remappingPointPos = VA.center(handInfo.j1.joint_pos,handInfo.j4.joint_pos);
        }
        else if(remappingPoint.equals("elbow"))
        {
            remappingPointPos = handInfo.j4.joint_pos;
        }
        else if(remappingPoint.equals("forearm"))
        {
            remappingPointPos = VA.center(handInfo.j4.joint_pos, handInfo.wristx.joint_pos);
        }
        return remappingPointPos;
    }

    private void updateHandWorkingMem(boolean dynRemapping, Double indexAperture, Double ang1, Double ang2,
                                      Point3d wristx, Point3d indexApertureCenter, Double axisDisp1, Double axisDisp2,
                                      int timeSlot)
    {
        handInWM=true;
        workingMemHand.set(0, indexAperture, false);
        workingMemHand.set(1, ang1, false);
        workingMemHand.set(2, ang2, false);
        workingMemHand.set(3, wristx, dynRemapping);
        workingMemHand.set(4, indexApertureCenter, dynRemapping);
        workingMemHand.set(5, axisDisp1, false);
        workingMemHand.set(6, axisDisp2, false);
        workingMemHand.setTimeStamp(timeSlot);
    }

    private void updateObjectWorkingMem(Point3d objCenter, Point3d lastOpposition, int timeSlot)
    {
        objInWM=true;
        workingMemObject.set(0, objCenter, false);
        workingMemObject.set(1, lastOpposition, false);
        workingMemObject.setTimeStamp(timeSlot);
    }

    private void clearWorkingMem()
    {
        objInWM=false;
        handInWM=false;
        workingMemObject.clear();
        workingMemHand.clear();
    }
}