package mns2.main;

import java.io.IOException;
import java.io.DataOutputStream;
import java.util.Vector;

import mns2.comp.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import sim.graphics.Point3d;
import sim.graphics.Mars;
import sim.graphics.MainPanel;
import sim.util.*;
import sim.util.Acme.JPM.Encoders.GifEncoder;
import sim.motor.ArmHand;

/**
 * Reads in simulation configuration XML files and executes the simulation experiments defined
 * in them.
 */
public class SimulationExperimentManager
{
    private static String baseDir;

    private static String[] mdsNames;
    private static int[] mdsNumTrials;
    private static double[][][][] mdsTrialHistories;
    private static int[][] mdsTrialHistoryLengths;
    private static String[][] mdsTrialNames;

    private static int numberMDSDefinitions;

    private static Node otherJointAngles=null;
    private static Node selfJointAngles=null;

    private static String imitationType="none";
    /**
     * Run the simulation defined in the given simulation config file.
     * @param configFile - Name of the XML file defining the configuration of the simulation experiment to be executed
     */
    public static void run(String configFile)
    {
        try
        {
            Document simulationConfig = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("SE/"+configFile);
            baseDir="";

            Element root = simulationConfig.getDocumentElement();
            NodeList children = root.getChildNodes();
            for(int i=0; i<children.getLength(); i++)
            {
                Node rootChild = children.item(i);
                if(rootChild.getNodeName().equals("BaseDir"))
                {
                    if(rootChild.getChildNodes().getLength() > 0)
                        baseDir = rootChild.getChildNodes().item(0).getNodeValue() + '/';
                }
                else if (rootChild.getNodeName().equals("MDS-Definitions"))
                {
                    readMDSDefinitions(rootChild);
                }
                else if (rootChild.getNodeName().equals("Simulations"))
                {
                    readSimulations(rootChild);
                    Log.setLogFile(null);
                }
            }
            executeMDS();
        }
        catch(Exception e)
        {
            System.err.println("Error parsing simulation experiment file");
            e.printStackTrace();
        }
    }

    /**
     * Reads a group of grasps and trials for MDS display from the simulation config file.
     * @param rootChild - XML node containing the MDS group
     */
    private static void readMDSDefinitions(Node rootChild)
    {
        int mdsDefIdx=0;
        numberMDSDefinitions=0;
        NodeList mdsChildren = rootChild.getChildNodes();
        for(int i=0; i<mdsChildren.getLength(); i++)
        {
            if(mdsChildren.item(i).getNodeName().equals("MDS"))
            {
                numberMDSDefinitions++;
            }
        }
        mdsNames=new String[numberMDSDefinitions];
        mdsNumTrials = new int[numberMDSDefinitions];
        mdsTrialNames = new String[numberMDSDefinitions][];
        mdsTrialHistories = new double[numberMDSDefinitions][][][];
        mdsTrialHistoryLengths = new int[numberMDSDefinitions][];
        for(int f=0; f<mdsChildren.getLength(); f++)
        {
            Node mdsChild = mdsChildren.item(f);
            if(mdsChild.getNodeName().equals("MDS"))
            {
                readMDS(mdsChild, mdsDefIdx);
                mdsDefIdx++;
            }
        }
    }

    private static void readMDS(Node mds, int idx)
    {
        NodeList mdsChildren = mds.getChildNodes();
        for(int i=0; i<mdsChildren.getLength(); i++)
        {
            Node mdsChild = mdsChildren.item(i);
            if(mdsChild.getNodeName().equals("Name"))
            {
                if(mdsChild.getChildNodes().getLength()>0)
                    mdsNames[idx] = mdsChild.getChildNodes().item(0).getNodeValue();
            }
            else if(mdsChild.getNodeName().equals("Trials"))
            {
                int numTrials=0;
                int trialIdx=0;
                NodeList trials = mdsChild.getChildNodes();
                for(int k=0; k<trials.getLength(); k++)
                {
                    Node trial = trials.item(k);
                    if(trial.getNodeName().equals("Trial"))
                    {
                        numTrials++;
                    }
                }
                mdsNumTrials[idx] = numTrials;
                mdsTrialHistories[idx] = new double[numTrials][][];
                mdsTrialHistoryLengths[idx] = new int[numTrials];
                mdsTrialNames[idx] =new String[numTrials];
                for(int j=0; j<trials.getLength(); j++)
                {
                    Node trial = trials.item(j);
                    if(trial.getNodeName().equals("Trial"))
                    {
                        if(trial.getChildNodes().getLength()>0)
                        {
                            mdsTrialNames[idx][trialIdx]=trial.getChildNodes().item(0).getNodeValue();
                            trialIdx++;
                        }
                    }
                }
            }
        }
    }

