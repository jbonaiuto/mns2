package mns2.comp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.*;
import javax.swing.event.ChangeEvent;

import mns2.graphics.BPTTPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import sim.util.*;
import sim.motor.Graspable;

/**
 * Back-propagation through time
 *
 * @author James Bonaiuto (bonaiuto@usc.edu) 2005
 */
public class BPTT extends BP
{
    // number of time steps to unravel the network during BPTT
    public int L = 44;

    // training input patterns for each time step of each sequence in the training set
    public double trainingInputSeq[][][];
    // target training output patterns for each time step of each sequence in the training set
    public double trainingOutputSeq[][][];
    // the length of each sequence in the training set
    public int trainingSeqLength[];

    // the number of recurrent input units
    public int recurrentInputDim;
    // the number of   Proj recurrent output units
    public int recurrentOutputDim;

    // the weights of recurrent output to recurrent input connections
    public double recurrentOutputToInputW[][];

    // output layer error
    public double outputErrorGradientHistory[][];
    // hidden layer error
    public double hiddenErrorGradientHistory[][];
    // recurrent input error
    public double recurrentInputErrorGradientHistory[][];

    // Weight file layer dimensions
    protected int fileRecurrentInputDim, fileRecurrentOutputDim;
    // Weight file recurrent output to input layer weights
    protected double[][] fileRecurrentOutputToInputW;

    // Pattern file layer dimensions
    public int pat_rindim = 0, pat_routdim = 0;
    // Pattern file learning parameters
    public int correctTimeSteps = 0, correctSequences = 0;
    protected double maxErr;

    public JTextField pattern_filename, weight_filename, epochs, training_threshold;
    public JLabel patternFilenameLabel, weightFileLabel, trainingEpochsLabel, successThresholdLabel;
    JButton loadPattern, loadWeight, train, test, randomizeWeights, makeNetworkFromPattern, makeNetworkFromWeight,
            generateWeightFile, quit;
    public JSlider wsc;
    JPanel mainPanel, centerPanel, networkPanel, bottomPanel, topPanel;
    private BPTTPanel panel;

    /**
     * Constructor
     */
    public BPTT()
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

        setTitle("Back Propagation Through Time - James Bonaiuto Aug'05");
        pattern_filename.setText("action_pattern_bptt.xml");
        weight_filename.setText("jjb_bptt.wgt");
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

    /**
     * Constructor
     *
     * @param s - pattern file name
     */
    public BPTT(final String s)
    {
        this(s, 0.05, 0.9, 0.01, 0.1);
    }

    /**
     * Constructor
     *
     * @param s           - pattern file name
     * @param def_eta     - bptt learning rate
     * @param def_beta    - bptt learning momentum
     * @param def_etaUP   - delta bptt learning rate up coeff
     * @param def_etaDOWN - delta bptt learning rate down coeff
     */
    private BPTT(final String s, final double def_eta, final double def_beta, final double def_etaUP,
                 final double def_etaDOWN)
    {
        this();

        // Set parameters
        beta = def_beta;
        eta = def_eta;
        etaUP = def_etaUP;
        etaDOWN = def_etaDOWN;

        //Create network from pattern file
        //System.out.println("Constructing the network with file:"+s);
        netFromPattern(s);
    }

    /**
     * Sets up the network display panel
     */
    public void setupCanvas()
    {
        panel = new BPTTPanel(this);
        panel.addMouseListener(this);
        panel.wscValue = wsc.getValue();
        networkPanel.add(panel);
    }

    /**
     * Create network
     *
     * @param extInDim - number of input units
     * @param hiddim   - number of hidden units = input units/2 + 1 if set to -1
     * @param outdim   - number of output units
     * @param rindim   - number of recurrent input units
     * @param routdim  - number of recurrent output units
     * @param Lsteps   - number of time steps to unravel the network
     */
    public final void createNet(final int extInDim, final int hiddim, final int outdim, final int rindim, final int routdim,
                                final int Lsteps)
    {
        L = Lsteps;

        // Initialize network layer dimensions
        inputLayerDim = extInDim;
        hiddenLayerDim = hiddim;
        outputLayerDim = outdim;
        recurrentInputDim = rindim;
        recurrentOutputDim = routdim;

        // Initialize network layers
        inputLayer = new double[inputLayerDim + recurrentInputDim];
        inputLayerNet = new double[inputLayerDim + recurrentInputDim];
        hiddenLayer = new double[hiddenLayerDim];
        hiddenLayerNet = new double[hiddenLayerDim];
        outputLayer = new double[outputLayerDim + recurrentOutputDim];
        outputLayerNet = new double[outputLayerDim + recurrentOutputDim];

        // Initialize network layer weights
        inputToHiddenW = new double[hiddenLayerDim][inputLayerDim + recurrentInputDim];
        hiddenToOutputW = new double[outputLayerDim + recurrentOutputDim][hiddenLayerDim];
        recurrentOutputToInputW = new double[recurrentInputDim][recurrentOutputDim];
        clearHistory();
        initNet();

        validNet = true;
    }

    /**
     * Create network from pattern file
     *
     * @param patternFilename - pattern filename
     */
    public void netFromPattern(final String patternFilename)
    {
        panel.showAll();
        patc = 0;

        // Read pattern file
        if (!readPattern(patternFilename))
        {
            Log.println("Error occured reading pattern file:" + patternFilename);
            return;
        }

        // Create the network
        createNet(pat_indim, pat_hiddim, pat_outdim, pat_rindim, pat_routdim, 30);
    }

