package mns2.comp;

import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import mns2.graphics.NetworkPanel;

import sim.motor.Graspable;
import sim.graphics.Point3d;
import sim.util.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class Network extends JFrame implements ActionListener, MouseListener, ChangeListener
{
    // the maximum length of a sequence in the training set
    public static int MAX_seqLength = 500;

    // is there a validNet net created?
    public boolean validNet = false;

    // Whether or not to record the network activity
    public boolean recordNetwork = false;

    public Vector lesionedConnection;
    public Vector lesionTime;

    // number of patterns
    public int patc = 0;

    // learning rate
    public double eta;
    // delta learning rate up coeff, delta learning rate down coeff
    public double etaDOWN, etaUP;
    // Momentum
    public double beta;

    // current time step
    public int t;

    // input layer
    public double inputLayer[];
    // input layer before squashed
    public double inputLayerNet[];
    // hidden layer
    public double hiddenLayer[];
    // hidden layerbefore squashed
    public double hiddenLayerNet[];
    // output layer
    public double outputLayer[];
    // output layer before squashed
    public double outputLayerNet[];

    // input layer history
    public double inputLayerHistory[][];
    // hidden layer history
    public double hiddenLayerHistory[][];
    // output layer history
    public double outputLayerHistory[][];

    // hidden to output weights (hiddenLayer->outputLayer weights)
    public double hiddenToOutputW[][];
    // input to hidden weights  (inputLayer->hiddenLayer weights)
    public double inputToHiddenW[][];

    // input dimension
    public int inputLayerDim;
    // number of hidden units
    public int hiddenLayerDim;
    // output dimension
    public int outputLayerDim;

    public static final int wscMAX = 100;
    public static final double wscrealMAX = 32; // max real value used by drawconn

    // Number of times trained
    int totalit = 0;

    // Pattern file layer dimensions
    public int pat_indim = 0, pat_hiddim = 0, pat_outdim = 0;
    // Pattern file learning parameters
    protected double pat_eta = 0;
    // Pattern file learning parameters
    public int correctPatterns = 0;
    protected double maxErr;

    // Weight file layer dimensions
    int fileInputLayerDim, fileHiddenLayerDim, fileOutputLayerDim;
    // Weight file input to hidden layer weights
    double[][] fileInputToHiddenW;
    // Weight file hidden to output layer weights
    double[][] fileHiddenToOutputW;

    public JTextField pattern_filename, weight_filename, epochs, training_threshold;
    public JLabel patternFilenameLabel, weightFileLabel, trainingEpochsLabel, successThresholdLabel;
    protected NetworkPanel panel = null;
    JButton loadPattern, loadWeight, train, test, randomizeWeights, makeNetworkFromPattern, makeNetworkFromWeight,
            generateWeightFile, quit;
    public JSlider wsc;
    JPanel mainPanel, centerPanel, networkPanel, bottomPanel, topPanel;

    public String[] plots = new String[]{"input", "hiddenMDS", "output"};
    public int[] plot_dimensions = new int[]{2, 2, 2};
    public String[] plot_extra_command = new String[]{"set xlabel \"Time\";set ylabel \"Activation\";", "", "set xlabel \"Time\";set ylabel \"Activation\";"};
    public String[][] plot_labels = new String[3][];
    public String[] plot_output_files = new String[3];

    public Point3d[] mdsCoords;

    // Whether or not to use dynamic remapping on the working memory trace of hand state
    public boolean dynRemapping = true;

    // Point of the arm to use in dynamic remapping
    public String remappingPoint = "forearm";

    public Network()
    {
        plot_labels[0] = NetworkInterface.params;
        plot_labels[1] = new String[0];
        plot_labels[2] = Graspable.grasps;
        //plot_output_files[1]="hidden_layer";

        lesionedConnection = new Vector(1);
        lesionTime = new Vector(1);

        loadPattern.addActionListener(this);
        loadWeight.addActionListener(this);
        train.addActionListener(this);
        test.addActionListener(this);
        randomizeWeights.addActionListener(this);
        makeNetworkFromPattern.addActionListener(this);
        makeNetworkFromWeight.addActionListener(this);
        generateWeightFile.addActionListener(this);
        quit.addActionListener(this);

        //setupCanvas();
        //networkPanel.add(panel);

        wsc.setMinimum(0);
        wsc.setMaximum(wscMAX);
        wsc.setValue(0);
        wsc.addChangeListener(this);

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        enableEvents(AWTEvent.COMPONENT_EVENT_MASK);
        add(mainPanel);
    }

    protected void processComponentEvent(ComponentEvent e)
    {
        if (e.getID() == ComponentEvent.COMPONENT_RESIZED)
        {
            Dimension d = this.getSize();
            networkPanel.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 150);
            panel.setSize(networkPanel.getSize());
            panel.setLocation(0, 0);
            panel.repaint();
        }
    }

    public void setupCanvas()
    {
        panel = new NetworkPanel(this);
        panel.addMouseListener(this);
        panel.wscValue = wsc.getValue();
        networkPanel.add(panel);
    }

    /**
     * Set input units to pattern
     *
     * @param input - input pattern
     */
    public void presentPattern(final double[] input)
    {
        System.arraycopy(input, 0, inputLayer, 0, inputLayerDim);
    }

    public void clearHistory()
    {
    }

    public boolean readPattern(final String fn)
    {
        return false;
    }

    /**
     * Converts the training patterns of the given network to a format suitable for this network type
     *
     * @param fromNetworkType  - Type of the network whose training patterns will be converted
     * @param fromNetwork      - Network with loaded training patterns
     * @param out              - Output stream to write the converted training patterns to
     * @param encodeDerivative - Whether or not to encode the derivative of each hand state
     * @param useSplines       - Whether or not to perform a temporal-to-spatial transformation on the hand state trajectory
     *                         using cubic splines
     */
    public static void convertPattern(String fromNetworkType, Network fromNetwork, DataOutputStream out,
                                      boolean encodeDerivative, boolean useSplines) throws IOException
    {

    }

    /**
     * Plots network layer activation patterns depending on values of plots
     */
    protected void plotNetworkActivity()
    {
        if (Resource.getString("gnuplotExecutable") != null && Resource.getString("gnuplotExecutable").trim().length() > 0 &&
            Resource.getString("gnuplotExecutable").charAt(0) != '#')
        {
            String frameString = '_' + FrameUtils.getFrameString(t + 1);
            for (int j = 0; j < plots.length; j++)
            {
                if (plots[j].equals("output"))
                {
                    Gplot.plot(outputLayerHistory, t + 1, outputLayerDim, plot_extra_command[j], plot_dimensions[j],
                               plot_labels[j], plot_output_files[j] + frameString);
                }
                else if (plots[j].equals("hidden"))
                {
                    Gplot.plot(hiddenLayerHistory, t + 1, hiddenLayerDim, plot_extra_command[j], plot_dimensions[j],
                               plot_labels[j], plot_output_files[j] + frameString);
                }
                else if (plots[j].equals("hiddenMDS"))
                {
                    if (Resource.getString("vistaExecutable") != null &&
                        Resource.getString("vistaExecutable").trim().length() > 0 &&
                                                                                  !Resource.getString("vistaExecutable").startsWith("#"))
                    {
                        Vista vista = new Vista(hiddenLayerHistory, t + 1, hiddenLayerDim, "Hidden", "",
                                                plot_output_files[j] + frameString + ".lsp");
                        mdsCoords = vista.getCoords();
                        Gplot.plot(mdsCoords, mdsCoords.length, plot_extra_command[j],
                                   plot_output_files[j] + frameString);
                    }
                }
                else if (plots[j].equals("input"))
                {
                    Gplot.plot(inputLayerHistory, t + 1, inputLayerDim, plot_extra_command[j], plot_dimensions[j],
                               plot_labels[j], plot_output_files[j] + frameString);
                }
            }
        }
    }

    public void train(final int maxiter, final double training_perc, final boolean verbose)
    {
    }

    /**
     * Plots the error history over the entire training period
     *
     * @param errHistory     - total error history
     * @param eta_original   - original eta value
     * @param maxiter        - maximum training iterations
     * @param maxErrHistory  - max unit error history
     * @param correctHistory - correct pattern history
     */
    protected void plotTrainingActivity(final double[] errHistory, final double eta_original, final int maxiter,
                                        final double[] maxErrHistory, final double[] correctHistory)
    {
        if (Resource.getString("gnuplotExecutable") != null && Resource.getString("gnuplotExecutable").trim().length() > 0 &&
            Resource.getString("gnuplotExecutable").charAt(0) != '#')
        {
            Gplot.plot(errHistory, errHistory.length, null, "set title \"total error over all patterns - eta= " +
                                                            eta_original + " - beta= " + beta + " - etaUp= " + etaUP + " - etaDOWN= " + etaDOWN + " - runs=" + maxiter +
                                                                                                                                                                       "\";set xlabel \"time\";set ylabel \"error\"", null);
            Gplot.plot(maxErrHistory, maxErrHistory.length, null, "set title \"max unit error - eta= " + eta_original +
                                                                  " - beta= " + beta + " - etaUp= " + etaUP + " - etaDOWN= " + etaDOWN + " - runs=" + maxiter +
                                                                                                                                                              "\";set xlabel \"time\";set ylabel \"max error\"", null);
            Gplot.plot(correctHistory, correctHistory.length, null, "set title \"correct patterns (" + patc + " MAX) - eta= " +
                                                                    eta_original + " - beta= " + beta + " - etaUp= " + etaUP + " - etaDOWN= " + etaDOWN + " - runs=" + maxiter +
                                                                                                                                                                               "\";set xlabel \"time\";set ylabel \"max error\"", null);
        }
    }

    public double testPattern()
    {
        return 0.0;
    }

    public void netFromWeight(final String with)
    {
    }

    public void netFromPattern(final String with)
    {
    }

    public void installWeight(final String with)
    {
    }

    public void writeWeight(final String fn)
    {
    }

    public void initNet()
    {
    }