    private static void executeMDS()
    {
        for(int a=0; a<numberMDSDefinitions; a++)
        {
            int totalLength=0;
            int hiddenLayerDim=-1;
            for(int i=0; i<mdsNumTrials[a]; i++)
            {
                totalLength+=mdsTrialHistoryLengths[a][i];
            }
            double[][] allPoints = new double[totalLength][];
            int curIdx=0;
            for(int i=0; i<mdsNumTrials[a]; i++)
            {
                for(int j=0; j<mdsTrialHistoryLengths[a][i]; j++)
                {
                    allPoints[curIdx++]=mdsTrialHistories[a][i][j];
                    if(hiddenLayerDim<0)
                        hiddenLayerDim=mdsTrialHistories[a][i][j].length;
                }
            }
            Vista vista = new Vista(allPoints, curIdx, hiddenLayerDim, "Hidden", "", baseDir+mdsNames[a]+".lsp");
            Point3d[] vistaCoords = vista.getCoords();
            Point3d[][] mdsCoords = new Point3d[mdsNumTrials[a]][];
            int idx=0;
            for(int i=0; i<mdsNumTrials[a]; i++)
            {
                mdsCoords[i]=new Point3d[mdsTrialHistoryLengths[a][i]];
                for(int j=0; j<mdsTrialHistoryLengths[a][i]-1; j++)
                {
                    mdsCoords[i][j] = vistaCoords[idx++];
                }
            }
            Gplot.plot(mdsCoords, mdsTrialHistoryLengths[a], mdsNumTrials[a], "", mdsTrialNames[a], baseDir+mdsNames[a]);
        }
    }
    /**
     * Reads a group of grasps from the simulation config file.
     * @param rootChild - XML node containing the grasps
     * @throws IOException
     */
    private static void readSimulations(Node rootChild) throws IOException
    {
        NodeList simulations = rootChild.getChildNodes();
        for(int a=0; a<simulations.getLength(); a++)
        {
            Node simulation = simulations.item(a);
            if(simulation.getNodeName().equals("Simulation"))
            {
                readSimulation(simulation);
            }
        }
    }

    /**
     * Reads simulation information from the simulation config file and executes simulation trials using that information
     * @param simulation - XML node containing the grasp information
     * @throws IOException
     */
    private static void readSimulation(Node simulation) throws IOException
    {
        otherJointAngles=null;
        selfJointAngles=null;
        NodeList simulationChildren = simulation.getChildNodes();
        for(int b=0; b<simulationChildren.getLength(); b++)
        {
            Node simulationChild = simulationChildren.item(b);
            if(simulationChild.getNodeName().equals("Object"))
            {
                readObject(simulationChild, "target");
            }
            else if(simulationChild.getNodeName().equals("Obstacle"))
            {
                readObject(simulationChild, "obstacle");
            }
            else if(simulationChild.getNodeName().equals("Other"))
            {
                readArmHand(simulationChild, (mns2.motor.ArmHand)Main.self.otherArmHand, "other");
            }
            else if(simulationChild.getNodeName().equals("Self"))
            {
                readArmHand(simulationChild, (mns2.motor.ArmHand)((mns2.main.Main)Main.self).selfArmHand, "self");
            }
            else if(simulationChild.getNodeName().equals("View"))
            {
                readView(simulationChild);
            }
            else if(simulationChild.getNodeName().equals("Trials"))
            {
                if(otherJointAngles!=null)
                    resetJointAngles(otherJointAngles, Main.self.otherArmHand, "other");
                if(selfJointAngles!=null)
                    resetJointAngles(selfJointAngles, ((mns2.main.Main)Main.self).selfArmHand, "self");
                Main.setTrace(false);
                Mars.clearStars(1);
                Mars.clearStars(2);
                Main.self.toggleReach("visual");
                while(Main.self.otherArmHand.reachActive())
                {
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch(InterruptedException e)
                    {}
                }

                NodeList trialsChildren = simulationChild.getChildNodes();
                for(int j=0; j<trialsChildren.getLength(); j++)
                {
                    Node trial = trialsChildren.item(j);
                    if(trial.getNodeName().equals("Trial"))
                    {
                        if(otherJointAngles!=null)
                            resetJointAngles(otherJointAngles, Main.self.otherArmHand, "other");
                        if(selfJointAngles!=null)
                            resetJointAngles(selfJointAngles, ((mns2.main.Main)Main.self).selfArmHand, "self");
                        readTrial(trial);
                    }
                }
            }
        }
    }

