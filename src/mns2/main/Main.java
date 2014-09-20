package mns2.main;

import sim.graphics.*;
import sim.util.Resource;
import sim.util.Elib;
import sim.util.Log;
import sim.motor.Graspable;
import sim.motor.ArmHand;
import sim.motor.Trajectory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;

import mns2.comp.VisualProcessor;
import mns2.comp.NetworkInterface;
import mns2.comp.Network;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Oct 17, 2005
 * Time: 10:05:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main extends sim.main.Main
{
    public static boolean negativeExample; // for NetworkInterface

    public mns2.motor.ArmHand selfArmHand;

    public static int recbarReq;  // no request
    public static double precBar, sideBar, powBar;

    protected JTextField commandTextField;
    protected JPanel mainPanel, canvasPanel, sliderPanel, toolbarPanel, buttonPanel;
    protected JSlider scale, xang, yang, zang, objscale;
    protected JTextField infoLabel;
    protected JButton quitButton;
    protected JButton resetEyeButton;
    protected JButton xEyeButton;
    protected JButton yEyeButton;
    protected JButton zEyeButton;
    protected JButton tiltTargetButton;
    protected JCheckBox obstacleVisibleCheckbox;
    protected JComboBox obstacleTypeCombo;
    protected JCheckBox targetVisibleCheckbox;
    protected JComboBox targetTypeCombo;
    protected JButton recordCanvasButton;
    protected JButton breakButton;
    protected JCheckBox otherVisibleCheckbox;
    protected JButton planReachButton;
    protected JButton clearTrajectoryButton;
    protected JCheckBox audibleCheckbox;
    protected JButton reachButton;
    protected JButton resetOtherArmButton;
    protected JButton otherJointControlButton;
    public JComboBox speedCombo;
    protected JLabel speedLabel;
    protected JButton executeButton;
    protected JLabel targetTypeLabel;
    protected JLabel obstacleTypeLabel;
    protected JComboBox graspTypeCombo;
    protected JLabel graspTypeLabel;
    protected JCheckBox bellshapeCheckbox;
    private JButton recognizeActionButton;
    private JButton recognizeStaticButton;
    private JCheckBox selfVisibleCheckbox;
    private JButton resetSelfArmButton;
    private JButton selfJointControlButton;
    public JComboBox netTypeCombo;
    private JCheckBox splineCheckbox;
    private JButton directImitateButton;
    private JButton indirectImitateButton;
    private JButton naturalImitateButton;
    private JButton generateDataButton;
    public JTextField weightTextField;
    private JLabel weightLabel;
    private JLabel netTypeLabel;

    public Main(String argv[])
    {
        readConfig();
        final String armHandFile;
        int rad = 20;
        int sidec = 4;
        if (argv != null)
        {
            if (argv.length > 0)
                armHandFile = argv[0];
            else
                armHandFile = Resource.getString("defaultArmHandFile");
            if (argv.length >= 3)
            {
                sidec = Elib.toInt(argv[1]);
                rad = Elib.toInt(argv[2]);
            }
            if (argv.length > 3)
            {
                if (argv[3].toLowerCase().equals("false"))
                    render = false;
            }
        }
        else
            armHandFile = Resource.getString("defaultArmHandFile");

        rx = 20;
        ry = (Main.minMER + Main.maxMER) >> 1;
        rz = Main.maxRAD; //1125

        Mars.clearStars(1);  //create the lists
        Mars.clearStars(2);  //create the lists

        Main.self = this;

        prepareMain(armHandFile, sidec, rad);
        sc = (int) (0.5 + Mars.eye.Mag / 5.0);
        setupLayout();
        updateScrollValues();

        setupArms();
        setGrasp("NATURAL");
        setObject(targetTypeCombo.getSelectedItem().toString());
        setObsObject(obstacleTypeCombo.getSelectedItem().toString());

        Mars.project(); // firs project so that first repaint works OK.

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        enableEvents(AWTEvent.COMPONENT_EVENT_MASK);
        enableEvents(AWTEvent.FOCUS_EVENT_MASK);

        if (render)
        {
            refreshDisplay();
            Main.cv.addMouseListener(this);
            Main.cv.addMouseMotionListener(this);
        }
    }

    protected void setupLayout()
    {
        add(mainPanel);

        scale.setMinimum(1);
        scale.setMaximum(901);
        scale.setValue(sc);
        scale.addChangeListener(this);

        objscale.setMinimum(1);
        objscale.setMaximum(101);
        objscale.setValue(objsc);
        objscale.addChangeListener(this);

        xang.setMinimum(Main.minPAR);
        xang.setMaximum(Main.maxPAR + 1);
        xang.setValue(rx);
        xang.addChangeListener(this);

        yang.setMinimum(Main.minMER);
        yang.setMaximum(Main.maxMER + 1);
        yang.setValue(ry);
        yang.addChangeListener(this);

        zang.setMinimum(Main.minRAD);
        zang.setMaximum(Main.maxRAD + 1);
        zang.setValue(rz);
        zang.addChangeListener(this);

        quitButton.addActionListener(this);
        resetEyeButton.addActionListener(this);
        xEyeButton.addActionListener(this);
        yEyeButton.addActionListener(this);
        zEyeButton.addActionListener(this);

        obstacleVisibleCheckbox.addActionListener(this);

        obstacleTypeCombo.addItem("PENT");
        obstacleTypeCombo.addItem("BOX");
        obstacleTypeCombo.addItem("SHEET");
        obstacleTypeCombo.addItem("BAR");
        obstacleTypeCombo.addItem("PLATE");
        obstacleTypeCombo.addItem("COIN");
        obstacleTypeCombo.addItem("MUG");
        obstacleTypeCombo.addItem("SCREEN");
        obstacleTypeCombo.setSelectedItem("BAR");
        obstacleTypeCombo.addActionListener(this);

        targetVisibleCheckbox.addActionListener(this);

        targetTypeCombo.addItem("PENT");
        targetTypeCombo.addItem("BOX");
        targetTypeCombo.addItem("SHEET");
        targetTypeCombo.addItem("BAR");
        targetTypeCombo.addItem("PLATE");
        targetTypeCombo.addItem("COIN");
        targetTypeCombo.addItem("MUG");
        targetTypeCombo.addItem("SCREEN");
        targetTypeCombo.setSelectedItem("BOX");
        targetTypeCombo.addActionListener(this);

        tiltTargetButton.addActionListener(this);

        for (int i = 0; i < Main.grasps.length; i++)
            graspTypeCombo.addItem(Main.grasps[i]);
        graspTypeCombo.setSelectedItem("NATURAL");
        graspTypeCombo.addActionListener(this);

        bellshapeCheckbox.addActionListener(this);
        otherVisibleCheckbox.addActionListener(this);
        recordCanvasButton.addActionListener(this);
        audibleCheckbox.addActionListener(this);
        breakButton.addActionListener(this);
        planReachButton.addActionListener(this);
        clearTrajectoryButton.addActionListener(this);
        reachButton.addActionListener(this);
        resetOtherArmButton.addActionListener(this);
        otherJointControlButton.addActionListener(this);
        for (int i = 1; i < 11; i++)
            speedCombo.addItem((new Integer(i)).toString());
        speedCombo.setSelectedItem("3");
        speedCombo.addActionListener(this);
        executeButton.addActionListener(this);
        recognizeActionButton.addActionListener(this);
        //recognizeStaticButton.addActionListener(this);
        //selfVisibleCheckbox.addActionListener(this);
        //resetSelfArmButton.addActionListener(this);
        //selfJointControlButton.addActionListener(this);
        //directImitateButton.addActionListener(this);
        //indirectImitateButton.addActionListener(this);
        //naturalImitateButton.addActionListener(this);
        splineCheckbox.addActionListener(this);
        generateDataButton.addActionListener(this);
        netTypeCombo.addItem("Hebbian");
        netTypeCombo.addItem("BP");
        netTypeCombo.addItem("BPTT");
        netTypeCombo.addItem("BPTTwithHebbian");
        netTypeCombo.setSelectedItem("BPTTwithHebbian");
        netTypeCombo.addActionListener(this);

        setTitle("3D Hand - Erhan Oztop Jan2000");

        Main.pal = new Palette(256);
        Main.pal.spread(20, 20 + 31, 175, 175, 175, 255, 255, 255);
        Main.pal.spread(20 + 32, 20 + 32 + 31, 75, 75, 75, 150, 150, 150);
        if (render)
        {
            Main.cv = new MainPanel();
            Main.cv.setBackground(Color.black);
            canvasPanel.add(Main.cv);
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setBounds(0, 0, (int) d.getWidth() - 50, (int) d.getHeight() - 100);
            setVisible(true);
        }
    }

    static public void updateRecBars(double prec, double side, double pow)
    {
        precBar = prec;
        sideBar = side;
        powBar = pow;
        recbarReq = 1;  //request update
    }

    protected void setupArms()
    {
        selfArmHand.imitator = true;
        selfArmHand.armHandNumber = 2;
        selfArmHand.noshow = true;
        //selfArmHand.showArmHandFrame();
        //selfArmHand.toggleArmHandFrame(); //hide it for now

        //otherArmHand.showArmHandFrame();
        //otherArmHand.toggleArmHandFrame(); //hide it for now
        otherArmHand.makeUpright();
    }

    public String setInfo(String s)
    {
        if (DLEV >= 0)
        {
            infoLabel.setText(s);
            Dimension d = canvasPanel.getSize();
            cv.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            cv.setLocation(0, 0);
            cv.refreshDisplay();
        }
        return s;
    }

    public void setInfoReady()
    {
        if (DLEV >= 0)
        {
            infoLabel.setText("Ready.");
            Dimension d = canvasPanel.getSize();
            cv.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            cv.setLocation(0, 0);
            cv.refreshDisplay();
        }
    }

    protected void setObsObject(String s, int i, boolean setObject)
    {
        if (i >= objNames.length)
            return;
        if (obsObj != null)
        {
            for (int j = 0; j < objDefinitions.length; j++)
            {
                if (objDefinitions[j].equals(obsObj.myname) && Mars.getObject(objNames[j]) != null &&
                    ((Graspable) Mars.getObject(objNames[j])).obstacle)
                {
                    Mars.removeObject(objNames[j]);
                    break;
                }
            }
        }
        obsObj = new Graspable(otherArmHand, objDefinitions[i], objSidec[i], objRad[i], objGrasps[i]);
        obsObj.obstacle = true;
        obsObj.root.scale(objScales[i]);
        Mars.addObject(objNames[i], obsObj);
        obsObj.visible = obstacleVisibleCheckbox.isSelected();
        obsObj.noshow = !obstacleVisibleCheckbox.isSelected();
        rx = -3121;
        ry = -1415; //whatever
        if (!firsttimer)
            setInfo("Ready.  - obstacle object set to " + s /*objlist[i].root.label*/);
        else
            firsttimer = false;
        updateScrollValues();
    }

    protected void setObject(String s, int i)//, boolean setObsObject)
    {
        if (i >= objNames.length)
            return;
        if (curObj != null)
        {
            for (int j = 0; j < objDefinitions.length; j++)
            {
                if (objDefinitions[j].equals(curObj.myname) && !((Graspable) Mars.getObject(objNames[j])).obstacle)
                {
                    Mars.removeObject(objNames[j]);
                    break;
                }
            }
        }
        curObj = new Graspable(otherArmHand, objDefinitions[i], objSidec[i], objRad[i], objGrasps[i]);
        Mars.addObject(objNames[i], curObj);
        curObj.root.scale(objScales[i]);
        curObj.visible = targetVisibleCheckbox.isSelected();
        curObj.noshow = !targetVisibleCheckbox.isSelected();
        rx = -3121;
        ry = -1415; //whatever
        if (!firsttimer)
            setInfo("Ready.  - object set to " + s /*objlist[i].root.label*/);
        else
            firsttimer = false;
        updateScrollValues();
    }

    public void prepareMain(String armHandFile, int sidec, int rad)
    {
        otherArmHand = new mns2.motor.ArmHand(armHandFile, sidec, rad);
        selfArmHand = new mns2.motor.ArmHand(armHandFile, sidec, rad);
        final Eye eye;
        if (otherArmHand.root.suggested_scale == 0)
            eye = new Eye(10, 20); // create an eye
        else
            eye = new Eye(otherArmHand.root.suggested_F, otherArmHand.root.suggested_scale);

        eye.lock(0, 0, 0);
        eye.YrotateViewPlane(Math.PI / 25);
        eye.XrotateViewPlane(Math.PI / 5);
        objNames = new String[]{"SHEET", "BAR", "BOX", "PENT", "PLATE", "COIN", "MUG", "SCREEN"};
        objDefinitions = new String[]{"objects/sheet.seg", "objects/ibar.seg", "objects/box.seg", "objects/pent.seg",
                                      "objects/plate.seg", "objects/coin.seg", "objects/ring.seg", "objects/screen.seg"};
        objSidec = new int[]{0, 6, 0, 0, 0, 7, 5, 0};
        objRad = new int[]{5, 100, 5, 5, 5, 100, 50, 5};
        objGrasps = new String[]{"SIDE", "PRECISION", "PRECISION", "POWER", "PRECISION", "SIDE", "POWER", "PRECISION"};
        objScales = new double[]{1.0, 1.0, 1.0, 0.4, 1.0, 1.0, 0.5, 1, 0};

        Mars.addObject("HAND", otherArmHand);
        Mars.addObject("SELFHAND", selfArmHand);

        Mars.setEye(eye);
        Mars.setCube(1900 * 1.3);
    }

    public static void main(String[] argv)
    {
        new Main(argv);     // when applet this is done by netscape
    }

    public void updateScrollValues()
    {
        if (objscale != null && xang != null && yang != null && zang != null && scale != null)
        {
            final int newosc = objscale.getValue();
            final int newrx = xang.getValue();
            final int newry = yang.getValue();
            final int newrz = zang.getValue();
            final int newsc = scale.getValue();
            if (newsc != sc)
            {
                Mars.eye.setMag(5 * newsc);
                sc = newsc;
            }

            if (newrx != rx || newry != ry || newrz != rz)
            {
                setTargetPosition(-newrx * Math.PI / 180, -newry * Math.PI / 180, newrz);
                rx = newrx;
                ry = newry;
                rz = newrz;
            }
            if (newosc != objsc)
            {
                setTargetScale(objsc / 10.0 + 0.1, newosc / 10.0 + 0.1);
                objsc = newosc;
            }
            refreshDisplay();
        }
    }

    public boolean executeCommand(String com)
    {
        if (!super.executeCommand(com) || com.equals("help"))
        {
            final String[] pars = new String[40];
            for (int i = 0; i < pars.length; i++)
                pars[i] = null;

            final StringTokenizer st = new StringTokenizer(com, " ");
            int parc = 0;
            while (st.hasMoreTokens())
            {
                pars[parc++] = st.nextToken();
            }
            final String command = pars[0];
            if (command == null)
            {
                Main.self.setInfo("Nothing to execute!");
                return false;
            }
            if (command.equals("get"))
            {
                if (parc < 2)
                {
                    Main.self.setInfo("Need parameter");
                    return false;
                }
                if (pars[1].equals("remappingPoint"))
                {
                    Main.self.setInfo("remappingPoint is:" + selfArmHand.net.remappingPoint);
                    return true;
                }
                Main.self.setInfo("Unknown parameter name!");
                return false;
            }

            if (command.equals("set"))
            {
                if (parc < 3)
                {
                    Main.self.setInfo("Need parameter and a value");
                    return false;
                }
                if (pars[1].equals("remappingPoint"))
                {
                    selfArmHand.net.remappingPoint = pars[2];
                    Main.self.setInfo("HV.rhand.remappingPoint set to:" + selfArmHand.net.remappingPoint);
                    return true;
                }
                if (pars[1].equals("viaPointNoiseLevel"))
                {
                    VisualProcessor.viaPointNoiseLevel = Elib.toDouble(pars[2]);
                    Main.self.setInfo("VisualProcessor.viaPointNoiseLevel set to:" + VisualProcessor.viaPointNoiseLevel);
                    return true;
                }
                if (pars[1].equals("viaPointSpeedThresh"))
                {
                    VisualProcessor.viaPointSpeedThresh = Elib.toDouble(pars[2]);
                    Main.self.setInfo("VisualProcessor.viaPointSpeedThresh set to:" + VisualProcessor.viaPointSpeedThresh);
                    return true;
                }
                Main.self.setInfo("Unknown parameter name!");
                return false;
            }
            if (command.equals("load"))
            {
                if (parc < 2)
                {
                    Main.self.setInfo("Need simulation experiment to load");
                    return false;
                }
                SimulationExperimentManager.run(pars[1]);
                return true;
            }
            if (command.equals("hiddenGrasp+"))
            {
                ((mns2.motor.ArmHand) otherArmHand).hiddenGrasp = true;
                Main.self.setInfo("Now -> hiddenGrasp:" + ((mns2.motor.ArmHand) otherArmHand).hiddenGrasp);
                return true;
            }
            if (command.equals("hiddenGrasp-"))
            {
                ((mns2.motor.ArmHand) otherArmHand).hiddenGrasp = false;
                Main.self.setInfo("Now -> hiddenGrasp:" + ((mns2.motor.ArmHand) otherArmHand).hiddenGrasp);
                return true;
            }
            if (command.equals("dynamicRemapping+"))
            {
                selfArmHand.net.dynRemapping = true;
                Main.self.setInfo("Now -> dynamicRemapping:" + selfArmHand.net.dynRemapping);
                return true;
            }
            if (command.equals("dynamicRemapping-"))
            {
                selfArmHand.net.dynRemapping = false;
                Main.self.setInfo("Now -> dynamicRemapping:" + selfArmHand.net.dynRemapping);
                return true;
            }
            if (command.equals("help"))
            {
                System.out.println("congruentSound+/-: turns on/off audio-visual congruency");
                System.out.println("dynamicRemapping+/-: turns on/off dynamic remapping");
                System.out.println("recurrent+/-: turns on/off network recurrent connections");
                return true;
            }
            Main.self.setInfo("No such command!");
            return false;
        }
        else
            return true;
    }

    protected void processWindowEvent(WindowEvent e)
    {
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
            exitMain();
    }

    protected void processComponentEvent(ComponentEvent e)
    {
        if (e.getID() == ComponentEvent.COMPONENT_RESIZED)
        {
            final Dimension d = canvasPanel.getSize();
            Main.cv.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            Main.cv.setLocation(0, 0);
            cv.refreshDisplay();
        }
    }

    /**
     * Modifies visibility of the arm and hand.
     *
     * @param arm     - the arm/hand to modify
     * @param visible - the new value of the arm/hand's visibility
     */
    public void setArmVisibility(ArmHand arm, boolean visible)
    {
        arm.visible = visible;
        arm.noshow = !visible;
        otherVisibleCheckbox.setSelected(visible);

        refreshDisplay();
    }

    /**
     * Modifies the current target object's visibility.
     *
     * @param visible - the new value of the target object's visibility
     */
    public void setObjectVisibility(boolean visible)
    {
        curObj.visible = visible;
        curObj.noshow = !visible;
        targetVisibleCheckbox.setSelected(visible);
        refreshDisplay();
    }

    /**
     * Modifies the current target object's visibility.
     *
     * @param visible - the new value of the target object's visibility
     */
    public void setObstacleVisibility(boolean visible)
    {
        obsObj.visible = visible;
        obsObj.noshow = !visible;
        obstacleVisibleCheckbox.setSelected(visible);
        refreshDisplay();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource().equals(graspTypeCombo))
        {
            setGrasp(graspTypeCombo.getSelectedItem().toString());
        }
        else if (e.getSource().equals(targetTypeCombo))
        {
            setObject(targetTypeCombo.getSelectedItem().toString());
        }
        else if (e.getSource().equals(obstacleTypeCombo))
        {
            setObsObject(obstacleTypeCombo.getSelectedItem().toString());
        }
        else if (e.getSource().equals(obstacleVisibleCheckbox))
        {
            setObstacleVisibility(obstacleVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(bellshapeCheckbox))
        {
            Main.bellshape = bellshapeCheckbox.isSelected();
        }
        else if (e.getSource().equals(otherVisibleCheckbox))
        {
            setArmVisibility(otherArmHand, otherVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(targetVisibleCheckbox))
        {
            setObjectVisibility(targetVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(audibleCheckbox))
        {
            otherArmHand.audible = audibleCheckbox.isSelected();
            selfArmHand.audible = audibleCheckbox.isSelected();
        }
        else if (e.getSource().equals(resetOtherArmButton))
        {
            resetOtherArm();
        }
        else if (e.getSource().equals(breakButton))
        {
            setInfo("Ready. -user interrupt");
            otherArmHand.kill_ifActive();
        }
        else if (e.getSource().equals(resetEyeButton))
        {
            Mars.eye.reset();
            Mars.project();
            if (render)
            {
                Main.cv.paint(Main.cv.getGraphics());
            }
        }
        else if (e.getSource().equals(xEyeButton))
        {
            if (!toggleEyeMove(0.1, 0, 0))
                System.out.println("EYE stopped.");
        }
        else if (e.getSource().equals(yEyeButton))
        {
            if (!toggleEyeMove(0, 0.1, 0))
                System.out.println("EYE stopped.");
        }
        else if (e.getSource().equals(zEyeButton))
        {
            if (!toggleEyeMove(0, 0, 0.1))
                System.out.println("EYE stopped.");
        }
        else if (e.getSource().equals(otherJointControlButton))
        {
            otherArmHand.toggleArmHandFrame();
        }
        else if (e.getSource().equals(bringToMouthButton))
        {
            toggleReach("eat");
        }
        else if (e.getSource().equals(reachButton))
        {
            toggleReach("execute");
        }
        else if (e.getSource().equals(executeButton))
        {
            executeCommand(commandTextField.getText());
        }
        else if (e.getSource().equals(recordCanvasButton))
        {
            if (!Main.recordCanvas)
            {
                Main.recordFrame = 0;
                Main.recordCanvas = true;
                setInfo("Canvas recording ON. (Each refresh will be recorded).");
            }
            else
            {
                Main.recordCanvas = false;
                setInfo("Canvas recording OFF. Last session recorded " + Main.recordFrame + " frames.");
            }
        }
        else if (e.getSource().equals(clearTrajectoryButton))
        {
            otherArmHand.clearTrajectory();
        }
        else if (e.getSource().equals(planReachButton))
        {
            setInfo("VISREACH: solving inverse kinematics...");
            toggleReach("visual");
        }
        else if (e.getSource().equals(tiltTargetButton))
        {
            tiltAngle = (tiltAngle + 15) % 360;
            curObj.setTilt(tiltAngle * Math.PI / 180);
            setInfo("Object tilt angle is set to:" + tiltAngle);
            refreshDisplay();
        }
        else if (e.getSource().equals(quitButton))
        {
            exitMain();
        }
        else if (e.getSource().equals(splineCheckbox))
        {
            NetworkInterface.useSplines = splineCheckbox.isSelected();
            if (splineCheckbox.isSelected())
            {
                if (netTypeCombo.getSelectedItem().equals("BP"))
                    weightTextField.setText("jjb_bp_spline.wgt");
                else if (netTypeCombo.getSelectedItem().equals("Hebbian"))
                    weightTextField.setText("jjb_hebbian_spline.wgt");
            }
            else
            {
                if (netTypeCombo.getSelectedItem().equals("BP"))
                    weightTextField.setText("jjb_bp.wgt");
                else if (netTypeCombo.getSelectedItem().equals("Hebbian"))
                    weightTextField.setText("jjb_hebbian.wgt");
            }
        }
        else if (e.getSource().equals(netTypeCombo))
        {
            if (netTypeCombo.getSelectedItem().equals("BPTTwithHebbian"))
                weightTextField.setText("jjb_bptt_hebbian.wgt");
            else if (netTypeCombo.getSelectedItem().equals("BPTT"))
                weightTextField.setText("jjb_bptt.wgt");
            else if (netTypeCombo.getSelectedItem().equals("BP"))
                weightTextField.setText("jjb_bp.wgt");
            else if (netTypeCombo.getSelectedItem().equals("Hebbian"))
                weightTextField.setText("jjb_hebbian.wgt");
        }
        else if (e.getSource().equals(selfVisibleCheckbox))
        {
            setArmVisibility(selfArmHand, selfVisibleCheckbox.isSelected());
        }
        else if (e.getSource().equals(resetSelfArmButton))
        {
            selfArmHand.grasped = false;
            selfArmHand.makeStanding();
            refreshDisplay();
        }
        else if (e.getSource().equals(selfJointControlButton))
        {
            selfArmHand.toggleArmHandFrame();
        }
        else if (e.getSource().equals(generateDataButton))
        {
            generateData(10);
            //generateAVData(15);
        }
        else if (e.getSource().equals(recognizeActionButton))
        {
            Main.self.setInfo("Reach started for recognition.");
            toggleTrace(1); // show thee profile
            object_scale_fix = 1;
            recognize(true);
        }
        else if (e.getSource().equals(recognizeStaticButton))
        {
            //((mns2.motor.ArmHand)otherArmHand).execute=false;
            recognize(false);
        }
        else if (e.getSource().equals(directImitateButton))
        {
            imitate("direct");
        }
        else if (e.getSource().equals(indirectImitateButton))
        {
            imitate("indirect");
        }
        else if (e.getSource().equals(naturalImitateButton))
        {
            imitate("natural");
        }

    }

    private void resetOtherArm()
    {
        otherArmHand.grasped = false;
        otherArmHand.makeStanding();
        refreshDisplay();
    }

    protected void configureNetwork()
    {
        selfArmHand.net = Network.configureNet(netTypeCombo.getSelectedItem().toString(), weightTextField.getText());
    }

    public void loadWeight()
    {
        String[] plotsTemp = new String[]{"input", "output"};
        int[] plot_dimsTemp = new int[]{2, 2};
        String[] plot_extracmdTemp = new String[]{"set xlabel \"Time\";set ylabel \"Activation\";", "set xlabel \"Time\";set ylabel \"Activation\";"};
        String[] plot_outputfilesTemp = new String[2];
        String[][] plot_labelsTemp = new String[2][];
        plot_labelsTemp[0] = NetworkInterface.params;

        if (selfArmHand.net != null)
        {
            plotsTemp = selfArmHand.net.plots.clone();
            plot_dimsTemp = selfArmHand.net.plot_dimensions.clone();
            plot_extracmdTemp = selfArmHand.net.plot_extra_command.clone();
            plot_outputfilesTemp = selfArmHand.net.plot_output_files.clone();
            plot_labelsTemp = selfArmHand.net.plot_labels.clone();
        }

        configureNetwork();
        selfArmHand.net.t = 0;
        selfArmHand.net.setSize(400, 300);
        selfArmHand.net.setVisible(true);
        selfArmHand.net.plots = plotsTemp;
        selfArmHand.net.plot_dimensions = plot_dimsTemp;
        selfArmHand.net.plot_extra_command = plot_extracmdTemp;
        selfArmHand.net.plot_output_files = plot_outputfilesTemp;
        selfArmHand.net.plot_labels = plot_labelsTemp;

        //System.out.println("Making the network from Weight file"+HV.weight.getText());
        selfArmHand.net.netFromWeight(weightTextField.getText());
        selfArmHand.net.clearHistory();
    }

    public void imitate(String type)
    {
        if (obsObj != null && obsObj.visible)
            selfArmHand.doImitate(type, curObj, obsObj, selfArmHand.net, netTypeCombo.getSelectedItem().toString(),
                                  weightTextField.getText());
        else
            selfArmHand.doImitate(type, curObj, null, selfArmHand.net, netTypeCombo.getSelectedItem().toString(),
                                  weightTextField.getText());
        try
        {
            while (selfArmHand.reachActive())
            {
                Thread.sleep(100);
            }
            Thread.sleep(1000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        traceon = false;
        Trajectory.showSplines(otherArmHand.jointpath, otherArmHand.jointpath.length);
        Trajectory.showSplines(selfArmHand.jointpath, selfArmHand.jointpath.length);
    }

    public void recognize(boolean dynamic)
    {
        if (selfArmHand.net != null)
        {
            selfArmHand.net.setVisible(false);
            selfArmHand.net.setBounds(100, 100, 600, 500);
            selfArmHand.net.t = 0;
        }
        refreshDisplay();
        loadWeight();
        if (selfArmHand.net == null)
        {
            Log.println("Doesn't have a network to recognize!");
            return;
        }
        if (dynamic)
            toggleReach("recognize");
        else
        {
            selfArmHand.net = Network.configureNet(netTypeCombo.getSelectedItem().toString(), weightTextField.getText());
            ((mns2.motor.ArmHand) otherArmHand).recognizeNet = selfArmHand.net;
            otherArmHand.finalizeReach("", curObj);
        }
    }

    public void generateData(double stepdeg)
    {
        final String[] sey = {"COIN", "BOX", "PENT"};
        final int sdlev = Main.DLEV;
        Main.DLEV = 0;

        final int N = (int) (0.5 + 180.0 / stepdeg);
        final int M = (int) (0.5 + 180.0 / stepdeg);
        final double step = stepdeg * Math.PI / 180;

        for (final String tar : sey)
        {
            final Point3d q = otherArmHand.root.joint_pos.duplicate();

            final double RAD = 1300;
            setObject(tar);
            setGrasp("NATURAL");
            double obj_scale;
            for (int k = 0; k <= M >> 1; k++)
            {
                for (int i = 0; i <= N >> 1; i++)
                {
                    if (k == 0 && i != 0)
                        continue;
                    if (k == M && i != 0)
                        continue;
                    if (tar.equals("PENT"))
                        obj_scale = Math.random() * 0.5 + 0.75; // x.75 - x1.25
                    else if (tar.equals("BOX"))
                        obj_scale = Math.random() * 1.5 + 0.5;  // x.5 - x2
                    else
                        obj_scale = Math.random() * 0.3 + 0.85;
                    curObj.root.scale(obj_scale);

                    final double par = step * k - Math.PI / 4;
                    final double mer = step * i - Math.PI / 4;
                    double x = q.x + RAD * Math.cos(par) * Math.sin(mer);
                    double y = q.y + RAD * Math.sin(par);
                    double z = q.z + RAD * Math.cos(par) * Math.cos(mer);

                    curObj.rect_moveto(x, y, z);
                    Log.println("\n" + (N * k + i) + '/' + M * N + " ) Reaching to:(" + x + ',' + y + ',' + z + ')');
                    resetOtherArm();
                    toggleTrace(1);
                    Mars.ignoreClear = true;

                    Mars.addStar(new Point3d(x, y, z), 1);
                    toggleTrace(1);
                    setInfo("VISREACH: solving inverse kinematics...");
                    toggleReach("visual");
                    waitFinish();
                    negativeExample = false;
                    toggleReach("record");
                    waitFinish();
                    if (otherArmHand.lasterr < 9)
                    {
                        if (otherArmHand.lasterr < 6) // den do negative too
                        {
                            final double ranx = x + (Math.random() > 0.5 ? -1 : 1) * (Math.random() * 200 + 250);
                            final double rany = y + (Math.random() > 0.5 ? -1 : 1) * (Math.random() * 200 + 250);
                            final double ranz = z + (Math.random() > 0.5 ? -1 : 1) * (Math.random() * 200 + 250);
                            curObj.moveto(otherArmHand.root.joint_pos, ranx, rany, ranz);
                            negativeExample = true;
                            toggleReach("record");
                            waitFinish();
                            negativeExample = false;
                        }
                        else
                            Log.println("Not doing negative example because last reach didn't succeed. Err was:" + otherArmHand.lasterr);
                    }
                    curObj.root.scale(1.0 / obj_scale); //back to normal size
                }
            }
            Mars.ignoreClear = false;
            toggleTrace(1);
        }
        Main.DLEV = sdlev;
        Mars.ignoreClear = false;
    }

    synchronized public void toggleReach(String s)
    {
        if (curObj == null)
            return;
        if (!otherArmHand.reachActive())
        {
            if (s.equals("visual"))
                curObj.computeAffordance(Main.grasps[Main.graspi], otherArmHand);

            if (s.equals("eat"))
            {
                if (obsObj != null)
                    otherArmHand.doEat(curObj, obsObj, s);
                else
                    otherArmHand.doEat(curObj, null, s);
            }
            else
            {
                ((mns2.motor.ArmHand) otherArmHand).doReach(curObj, obsObj, s, selfArmHand.net,
                                                            netTypeCombo.getSelectedItem().toString(), weightTextField.getText());
            }
        }
        else
        {
            otherArmHand.kill_ifActive();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(toolbarPanel, gbc);
        infoLabel = new JTextField();
        infoLabel.setEditable(false);
        infoLabel.setText("3D Hand - Erhan Oztop Jan2000");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        toolbarPanel.add(infoLabel, gbc);
        commandTextField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        toolbarPanel.add(commandTextField, gbc);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        toolbarPanel.add(buttonPanel, gbc);
        quitButton = new JButton();
        quitButton.setText("QUIT");
        quitButton.setToolTipText("Quit the simulator");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(quitButton, gbc);
        breakButton = new JButton();
        breakButton.setText("BREAK");
        breakButton.setToolTipText("Interrupt the current grasp");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(breakButton, gbc);
        obstacleTypeLabel = new JLabel();
        obstacleTypeLabel.setText("OBSTACLE");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(obstacleTypeLabel, gbc);
        otherVisibleCheckbox = new JCheckBox();
        otherVisibleCheckbox.setSelected(true);
        otherVisibleCheckbox.setText("ARM VISIBLE");
        otherVisibleCheckbox.setToolTipText("Visibility of the arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(otherVisibleCheckbox, gbc);
        planReachButton = new JButton();
        planReachButton.setText("PLAN");
        planReachButton.setToolTipText("Plan a reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(planReachButton, gbc);
        generateDataButton = new JButton();
        generateDataButton.setInheritsPopupMenu(false);
        generateDataButton.setText("GENERATE");
        generateDataButton.setToolTipText("Generate grasp examples to train the network with");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(generateDataButton, gbc);
        weightLabel = new JLabel();
        weightLabel.setText("WEIGHT FILE");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(weightLabel, gbc);
        obstacleVisibleCheckbox = new JCheckBox();
        obstacleVisibleCheckbox.setText("VISIBLE");
        obstacleVisibleCheckbox.setToolTipText("Visibility of the obstacle object");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(obstacleVisibleCheckbox, gbc);
        recognizeActionButton = new JButton();
        recognizeActionButton.setText("RECOGNIZE");
        recognizeActionButton.setToolTipText("Attempt recognition of the reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(recognizeActionButton, gbc);
        netTypeLabel = new JLabel();
        netTypeLabel.setText("NET TYPE");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(netTypeLabel, gbc);
        netTypeCombo = new JComboBox();
        netTypeCombo.setToolTipText("Type of network to use for recognition");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(netTypeCombo, gbc);
        resetEyeButton = new JButton();
        resetEyeButton.setText("resetEYE");
        resetEyeButton.setToolTipText("Reset the eye angle");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(resetEyeButton, gbc);
        targetTypeLabel = new JLabel();
        targetTypeLabel.setText("TARGET");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(targetTypeLabel, gbc);
        targetTypeCombo = new JComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(targetTypeCombo, gbc);
        xEyeButton = new JButton();
        xEyeButton.setText("xEYE");
        xEyeButton.setToolTipText("Rotate eye around x-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(xEyeButton, gbc);
        splineCheckbox = new JCheckBox();
        splineCheckbox.setText("SPLINES");
        splineCheckbox.setToolTipText("Whether or not to fit input to spline before applying to the network");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(splineCheckbox, gbc);
        yEyeButton = new JButton();
        yEyeButton.setText("yEYE");
        yEyeButton.setToolTipText("Rotate eye around y-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(yEyeButton, gbc);
        clearTrajectoryButton = new JButton();
        clearTrajectoryButton.setText("CLEAR");
        clearTrajectoryButton.setToolTipText("Clear the current reach trajectory");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        buttonPanel.add(clearTrajectoryButton, gbc);
        targetVisibleCheckbox = new JCheckBox();
        targetVisibleCheckbox.setSelected(true);
        targetVisibleCheckbox.setText("VISIBLE");
        targetVisibleCheckbox.setToolTipText("Visibility of the target object");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(targetVisibleCheckbox, gbc);
        tiltTargetButton = new JButton();
        tiltTargetButton.setText("TILT");
        tiltTargetButton.setToolTipText("Tilt the target object by 15 degrees about the z-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(tiltTargetButton, gbc);
        recordCanvasButton = new JButton();
        recordCanvasButton.setText("RECORD");
        recordCanvasButton.setToolTipText("Write the panel to an image file");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(recordCanvasButton, gbc);
        zEyeButton = new JButton();
        zEyeButton.setText("zEYE");
        zEyeButton.setToolTipText("Rotate eye around z-axis");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(zEyeButton, gbc);
        executeButton = new JButton();
        executeButton.setText("EXECUTE");
        executeButton.setToolTipText("Execute the command");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(executeButton, gbc);
        audibleCheckbox = new JCheckBox();
        audibleCheckbox.setSelected(true);
        audibleCheckbox.setText("AUDIBLE");
        audibleCheckbox.setToolTipText("Audibility of the grasp");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(audibleCheckbox, gbc);
        obstacleTypeCombo = new JComboBox();
        obstacleTypeCombo.setToolTipText("Type of obstacle object");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        buttonPanel.add(obstacleTypeCombo, gbc);
        reachButton = new JButton();
        reachButton.setText("REACH");
        reachButton.setToolTipText("Execute the planned reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        buttonPanel.add(reachButton, gbc);
        weightTextField = new JTextField();
        weightTextField.setEnabled(true);
        weightTextField.setText("jjb_bptt_hebbian.wgt");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        buttonPanel.add(weightTextField, gbc);
        resetOtherArmButton = new JButton();
        resetOtherArmButton.setText("RESET ARM");
        resetOtherArmButton.setToolTipText("Reset the arm configuration");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        buttonPanel.add(resetOtherArmButton, gbc);
        otherJointControlButton = new JButton();
        otherJointControlButton.setText("JOINTS");
        otherJointControlButton.setToolTipText("Manually control arm joint configurations");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(otherJointControlButton, gbc);
        graspTypeLabel = new JLabel();
        graspTypeLabel.setText("GRASP");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(graspTypeLabel, gbc);
        graspTypeCombo = new JComboBox();
        graspTypeCombo.setToolTipText("Type of grasp to plan and execute");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(graspTypeCombo, gbc);
        bellshapeCheckbox = new JCheckBox();
        bellshapeCheckbox.setLabel("BELLSHAPE");
        bellshapeCheckbox.setName("Whether or not to generate reach movements with bellshaped velocity profiles");
        bellshapeCheckbox.setSelected(true);
        bellshapeCheckbox.setText("BELLSHAPE");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        buttonPanel.add(bellshapeCheckbox, gbc);
        speedLabel = new JLabel();
        speedLabel.setText("SPEED");
        speedLabel.setToolTipText("Speed to execute the reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(speedLabel, gbc);
        speedCombo = new JComboBox();
        speedCombo.setToolTipText("Speed to execute the reach-to-grasp movement");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(speedCombo, gbc);
        sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(sliderPanel, gbc);
        xang = new JSlider();
        xang.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(xang, gbc);
        yang = new JSlider();
        yang.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(yang, gbc);
        zang = new JSlider();
        zang.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(zang, gbc);
        scale = new JSlider();
        scale.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(scale, gbc);
        objscale = new JSlider();
        objscale.setOrientation(1);
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        sliderPanel.add(objscale, gbc);
        canvasPanel = new JPanel();
        canvasPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(canvasPanel, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return mainPanel;
    }
}
