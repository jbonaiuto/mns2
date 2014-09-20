package mns2.motor;


import sim.util.Log;
import sim.util.Elib;
import sim.motor.Trajectory;
import sim.motor.Graspable;
import sim.graphics.Point3d;
import sim.graphics.Mars;
import mns2.comp.*;
import mns2.util.ParamsNode;
import mns2.main.Main;

/**
 * Arm and hand simulator
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class ArmHand extends sim.motor.ArmHand
{
    // Whether or not the grasp is hidden
    public boolean hiddenGrasp = false;

    public Network net;

    public static int recognizeDelay=0;

    public ParamsNode handState;

    // Record the hand state trajectory for training data
    public boolean recordit=false;

    // Apply hand state to network input for recognition
    public boolean recognize=false;

    public boolean imitator=false;

    //public boolean execute=true;

    public Network recognizeNet;

    /**
     * Constructor
     * @param s
     * @param pipesidec
     * @param piperad
     */
    public ArmHand(final String s, final int pipesidec, final double piperad)
    {
        super(s,pipesidec,piperad);
        imitator=false;
        handState=new ParamsNode(NetworkInterface.MAX_patternlength,NetworkInterface.params);
        visualProcessor=new mns2.comp.VisualProcessor(segc);
    }

    /**
     * Called by the reach thread when executing a stored trajectory during each time step.
     * @param tr - The reach trajectory
     * @param obj - The object that the reach is directed toward
     */
    public boolean tickReachGesture(final Trajectory tr, final sim.motor.Graspable obj, final sim.motor.Graspable obstacle)
    {
        // If this is a hidden grasp and its time to disappear - set myself to invisible
        if(hiddenGrasp && visible && obstacle!=null && obstacle.visible)
        {
            Point3d occluderEdge = obstacle.objectCenter.duplicate();
            occluderEdge.z-=1075;
            //occluderEdge.z-=obstacle.objectRadius;
            if(occluderEdge.z<=wristx.joint_pos.z && obstacle.objectCenter.x>wristx.joint_pos.x)
            {
                //HV.self.setArmVisibility(this, false);
                Log.println("Hand disappearing behind screen");
                visible=false;
            }
        }
        return super.tickReachGesture(tr, obj, obstacle);
    }

    /**
     * Main reach/grasp starting entry. All kinds of reaches,
     * visearch, silet, execute, record etc. are started from here.
     * @param obj - The object that the reach is directed toward
     * @param obs - Obstacle object
     * @param kind - Type of reach
     * @param net - The network to recognize with
     * @param netType - The type of network
     */
    public void doReach(final sim.motor.Graspable obj, final sim.motor.Graspable obs,
                        final String kind, final Network net, final String netType, final String weightFile)
    {
        this.recognizeNet=net;
        //this.recognizeNet=Network.configureNet(netType,weightFile);
        recordit=false;
        recognize=false;
        if (kind.equals("record") )
            recordit=true;
        else if (kind.equals("recognize") )
            recognize=true;
        if(!kind.equals("visual"))
        {
            NetworkInterface.prepareForInput(netType);
            visualProcessor.reset(segc);
            if(netType.startsWith("BPTT"))
            {
                ((mns2.comp.VisualProcessor)visualProcessor).encodeDerivative=false;
            }
            else if(netType.startsWith("BP"))
            {
                ((mns2.comp.VisualProcessor)visualProcessor).encodeDerivative=true;
            }
            else if(netType.equals("Hebbian"))
            {
                ((mns2.comp.VisualProcessor)visualProcessor).encodeDerivative=true;
            }
            AuditoryProcessor.reset();
            handState=new ParamsNode(NetworkInterface.MAX_patternlength,NetworkInterface.params);
            handState.reset();
        }
        super.doReach(obj, obs, kind);
    }

    /**
     * Instantiates a reach thread and starts it.
     * @param com
     * @param obj - The object the reach is directed toward
     * @param obs - Obstacle object
     */
    public Reach createReachThread(final String com, final sim.motor.Graspable obj, final sim.motor.Graspable obs)
    {
        if(obs!=null && obs.visible)
            search_phase=-1;
        else
            search_phase=0;
        rtime=0;
        contact=false;
        reach_speed= Elib.toDouble(((mns2.main.Main)Main.self).speedCombo.getSelectedItem().toString());
        reach_deltat=reach_speed*reach_basetime;
        activeReachThread=new Reach(this,obj,obs,lasttr,com);
        activeReachThread.start();
        return (mns2.motor.Reach)activeReachThread;
    }

    /**
     * Performs final necessary steps for reaches
     * @param com
     * @param obj - The object the reach was directed towards
     */
    public void finalizeReach(final String com, final sim.motor.Graspable obj)
    {
        Main.refreshDisplay();
        Main.self.setInfoReady();

        String tip="EXECUTE";
        if (search_mode==ArmHand.VISUAL_SEARCH)
            tip="VISUAL_SEARCH";

        // If this was a grasp and it was successful
        if(!eating && dis<zeroError*3)
        {
            grasped=true;
        }

        // If recording or recognizing - Run the network for 10 more time steps - as if the hand is holding the object
        // for 10 time steps
        if(recordit || recognize)
        {
            for(int i=0; i<10; i++)
            {
                perceive(this, obj, true);
                if(i>1 && Main.recordCanvas)
                {
                    Main.createImage();
                }
            }
        }
        // If recording - write the input and output sequence patterns to file
        if (recordit)
        {
            double thresh = zeroError*3;
            if(obj.lastPlan== Graspable.SIDE)
                thresh = zeroError*6;
            if (dis<thresh)  // somehow the dis usually is not smaller than zeroError
                NetworkInterface.writePattern(obj,"reach error:"+Elib.nice(dis,1e4),handState);
            else if (Main.negativeExample)
                NetworkInterface.writeNegPattern(obj,"reach error:"+Elib.nice(dis,1e4),handState);
            else
                Log.println(" *** Discarding this reach. dis :"+dis+" not requested as negative but dis is too big.");
            lasterr=dis;
        }
        Main.self.reportFinish("   ===> ("+tip+") Grasp Final error:"+dis+".  "+com);
    }

    public void perceive(mns2.motor.ArmHand observedArmHand, sim.motor.Graspable obj, boolean staticAction)
    {
        if(recognize || recordit)
        {
            ((mns2.comp.VisualProcessor)visualProcessor).collectHandState(observedArmHand, recognizeNet, obj, handState);
            if(NetworkInterface.netType.equals("BPTTwithHebbian"))
                    handState=AuditoryProcessor.collectAuditoryInput(observedArmHand, recognizeNet, obj, handState,
                            congruentSound);

            final int hh=(int)(0.5+observedArmHand.rtime/observedArmHand.reach_deltat);
            if(recognize && hh > recognizeDelay)
            {
                final String mirr=NetworkInterface.recognize(recognizeNet,handState,recognizeNet.recordNetwork,
                        staticAction);
                if(staticAction)
                {
                    System.out.print(" ========> "+mirr+" <========\n");
                    Main.self.setInfo("Ready. - Action recognized as "+mirr);
                }
                else
                {
                    Log.println(" --------> "+mirr+" <-------- (at t="+ Elib.nice(observedArmHand.rtime,1e3)+')');
                    Main.self.setInfo("Grasp in progress. - so far action looks like (recognized as)  "+mirr);
                }
                Main.updateRecBars(NetworkInterface.lastout[0],NetworkInterface.lastout[1], NetworkInterface.lastout[2]);
            }
            handState.advance();
        }
    }

    public void doImitate(final String imitationType, final sim.motor.Graspable obj, final sim.motor.Graspable obs, Network net,
                          String netType, String weightFile)
    {
        if(imitationType.equals("direct"))
            doDirectImitate(obj,obs);
        else if(imitationType.equals("indirect"))
            doIndirectImitate(obj,obs,net,netType,weightFile);
        else
            doNaturalImitate(obj,obs,net,netType,weightFile);
    }

    public void doDirectImitate(final sim.motor.Graspable obj, final sim.motor.Graspable obs)
    {
        if(visualProcessor.viaPointIdx>0)
        {
            Main.traceon=true;
            Mars.clearStars(armHandNumber);
            recordit=false;
            recognize=false;
            makeNeutral();

            // Store current joint angles
            theta1 =new double[segc];
            storeAngles(theta1);

            // Reset distance
            dis=1e10;

            Main.self.reportStart("     Starting to imitate");

            // target joint angles
            final double[] sol=new double[segc];
            System.arraycopy(visualProcessor.viaPointSnapshots[visualProcessor.viaPointIdx-1], 0, sol, 0, segc);
            // via point joint angles
            final double[][] viaPoints=new double[visualProcessor.viaPointIdx-1][segc];
            final double[] viaRatios=new double[visualProcessor.viaPointIdx-1];
            if(viaPoints.length>0)
            {
                for(int i=0; i<visualProcessor.viaPointIdx-1; i++)
                    System.arraycopy(visualProcessor.viaPointSnapshots[i], 0, viaPoints[i], 0, segc);
                System.arraycopy(visualProcessor.viaPointRatios, 0, viaRatios, 0, visualProcessor.viaPointIdx-1);
                lasttr=trimTrajectory(sol,viaPoints,viaRatios);
            }
            else
                lasttr=trimTrajectory(sol);
            fireExecution(obj,obs);
        }
    }

    public void doIndirectImitate(sim.motor.Graspable obj, sim.motor.Graspable obs, Network net, String netType,
                                  String weightFile)
    {
        boolean recordingCanvas = Main.recordCanvas;
        if(Main.recordCanvas=true)
            Main.recordCanvas=false;
        String graspType = NetworkInterface.interpret(NetworkInterface.lastout);
        if(graspType.length()>0 && !graspType.startsWith("[not confident]"))
        {
            obj.computeAffordance(graspType, this);
            doReach(obj, obs, "visual", net, netType, weightFile);
            try
            {
                while(reachActive())
                {
                    Thread.sleep(100);
                }
                Thread.sleep(1000);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        Main.traceon=true;
        Mars.clearStars(armHandNumber);
        if(recordingCanvas)
            Main.recordCanvas=true;
        doReach(obj,obs,"execute", net, netType, weightFile);
    }

    public void doNaturalImitate(sim.motor.Graspable obj, sim.motor.Graspable obs, Network net, String netType,
                                 String weightFile)
    {
        // target joint angles
        double[] sol=new double[segc];
        if(visualProcessor.viaPointIdx>0)
            System.arraycopy(visualProcessor.viaPointSnapshots[visualProcessor.viaPointIdx-1], 0, sol, 0, segc);
        theta1 =new double[segc];
        storeAngles(theta1);

        String graspType = NetworkInterface.interpret(NetworkInterface.lastout);
        if(graspType.length()>0 && !graspType.startsWith("[not confident]"))
        {
            boolean recordingCanvas = Main.recordCanvas;
            if(Main.recordCanvas=true)
                Main.recordCanvas=false;

            obj.computeAffordance(graspType, this);
            doReach(obj,obs,"visual",net,netType,weightFile);
            try
            {
                while(reachActive())
                {
                    Thread.sleep(100);
                }
                Thread.sleep(1000);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            sol=new double[segc];
            storeAngles(sol);
            if(recordingCanvas)
                Main.recordCanvas=true;
        }
        // via point joint angles
        int viaCnt=0;
        for(int i=0; i<visualProcessor.viaPointIdx-1; i++)
        {
            if(graspType.length()==0 || graspType.startsWith("[not confident]") || visualProcessor.viaPointRatios[i] < .75)
                viaCnt++;
        }
        final double[][] viaPoints=new double[viaCnt][segc];
        final double[] viaRatios=new double[viaCnt];
        int idx=0;
        for(int i=0; i<visualProcessor.viaPointIdx-1; i++)
        {
            if(graspType.length()==0 || graspType.startsWith("[not confident]") || visualProcessor.viaPointRatios[i] < .75)
            {
                System.arraycopy(visualProcessor.viaPointSnapshots[i], 0, viaPoints[idx], 0, segc);
                viaRatios[idx++] = visualProcessor.viaPointRatios[i];
            }
        }
        if(viaPoints.length>0)
            lasttr=trimTrajectory(sol,viaPoints,viaRatios);

        Main.traceon=true;
        doReach(obj,obs,"execute",net,netType,weightFile);
    }
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/