    /*private static void readAvatar(Node avatarNode, mns2.main.Actor avatar, String avatarType)
    {
        NodeList avatarChildren = avatarNode.getChildNodes();
        for(int i=0; i<avatarChildren.getLength(); i++)
        {
            Node avatarChild = avatarChildren.item(i);
            if(avatarChild.getNodeName().equals("ArmHand"))
            {
                readArmHand(avatarChild, (mns2.motor.ArmHand)avatar.getArmHand(), avatarType);
            }
            else if(avatarChild.getNodeName().equals("Network"))
            {
                readNetwork(avatarChild, avatar);
            }
        }
    }*/

    private static void readArmHand(Node armHandNode, mns2.motor.ArmHand armHand, String avatarType)
    {
        NodeList armHandChildren = armHandNode.getChildNodes();
        for(int i=0; i<armHandChildren.getLength(); i++)
        {
            Node armHandChild = armHandChildren.item(i);
            if(armHandChild.getNodeName().equals("JointAngles"))
            {
                readJointAngles(armHandChild, armHand, avatarType);
            }
            else if(armHandChild.getNodeName().equals("Visible"))
            {
                readArmHandVisibility(armHandChild, armHand, true);
            }
            else if(armHandChild.getNodeName().equals("Network"))
            {
                readNetwork(armHandChild, armHand);
            }
        }
    }

    private static void readNetwork(Node armHandChild, mns2.motor.ArmHand armHand)
    {
        armHand.net.lesionedConnection=new Vector();
        armHand.net.lesionTime=new Vector();
        NodeList networkChildren = armHandChild.getChildNodes();
        for(int i=0; i<networkChildren.getLength(); i++)
        {
            Node networkChild = networkChildren.item(i);
            if(networkChild.getNodeName().equals("LearningRule"))
            {
                if(networkChild.getChildNodes().getLength()>0)
                {
                    String netType=networkChild.getChildNodes().item(0).getNodeValue();
                    ((mns2.main.Main)Main.self).netTypeCombo.setSelectedItem(netType);
                }
            }
            else if(networkChild.getNodeName().equals("Lesions"))
            {
                readLesions(networkChild, armHand.net);
            }
            else if(networkChild.getNodeName().equals("DynamicRemapping"))
            {
                if(networkChild.getChildNodes().getLength()>0)
                    armHand.net.dynRemapping = Boolean.parseBoolean(networkChild.getChildNodes().item(0).getNodeValue());
            }
            else if(networkChild.getNodeName().equals("RemappingPoint"))
            {
                if(networkChild.getChildNodes().getLength()>0)
                    armHand.net.remappingPoint = networkChild.getChildNodes().item(0).getNodeValue();
            }
            else if(networkChild.getNodeName().equals("WeightFile"))
            {
                if(networkChild.getChildNodes().getLength()>0)
                {
                    String filename=networkChild.getChildNodes().item(0).getNodeValue();
                    armHand.net.netFromWeight(filename);
                    ((mns2.main.Main)Main.self).weightTextField.setText(filename);
                }
            }
            else if(networkChild.getNodeName().equals("SoundWeightFile"))
            {
                if(networkChild.getChildNodes().getLength()>0)
                {
                    AuditoryProcessor.soundWeightFile=networkChild.getChildNodes().item(0).getNodeValue();
                }
            }
        }
    }

    private static void readLesions(Node lesionsNode, Network net)
    {
        NodeList lesionsChildren = lesionsNode.getChildNodes();
        net.lesionedConnection=new Vector();
        net.lesionTime =new Vector(0);
        for(int i=0; i<lesionsChildren.getLength(); i++)
        {
            Node lesionChild = lesionsChildren.item(i);
            if(lesionChild.getNodeName().equals("Lesion"))
            {
                readLesion(lesionChild, net);
            }
        }
    }

    private static void readLesion(Node lesionNode, Network net)
    {
        NodeList lesionChildren = lesionNode.getChildNodes();
        for(int i=0; i<lesionChildren.getLength(); i++)
        {
            Node lesionChild = lesionChildren.item(i);
            if(lesionChild.getNodeName().equals("Connection"))
            {
                if(lesionChild.getChildNodes().getLength()>0)
                    net.lesionedConnection.add(lesionChild.getChildNodes().item(0).getNodeValue());
            }
            else if(lesionChild.getNodeName().equals("Time"))
            {
                if(lesionChild.getChildNodes().getLength()>0)
                    net.lesionTime.add(lesionChild.getChildNodes().item(0).getNodeValue());
            }
        }
    }

