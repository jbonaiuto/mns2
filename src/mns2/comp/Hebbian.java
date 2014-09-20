package mns2.comp;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.*;
import javax.swing.event.ChangeEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import mns2.graphics.HebbianPanel;
import mns2.util.ParamsNode;
import sim.util.*;
import sim.motor.Graspable;

public class Hebbian extends Network
{
    // visual input patterns for each time step of each sequence in the training set
    public double trainingInput[][];
    // target output patterns for each time step of each sequence in the training set
    public double trainingTeacher[][];

    // Teacher layer dimension
    public int teacherDim;

    // Teacher layer
    public double teacherLayer[];
    // Teacher layer before squashed
    public double teacherLayerNet[];

    public double teacherToOutputW[][];

    // teacher layer history
    public double teacherLayerHistory[][];

    // Pattern file layer dimensions
    public int pat_teacherdim = 0;

    // Weight file layer dimensions
    protected int fileTeacherDim;
    protected double[][] fileTeacherToOutputW;

    public String[] plots;
    public int[] plot_dimensions;
    public String[] plot_extra_command;
    public String[][] plot_labels;
    public String[] plot_output_files;

    public JTextField pattern_filename, weight_filename, epochs, training_threshold;
    public JLabel patternFilenameLabel, weightFileLabel, trainingEpochsLabel, successThresholdLabel;
    JButton loadPattern, loadWeight, train, test, randomizeWeights, makeNetworkFromPattern, makeNetworkFromWeight,
            generateWeightFile, quit;
    public JSlider wsc;
    JPanel mainPanel, centerPanel, networkPanel, bottomPanel, topPanel;

    private HebbianPanel panel;

    public Hebbian()
    {
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

        setTitle("Hebbian Learning - James Bonaiuto Aug'05");
        if (NetworkInterface.useSplines)
        {
            pattern_filename.setText("action_pattern_hebbian_spline.xml");
            weight_filename.setText("jjb_hebbian_spline.wgt");
        }
        else
        {
            pattern_filename.setText("action_pattern_hebbian.xml");
            weight_filename.setText("jjb_hebbian.wgt");
        }
    }

    protected void processComponentEvent(ComponentEvent e)
    {
        if (e.getID() == ComponentEvent.COMPONENT_RESIZED)
        {
            final Dimension d = networkPanel.getSize();
            panel.setSize((int) d.getWidth() - 10, (int) d.getHeight() - 10);
            panel.setLocation(0, 0);
            panel.repaint();
        }
    }

    public void setupCanvas()
    {
        panel = new HebbianPanel(this);
        panel.addMouseListener(this);
        panel.wscValue = wsc.getValue();
        networkPanel.add(panel);
    }

    /**
     * Constructor
     *
     * @param indim      - number of input units
     * @param outdim     - number of output units
     * @param teacherdim - number of teacher units
     * @param def_eta    - learning rate
     */
    public Hebbian(final int indim, final int hiddim, final int outdim, final int teacherdim,
                   final double def_eta)
    {
        this();
        // Set learning rate
        eta = def_eta;

        //Create network
        createNet(indim, hiddim, outdim, teacherdim);
    }

    /**
     * Constructor
     *
     * @param s - pattern filename
     */
    public Hebbian(final String s)
    {
        this(s, 0.01);
    }

    /**
     * Constructor
     *
     * @param s       - pattern filename
     * @param def_eta - learning rate
     */
    public Hebbian(final String s, final double def_eta)
    {
        this();
        // Set learning rate
        eta = def_eta;

        //Create network from pattern file
        //System.out.println("Constructing the network with file:"+s);
        netFromPattern(s);
    }

    /**
     * Create network
     *
     * @param indim      - number of input units
     * @param hiddim     - number of hidden units
     * @param outdim     - number of output units
     * @param teacherdim - number of teacher units
     */
    public void createNet(final int indim, final int hiddim, final int outdim, int teacherdim)
    {
        //initialize layers
        inputLayerDim = indim;
        hiddenLayerDim = hiddim;
        teacherDim = teacherdim;
        outputLayerDim = outdim;

        inputLayer = new double[inputLayerDim];
        inputLayerNet = new double[inputLayerDim];
        hiddenLayer = new double[hiddenLayerDim];
        hiddenLayerNet = new double[hiddenLayerDim];
        outputLayer = new double[outputLayerDim];
        outputLayerNet = new double[outputLayerDim];
        teacherLayer = new double[teacherDim];
        teacherLayerNet = new double[teacherDim];

        inputToHiddenW = new double[hiddenLayerDim][inputLayerDim];
        hiddenToOutputW = new double[outputLayerDim][hiddenLayerDim];
        teacherToOutputW = new double[outputLayerDim][teacherDim];

        clearHistory();

        //initialize network weights
        initNet();
        validNet = true;
    }