    /**
     * Create network from weight file
     *
     * @param weightFilename - weight file
     */
    public void netFromWeight(final String weightFilename)
    {
        //panel.showAll();

        // read weight file
        if (!readWeight(weightFilename))
        {
            Log.println("Error occured reading weight file:" + weightFilename);
            return;
        }

        //create the network
        createNet(fileInputLayerDim, fileHiddenLayerDim, fileOutputLayerDim, fileRecurrentInputDim,
                  fileRecurrentOutputDim, L);

        //Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim + fileRecurrentInputDim);

        //Set hidden to output layer weights
        for (int k = 0; k < fileOutputLayerDim + fileRecurrentOutputDim; k++)
            System.arraycopy(fileHiddenToOutputW[k], 0, hiddenToOutputW[k], 0, fileHiddenLayerDim);

        //Set recurrent output to input layer weights
        for (int l = 0; l < fileRecurrentInputDim; l++)
            System.arraycopy(fileRecurrentOutputToInputW[l], 0, recurrentOutputToInputW[l], 0, fileRecurrentOutputDim);
    }

    /**
     * Initialize weights from file - network must already be created
     *
     * @param weightFilename - weight filename
     */
    public void installWeight(final String weightFilename)
    {
        super.installWeight(weightFilename);

        if (fileRecurrentOutputToInputW == null)
        {
            Log.println("Error occured reading weight file:" + weightFilename);
            return;
        }

        if (fileRecurrentInputDim != recurrentInputDim)
        {
            Log.println("Mismatch in recurrent input dimension!");
            return;
        }
        if (fileRecurrentOutputDim != recurrentOutputDim)
        {
            Log.println("Mismatch in recurrent output dimension!");
            return;
        }

        // Set hidden to output layer weights
        for (int k = 0; k < fileOutputLayerDim + fileRecurrentOutputDim; k++)
        {
            System.arraycopy(fileHiddenToOutputW[k], 0, hiddenToOutputW[k], 0, fileHiddenLayerDim);
        }

        //Set recurrent output to input layer weights
        for (int l = 0; l < fileRecurrentInputDim; l++)
        {
            System.arraycopy(fileRecurrentOutputToInputW[l], 0, recurrentOutputToInputW[l], 0, fileRecurrentOutputDim);
        }
    }

    /**
     * Initialize network weights randomly
     */
    public void initNet()
    {
        totalit = 0;
        for (int i = 0; i < hiddenLayerDim; i++)
        {
            for (int j = 0; j < inputLayerDim + recurrentInputDim; j++)
            {
                inputToHiddenW[i][j] = (Math.random() - 0.5) * 0.1;
            }
        }

        for (int k = 0; k < outputLayerDim + recurrentOutputDim; k++)
        {
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                hiddenToOutputW[k][i] = (Math.random() - 0.5) * 0.1;
            }
        }