    /**
     * Reads grasp trial information from the simulation config file and executes a recognize grasp using that
     * information.
     * @param trial - XML node containing the grasp trial information
     * @throws IOException
     */
    private static void readTrial(Node trial) throws IOException
    {
        ((mns2.main.Main)Main.self).selfArmHand.net = new BPTTwithHebbian();
        String trialName="";
        boolean postScreenshot=false;
        boolean moveObject=false;
        Point3d finalObjectPos=new Point3d();
        boolean moveObstacle=false;
        Point3d finalObstaclePos=new Point3d();
        boolean recordSim=false;
        boolean recordNet=false;
        imitationType="none";

        NodeList trialSettings = trial.getChildNodes();
        for(int k=0; k<trialSettings.getLength(); k++)
        {
            Node trialSetting = trialSettings.item(k);
            if(trialSetting.getNodeName().equals("TrialName"))
            {
                if(trialSetting.getChildNodes().getLength()>0)
                {
                    trialName = trialSetting.getChildNodes().item(0).getNodeValue();
                    Log.setLogFile(baseDir+trialName+".log");
                }
            }
            else if(trialSetting.getNodeName().equals("Output"))
            {
                NodeList outputChildren=trialSetting.getChildNodes();
                for(int i=0; i<outputChildren.getLength(); i++)
                {
                    Node outputChild=outputChildren.item(i);
                    if(outputChild.getNodeName().equals("Simulation"))
                    {
                        NodeList simChildren = outputChild.getChildNodes();
                        for(int j=0; j<simChildren.getLength(); j++)
                        {
                            Node simChild = simChildren.item(j);
                            if(simChild.getNodeName().equals("Record"))
                            {
                                if(simChild.getChildNodes().getLength()>0)
                                    recordSim=Boolean.parseBoolean(simChild.getChildNodes().item(0).getNodeValue());
                            }
                            else if(simChild.getNodeName().equals("Format"))
                            {
                                if(simChild.getChildNodes().getLength()>0)
                                {
                                    if(simChild.getChildNodes().item(0).getNodeValue().equals("gif"))
                                        Main.recordImageFormat= MainPanel.IMAGE_FORMAT_GIF;
                                    else if(simChild.getChildNodes().item(0).getNodeValue().equals("ppm"))
                                        Main.recordImageFormat= MainPanel.IMAGE_FORMAT_PPM;
                                }
                            }
                        }
                    }
                    else if(outputChild.getNodeName().equals("Network"))
                    {
                        NodeList netChildren = outputChild.getChildNodes();
                        for(int j=0; j<netChildren.getLength(); j++)
                        {
                            Node netChild = netChildren.item(j);
                            if(netChild.getNodeName().equals("Record"))
                            {
                                if(netChild.getChildNodes().getLength()>0)
                                    recordNet=Boolean.parseBoolean(netChild.getChildNodes().item(0).getNodeValue());
                            }
                        }
                    }
                }
            }
            else if(trialSetting.getNodeName().equals("Object"))
            {
                readObject(trialSetting, "target");
            }
            else if(trialSetting.getNodeName().equals("Obstacle"))
            {
                readObject(trialSetting, "obstacle");
            }
            else if(trialSetting.getNodeName().equals("Grasp"))
            {
                readGrasp(trialSetting);
            }
            else if(trialSetting.getNodeName().equals("Other"))
            {
                readArmHand(trialSetting, (mns2.motor.ArmHand)Main.self.otherArmHand, "other");
            }
            else if(trialSetting.getNodeName().equals("Self"))
            {
                readArmHand(trialSetting, ((mns2.main.Main)Main.self).selfArmHand, "self");
            }
            else if(trialSetting.getNodeName().equals("Screenshots"))
            {
                postScreenshot = readScreenshots(trialSetting);
            }
            else if(trialSetting.getNodeName().equals("Plots"))
            {
                readPlots(trialSetting, trialName);
            }
        }
        if(moveObject)
        {
            Main.self.curObj.rect_moveto(finalObjectPos);
        }
        if(moveObstacle && Main.self.obsObj!=null)
        {
            Main.self.obsObj.rect_moveto(finalObstaclePos);
        }
        Main.setTrace(true);
        Main.self.object_scale_fix=1;
        Mars.clearStars(1);
        Mars.clearStars(2);
        ((mns2.main.Main)Main.self).selfArmHand.net.recordNetwork=recordNet;
        AuditoryProcessor.network.recordNetwork=recordNet;
        Main.recordCanvas=recordSim;
        if(recordSim)
        {
            Main.recordFrame=0;
            Main.recordImageBase=baseDir+"/";
            Main.recordPrefix=trialName;
        }
        ((Main) Main.self).selfArmHand.net = Network.configureNet(((Main) Main.self).netTypeCombo.getSelectedItem().toString(), ((Main) Main.self).weightTextField.getText());
        Main.self.toggleReach("recognize");
        //((Main) Main.self).recognize(true);

        while(Main.self.otherArmHand.reachActive())
        {
            try
            {
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {}
        }
        if(imitationType.equals("none"))
        {
            Main.recordCanvas=false;
        }
        else
        {
            ((mns2.main.Main)Main.self).imitate(imitationType);
            while(Main.self.otherArmHand.reachActive())
            {

                try
                {
                    Thread.sleep(100);
                }
                catch(InterruptedException e)
                {}
            }
            Main.recordCanvas=false;
        }
        ((mns2.main.Main)Main.self).selfArmHand.net.setVisible(false);
        if(NetworkInterface.ndisp!=null)
            NetworkInterface.ndisp.setVisible(false);
        if(postScreenshot && Main.render)
        {
            final DataOutputStream d= Elib.openfileWRITE(baseDir+trialName+"_post.gif");
            final GifEncoder g=new GifEncoder(Main.cv.lastFrame(),d);
            g.encode();
        }
        for(int i=0; i<numberMDSDefinitions; i++)
        {
            for(int j=0; j<mdsTrialNames[i].length; j++)
            {
                if(mdsTrialNames[i][j].equals(trialName))
                {
                    mdsTrialHistoryLengths[i][j] = ((mns2.main.Main)Main.self).selfArmHand.net.t+1;
                    mdsTrialHistories[i][j]=((mns2.main.Main)Main.self).selfArmHand.net.hiddenLayerHistory;
                }
            }
        }
        Main.recordImageBase="panel/";
        Main.recordPrefix="";
    }

    private static void readGrasp(Node trialSetting)
    {
        NodeList graspChildren = trialSetting.getChildNodes();
        for(int i=0; i<graspChildren.getLength(); i++)
        {
            Node graspChild = graspChildren.item(i);
            if(graspChild.getNodeName().equals("Sound"))
            {
                readGraspSound(graspChild);
            }
            else if(graspChild.getNodeName().equals("Hidden"))
            {
                if(graspChild.getChildNodes().getLength()>0)
                    ((mns2.motor.ArmHand)Main.self.otherArmHand).hiddenGrasp = Boolean.parseBoolean(graspChild.getChildNodes().item(0).getNodeValue());
            }
            else if(graspChild.getNodeName().equals("Speed"))
            {
                if(graspChild.getChildNodes().getLength()>0)
                    ((mns2.main.Main)Main.self).speedCombo.setSelectedItem(graspChild.getChildNodes().item(0).getNodeValue());
            }
            else if(graspChild.getNodeName().equals("ImitationType"))
            {
                imitationType=graspChild.getChildNodes().item(0).getNodeValue();
            }
        }
    }

    private static void readGraspSound(Node soundNode)
    {
        NodeList soundChildren = soundNode.getChildNodes();
        for(int i=0; i<soundChildren.getLength(); i++)
        {
            Node soundChild = soundChildren.item(i);
            if(soundChild.getNodeName().equals("Audible"))
            {
                if(soundChild.getChildNodes().getLength()>0)
                    Main.self.otherArmHand.audible = Boolean.parseBoolean(soundChild.getChildNodes().item(0).getNodeValue());
            }
            else if(soundChild.getNodeName().equals("Congruent"))
            {
                if(soundChild.getChildNodes().getLength()>0)
                    Main.self.otherArmHand.congruentSound = Boolean.parseBoolean(soundChild.getChildNodes().item(0).getNodeValue());
            }
        }
    }
    private static void readView(Node graspChild)
    {
        Mars.eye.reset();
        NodeList viewChildren = graspChild.getChildNodes();

        for(int i=0; i<viewChildren.getLength(); i++)
        {
            Node viewChild = viewChildren.item(i);
            if(viewChild.getNodeName().equals("f"))
            {
                readCartesianPosition(viewChild, Mars.eye.Fpos);

            }
            if(viewChild.getNodeName().equals("x"))
            {
                readCartesianPosition(viewChild, Mars.eye.X);
            }
            if(viewChild.getNodeName().equals("y"))
            {
                readCartesianPosition(viewChild, Mars.eye.Y);
            }
            if(viewChild.getNodeName().equals("z"))
            {
                readCartesianPosition(viewChild, Mars.eye.Z);
            }
            if(viewChild.getNodeName().equals("zoom"))
            {
                double zoom = Double.parseDouble(viewChild.getChildNodes().item(0).getNodeValue());
                Mars.eye.setMag(zoom);
            }
        }
        Main.refreshDisplay();
    }

    /*private static void readObstacle(Node graspChild)
    {
        if(graspChild.getChildNodes().getLength()>0)
        {
            Main.self.setObsObject(graspChild.getChildNodes().item(0).getNodeValue());
            Main.self.refreshDisplay();
        }
    }*/

    private static boolean readObject(Node objectNode, String objectType)
    {
        boolean visible=false;
        NodeList objectChildren = objectNode.getChildNodes();
        for(int i=0; i<objectChildren.getLength(); i++)
        {
            Node objectChild = objectChildren.item(i);
            if(objectChild.getNodeName().equals("Name"))
            {
                readObjectIdentity(objectChild, objectType);
            }
            else if(objectChild.getNodeName().equals("Position"))
            {
                readObjectPosition(objectChild, objectType);
            }
            else if(objectChild.getNodeName().equals("Tilt"))
            {
                readObjectTilt(objectChild, objectType);
            }
            else if(objectChild.getNodeName().equals("Visible"))
            {
                visible=readObjectVisibility(objectChild, objectType, objectType.equals("target"));
            }
        }
        return visible;
    }

    /**
     * Reads the object identity from the simulation config file and sets the current target or obstacle object in the
     * simulation environment accordingly.
     * @param objectChild - XML node containing the object identity information
     * @param objectType - target or obstacle object
     */
    private static void readObjectIdentity(Node objectChild, String objectType)
    {
        if(objectChild.getChildNodes().getLength()>0)
        {
            if(objectType.equals("target"))
                Main.self.setObject(objectChild.getChildNodes().item(0).getNodeValue());
            else if(objectType.equals("obstacle"))
                Main.self.setObsObject(objectChild.getChildNodes().item(0).getNodeValue());
            Main.refreshDisplay();
        }
    }

    /**
     * Reads the object's initial position from the simulation config file and moves the object to this position.
     * @param objectChild - XML node containing the object's initial position
     * @param objectType - target or obstacle object
     */
    private static void readObjectPosition(Node objectChild, String objectType)
    {
        if(objectType.equals("target"))
            Main.self.curObj.rect_moveto(readCartesianPosition(objectChild, new Point3d()));
        else
            Main.self.obsObj.rect_moveto(readCartesianPosition(objectChild, new Point3d()));
    }

    private static boolean readObjectVisibility(Node objectChild, String objectType, boolean defaultValue)
    {
        Boolean visible=new Boolean(defaultValue);
        if(objectChild.getChildNodes().getLength()>0)
            visible = Boolean.parseBoolean(objectChild.getChildNodes().item(0).getNodeValue());
        if(objectType.equals("target"))
            Main.self.setObjectVisibility(visible.booleanValue());
        else if(objectType.equals("obstacle"))
            Main.self.setObstacleVisibility(visible.booleanValue());
        return visible.booleanValue();
    }

    private static void readObjectTilt(Node objectChild, String objectType)
    {
        if(objectChild.getChildNodes().getLength()>0)
        {
            double tilt = Double.parseDouble(objectChild.getChildNodes().item(0).getNodeValue());
            if(objectType.equals("target"))
                Main.self.curObj.setTilt(tilt);
            else if(objectType.equals("obstacle"))
                Main.self.obsObj.setTilt(tilt);
        }
    }
    /**
     * Reads a 3d position from the simulation config file.
     * @param trialSetting - XML node containing the position
     * @param pos - 3d coordinates
     */
    private static Point3d readCartesianPosition(Node trialSetting, Point3d pos)
    {
        NodeList position = trialSetting.getChildNodes();
        for(int l=0; l<position.getLength(); l++)
        {
            Node position_elem = position.item(l);
            if(position_elem.getNodeName().equals("x"))
            {
                if(position_elem.getChildNodes().getLength()>0)
                    pos.x = Double.parseDouble(position_elem.getChildNodes().item(0).getNodeValue());
            }
            else if(position_elem.getNodeName().equals("y"))
            {
                if(position_elem.getChildNodes().getLength()>0)
                    pos.y = Double.parseDouble(position_elem.getChildNodes().item(0).getNodeValue());
            }
            else if(position_elem.getNodeName().equals("z"))
            {
                if(position_elem.getChildNodes().getLength()>0)
                    pos.z = Double.parseDouble(position_elem.getChildNodes().item(0).getNodeValue());
            }
        }
        return pos;
    }

    private static void resetJointAngles(Node jointAngles, ArmHand armHand, String avatar)
    {
        if(avatar.equals("self"))
            armHand.makeUpright();
        else if(avatar.equals("other"))
            armHand.resetJoints();
        readJointAngles(jointAngles, armHand, avatar);
    }

    private static void readJointAngles(Node jointAngles, ArmHand armHand, String avatar)
    {
        if(avatar.equals("self"))
            selfJointAngles=jointAngles;
        else if(avatar.equals("other"))
            otherJointAngles=jointAngles;
        NodeList jointAnglesChildren = jointAngles.getChildNodes();
        for(int l=0; l<jointAnglesChildren.getLength(); l++)
        {
            Node joint = jointAnglesChildren.item(l);
            if(joint.getNodeName().equals("Joint"))
            {
                readJointAngle(joint, armHand);
            }
        }
    }

    /**
     * Reads a joint angle from the simulation config file and updates the arm/hand model.
     * @param joint - XML node containing the joint angle information.
     */
    private static void readJointAngle(Node joint, ArmHand armHand)
    {
        double angle = 0.0;
        String name="";
        NodeList jointNodes = joint.getChildNodes();
        for(int m=0; m<jointNodes.getLength(); m++)
        {
            Node jointNode = jointNodes.item(m);
            if(jointNode.getNodeName().equals("name"))
            {
                if(jointNode.getChildNodes().getLength()>0)
                    name = jointNode.getChildNodes().item(0).getNodeValue();
            }
            else if(jointNode.getNodeName().equals("angle"))
            {
                if(jointNode.getChildNodes().getLength()>0)
                    angle = Double.parseDouble(jointNode.getChildNodes().item(0).getNodeValue());
            }
        }
        double newbeta = angle*(Math.PI/180.0);
        for (int m=0;m< armHand.root.segc;m++)
        {
            if (armHand.root.seg[m].label.equals(name))
            {
                ArmHand.constrainedRotate(armHand.root.seg[m],newbeta-armHand.root.seg[m].beta);
                break;
            }
        }
        Main.refreshDisplay();
    }

    private static void readArmHandVisibility(Node armHandChild, mns2.motor.ArmHand armHand, boolean defaultValue)
    {
        Boolean visible=new Boolean(defaultValue);
        if(armHandChild.getChildNodes().getLength()>0)
            visible = Boolean.parseBoolean(armHandChild.getChildNodes().item(0).getNodeValue());
        Main.self.setArmVisibility(armHand, visible);
    }

    /**
     * Reads a set of plots from the simulation config file.
     * @param trialSetting - XML node containing the plot definitions
     * @param trialName - Name of the grasp trial
     */
    private static void readPlots(Node trialSetting, String trialName)
    {
        NodeList plotsNodes = trialSetting.getChildNodes();
        int plotMainIdx=0;
        int plotAudioIdx=0;
        int numMain=0;
        int numAud=0;
        for(int l=0; l<plotsNodes.getLength(); l++)
        {
            Node plotNode = plotsNodes.item(l);
            if(plotNode.getNodeName().equals("Plot"))
            {
                NodeList plotNodes=plotNode.getChildNodes();
                for(int m=0; m<plotNodes.getLength(); m++)
                {
                    Node plotChildNode=plotNodes.item(m);
                    if(plotChildNode.getNodeName().equals("network"))
                    {
                        if(plotChildNode.getChildNodes().getLength()>0)
                        {
                            String netType=plotChildNode.getChildNodes().item(0).getNodeValue();
                            if(netType.equals("main"))
                                numMain++;
                            else if(netType.equals("audio"))
                                numAud++;
                        }
                    }
                }
            }
        }
        ((mns2.main.Main)Main.self).selfArmHand.net.plots = new String[numMain];
        ((mns2.main.Main)Main.self).selfArmHand.net.plot_extra_command = new String[numMain];
        ((mns2.main.Main)Main.self).selfArmHand.net.plot_labels = new String[numMain][];
        ((mns2.main.Main)Main.self).selfArmHand.net.plot_dimensions = new int[numMain];
        ((mns2.main.Main)Main.self).selfArmHand.net.plot_output_files = new String[numMain];

        AuditoryProcessor.reset();
        AuditoryProcessor.network.plots = new String[numAud];
        AuditoryProcessor.network.plot_extra_command = new String[numAud];
        AuditoryProcessor.network.plot_labels = new String[numAud][];
        AuditoryProcessor.network.plot_dimensions = new int[numAud];
        AuditoryProcessor.network.plot_output_files = new String[numAud];

        for(int l=0; l<plotsNodes.getLength(); l++)
        {
            Node plotNode = plotsNodes.item(l);
            if(plotNode.getNodeName().equals("Plot"))
            {
                String networkType=readPlot(plotNode, plotMainIdx, plotAudioIdx, trialName);
                if(networkType.equals("main"))
                    plotMainIdx++;
                else if(networkType.equals("audio"))
                    plotAudioIdx++;
            }
        }
    }

    /**
     * Reads a plot definition from the simulation config file.
     * @param plotNode - XML node containing the plot information
     * @param plotMainIdx - Index of the main network plots
     * @param plotAudioIdx - Index of the audio network plots
     * @param trialName - Name of the grasp trial
     * @return network type
     */
    private static String readPlot(Node plotNode, int plotMainIdx, int plotAudioIdx, String trialName)
    {
        NodeList plotNodes = plotNode.getChildNodes();
        String networkType="";
        Network network=null;
        int plotIdx=-1;
        boolean setLabels=false;

        for(int m=0; m<plotNodes.getLength(); m++)
        {
            Node plotSetting = plotNodes.item(m);
            if(plotSetting.getNodeName().equals("network"))
            {
                if(plotSetting.getChildNodes().getLength()>0)
                {
                    networkType=plotSetting.getChildNodes().item(0).getNodeValue();
                    if(networkType.equals("main"))
                    {
                        network=((mns2.main.Main)Main.self).selfArmHand.net;
                        plotIdx=plotMainIdx;
                    }
                    else if(networkType.equals("audio"))
                    {
                        network=AuditoryProcessor.network;
                        plotIdx=plotAudioIdx;
                    }
                }
            }
            if(plotSetting.getNodeName().equals("name"))
            {
                if(plotSetting.getChildNodes().getLength()>0)
                {
                    if(network.plots!=null)
                    {
                        network.plots[plotIdx] = plotSetting.getChildNodes().item(0).getNodeValue();
                        network.plot_output_files[plotIdx] = baseDir + trialName + '_' + networkType + '_' +
                                network.plots[plotIdx];
                    }
                }
            }
            else if(plotSetting.getNodeName().equals("dimensions"))
            {
                if(plotSetting.getChildNodes().getLength()>0)
                {
                    if(network.plot_dimensions!=null)
                        network.plot_dimensions[plotIdx] = Integer.parseInt(plotSetting.getChildNodes().item(0).getNodeValue());
                }
            }
            else if(plotSetting.getNodeName().equals("extracommand"))
            {
                if(plotSetting.getChildNodes().getLength()>0)
                {
                    if(network.plot_extra_command!=null)
                        network.plot_extra_command[plotIdx] = plotSetting.getChildNodes().item(0).getNodeValue();
                }
            }
            else if(plotSetting.getNodeName().equals("labels"))
            {
                int labelIdx = 0;
                int numLabels=0;
                NodeList labelNodes = plotSetting.getChildNodes();
                for(int n=0; n<labelNodes.getLength(); n++)
                {
                    Node labelNode = labelNodes.item(n);
                    if(labelNode.getNodeName().equals("label"))
                    {
                        numLabels++;
                    }
                }
                if(numLabels>0)
                    setLabels=true;
                if(network!=null && network.plot_labels!=null)
                {
                    network.plot_labels[plotIdx] = new String[numLabels];
                    for(int n=0; n<labelNodes.getLength(); n++)
                    {
                        Node labelNode = labelNodes.item(n);
                        if(labelNode.getNodeName().equals("label"))
                        {
                            if(labelNode.getChildNodes().getLength()>0)
                                network.plot_labels[plotIdx][labelIdx++] = labelNode.getChildNodes().item(0).getNodeValue();
                        }
                    }
                }
            }
        }
        return networkType;
    }

    /**
     * Reads screenshot definitions from the simulation config file (currently only post-grasp screenshot).
     * @param trialSetting - XML node containing the screenshot definitions
     * @return - Whether or not to take a screenshot of the simulation environment after grasp completion
     */
    private static boolean readScreenshots(Node trialSetting)
    {
        boolean postScreenshot=false;
        NodeList screenshotsNodes = trialSetting.getChildNodes();
        for(int l=0; l<screenshotsNodes.getLength(); l++)
        {
            Node screenshotNode = screenshotsNodes.item(l);
            if(screenshotNode.getNodeName().equals("Screenshot"))
            {
                if(screenshotNode.getChildNodes().getLength()>0)
                {
                    Node valueNode = screenshotNode.getChildNodes().item(0);
                    if(valueNode.getNodeValue().equals("post"))
                        postScreenshot=true;
                }
            }
        }
        return postScreenshot;
    }
}