    public void initNet()
    {
        for (int i = 0; i < hiddenLayerDim; i++)
        {
            for (int j = 0; j < inputLayerDim; j++)
            {
                inputToHiddenW[i][j] = (Math.random() - 0.5) * 0.1;
            }
        }
        for (int i = 0; i < outputLayerDim; i++)
        {
            for (int j = 0; j < hiddenLayerDim; j++)
            {
                hiddenToOutputW[i][j] = (Math.random() - 0.5) * 0.1;
            }
        }
        Vector outputIdx = new Vector();
        for (int i = 0; i < outputLayerDim; i++)
            outputIdx.add(new Integer(i));
        while (outputIdx.size() > 0)
        {
            for (int i = 0; i < teacherDim; i++)
            {
                if (outputIdx.size() > 0)
                {
                    int idx = (int) (Math.random() * (outputIdx.size() - 1));
                    int targetIdx = ((Integer) outputIdx.get(idx)).intValue();
                    teacherToOutputW[targetIdx][i] = (Math.random() + 2.0) * 5.0;
                    outputIdx.remove(idx);
                }
                else
                    break;
            }
        }
    }

    /**
     * Create net from pattern file
     *
     * @param with - pattern filename
     */
    public void netFromPattern(final String with)
    {
        panel.showAll();
        patc = 0;

        // Read pattern file
        if (!readPattern(with))
        {
            Log.println("Error occured reading pattern file:" + with);
            return;
        }

        if (pat_eta > -1)
            eta = pat_eta;

        // Create the network
        createNet(pat_indim, pat_hiddim, pat_outdim, pat_teacherdim);
    }

    /**
     * Create network from local weight file
     *
     * @param with - weight filename
     */
    public void netFromWeight(final String with)
    {
        netFromWeight(null, with);
    }

    /**
     * Create network from weight file
     *
     * @param base - base URL, NULL if local file
     * @param with - weight file
     */
    public void netFromWeight(final URL base, final String with)
    {
        //panel.showAll();

        // read weight file
        if (!readWeight(with))
        {
            Log.println("Error occured reading weight file:" + with);
            return;
        }

        //create the network
        createNet(fileInputLayerDim, fileHiddenLayerDim, fileOutputLayerDim, fileTeacherDim);

        //Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
        {
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim);
        }
        // Set hidden to output layer weights
        for (int i = 0; i < fileOutputLayerDim; i++)
        {
            System.arraycopy(fileHiddenToOutputW[i], 0, hiddenToOutputW[i], 0, fileHiddenLayerDim);
        }

        //Set teacher to output layer weights
        for (int k = 0; k < fileOutputLayerDim; k++)
        {
            System.arraycopy(fileTeacherToOutputW[k], 0, teacherToOutputW[k], 0, fileTeacherDim);
        }