//	 -------------------------------------------------------------------
//	 AWT events

    public void mouseClicked(final MouseEvent e)
    {
    }

    public void mousePressed(final MouseEvent e)
    {
    }

    public void mouseReleased(final MouseEvent e)
    {
    }

    public void mouseEntered(final MouseEvent e)
    {
    }

    public void mouseExited(final MouseEvent e)
    {
    }

    public void stateChanged(final ChangeEvent e)
    {
        panel.repaint();
    }

    protected void processWindowEvent(final WindowEvent e)
    {
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
            this.dispose();
        else
            panel.repaint();
    }

    public void actionPerformed(final ActionEvent e)
    {
        if (e.getSource().equals(loadPattern))
        {
            patc = 0;
            clearHistory();
            readPattern(pattern_filename.getText());
        }
        else if (e.getSource().equals(train))
        {
            final int cc = Elib.toInt(epochs.getText());
            final double perc = Elib.toDouble(training_threshold.getText());
            Log.println("* Training Epoch Started [" + cc + " steps] *");
            train(cc, perc, true);
            testPattern();
            Log.println("* Epoch Done *");
            panel.repaint();
        }
        else if (e.getSource().equals(makeNetworkFromPattern))
        {
            Log.println("Making a new network for " + pattern_filename.getText());
            netFromPattern(pattern_filename.getText());
            panel.repaint();
            Log.println("Made a new network for " + pattern_filename.getText() + " and loaded the patterns.");
        }
        else if (e.getSource().equals(makeNetworkFromWeight))
        {
            Log.println("Making a new network from the weight file " + weight_filename.getText());
            netFromWeight(weight_filename.getText());
            panel.repaint();
        }
        else if (e.getSource().equals(loadWeight))
        {
            totalit = 0;
            Log.println("Loading weights from " + weight_filename.getText());
            clearHistory();
            installWeight(weight_filename.getText());
            panel.repaint();
        }
        else if (e.getSource().equals(generateWeightFile))
        {
            Log.println("Writing weight file " + weight_filename.getText());
            writeWeight(weight_filename.getText());
        }
        else if (e.getSource().equals(randomizeWeights))
        {
            initNet();
        }
        else if (e.getSource().equals(test))
        {
            testPattern();
        }
        else if (e.getSource().equals(quit))
        {
            System.exit(0);
        }
    }

    public static Network configureNet(String netType, String weightFile)
    {
        Network net = null;
        if (netType.equals("BP"))
            net = new BP();
        else if (netType.startsWith("BPTT"))
        {
            if (netType.equals("BPTT"))
                net = new BPTT();
            else if (netType.equals("BPTTwithHebbian"))
                net = new BPTTwithHebbian();
        }
        else if (netType.equals("Hebbian"))
            net = new Hebbian();
        if (net != null)
            net.netFromWeight(weightFile);

        net.setupCanvas();
        return net;
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
        topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(topPanel, gbc);
        loadPattern = new JButton();
        loadPattern.setText("Load Pattern");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(loadPattern, gbc);
        train = new JButton();
        train.setText("Train");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(train, gbc);
        makeNetworkFromPattern = new JButton();
        makeNetworkFromPattern.setText("Make Network from Pattern");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(makeNetworkFromPattern, gbc);
        makeNetworkFromWeight = new JButton();
        makeNetworkFromWeight.setText("Make Network from Weights");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(makeNetworkFromWeight, gbc);
        loadWeight = new JButton();
        loadWeight.setText("Load Weights");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(loadWeight, gbc);
        test = new JButton();
        test.setText("Test");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(test, gbc);
        randomizeWeights = new JButton();
        randomizeWeights.setText("Randomize Weights");
        randomizeWeights.setToolTipText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(randomizeWeights, gbc);
        generateWeightFile = new JButton();
        generateWeightFile.setText("Generate Weight File");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(generateWeightFile, gbc);
        quit = new JButton();
        quit.setText("Quit");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(quit, gbc);
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(bottomPanel, gbc);
        pattern_filename = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(pattern_filename, gbc);
        weight_filename = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(weight_filename, gbc);
        epochs = new JTextField();
        epochs.setText("10000");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(epochs, gbc);
        training_threshold = new JTextField();
        training_threshold.setText("1.0");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(training_threshold, gbc);
        patternFilenameLabel = new JLabel();
        patternFilenameLabel.setText("Pattern File:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        bottomPanel.add(patternFilenameLabel, gbc);
        weightFileLabel = new JLabel();
        weightFileLabel.setText("Weight File:");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        bottomPanel.add(weightFileLabel, gbc);
        trainingEpochsLabel = new JLabel();
        trainingEpochsLabel.setText("Training Epochs:");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        bottomPanel.add(trainingEpochsLabel, gbc);
        successThresholdLabel = new JLabel();
        successThresholdLabel.setText("Success Threshold Ratio:");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        bottomPanel.add(successThresholdLabel, gbc);
        centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(centerPanel, gbc);
        wsc = new JSlider();
        wsc.setOrientation(1);
        wsc.setValue(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        centerPanel.add(wsc, gbc);
        networkPanel = new JPanel();
        networkPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(networkPanel, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return mainPanel;
    }
}