        for (int k = 0; k < recurrentInputDim; k++)
        {
            for (int i = 0; i < recurrentOutputDim; i++)
            {
                recurrentOutputToInputW[k][i] = (Math.random() - 0.5) * 0.1;
            }
        }
    }

    /**
     * Clear weight change history
     */
    public void clearHistory()
    {
        inputLayerHistory = new double[MAX_seqLength][inputLayerDim + recurrentInputDim];
        hiddenLayerHistory = new double[MAX_seqLength][hiddenLayerDim];
        outputLayerHistory = new double[MAX_seqLength][outputLayerDim + recurrentOutputDim];

        hiddenErrorGradientHistory = new double[MAX_seqLength][hiddenLayerDim];   //  error at the hidden layer
        outputErrorGradientHistory = new double[MAX_seqLength][outputLayerDim + recurrentOutputDim];     //  error at the output layer
        recurrentInputErrorGradientHistory = new double[MAX_seqLength][recurrentInputDim];
    }

    /**
     * Run network forward
     */
    public void forward(final boolean plot, final boolean staticAction)
    {
        boolean lesionRecurrent = false;
        if (lesionedConnection.contains("recurrent"))
        {
            String time = lesionTime.get(lesionedConnection.indexOf("recurrent")).toString();
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) || (!staticAction &&
                                                                                              time.equals("duringGrasp")))
                lesionRecurrent = true;
        }

        boolean lesionInputToHidden = false;
        if (lesionedConnection.contains("inputToHidden"))
        {
            String time = lesionTime.get(lesionedConnection.indexOf("inputToHidden")).toString();
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) ||
                (!staticAction && time.equals("duringGrasp")))
                lesionInputToHidden = true;
        }

        if (t > 0)
        {
            // get the activity of the recurrent output units of the previous time step
            final double[] recurrentOut = new double[recurrentOutputDim];
            System.arraycopy(outputLayerHistory[t - 1], outputLayerDim, recurrentOut, 0, recurrentOutputDim);

            // calculate recurrent input
            final double[] recurrentIn = new double[recurrentInputDim];
            final double[] recurrentInNet = new double[recurrentInputDim];

            if (!lesionRecurrent)
            {
                // multiply output layer by recurrent output to input layer weights and put results in recurrentInNet
                VA.multiply(recurrentOutputToInputW, recurrentOut, recurrentInputDim, recurrentOutputDim, recurrentInNet);
            }

            // push recurrentInNet through sigmoid function and put result in recurrentIn
            VA.squash(recurrentInNet, recurrentInputDim, recurrentIn);

            // transfer recurrent input unit values to the input layer
            System.arraycopy(recurrentIn, 0, inputLayer, inputLayerDim, recurrentInputDim);
            System.arraycopy(recurrentInNet, 0, inputLayerNet, inputLayerDim, recurrentInputDim);

            if (!lesionInputToHidden)
            {
                // multiply input layer by input to hidden layer weights and put results in hiddenLayerNet
                VA.multiply(inputToHiddenW, inputLayer, hiddenLayerDim, inputLayerDim + recurrentInputDim, hiddenLayerNet);
            }
        }
        else if (!lesionInputToHidden)
            // multiply input layer by input to hidden layer weights and put results in hiddenLayerNet
            VA.multiply(inputToHiddenW, inputLayer, hiddenLayerDim, inputLayerDim, hiddenLayerNet);

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
            VA.multiply(hiddenToOutputW, hiddenLayer, outputLayerDim + recurrentOutputDim, hiddenLayerDim, outputLayerNet);
        }
        // push outputLayerNet through sigmoid function and put result in output layer
        VA.squash(outputLayerNet, outputLayerDim + recurrentOutputDim, outputLayer);

        // transfer current network state to history
        System.arraycopy(inputLayer, 0, inputLayerHistory[t], 0, inputLayerDim + recurrentInputDim);
        System.arraycopy(hiddenLayer, 0, hiddenLayerHistory[t], 0, hiddenLayerDim);
        System.arraycopy(outputLayer, 0, outputLayerHistory[t], 0, outputLayerDim + recurrentOutputDim);

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
        if (Resource.getString("gnuplotExecutable") != null && Resource.getString("gnuplotExecutable").trim().length() > 0)
        {
            String frameString = '_' + FrameUtils.getFrameString(t + 1);
            for (int j = 0; j < plots.length; j++)
            {
                if (plots[j].equals("recurrentInput"))
                {
                    final double recurrentInputHistory[][] = new double[t + 1][recurrentInputDim];
                    for (int i = 1; i < t + 1; i++)
                        System.arraycopy(inputLayerHistory[i], inputLayerDim, recurrentInputHistory[i], 0,
                                         recurrentInputDim);
                    Gplot.plot(recurrentInputHistory, t + 1, recurrentInputDim, plot_extra_command[j], plot_dimensions[j],
                               plot_labels[j], plot_output_files[j] + frameString);
                }
                else if (plots[j].equals("recurrentOutput"))
                {
                    final double recurrentOutputHistory[][] = new double[t + 1][recurrentOutputDim];
                    for (int i = 0; i < t + 1; i++)
                        System.arraycopy(outputLayerHistory[i], outputLayerDim, recurrentOutputHistory[i], 0,
                                         recurrentOutputDim);
                    Gplot.plot(recurrentOutputHistory, t + 1, recurrentOutputDim, plot_extra_command[j], plot_dimensions[j],
                               plot_labels[j], plot_output_files[j] + frameString);
                }
            }
        }
    }

    /**
     * Set input units to pattern p
     *
     * @param p - pattern index
     */
    public void presentPattern(final int p)
    {
        // Set input
        System.arraycopy(trainingInputSeq[p][t], 0, inputLayer, 0, inputLayerDim);
    }

    /**
     * Get output for given input
     *
     * @param inp  - input pattern
     * @param size - input pattern size - must match input layer size
     * @param plot - whether or not to plot the network activity
     * @return - network output
     */
    public double[] ask(final double[] inp, final int size, final boolean plot, final boolean staticAction)
    {
        // Check size of input
        if (size != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
        }

        // present unput pattern
        presentPattern(inp);
        // run the network forward one time step
        forward(plot, staticAction);

        // return the network's external output
        final double[] externalOut = new double[outputLayerDim];
        System.arraycopy(outputLayer, 0, externalOut, 0, outputLayerDim);
        return externalOut;
    }

    /**
     * Set input units to pattern
     *
     * @param input - input pattern
     */
    public void presentPattern(final double[] input)
    {
        // Set input
        System.arraycopy(input, 0, inputLayer, 0, inputLayerDim);
    }

    /**
     * Learn sequence p
     *
     * @param p - sequence index
     * @return - output error averaged over runs
     */
    public double learn(final int p)
    {
        // Total error over all runs
        double err = 0;
        t = 0;
        // Try unrolling the network for the whole length of this sequence
        L = trainingSeqLength[p];
        // run the sequence for L time steps or until sequence is finished
        for (int i = 0; i < L; i++)
        {
            // present input pattern for this time step of the sequence
            presentPattern(p);
            // Calc forward net dynamics
            forward(false, false);
        }

        final double[][] inputToHiddenTotalDW = new double[hiddenLayerDim][inputLayerDim + recurrentInputDim];
        final double[][] hiddenToOutputTotalDW = new double[outputLayerDim + recurrentOutputDim][hiddenLayerDim];
        final double[][] recurrentOutputToInputTotalDW = new double[recurrentInputDim][recurrentOutputDim];

        // Run BP through the unraveled network
        for (int j = L - 1; j > -1; j--)
        {
            //Calculate external output layer error
            err = getOutputLayerError(p, j, err);

            //Calculate recurrent output error if not at the end of the sequence
            calculateRecurrentOutputLayerError(j);

            //Calculate hidden layer error
            calculateHiddenLayerError(j);

            //Calculate recurrent input error
            calculateRecurrentInputLayerError(j);

            // Calculate change in external and recurrent input to hidden layer weights
            calculateDW(j, inputToHiddenTotalDW, hiddenToOutputTotalDW, recurrentOutputToInputTotalDW);
        }
        //Find average weight changes
        VA.divideBy(inputToHiddenTotalDW, hiddenLayerDim, inputLayerDim + recurrentInputDim, L);
        VA.divideBy(hiddenToOutputTotalDW, outputLayerDim + recurrentOutputDim, hiddenLayerDim, L);
        VA.divideBy(recurrentOutputToInputTotalDW, recurrentInputDim, recurrentOutputDim, L - 1);

        // Add average weight change to weights
        VA.addto(inputToHiddenW, hiddenLayerDim, inputLayerDim + recurrentInputDim, inputToHiddenTotalDW);
        VA.addto(hiddenToOutputW, outputLayerDim + recurrentOutputDim, hiddenLayerDim, hiddenToOutputTotalDW);
        VA.addto(recurrentOutputToInputW, recurrentInputDim, recurrentOutputDim, recurrentOutputToInputTotalDW);

        return err;
    }

    private void calculateDW(int timeStepIndex, double[][] inputToHiddenTotalDW, double[][] hiddenToOutputTotalDW, double[][] recurrentOutputToInputTotalDW)
    {
        for (int i = 0; i < hiddenLayerDim; i++)
        {
            for (int k = 0; k < inputLayerDim + recurrentInputDim; k++)
                inputToHiddenTotalDW[i][k] += eta * hiddenErrorGradientHistory[timeStepIndex][i] * inputLayerHistory[timeStepIndex][k];
        }

        // Calculate change in hidden to output layer weights
        for (int k = 0; k < outputLayerDim + recurrentOutputDim; k++)
        {
            for (int i = 0; i < hiddenLayerDim; i++)
                hiddenToOutputTotalDW[k][i] += eta * outputErrorGradientHistory[timeStepIndex][k] * hiddenLayerHistory[timeStepIndex][i];
        }

        // if not at the end of the sequence
        if (timeStepIndex < L - 1)
        {
            // Calculate change in recurrent output to input layer weights
            for (int i = 0; i < recurrentInputDim; i++)
            {
                for (int k = 0; k < recurrentOutputDim; k++)
                {
                    recurrentOutputToInputTotalDW[i][k] += eta * recurrentInputErrorGradientHistory[timeStepIndex + 1][i] * outputLayerHistory[timeStepIndex][outputLayerDim + k];
                }
            }
        }
    }

    private void calculateRecurrentInputLayerError(int timeStepIndex)
    {
        for (int i = 0; i < recurrentInputDim; i++)
        {
            recurrentInputErrorGradientHistory[timeStepIndex][i] = 0;
            for (int l = 0; l < hiddenLayerDim; l++)
                //each recurrentInputLayer[i]'s error share of hidden error
                recurrentInputErrorGradientHistory[timeStepIndex][i] += inputToHiddenW[l][i + inputLayerDim] *
                                                                        hiddenErrorGradientHistory[timeStepIndex][l];
            // this part equals G'(recurrentInputErrorGradient) for b=0.5
            recurrentInputErrorGradientHistory[timeStepIndex][i] *= (1 - inputLayerHistory[timeStepIndex][i + inputLayerDim]) *
                                                                    inputLayerHistory[timeStepIndex][i + inputLayerDim];
        }
    }

    private void calculateHiddenLayerError(int timeStepIndex)
    {
        for (int i = 0; i < hiddenLayerDim; i++)
        {
            hiddenErrorGradientHistory[timeStepIndex][i] = 0;
            for (int k = 0; k < outputLayerDim + recurrentOutputDim; k++)
                //each hiddenLayer[i]'s error share of output error
                hiddenErrorGradientHistory[timeStepIndex][i] += hiddenToOutputW[k][i] * outputErrorGradientHistory[timeStepIndex][k];
            // this part equals G'(hiddenErrorGradient) for b=0.5
            hiddenErrorGradientHistory[timeStepIndex][i] *= (1 - hiddenLayerHistory[timeStepIndex][i]) * hiddenLayerHistory[timeStepIndex][i];
        }
    }

    private void calculateRecurrentOutputLayerError(int timeStepIndex)
    {
        for (int k = 0; k < recurrentOutputDim; k++)
        {
            outputErrorGradientHistory[timeStepIndex][k + outputLayerDim] = 0;
            if (timeStepIndex < L - 1)
            {
                for (int i = 0; i < recurrentInputDim; i++)
                    //each recurrentOutputLayer[k]'s error share of recurrent input error
                    outputErrorGradientHistory[timeStepIndex][k + outputLayerDim] += recurrentOutputToInputW[i][k] *
                                                                                     recurrentInputErrorGradientHistory[timeStepIndex + 1][i];
                // this part equals G'(recurrentOutputErrorGradient) for b=0.5
                outputErrorGradientHistory[timeStepIndex][k + outputLayerDim] *= (1 - outputLayerHistory[timeStepIndex][outputLayerDim + k]) *
                                                                                 outputLayerHistory[timeStepIndex][outputLayerDim + k];
            }
        }
    }

    private double getOutputLayerError(int patternIndex, int j, double err)
    {
        for (int k = 0; k < outputLayerDim; k++)
        {
            // difference between desired output and produced output
            final double ee = (trainingOutputSeq[patternIndex][j][k] - outputLayerHistory[j][k]);
            // this part equals G'(outputLayerNet) for b=0.5
            outputErrorGradientHistory[j][k] = ee * (1 - outputLayerHistory[j][k]) * outputLayerHistory[j][k];

            err += ee * ee;
        }
        return err;
    }

    /**
     * Train network until maxiter or error is less than .001
     *
     * @param maxiter         - maximum times to train
     * @param training_thresh - quit when this percentage of training patterns is correctly identified
     * @param verbose         - whether or not to output debug info
     */
    public final void train(final int maxiter, final double training_thresh, final boolean verbose)
    {
        final double eta_original = eta;
        final Vector<Double> errHistory = new Vector<Double>(100000);
        final Vector<Double> maxErrHistory = new Vector<Double>(100000);
        final Vector<Double> correctHistory = new Vector<Double>(100000);
        double err = 0, olderr, dE = 0;
        // countdown until changing learning rate - every 20 iterations
        int avc = 0;
        // pattern index
        int p;

        if (pat_indim != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
            return;
        }
        if (pat_outdim != outputLayerDim)
        {
            Log.println("Pattern output dim does not match network output dim!!");
            return;
        }
        final int testSet[] = new int[patc];
        for (int i = 0; i < patc; i++)
            testSet[i] = i;
        // if maxiter is less than zero, keep going until min error, otherwise train for maxiter iterations
        for (int it = 0; (maxiter < 1 || it < maxiter); it++)
        {
            totalit++;

            // Choose a random pattern from the list
            p = (int) (Math.random() * patc);

            // Clear history
            clearHistory();
            // Learn pattern and get error
            // old error
            olderr = err;
            err = learn(p);
            // direction of change in error
            dE += err - olderr;

            // Time to change the learning rate?
            avc++;
            if (avc == 20 * patc)
            {
                // If the error is decreasing
                if (dE < 0)
                {
                    // increase the learning rate
                    eta += etaUP;
                }
                // If the error is staying the same or increasing
                else
                {
                    // decrease the learning rate
                    eta -= etaDOWN * eta;
                }
                // reset countdown and direction of change in error
                avc = 0;
                dE = 0;
            }

            // check if error is small enough to stop train
            if (it % 100 == 0)
            {
                errHistory.add((double) testPattern(verbose, testSet));
                maxErrHistory.add((double) maxErr);
                correctHistory.add((double) correctSequences);
                if (errHistory.get(it / 100) < 0.001)
                {
                    Log.println("At " + it + " iterations, error is less than 0.001, stopping training.");
                    break;
                }
                else if (correctSequences >= (training_thresh * patc))
                    break;
            }
        }
        errHistory.add(testPattern(true, testSet));
        maxErrHistory.add(maxErr);
        correctHistory.add((double) correctSequences);
        if (verbose)
        {
            final double errHistoryA[] = new double[errHistory.size()];
            final double maxErrHistoryA[] = new double[maxErrHistory.size()];
            final double correctHistoryA[] = new double[correctHistory.size()];
            for (int i = 0; i < errHistory.size(); i++)
            {
                errHistoryA[i] = errHistory.get(i);
                maxErrHistoryA[i] = maxErrHistory.get(i);
                correctHistoryA[i] = correctHistory.get(i);
            }
            plotTrainingActivity(errHistoryA, eta_original, maxiter, maxErrHistoryA, correctHistoryA);
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

    /**
     * Test all patterns in the test set
     *
     * @param verbose
     * @return - total output error over all patterns
     */
    public double testPattern(final boolean verbose, final int testSet[])
    {
        // Total error
        double err = 0;
        // Total correct output
        correctSequences = 0;
        correctTimeSteps = 0;

        if (pat_indim != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
            return -1;
        }
        if (pat_outdim != outputLayerDim)
        {
            Log.println("Pattern output dim does not match network output dim!!");
            return -1;
        }

        // Max output unit error
        maxErr = -1;

        // For all patterns
        for (final int p : testSet)
        {
            double patt_err = 0.0;
            t = 0;
            while (t < trainingSeqLength[p])
            {
                // Input pattern and compute network output
                presentPattern(p);
                forward(false, false);

                // Number of units with correct output
                int oo = 0;
                // Output layer error
                double thiserr = 0;

                // Compute error for all output units
                for (int k = 0; k < outputLayerDim; k++)
                {
                    // error squared for this unit
                    final double ee = (trainingOutputSeq[p][t - 1][k] - outputLayer[k]) * (trainingOutputSeq[p][t - 1][k] - outputLayer[k]);

                    // if error small enough, output is correct
                    if (ee < 0.05)
                        oo++;
                    // update max output unit error
                    if ((ee * .5) > maxErr && t == trainingSeqLength[p])
                        maxErr = ee * .5;
                    // update layer error
                    thiserr += ee * .5;
                }

                // accumulate error
                patt_err += thiserr;

                // If all units have correct output, increment number of correct outputs
                if (oo == outputLayerDim)
                {
                    correctTimeSteps++;
                    if (t == trainingSeqLength[p])
                        correctSequences++;
                }
            }
            err += patt_err;
        }
        final String rep = totalit + ":Total error over patterns:" + err + " # Correct time steps:" + correctTimeSteps + '/' +
                           (testSet.length * trainingSeqLength[0]) + " # Correct sequences:" + correctSequences + '/' + testSet.length +
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
     * @param patternFilename - pattern filename
     * @return - successful
     */
    public boolean readPattern(final String patternFilename)
    {
        pat_indim = 0;
        pat_outdim = 0;
        pat_hiddim = 0;
        pat_rindim = 0;
        pat_routdim = 0;
        pat_eta = -1;
        pat_beta = -1;
        pat_etaUP = -1;
        pat_etaDOWN = -1;

        try
        {
            final DataInputStream in;
            in = Elib.openfileREAD(patternFilename);
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
                        else if (setting.getNodeName().equals("InputDim"))
                            pat_indim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("RecurrentInputDim"))
                            pat_rindim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("RecurrentOutputDim"))
                            pat_routdim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
                    }
                }
                else if (rootChild.getNodeName().equals("OptionalNetworkSettings"))
                {
                    final NodeList optionalNetworkSettings = rootChild.getChildNodes();
                    for (int j = 0; j < optionalNetworkSettings.getLength(); j++)
                    {
                        final Node setting = optionalNetworkSettings.item(j);
                        if (setting.getNodeName().equals("BPTTLearningRate"))
                            pat_eta = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("Momentum"))
                            pat_beta = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("LearningIncrease"))
                            pat_etaUP = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("LearningDecrease"))
                            pat_etaDOWN = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                    }
                }
                else if (rootChild.getNodeName().equals("Sequences"))
                {
                    final NodeList sequences = rootChild.getChildNodes();
                    patc = 0;
                    for (int j = 0; j < sequences.getLength(); j++)
                    {
                        if (sequences.item(j).getNodeName().equals("Sequence"))
                            patc++;
                    }
                    trainingInputSeq = new double[patc][MAX_seqLength][pat_indim];
                    trainingOutputSeq = new double[patc][MAX_seqLength][pat_outdim];
                    trainingSeqLength = new int[patc];

                    int seqIdx = 0;
                    for (int j = 0; j < sequences.getLength(); j++)
                    {
                        if (sequences.item(j).getNodeName().equals("Sequence"))
                        {
                            final NodeList sequenceNodes = sequences.item(j).getChildNodes();
                            for (int k = 0; k < sequenceNodes.getLength(); k++)
                            {
                                final Node sequenceNode = sequenceNodes.item(k);
                                if (sequenceNode.getNodeName().equals("SequenceLength"))
                                    trainingSeqLength[seqIdx] = Integer.parseInt(sequenceNode.getChildNodes().item(0).getNodeValue());
                                else if (sequenceNode.getNodeName().equals("InputSequence"))
                                {
                                    final NodeList inputs = sequenceNode.getChildNodes();
                                    int inpIdx = 0;
                                    for (int l = 0; l < inputs.getLength(); l++)
                                    {
                                        final Node input = inputs.item(l);
                                        if (input.getNodeName().equals("Input"))
                                        {
                                            final NodeList values = input.getChildNodes();
                                            int valIdx = 0;
                                            for (int m = 0; m < values.getLength(); m++)
                                            {
                                                final Node value = values.item(m);
                                                if (value.getNodeName().equals("Value"))
                                                {
                                                    trainingInputSeq[seqIdx][inpIdx][valIdx++] = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                                    if (valIdx >= pat_indim)
                                                        break;
                                                }
                                            }
                                            inpIdx++;
                                        }
                                    }
                                }
                                else if (sequenceNode.getNodeName().equals("OutputSequence"))
                                {
                                    final NodeList outputs = sequenceNode.getChildNodes();
                                    int outIdx = 0;
                                    for (int l = 0; l < outputs.getLength(); l++)
                                    {
                                        final Node output = outputs.item(l);
                                        if (output.getNodeName().equals("Output"))
                                        {
                                            final NodeList values = output.getChildNodes();
                                            int valIdx = 0;
                                            for (int m = 0; m < values.getLength(); m++)
                                            {
                                                final Node value = values.item(m);
                                                if (value.getNodeName().equals("Value"))
                                                {
                                                    trainingOutputSeq[seqIdx][outIdx][valIdx++] = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                                    if (valIdx >= pat_outdim)
                                                        break;
                                                }
                                            }
                                            outIdx++;
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
            Log.println("BP.readPattern() : EXCEPTION " + e);
            return false;
        }
        // Read learning rate
        if (pat_beta > -1)
            beta = pat_beta;
        if (pat_eta > -1)
            eta = pat_eta;
        if (pat_etaUP > -1)
            etaUP = pat_etaUP;
        if (pat_etaDOWN > -1)
            etaDOWN = pat_etaDOWN;
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
        out.writeBytes("<PatternSet>\n");
        out.writeBytes("<RequiredNetworkSettings>\n");
        out.writeBytes("<OutputDim>" + fromNetwork.outputLayerDim + "</OutputDim>\n");
        out.writeBytes("<HiddenDim>" + fromNetwork.hiddenLayerDim + "</HiddenDim>\n");
        out.writeBytes("<InputDim>" + params.length + "</InputDim>\n");
        if (fromNetworkType.startsWith("BPTT"))
        {
            out.writeBytes("<RecurrentInputDim>" + ((BPTT) fromNetwork).recurrentInputDim + "</RecurrentInputDim>\n");
            out.writeBytes("<RecurrentOutputDim>" + ((BPTT) fromNetwork).recurrentOutputDim + "</RecurrentOutputDim>\n");
        }
        else
        {
            out.writeBytes("<RecurrentInputDim>" + 5 + "</RecurrentInputDim>\n");
            out.writeBytes("<RecurrentOutputDim>" + 5 + "</RecurrentOutputDim>\n");
        }
        out.writeBytes("</RequiredNetworkSettings>\n");
        out.writeBytes("<OptionalNetworkSettings>\n");
        out.writeBytes("<!--these are optional network settings. If not supplied defaults will be used-->\n");
        out.writeBytes("<BPTTLearningRate>" + fromNetwork.eta + "</BPTTLearningRate>\n");
        out.writeBytes("<Momentum>" + ((BP) fromNetwork).beta + "</Momentum>\n");
        out.writeBytes("<LearningIncrease>" + ((BP) fromNetwork).etaUP + "</LearningIncrease>\n");
        out.writeBytes("<LearningDecrease>" + ((BP) fromNetwork).etaDOWN + "</LearningDecrease>\n");
        out.writeBytes("</OptionalNetworkSettings>\n");
        out.writeBytes("<Sequences>\n");
        for (int i = 0; i < fromNetwork.patc; i++)
        {
            if (fromNetworkType.equals("BPTTwithHebbian"))
            {
                out.writeBytes("<Sequence>\n");
                out.writeBytes("<SequenceLength>" + ((BPTTwithHebbian) fromNetwork).trainingSeqLength[i] + "</SequenceLength>\n");
                out.writeBytes("<InputSequence>\n");
                for (int j = 0; j < ((BPTTwithHebbian) fromNetwork).trainingSeqLength[i]; j++)
                {
                    out.writeBytes("<Input>");
                    for (int k = 0; k < params.length; k++)
                    {
                        out.writeBytes("<Value>" + ((BPTTwithHebbian) fromNetwork).trainingInputSeq[i][j][k] + "</Value>");
                    }
                    out.writeBytes("</Input>\n");
                }
                out.writeBytes("</InputSequence>\n");
                out.writeBytes("<OutputSequence>\n");
                for (int j = 0; j < ((BPTTwithHebbian) fromNetwork).trainingSeqLength[i]; j++)
                {
                    out.writeBytes("<Output>");
                    for (int k = 0; k < ((BPTTwithHebbian) fromNetwork).outputLayerDim; k++)
                    {
                        out.writeBytes("<Value>" + ((BPTTwithHebbian) fromNetwork).trainingOutputSeq[i][j][k] + "</Value>");
                    }
                    out.writeBytes("</Output>\n");
                }
                out.writeBytes("</OutputSequence>\n");
                out.writeBytes("</Sequence>\n");
            }
        }
        out.writeBytes("</Sequences>\n");
        out.writeBytes("</PatternSet>\n");
    }

    /**
     * Reads network weights from a file
     *
     * @param weightFilename - filename
     * @return - successful
     */
    public boolean readWeight(final String weightFilename)
    {
        fileInputToHiddenW = null;
        fileHiddenToOutputW = null;
        fileRecurrentOutputToInputW = null;
        fileInputLayerDim = 0;
        fileHiddenLayerDim = 0;
        fileOutputLayerDim = 0;
        fileRecurrentInputDim = 0;
        fileRecurrentOutputDim = 0;

        int tc, linec, row = 0;
        String s, u;
        boolean added;
        try
        {
            final BufferedReader in;

            in = new BufferedReader(new InputStreamReader(Elib.openfileREAD(weightFilename)));
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
                    if (tc == 0 && u.equals("inputdim"))
                    {
                        u = st.nextToken();
                        fileInputLayerDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("outputdim"))
                    {
                        u = st.nextToken();
                        fileOutputLayerDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("hiddendim"))
                    {
                        u = st.nextToken();
                        fileHiddenLayerDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("rindim"))
                    {
                        u = st.nextToken();
                        fileRecurrentInputDim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("routdim"))
                    {
                        u = st.nextToken();
                        fileRecurrentOutputDim = Elib.toInt(u);
                        continue;
                    }
                    if (fileInputLayerDim == 0 || fileHiddenLayerDim == 0 || fileOutputLayerDim == 0 ||
                        fileRecurrentOutputDim == 0 || fileRecurrentInputDim == 0)
                    {
                        Log.println("The weight file doesn't specify the net size properly!");
                    }
                    if (fileInputToHiddenW == null)
                        fileInputToHiddenW = new double[fileHiddenLayerDim][fileInputLayerDim + fileRecurrentInputDim];
                    if (fileHiddenToOutputW == null)
                        fileHiddenToOutputW = new double[fileOutputLayerDim + fileRecurrentOutputDim][fileHiddenLayerDim];
                    if (fileRecurrentOutputToInputW == null)
                        fileRecurrentOutputToInputW = new double[fileRecurrentInputDim][fileRecurrentOutputDim];
                    if (row < fileHiddenLayerDim)
                        fileInputToHiddenW[row][tc] = Elib.toDouble(u);
                    else if (row < (fileHiddenLayerDim) + (fileOutputLayerDim + fileRecurrentOutputDim))
                        fileHiddenToOutputW[row - fileHiddenLayerDim][tc] = Elib.toDouble(u);
                    else
                        fileRecurrentOutputToInputW[row - fileHiddenLayerDim - fileOutputLayerDim - fileRecurrentOutputDim][tc] = Elib.toDouble(u);
                    added = true;
                    tc++;
                }
                if (!added)
                    continue;
                if (tc != ((row < fileHiddenLayerDim) ? fileInputLayerDim + fileRecurrentInputDim :
                           ((row < fileHiddenLayerDim + fileOutputLayerDim + fileRecurrentOutputDim) ? fileHiddenLayerDim :
                            ((row < fileHiddenLayerDim + fileOutputLayerDim + fileRecurrentOutputDim + fileRecurrentInputDim) ? fileRecurrentOutputDim :
                             0))))
                {
                    Log.println("File format Error in " + weightFilename + " line " + linec);
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
            Log.println("BP.readWeight() : EXCEPTION " + e);
            return false;
        }

        return true;
    }

    /**
     * Write network weights to a file
     *
     * @param weightFilename - filename
     */
    public void writeWeight(final String weightFilename)
    {
        //System.out.println("Creating weight file:"+weightFilename);

        try
        {
            final DataOutputStream out = Elib.openfileWRITE(weightFilename);
            out.writeBytes("# This weight file is generated by BP (Erhan Oztop -Dec'99)\n");
            out.writeBytes("# This file specfies the network size and the weight values\n");
            out.writeBytes("# That the network sizes excludes the clamped 1's for input and hidden layer\n");
            out.writeBytes("# So the weight matrices has one more column for the clamped unit.\n");
            out.writeBytes("\n# Note: To train the network you need to load a pattern file\n");
            out.writeBytes("# Note: You can not specify learning parameters from this file\n");
            out.writeBytes("# Note: If you want to continue a learning session that you saved the \n");
            out.writeBytes("# weights from, use Make Network from Weight followed by Load Pattern then continue training.\n\n");
            out.writeBytes("# First matrix is the input(x)->hidden(y) weights(inputToHiddenW) \n");
            out.writeBytes("# Second matrix is the hidden(y)->output(z) weights(hiddenToOutputW) \n");
            out.writeBytes("# Third matrix is the recurrent output(z)->recurrent input(x) weights(recurrentOutputToInputW) \n");
            out.writeBytes("# The network computes  sgn(recurrentOutputToInputW.sgn(hiddenToOutputW.sgn(inputToHiddenW.x))) where sgn(t)=1/(1+exp(-t))\n\n");

            out.writeBytes("outputdim  " + outputLayerDim + "\nhiddendim  " + hiddenLayerDim + "\ninputdim   " + inputLayerDim +
                           "\nrindim   " + recurrentInputDim + "\nroutdim   " + recurrentOutputDim + "\n\n");
            out.writeBytes("#input  -> hidden weights  inputToHiddenW[" + (hiddenLayerDim) + "][" + (inputLayerDim + recurrentInputDim) + "]\n");
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                for (int j = 0; j < inputLayerDim + recurrentInputDim; j++)
                    out.writeBytes(inputToHiddenW[i][j] + " ");
                out.writeBytes("\n");
            }

            out.writeBytes("\n#hidden -> output weights  hiddenToOutputW[" + (outputLayerDim + recurrentOutputDim) + "][" + (hiddenLayerDim) + "]:\n");
            for (int k = 0; k < outputLayerDim + recurrentOutputDim; k++)
            {
                for (int i = 0; i < hiddenLayerDim; i++)
                    out.writeBytes(hiddenToOutputW[k][i] + " ");
                out.writeBytes("\n");
            }

            out.writeBytes("\n#recurrent hidden -> recurrent input weights recurrentOutputToInputW[" + recurrentInputDim + "][" + recurrentOutputDim + "]:\n");
            for (int j = 0; j < recurrentInputDim; j++)
            {
                for (int k = 0; k < recurrentOutputDim; k++)
                    out.writeBytes(recurrentOutputToInputW[j][k] + " ");
                out.writeBytes("\n");
            }

            out.close();
        }
        catch (IOException e)
        {
            Log.println("writeWeight() : EXCEPTION " + e);
        }
    }

    /**
     * Print network profile
     */
    public final void dumpNet()
    {
        Log.println("** Below data reflects the current Network **");
        Log.println("(L.rate)eta          :" + eta);
        Log.println("(L.rate+)etaUP       :" + etaUP);
        Log.println("(L.rate-)etaDOWN     :" + etaDOWN);
        Log.println("(momentum)beta       :" + eta);
        Log.println("-------------------------------------");
        Log.println("(recurrent input) recurrentInputLayerDim:" + recurrentInputDim);
        Log.println("(input)   inputLayerDim:" + inputLayerDim);
        Log.println("(hidden)  hiddenLayerDim:" + hiddenLayerDim);
        Log.println("(output)  outputLayerDim:" + outputLayerDim);
        Log.println("(recurrent output) recurrentOutputLayerDim:" + recurrentOutputDim);
        Log.println("** Above data reflects the current Network **");
        Elib.dumpMatrix("Input->Hidden layer weights:", inputToHiddenW, hiddenLayerDim, inputLayerDim + recurrentInputDim);
        Elib.dumpMatrix("Hidden->Output layer weights:", hiddenToOutputW, outputLayerDim + recurrentOutputDim, hiddenLayerDim);
        Elib.dumpMatrix("Recurrent output->Recurrent input layer weights:", recurrentOutputToInputW, recurrentInputDim, recurrentOutputDim);
    }

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