        Log.println("inputLayerDim:" + inputLayerDim + " outputLayerDim:" + outputLayerDim + " teacherDim:" + teacherDim);
    }

    /**
     * Initialize weights from file
     *
     * @param with - weight filename
     */
    public void installWeight(final String with)
    {
        panel.showAll();

        // Read weights from file
        if (!readWeight(with))
        {
            Log.println("Error occured reading weight file:" + with);
            return;
        }

        if (fileInputToHiddenW == null || fileHiddenToOutputW == null || fileTeacherToOutputW == null)
        {
            Log.println("Error occured reading weight file:" + with);
            return;
        }

        if (fileInputLayerDim != inputLayerDim)
        {
            Log.println("Mismatch in input dimension!");
            return;
        }
        if (fileHiddenLayerDim != hiddenLayerDim)
        {
            Log.println("Mismatch in hidden dimension!");
            return;
        }
        if (fileTeacherDim != teacherDim)
        {
            Log.println("Mismatch in teacher dimension!");
            return;
        }
        if (fileOutputLayerDim != outputLayerDim)
        {
            Log.println("Mismatch in output dimension!");
            return;
        }

        // Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
        {
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim);
        }

        // Set hidden to output layer weights
        for (int i = 0; i < fileOutputLayerDim; i++)
        {
            System.arraycopy(fileHiddenToOutputW[i], 0, hiddenToOutputW[i], 0, fileHiddenLayerDim);
        }
        // Set teacher to output layer weights
        for (int k = 0; k < fileOutputLayerDim; k++)
        {
            System.arraycopy(fileTeacherToOutputW[k], 0, teacherToOutputW[k], 0, fileTeacherDim);
        }
    }

    /**
     * Clear weight change history
     */
    public void clearHistory()
    {
        inputLayerHistory = new double[MAX_seqLength][inputLayerDim];
        hiddenLayerHistory = new double[MAX_seqLength][hiddenLayerDim];
        teacherLayerHistory = new double[MAX_seqLength][teacherDim];
        outputLayerHistory = new double[MAX_seqLength][outputLayerDim];
    }

    /**
     * Run network forward
     */
    public void forwardFromInput(final boolean plot, final boolean staticAction)
    {
        boolean lesionInputToHidden = false;
        if (lesionedConnection.contains("inputToHidden"))
        {
            String time = lesionTime.get(lesionedConnection.indexOf("inputToHidden")).toString();
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) ||
                (!staticAction && time.equals("duringGrasp")))
                lesionInputToHidden = true;
        }
        if (!lesionInputToHidden)
        {
            // multiply input layer by input to hidden layer weights and put results in hiddenLayerNet
            VA.multiply(inputToHiddenW, inputLayer, hiddenLayerDim, inputLayerDim, hiddenLayerNet);
        }
        // push hiddenLayerNet through sigmoid function and put result in hidden layer
        VA.squash(hiddenLayerNet, hiddenLayerDim, hiddenLayer);

        boolean lesionHiddenToOutput = false;
        if (lesionedConnection.contains("hiddenToOutput"))
        {
            String time = lesionTime.get(lesionedConnection.indexOf("hiddenToOutput")).toString();
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) ||
                (!staticAction && time.equals("duringGrasp")))
                lesionHiddenToOutput = true;
        }
        if (!lesionHiddenToOutput)
        {
            // multiply hidden layer by hidden to output layer weights and put results in outputLayerNet
            VA.multiply(hiddenToOutputW, hiddenLayer, outputLayerDim, hiddenLayerDim, outputLayerNet);
        }
        // push outputLayerNet through sigmoid function and put result in output layer
        VA.squash(outputLayerNet, outputLayerDim, outputLayer);

        // transfer current network state to history
        System.arraycopy(inputLayer, 0, inputLayerHistory[t], 0, inputLayerDim);
        System.arraycopy(hiddenLayer, 0, hiddenLayerHistory[t], 0, hiddenLayerDim);
        System.arraycopy(outputLayer, 0, outputLayerHistory[t], 0, outputLayerDim);

        // plot network activation history
        if (plot)
        {
            plotNetworkActivity();
        }

        t++;
    }

    /**
     * Plots network layer activation patterns depending on values of plots
     */
    protected void plotNetworkActivity()
    {
        super.plotNetworkActivity();
        if (Resource.getString("gnuplotExecutable") != null && Resource.getString("gnuplotExecutable").trim().length() > 0 &&
            Resource.getString("gnuplotExecutable").charAt(0) != '#')
        {
            String frameString = '_' + FrameUtils.getFrameString(t + 1);
            for (int j = 0; j < super.plots.length; j++)
            {
                if (plots[j].equals("teacher"))
                {
                    Gplot.plot(teacherLayerHistory, t + 1, teacherDim, plot_extra_command[j], plot_dimensions[j],
                               plot_labels[j], plot_output_files[j] + frameString);
                }
            }
        }
    }

    /**
     * Run network forward
     */
    public void forwardFromTeacher(final boolean plot, final boolean staticAction)
    {
        boolean lesionTeacherToOutput = false;
        if (lesionedConnection.contains("teacherToOutput"))
        {
            String time = lesionTime.get(lesionedConnection.indexOf("teacherToOutput")).toString();
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) ||
                (!staticAction && time.equals("duringGrasp")))
                lesionTeacherToOutput = true;
        }
        if (!lesionTeacherToOutput)
        {
            // multiply teacher layer by teacher to output layer weights and put results in teacherLayerNet
            VA.multiply(teacherToOutputW, teacherLayer, outputLayerDim, teacherDim, outputLayerNet);
        }
        // push outputLayerNet through sigmoid function and put result in output layer
        VA.squash(outputLayerNet, outputLayerDim, outputLayer);

        // transfer current network state to history
        System.arraycopy(teacherLayer, 0, teacherLayerHistory[t], 0, teacherDim);
        System.arraycopy(outputLayer, 0, outputLayerHistory[t], 0, outputLayerDim);

        // plot network activation history
        if (plot)
        {
            plotNetworkActivity();
        }

        t++;
    }

    /**
     * Set input and teacher units to pattern p
     *
     * @param p - pattern index
     */
    public void presentPattern(final int p)
    {
        System.arraycopy(trainingInput[p], 0, inputLayer, 0, inputLayerDim);
        System.arraycopy(trainingTeacher[p], 0, teacherLayer, 0, teacherDim);
    }

    /**
     * Get output for given input
     *
     * @param inp  - input pattern
     * @param size - input pattern size - must match input layer size
     * @return - network output
     */
    public double[] ask(final double[] inp, final int size, final boolean plot, final boolean staticAction)
    {
        // Check size of input
        if (size != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
            //    return null;
        }

        // present input pattern
        presentPattern(inp);
        // run the network forward one time step
        forwardFromInput(plot, staticAction);

        // return the network's external output
        final double[] externalOut = new double[outputLayerDim];
        System.arraycopy(outputLayer, 0, externalOut, 0, outputLayerDim);
        return externalOut;
    }

    /**
     * Learn pattern p
     *
     * @param p - pattern index
     */
    public void learn(final int p)
    {
        // present input pattern for this time step of the sequence
        presentPattern(p);
        // Calc forward net dynamics
        forwardFromTeacher(false, false);

        // multiply input layer by input to hidden layer weights and put results in hiddenLayerNet
        VA.multiply(inputToHiddenW, inputLayer, hiddenLayerDim, inputLayerDim, hiddenLayerNet);
        // push hiddenLayerNet through sigmoid function and put result in hidden layer
        VA.squash(hiddenLayerNet, hiddenLayerDim, hiddenLayer);

        // Update hebbian hidden to output layer weights with hebbian learning
        double[] totalWeightSumsExt = new double[hiddenLayerDim];

        for (int j = 0; j < outputLayerDim; j++)
        {
            for (int k = 0; k < hiddenLayerDim; k++)
            {
                final double dw = eta * hiddenLayer[k] * outputLayer[j];
                if (outputLayer[j] > 0.5)
                    hiddenToOutputW[j][k] += dw;
                else
                    hiddenToOutputW[j][k] -= dw;
                totalWeightSumsExt[k] += Math.abs(hiddenToOutputW[j][k]);
            }
        }
        for (int k = 0; k < hiddenLayerDim; k++)
        {
            for (int j = 0; j < outputLayerDim; j++)
            {
                hiddenToOutputW[j][k] /= totalWeightSumsExt[k] / 10.0;
            }
        }

        // Update hebbian input to hidden layer weights with hebbian learning
        totalWeightSumsExt = new double[inputLayerDim];

        for (int j = 0; j < hiddenLayerDim; j++)
        {
            for (int k = 0; k < inputLayerDim; k++)
            {
                final double dw = eta * inputLayer[k] * hiddenLayer[j];
                if (hiddenLayer[j] > 0.5)
                    inputToHiddenW[j][k] += dw;
                else
                    inputToHiddenW[j][k] -= dw;
                totalWeightSumsExt[k] += Math.abs(inputToHiddenW[j][k]);
            }
        }
        for (int k = 0; k < inputLayerDim; k++)
        {
            for (int j = 0; j < hiddenLayerDim; j++)
            {
                inputToHiddenW[j][k] /= totalWeightSumsExt[k] / 10.0;
            }
        }
    }

    /**
     * Train network until maxiter or error is less than .001
     *
     * @param maxiter         - maximum times to train
     * @param training_thresh - quit when this percentage of training patterns is correctly identified
     * @param verbose         - whether or not to output debug info
     */
    public void train(final int maxiter, final double training_thresh, final boolean verbose)
    {
        final double errHistory[] = new double[maxiter / 100 + 1];
        final double maxErrHistory[] = new double[maxiter / 100 + 1];
        final double correctHistory[] = new double[maxiter / 100 + 1];
        // pattern index
        int p;

        if (pat_indim != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
            //    return;
        }
        if (pat_hiddim != hiddenLayerDim)
        {
            Log.println("Pattern hidden dim does not match network hidden dim!!");
            //    return;
        }
        if (pat_outdim != outputLayerDim)
        {
            Log.println("Pattern output dim does not match network output dim!!");
            return;
        }
        if (pat_teacherdim != teacherDim)
        {
            Log.println("Pattern teacher dim does not match network teacher dim!!");
            return;
        }
        final int testSet[] = new int[patc];
        for (int i = 0; i < patc; i++)
            testSet[i] = i;
        // if maxiter is greater than zero, keep going until min error, otherwise train for maxiter iterations
        for (int it = 0; (maxiter <= 0 || it < maxiter); it++)
        {
            totalit++;

            // Choose a random pattern from the list
            p = (int) (Math.random() * patc);

            // Clear history
            clearHistory();
            // Learn pattern
            learn(p);
            testPattern(p);

            // check if error is small enough to stop train
            if (it % 100 == 0)
            {
                errHistory[it / 100] = testPattern(verbose, testSet);
                maxErrHistory[it / 100] = maxErr;
                correctHistory[it / 100] = correctPatterns;
                if (errHistory[it / 100] < 0.001)
                {
                    Log.println("At " + it + " iterations, error is less than 0.001, stopping training.");
                    break;
                }
            }
            if (correctPatterns >= (training_thresh * patc))
                break;
        }
        errHistory[errHistory.length - 1] = testPattern(true, testSet);
        maxErrHistory[maxErrHistory.length - 1] = maxErr;
        correctHistory[correctHistory.length - 1] = correctPatterns;
        if (verbose)
        {
            plotTrainingActivity(errHistory, eta, maxiter, maxErrHistory, correctHistory);
        }
    }

    /**
     * Test all patterns
     *
     * @return - total output error over all patterns
     */
    public double testPattern()
    {
        final int testSet[] = new int[patc];
        for (int i = 0; i < patc; i++)
            testSet[i] = i;
        return testPattern(false, testSet);
    }

    public double testPattern(final int p)
    {
        double patt_err = 0.0;
        t = 0;
        // Input pattern and compute network output
        presentPattern(p);
        forwardFromInput(false, false);
        // Output layer error
        double thiserr = 0;

        // Compute error for all output units
        for (int k = 0; k < outputLayerDim; k++)
        {
            for (int j = 0; j < teacherDim; j++)
            {
                if (teacherToOutputW[k][j] > 0)
                {
                    // error squared for this unit
                    final double ee = (trainingTeacher[p][j] - outputLayer[k]) * (trainingTeacher[p][j] - outputLayer[k]);

                    // update layer error
                    thiserr += ee * .5;
                }
            }
        }
        // accumulate error
        patt_err += thiserr;

        return patt_err;
    }

    /**
     * Test all patterns
     *
     * @param verbose
     * @return - total output error over all patterns
     */
    public double testPattern(final boolean verbose, final int testSet[])
    {
        // Total error
        double err = 0;
        // Total correct output
        correctPatterns = 0;

        if (pat_indim != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
            //return -1;
        }
        if (pat_hiddim != hiddenLayerDim)
        {
            Log.println("Pattern hidden dim does not match network hidden dim!!");
            //return -1;
        }
        if (pat_outdim != outputLayerDim)
        {
            Log.println("Pattern output dim does not match network output dim!!");
            //return -1;
        }
        if (pat_teacherdim != teacherDim)
        {
            Log.println("Pattern teacher dim does not match network teacher dim!!");
            return -1;
        }

        // Max output unit error
        maxErr = -1;

        // For all patterns
        for (int i = 0; i < testSet.length; i++)
        {
            final int p = testSet[i];
            double patt_err = 0.0;
            t = 0;
            // Input pattern and compute network output
            presentPattern(p);
            forwardFromInput(false, false);

            // Number of units with correct output
            int oo = 0;
            // Output layer error
            double thiserr = 0;

            // Compute error for all output units
            for (int k = 0; k < outputLayerDim; k++)
            {
                for (int j = 0; j < teacherDim; j++)
                {
                    if (teacherToOutputW[k][j] > 0)
                    {
                        // error squared for this unit
                        final double ee = (trainingTeacher[p][j] - outputLayer[k]) * (trainingTeacher[p][j] - outputLayer[k]);

                        // if error small enough, output is correct
                        if (ee < 0.05)
                            oo++;
                        // update max output unit error
                        if ((ee * .5) > maxErr)
                            maxErr = ee * .5;
                        // update layer error
                        thiserr += ee * .5;
                    }
                }
            }

            // accumulate error
            patt_err += thiserr;

            // If all units have correct output, increment number of correct outputs
            if (oo == outputLayerDim)
            {
                correctPatterns++;
            }
            err += patt_err;
        }
        final String rep = totalit + ":Total error over patterns:" + err + " # Correct sequences:" + correctPatterns + '/' + testSet.length +
                           " MAX (unit) err:" + maxErr + " [L.rate:" + eta + ']';

        if (verbose)
        {
            Log.println(rep);
            setTitle(rep);
        }
        return err;
    }

    /**
     * Reads a network pattern from a file
     *
     * @param fn - pattern filename
     * @return - successful
     */
    public boolean readPattern(final String fn)
    {
        pat_indim = 0;
        pat_outdim = 0;
        pat_teacherdim = 0;
        pat_eta = -1;

        try
        {
            final DataInputStream in;
            in = Elib.openfileREAD(fn);
            if (in == null)
                return false;
            final Document patternDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);

            final Element root = patternDoc.getDocumentElement();
            final NodeList rootChildren = root.getChildNodes();
            for (int i = 0; i < rootChildren.getLength(); i++)
            {
                final Node rootChild = rootChildren.item(i);
                if (rootChild.getNodeName().equals("RequiredNetworkSettings"))
                {
                    final NodeList requiredNetworkSettings = rootChild.getChildNodes();
                    for (int j = 0; j < requiredNetworkSettings.getLength(); j++)
                    {
                        final Node setting = requiredNetworkSettings.item(j);
                        if (setting.getNodeName().equals("OutputDim"))
                            pat_outdim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("HiddenDim"))
                            pat_hiddim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("TeacherDim"))
                            pat_teacherdim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("InputDim"))
                            pat_indim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                    }
                }
                else if (rootChild.getNodeName().equals("OptionalNetworkSettings"))
                {
                    final NodeList optionalNetworkSettings = rootChild.getChildNodes();
                    for (int j = 0; j < optionalNetworkSettings.getLength(); j++)
                    {
                        final Node setting = optionalNetworkSettings.item(j);
                        if (setting.getNodeName().equals("LearningRate"))
                            pat_eta = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                    }
                }
                else if (rootChild.getNodeName().equals("Patterns"))
                {
                    final NodeList sequences = rootChild.getChildNodes();
                    patc = 0;
                    for (int j = 0; j < sequences.getLength(); j++)
                    {
                        if (sequences.item(j).getNodeName().equals("Pattern"))
                            patc++;
                    }
                    trainingInput = new double[patc][pat_indim];
                    trainingTeacher = new double[patc][pat_teacherdim];

                    int seqIdx = 0;
                    for (int j = 0; j < sequences.getLength(); j++)
                    {
                        if (sequences.item(j).getNodeName().equals("Pattern"))
                        {
                            final NodeList sequenceNodes = sequences.item(j).getChildNodes();
                            for (int k = 0; k < sequenceNodes.getLength(); k++)
                            {
                                final Node sequenceNode = sequenceNodes.item(k);
                                if (sequenceNode.getNodeName().equals("InputPattern"))
                                {
                                    final NodeList inputs = sequenceNode.getChildNodes();
                                    int inpIdx = 0;
                                    for (int l = 0; l < inputs.getLength(); l++)
                                    {
                                        final Node value = inputs.item(l);
                                        if (value.getNodeName().equals("Value"))
                                        {
                                            trainingInput[seqIdx][inpIdx++] = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                        }
                                    }
                                }
                                else if (sequenceNode.getNodeName().equals("TeacherPattern"))
                                {
                                    final NodeList teachers = sequenceNode.getChildNodes();
                                    int outIdx = 0;
                                    for (int l = 0; l < teachers.getLength(); l++)
                                    {
                                        final Node value = teachers.item(l);
                                        if (value.getNodeName().equals("Value"))
                                        {
                                            trainingTeacher[seqIdx][outIdx++] = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                        }
                                    }
                                }
                            }
                            seqIdx++;
                        }
                    }
                }
            }

            in.close();
        }
        catch (FileNotFoundException e)
        {
            return false;
        }
        catch (Exception e)
        {
            Log.println("Hebbian.readPattern() : EXCEPTION " + e);
            return false;
        }

        return true;
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
        String[] params = new String[]{"aper1", "ang1", "ang2", "speed", "dist", "axisdisp1", "axisdisp2"};
        if (encodeDerivative)
            params = new String[]{"aper1", "ang1", "ang2", "speed", "dist", "axisdisp1", "axisdisp2", "d_aper1", "d_ang1", "d_ang2", "d_speed", "d_dist", "d_axisdisp1", "d_axisdisp2"};
        out.writeBytes("<PatternSet>\n");
        out.writeBytes("<RequiredNetworkSettings>\n");
        out.writeBytes("<OutputDim>" + fromNetwork.hiddenLayerDim + "</OutputDim>\n");
        out.writeBytes("<TeacherDim>" + fromNetwork.outputLayerDim + "</TeacherDim>\n");
        out.writeBytes("<HiddenDim>" + fromNetwork.hiddenLayerDim + "</HiddenDim>\n");
        if (useSplines)
            out.writeBytes("<InputDim>" + (params.length * NetworkInterface.splineRepresentationRes) + "</InputDim>\n");
        else
            out.writeBytes("<InputDim>" + params.length + "</InputDim>\n");
        out.writeBytes("</RequiredNetworkSettings>\n");
        out.writeBytes("<OptionalNetworkSettings>\n");
        out.writeBytes("<!--these are optional network settings. If not supplied defaults will be used-->\n");
        out.writeBytes("<LearningRate>" + fromNetwork.eta + "</LearningRate>\n");
        out.writeBytes("</OptionalNetworkSettings>\n");
        out.writeBytes("<Patterns>\n");
        for (int i = 0; i < fromNetwork.patc; i++)
        {
            if (useSplines)
            {
                ParamsNode parnode = new ParamsNode(NetworkInterface.MAX_patternlength, params);
                out.writeBytes("<Pattern>\n");
                if (fromNetworkType.equals("BPSpline"))
                {
                    out.writeBytes("<InputPattern>\n");
                    for (int k = 0; k < NetworkInterface.splineRepresentationRes * params.length; k++)
                    {
                        out.writeBytes("<Value>" + ((BP) fromNetwork).trainingInput[i][k] + "</Value>");
                    }
                    out.writeBytes("</InputPattern>\n");
                    out.writeBytes("<TeacherPattern>");
                    for (int k = 0; k < ((BP) fromNetwork).outputLayerDim; k++)
                    {
                        out.writeBytes("<Value>" + ((BP) fromNetwork).trainingOutput[i][k] + "</Value>");
                    }
                    out.writeBytes("</TeacherPattern>\n");
                    out.writeBytes("</Pattern>\n");
                }
                else if (fromNetworkType.startsWith("BPTT"))
                {
                    double old[] = new double[7];
                    for (int j = 0; j < ((BPTT) fromNetwork).trainingSeqLength[i]; j++)
                    {
                        for (int k = 0; k < 7; k++)
                        {
                            parnode.put(params[k], ((BPTT) fromNetwork).trainingInputSeq[i][j][k]);
                        }
                        for (int k = 0; k < 7; k++)
                        {
                            if (j == 0)
                                parnode.put(params[7 + k], 0.0);
                            else
                                parnode.put(params[7 + k], (((BPTT) fromNetwork).trainingInputSeq[i][j][k] - old[k]));
                            old[k] = ((BPTT) fromNetwork).trainingInputSeq[i][j][k];
                        }
                        parnode.advance();
                    }
                    out.writeBytes("<InputPattern>\n");
                    Spline sp[] = parnode.getSplines();
                    double step = 1.0 / (NetworkInterface.splineRepresentationRes - 1);
                    for (int j = 0; j < sp.length; j++)
                    {
                        for (int k = 0; k < NetworkInterface.splineRepresentationRes; k++)
                        {
                            double v = sp[j].eval(k * step);
                            out.writeBytes("<Value>" + v + "</Value>");
                        }
                    }
                    out.writeBytes("</InputPattern>\n");
                    out.writeBytes("<TeacherPattern>");
                    for (int k = 0; k < ((BPTT) fromNetwork).outputLayerDim; k++)
                    {
                        out.writeBytes("<Value>" + ((BPTT) fromNetwork).trainingOutputSeq[i][10][k] + "</Value>");
                    }
                    out.writeBytes("</TeacherPattern>\n");
                    out.writeBytes("</Pattern>\n");
                }
            }
            else
            {
                if (fromNetworkType.equals("BP") || fromNetworkType.equals("BPDelta"))
                {
                    out.writeBytes("<InputPattern>\n");
                    for (int k = 0; k < params.length; k++)
                    {
                        out.writeBytes("<Value>" + ((BP) fromNetwork).trainingInput[i][k] + "</Value>");
                    }
                    out.writeBytes("</InputPattern>\n");
                    out.writeBytes("<TeacherPattern>");
                    for (int k = 0; k < ((BP) fromNetwork).outputLayerDim; k++)
                    {
                        out.writeBytes("<Value>" + ((BP) fromNetwork).trainingOutput[i][k] + "</Value>");
                    }
                    out.writeBytes("</TeacherPattern>\n");
                    out.writeBytes("</Pattern>\n");
                }
                else if (fromNetworkType.startsWith("BPTT"))
                {
                    double old[] = new double[7];
                    for (int j = 0; j < ((BPTT) fromNetwork).trainingSeqLength[i]; j++)
                    {
                        out.writeBytes("<Pattern>\n");
                        out.writeBytes("<InputPattern>");
                        for (int k = 0; k < 7; k++)
                        {
                            out.writeBytes("<Value>" + ((BPTT) fromNetwork).trainingInputSeq[i][j][k] + "</Value>");
                        }
                        if (encodeDerivative)
                        {
                            for (int k = 0; k < 7; k++)
                            {
                                out.writeBytes("<Value>");
                                if (j == 0)
                                    out.writeBytes("0.5");
                                else
                                    out.writeBytes("" + (((((BPTT) fromNetwork).trainingInputSeq[i][j][k] - old[k]) / 2) + .5));
                                old[k] = ((BPTT) fromNetwork).trainingInputSeq[i][j][k];
                                out.writeBytes("</Value>");
                            }
                        }
                        out.writeBytes("</InputPattern>\n");
                        out.writeBytes("<TeacherPattern>");
                        for (int k = 0; k < ((BPTT) fromNetwork).outputLayerDim; k++)
                        {
                            out.writeBytes("<Value>" + ((BPTT) fromNetwork).trainingOutputSeq[i][j][k] + "</Value>");
                        }
                        out.writeBytes("</TeacherPattern>\n");
                        out.writeBytes("</Pattern>\n");
                    }
                }
            }
        }
        out.writeBytes("</Patterns>\n");
        out.writeBytes("</PatternSet>\n");
    }

    /**
     * Reads network weights from a file
     *
     * @param fn - filename
     * @return - successful
     */
    public boolean readWeight(final String fn)
    {
        fileInputToHiddenW = null;
        fileHiddenToOutputW = null;
        fileTeacherToOutputW = null;
        fileInputLayerDim = 0;
        fileHiddenLayerDim = 0;
        fileTeacherDim = 0;
        fileOutputLayerDim = 0;

        int tc, linec, row = 0;
        String s, u;
        boolean added;
        try
        {
            final BufferedReader in;

            in = new BufferedReader(new InputStreamReader(Elib.openfileREAD(fn)));
            if (!in.ready())
                return false;
            linec = 0;
            while (null != (s = in.readLine()))
            {
                linec++;
                if (s.length() == 0)
                    continue;
                if (s.charAt(0) == '#')
                    continue;
                final StringTokenizer st = new StringTokenizer(s, " ");
                tc = 0;
                added = false;
                while (st.hasMoreTokens())
                {
                    u = st.nextToken();
                    if (tc == 0 && u.equals("inputLayerDim"))
                    {
                        u = st.nextToken();
                        fileInputLayerDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("hiddenLayerDim"))
                    {
                        u = st.nextToken();
                        fileHiddenLayerDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("outputLayerDim"))
                    {
                        u = st.nextToken();
                        fileOutputLayerDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("teacherdim"))
                    {
                        u = st.nextToken();
                        fileTeacherDim = Elib.toInt(u);
                        continue;
                    }
                    if (fileInputLayerDim == 0 || fileHiddenLayerDim == 0 || fileTeacherDim == 0 || fileOutputLayerDim == 0)
                    {
                        Log.println("The weight file doesn't specify the net size properly!");
                    }
                    if (fileInputToHiddenW == null)
                        fileInputToHiddenW = new double[fileHiddenLayerDim][fileInputLayerDim];
                    if (fileHiddenToOutputW == null)
                        fileHiddenToOutputW = new double[fileOutputLayerDim][fileHiddenLayerDim];
                    if (fileTeacherToOutputW == null)
                        fileTeacherToOutputW = new double[fileOutputLayerDim][fileTeacherDim];
                    //Log.println("row,tc:"+row+","+tc+" = "+u);
                    if (row < fileHiddenLayerDim)
                        fileInputToHiddenW[row][tc] = Elib.toDouble(u);
                    else if (row < fileHiddenLayerDim + fileOutputLayerDim)
                        fileHiddenToOutputW[row - fileHiddenLayerDim][tc] = Elib.toDouble(u);
                    else if (row < fileHiddenLayerDim + fileOutputLayerDim + fileOutputLayerDim)
                        fileTeacherToOutputW[row - fileHiddenLayerDim - fileOutputLayerDim][tc] = Elib.toDouble(u);
                    added = true;
                    tc++;
                }
                if (!added)
                    continue;
                if (tc != ((row < fileHiddenLayerDim) ? fileInputLayerDim :
                           ((row < fileHiddenLayerDim + fileOutputLayerDim) ? fileHiddenLayerDim :
                            ((row < fileHiddenLayerDim + fileOutputLayerDim + fileOutputLayerDim) ? fileTeacherDim : 0))))
                {
                    Log.println("File format Error in " + fn + " line " + linec);
                    return false;
                }
                row++;
            }
            in.close();
        }
        catch (FileNotFoundException e)
        {
            return false;
        }
        catch (IOException e)
        {
            Log.println("Hebbian.readWeight() : EXCEPTION " + e);
            return false;
        }

        return true;
    }

    /**
     * Write network weights to a file
     *
     * @param fn - filename
     */
    public void writeWeight(final String fn)
    {
        //System.out.println("Creating weight file:"+fn);

        try
        {
            final DataOutputStream out = Elib.openfileWRITE(fn);
            out.writeBytes("# This weight file is generated by Hebbian\n");
            out.writeBytes("# This file specfies the network size and the weight values\n\n");
            out.writeBytes("# Note: To train the network you need to load a pattern file\n");
            out.writeBytes("# Note: You can not specify learning parameters from this file\n");
            out.writeBytes("# Note: If you want to continue a learning session that you saved the\n");
            out.writeBytes("# weights from, use Make Network from Weight followed by Load Pattern then continue training.\n\n");
            out.writeBytes("# First matrix is the input(x)->hidden(y) weights(inputToHiddenW)\n");
            out.writeBytes("# Second matrix is the hidden(x)->output(y) weights(hiddenToOutputW)\n");
            out.writeBytes("# Third matrix is the teacher(y)->output(z) weights(teacherToOutputW)\n");

            out.writeBytes("outputLayerDim  " + outputLayerDim + "\nhiddenLayerDim  " + hiddenLayerDim + "\nteacherdim  " + teacherDim + "\ninputLayerDim   " + inputLayerDim + "\n\n");
            out.writeBytes("#input  -> hidden weights  inputToHiddenW[" + (hiddenLayerDim) + "][" + (inputLayerDim) + "]\n");
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                for (int j = 0; j < inputLayerDim; j++)
                    out.writeBytes(inputToHiddenW[i][j] + " ");
                out.writeBytes("\n");
            }

            out.writeBytes("#hidden  -> output weights  hiddenToOutputW[" + (outputLayerDim) + "][" + (hiddenLayerDim) + "]\n");
            for (int i = 0; i < outputLayerDim; i++)
            {
                for (int j = 0; j < hiddenLayerDim; j++)
                    out.writeBytes(hiddenToOutputW[i][j] + " ");
                out.writeBytes("\n");
            }

            out.writeBytes("\n#teacher -> output weights  teacherToOutputW[" + (outputLayerDim) + "][" + (teacherDim) + "]:\n");
            for (int k = 0; k < outputLayerDim; k++)
            {
                for (int i = 0; i < teacherDim; i++)
                    out.writeBytes(teacherToOutputW[k][i] + " ");
                out.writeBytes("\n");
            }
            out.close();
        }
        catch (IOException e)
        {
            Log.println("writeWeight() : EXCEPTION " + e);
        }
    }

// -------------------------------------------------------------------
// AWT events

    public void mouseClicked(final MouseEvent e)
    {
    }

    public void mousePressed(final MouseEvent e)
    {
        final int x = e.getX();
        final int y = e.getY();
        final Point pp = panel.which(x, y);
        if (pp != null)
        {
            /*if (pp.x==0)
                System.out.println("input unit "+pp.y+ " selected.");
            if (pp.x==1)
                System.out.println("hidden unit "+pp.y+ " selected.");
            if (pp.x==2)
                System.out.println("output unit "+pp.y+ " selected.");*/
            panel.showThis(pp);
        }
        else
        {
            //System.out.println("All network connections is shown.");
            panel.showAll();
        }
    }

    public void stateChanged(final ChangeEvent e)
    {
        if (e.getSource().equals(wsc))
        {
            panel.wscValue = wsc.getValue();
            panel.repaint();
            networkPanel.repaint();
        }
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