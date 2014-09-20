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

import mns2.graphics.BPTTwithHebbianPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import sim.util.*;
import sim.motor.Graspable;

/**
 * Back-propagation through time and hebbian learning
 *
 * @author James Bonaiuto (bonaiuto@usc.edu) 2005
 */
public class BPTTwithHebbian extends BPTT
{
    // learning rate for hebbian learning
    public double hebbianEta = 0.01;

    // the number of external hebbian input units
    public int extHebbianInputDim;

    // the weights of the hebbian input to output connections
    public double hebbianInputToOutputW[][];

    // hebbian input layer
    public double hebbianInputLayer[];

    // hebbian input layer history
    public double hebbianInputLayerHistory[][];

    // Weight file layer dimensions
    protected int fileExtHebbianInputDim;
    protected double[][] fileHebbianInputToOutputW;

    // Pattern file layer dimensions
    public int pat_exthebbindim = 0;
    // Pattern file learning parameters
    protected double pat_hebb_eta = 0;

    public JTextField pattern_filename, weight_filename, epochs, training_threshold;
    public JLabel patternFilenameLabel, weightFileLabel, trainingEpochsLabel, successThresholdLabel;
    JButton loadPattern, loadWeight, train, test, randomizeWeights, makeNetworkFromPattern, makeNetworkFromWeight,
            generateWeightFile, quit;
    public JSlider wsc;
    JPanel mainPanel, centerPanel, networkPanel, bottomPanel, topPanel;

    public BPTTwithHebbianPanel panel;

    /**
     * Constructor
     */
    public BPTTwithHebbian()
    {
        setTitle("Back Propagation Through Time with Hebbian - James Bonaiuto Aug'05");
        pattern_filename.setText("action_pattern_bptt_hebbian.xml");
        weight_filename.setText("jjb_bptt_hebbian.wgt");

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

        wsc.setMinimum(0);
        wsc.setMaximum(wscMAX);
        wsc.setValue(0);
        wsc.addChangeListener(this);

        //panel.wscValue=wsc.getValue();
        //networkPanel.add(panel);

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

    /**
     * Constructor
     *
     * @param s - pattern file name
     */
    public BPTTwithHebbian(final String s)
    {
        this(s, 0.05, 0.9, 0.01, 0.1, 0.01);
    }

    /**
     * Constructor
     *
     * @param s               - pattern file name
     * @param def_eta         - bptt learning rate
     * @param def_beta        - bptt learning momentum
     * @param def_etaUP       - delta bptt learning rate up coeff
     * @param def_etaDOWN     - delta bptt learning rate down coeff
     * @param def_hebbian_eta - hebbian learning rate
     */
    public BPTTwithHebbian(final String s, final double def_eta, final double def_beta, final double def_etaUP,
                           final double def_etaDOWN, final double def_hebbian_eta)
    {
        this();

        // Set parameters
        beta = def_beta;
        eta = def_eta;
        etaUP = def_etaUP;
        etaDOWN = def_etaDOWN;
        hebbianEta = def_hebbian_eta;

        //Create network from pattern file
        //System.out.println("Constructing the network with file:"+s);
        netFromPattern(s);
    }

    /**
     * Sets up the network display panel
     */
    public void setupCanvas()
    {
        panel = new BPTTwithHebbianPanel(this);
        panel.addMouseListener(this);
        panel.wscValue = wsc.getValue();
        networkPanel.add(panel);
    }

    /**
     * Create network
     *
     * @param extInDim        - number of input units
     * @param extHebbianInDim - number of hebbian input units
     * @param hiddim          - number of hidden units = input units/2 + 1 if set to -1
     * @param outdim          - number of output units
     * @param rindim          - number of recurrent input units
     * @param routdim         - number of recurrent output units
     * @param Lsteps          - number of time steps to unravel the network
     */
    public void createNet(final int extInDim, final int extHebbianInDim, final int hiddim, final int outdim,
                          final int rindim, final int routdim, final int Lsteps)
    {
        //initialize network layer dimensions
        inputLayerDim = extInDim;
        extHebbianInputDim = extHebbianInDim;
        hiddenLayerDim = hiddim;
        outputLayerDim = outdim;
        recurrentInputDim = rindim;
        recurrentOutputDim = routdim;
        L = Lsteps;

        // Initialize network layers
        inputLayer = new double[inputLayerDim + recurrentInputDim];
        inputLayerNet = new double[inputLayerDim + recurrentInputDim];
        hiddenLayer = new double[hiddenLayerDim];
        hiddenLayerNet = new double[hiddenLayerDim];
        outputLayer = new double[outputLayerDim + recurrentOutputDim];
        outputLayerNet = new double[outputLayerDim + recurrentOutputDim];
        hebbianInputLayer = new double[extHebbianInputDim];

        // Initialize network layer weights
        inputToHiddenW = new double[hiddenLayerDim][inputLayerDim + recurrentInputDim];
        hiddenToOutputW = new double[outputLayerDim + recurrentOutputDim][hiddenLayerDim];
        hebbianInputToOutputW = new double[outputLayerDim][extHebbianInputDim];
        recurrentOutputToInputW = new double[recurrentInputDim][recurrentOutputDim];
        clearHistory();
        initNet();

        validNet = true;
    }

    /**
     * Create net from pattern file
     *
     * @param patternFile - pattern filename
     */
    public void netFromPattern(final String patternFile)
    {
        panel.showAll();
        patc = 0;

        // Read pattern file
        if (!readPattern(patternFile))
        {
            Log.println("Error occured reading pattern file:" + patternFile);
            return;
        }

        // Create the network
        createNet(pat_indim, pat_exthebbindim, pat_hiddim, pat_outdim, pat_rindim, pat_routdim, 30);
    }

    /**
     * Create network from weight file
     *
     * @param weightFile - weight file
     */
    public void netFromWeight(final String weightFile)
    {
        //panel.showAll();

        // read weight file
        if (!readWeight(weightFile))
        {
            Log.println("Error occured reading weight file:" + weightFile);
            return;
        }

        //create the network
        createNet(fileInputLayerDim, fileExtHebbianInputDim, fileHiddenLayerDim, fileOutputLayerDim,
                  fileRecurrentInputDim, fileRecurrentOutputDim, L);

        //Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim + fileRecurrentInputDim);

        //Set hidden to output layer weights
        for (int k = 0; k < fileOutputLayerDim + fileRecurrentOutputDim; k++)
            System.arraycopy(fileHiddenToOutputW[k], 0, hiddenToOutputW[k], 0, fileHiddenLayerDim);

        //Set hebbian input to output layer weights
        for (int i = 0; i < fileOutputLayerDim; i++)
            System.arraycopy(fileHebbianInputToOutputW[i], 0, hebbianInputToOutputW[i], 0, fileExtHebbianInputDim);

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
        panel.showAll();

        // Read weights from file
        if (!readWeight(weightFilename))
        {
            Log.println("Error occured reading weight file:" + weightFilename);
            return;
        }

        if (fileInputToHiddenW == null || fileHiddenToOutputW == null || fileRecurrentOutputToInputW == null)
        {
            Log.println("Error occured reading weight file:" + weightFilename);
            return;
        }

        // Print weights
        Elib.dumpMatrix("input->hidden weights", fileInputToHiddenW, fileHiddenLayerDim, fileInputLayerDim + fileRecurrentInputDim);
        Elib.dumpMatrix("hidden->output weights", fileHiddenToOutputW, fileOutputLayerDim + fileRecurrentOutputDim,
                        fileHiddenLayerDim);
        Elib.dumpMatrix("hebbian input->output weights", fileHebbianInputToOutputW, fileOutputLayerDim, fileExtHebbianInputDim);
        Elib.dumpMatrix("recurrent output->input weights", fileRecurrentOutputToInputW, fileRecurrentInputDim,
                        fileRecurrentOutputDim);

        if (fileInputLayerDim != inputLayerDim)
        {
            Log.println("Mismatch in input dimension!");
            return;
        }
        if (fileExtHebbianInputDim != extHebbianInputDim)
        {
            Log.println("Mismatch in hebbian input dimension!");
            return;
        }
        if (fileHiddenLayerDim != hiddenLayerDim)
        {
            Log.println("Mismatch in hidden dimension!");
            return;
        }
        if (fileOutputLayerDim != outputLayerDim)
        {
            Log.println("Mismatch in output dimension!");
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

        // Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim + fileRecurrentInputDim);

        // Set hidden to output layer weights
        for (int k = 0; k < fileOutputLayerDim + fileRecurrentOutputDim; k++)
            System.arraycopy(fileHiddenToOutputW[k], 0, hiddenToOutputW[k], 0, fileHiddenLayerDim);

        // Set hebbian input to output layer weights
        for (int i = 0; i < fileOutputLayerDim; i++)
            System.arraycopy(fileHebbianInputToOutputW[i], 0, hebbianInputToOutputW[i], 0, fileExtHebbianInputDim);

        //Set recurrent output to input layer weights
        for (int l = 0; l < fileRecurrentInputDim; l++)
            System.arraycopy(fileRecurrentOutputToInputW[l], 0, recurrentOutputToInputW[l], 0, fileRecurrentOutputDim);

        Log.println("inputLayerDim:" + inputLayerDim + " hebbianInputLayerDim:" + extHebbianInputDim + " hiddenLayerDim:" +
                    hiddenLayerDim + " outputLayerDim:" + outputLayerDim + " recurrentInputLayerDim:" + recurrentInputDim +
                                                                                                                          " recurrentOutputLayerDim:" + recurrentOutputDim);
    }

    /**
     * Initialize network weights randomly
     */
    public void initNet()
    {
        super.initNet();
        for (int k = 0; k < outputLayerDim; k++)
        {
            for (int i = 0; i < extHebbianInputDim; i++)
            {
                hebbianInputToOutputW[k][i] = (Math.random() - 0.5) * 0.1;
            }
        }
    }

    /**
     * Clear weight change history
     */
    public void clearHistory()
    {
        super.clearHistory();
        hebbianInputLayerHistory = new double[MAX_seqLength][extHebbianInputDim];
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
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) ||
                (!staticAction && time.equals("duringGrasp")))
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
                // multiply recurrent output layer by recurrent output to input layer weights and put results in
                // recurrentInNet
                VA.multiply(recurrentOutputToInputW, recurrentOut, recurrentInputDim, recurrentOutputDim,
                            recurrentInNet);
            }

            // push recurrentInNet through sigmoid function and put result in recurrentIn
            VA.squash(recurrentInNet, recurrentInputDim, recurrentIn);

            // transfer recurrent input unit values to the input layer
            System.arraycopy(recurrentIn, 0, inputLayer, inputLayerDim, recurrentInputDim);
            System.arraycopy(recurrentInNet, 0, inputLayerNet, inputLayerDim, recurrentInputDim);

            if (!lesionInputToHidden)
            {
                // multiply input layer by input to hidden layer weights and put results in hiddenLayerNet
                VA.multiply(inputToHiddenW, inputLayer, hiddenLayerDim, inputLayerDim + recurrentInputDim,
                            hiddenLayerNet);
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

        boolean lesionAudio = false;
        if (lesionedConnection.contains("audio"))
        {
            String time = lesionTime.get(lesionedConnection.indexOf("audio")).toString();
            if (time.equals("wholeGrasp") || (staticAction && time.equals("graspStatic")) ||
                (!staticAction && time.equals("duringGrasp")))
                lesionAudio = true;
        }

        final double[] totalToOutputLayer = new double[hiddenLayerDim + extHebbianInputDim];
        System.arraycopy(hiddenLayer, 0, totalToOutputLayer, 0, hiddenLayerDim);
        System.arraycopy(hebbianInputLayer, 0, totalToOutputLayer, hiddenLayerDim, extHebbianInputDim);
        final double[][] totalToOutputW = new double[outputLayerDim + recurrentOutputDim][hiddenLayerDim + extHebbianInputDim];
        for (int i = 0; i < outputLayerDim + recurrentOutputDim; i++)
        {
            System.arraycopy(hiddenToOutputW[i], 0, totalToOutputW[i], 0, hiddenLayerDim);
            if (i < outputLayerDim)
                System.arraycopy(hebbianInputToOutputW[i], 0, totalToOutputW[i], hiddenLayerDim, extHebbianInputDim);
        }
        if (!lesionAudio && !lesionHiddenToOutput)
            // multiply hidden layer by hidden to output layer weights and put results in outputLayerNet
            VA.multiply(totalToOutputW, totalToOutputLayer, outputLayerDim + recurrentOutputDim, hiddenLayerDim + extHebbianInputDim, outputLayerNet);
        else if (!lesionHiddenToOutput)
            // multiply hidden layer by hidden to output layer weights and put results in outputLayerNet
            VA.multiply(totalToOutputW, totalToOutputLayer, outputLayerDim + recurrentOutputDim, hiddenLayerDim, outputLayerNet);
        // push outputLayerNet through sigmoid function and put result in output layer
        VA.squash(outputLayerNet, outputLayerDim + recurrentOutputDim, outputLayer);

        // transfer current network state to history
        System.arraycopy(inputLayer, 0, inputLayerHistory[t], 0, inputLayerDim + recurrentInputDim);
        System.arraycopy(hiddenLayer, 0, hiddenLayerHistory[t], 0, hiddenLayerDim);
        System.arraycopy(hebbianInputLayer, 0, hebbianInputLayerHistory[t], 0, extHebbianInputDim);
        System.arraycopy(outputLayer, 0, outputLayerHistory[t], 0, outputLayerDim + recurrentOutputDim);

        // plot network activation history
        if (plot)
        {
            plotNetworkActivity();
        }
        panel.repaint();
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
            String frameString = FrameUtils.getFrameString(t + 1);
            for (int j = 0; j < plots.length; j++)
            {
                if (plots[j].equals("hebbian"))
                    Gplot.plot(hebbianInputLayerHistory, t + 1, extHebbianInputDim, plot_extra_command[j],
                               plot_dimensions[j], plot_labels[j], plot_output_files[j] + "_" + frameString);
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
        super.presentPattern(p);
        for (int j = 0; j < extHebbianInputDim; j++)
            hebbianInputLayer[j] = trainingInputSeq[p][t][inputLayerDim + j];
    }

    /**
     * Get output for given input
     *
     * @param inp  - multimodal input pattern
     * @param size - multimodal input pattern size - must match input layer size
     * @param plot - whether or not to plot the network activity
     * @return - network output
     */
    public double[] ask(final double[] inp, final int size, final boolean plot, final boolean staticGrasp)
    {
        // Check size of input
        if (size != inputLayerDim + extHebbianInputDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
        }

        // present input pattern
        presentPattern(inp);
        // run the network forward one time step
        forward(plot, staticGrasp);

        // return the network's external output
        final double[] externalOut = new double[outputLayerDim];
        System.arraycopy(outputLayer, 0, externalOut, 0, outputLayerDim);
        return externalOut;
    }

    /**
     * Set input units to pattern
     *
     * @param input - multimodal input pattern
     */
    public void presentPattern(final double[] input)
    {
        super.presentPattern(input);
        for (int j = 0; j < extHebbianInputDim; j++)
            hebbianInputLayer[j] = input[inputLayerDim + j];
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
            for (int k = 0; k < outputLayerDim; k++)
            {
                // difference between desired output and produced output
                final double ee = (trainingOutputSeq[p][j][k] - outputLayerHistory[j][k]);
                // this part equals G'(outputLayerNet) for b=0.5
                outputErrorGradientHistory[j][k] = ee * (1 - outputLayerHistory[j][k]) * outputLayerHistory[j][k];

                err += ee * ee;
            }

            //Calculate recurrent output error if not at the end of the sequence
            for (int k = 0; k < recurrentOutputDim; k++)
            {
                outputErrorGradientHistory[j][k + outputLayerDim] = 0;
                if (j < L - 1)
                {
                    for (int i = 0; i < recurrentInputDim; i++)
                        //each recurrentOutputLayer[k]'s error share of recurrent input error
                        outputErrorGradientHistory[j][k + outputLayerDim] += recurrentOutputToInputW[i][k] *
                                                                             recurrentInputErrorGradientHistory[j + 1][i];
                    // this part equals G'(recurrentOutputErrorGradient) for b=0.5
                    outputErrorGradientHistory[j][k + outputLayerDim] *= (1 - outputLayerHistory[j][outputLayerDim + k]) *
                                                                         outputLayerHistory[j][outputLayerDim + k];
                }
            }

            //Calculate hidden layer error
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                hiddenErrorGradientHistory[j][i] = 0;
                for (int k = 0; k < outputLayerDim + recurrentOutputDim; k++)
                    //each hiddenLayer[i]'s error share of output error
                    hiddenErrorGradientHistory[j][i] += hiddenToOutputW[k][i] * outputErrorGradientHistory[j][k];
                // this part equals G'(hiddenErrorGradient) for b=0.5
                hiddenErrorGradientHistory[j][i] *= (1 - hiddenLayerHistory[j][i]) * hiddenLayerHistory[j][i];
            }

            //Calculate recurrent input error
            for (int i = 0; i < recurrentInputDim; i++)
            {
                recurrentInputErrorGradientHistory[j][i] = 0;
                for (int l = 0; l < hiddenLayerDim; l++)
                    //each recurrentInputLayer[i]'s error share of hidden error
                    recurrentInputErrorGradientHistory[j][i] += inputToHiddenW[l][i + inputLayerDim] *
                                                                hiddenErrorGradientHistory[j][l];
                // this part equals G'(recurrentInputErrorGradient) for b=0.5
                recurrentInputErrorGradientHistory[j][i] *= (1 - inputLayerHistory[j][i + inputLayerDim]) *
                                                            inputLayerHistory[j][i + inputLayerDim];
            }

            // Calculate change in external and recurrent input to hidden layer weights
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                for (int k = 0; k < inputLayerDim + recurrentInputDim; k++)
                    inputToHiddenTotalDW[i][k] += eta * hiddenErrorGradientHistory[j][i] * inputLayerHistory[j][k];
            }

            // Update hebbian input to output layer weights with anti/hebbian learning
            final double[] totalWeightSumsExt = new double[extHebbianInputDim];

            for (int i = 0; i < outputLayerDim; i++)
            {
                for (int k = 0; k < extHebbianInputDim; k++)
                {
                    //if(outputLayerHistory[j][i] > 0.5 && hebbianInputLayerHistory[j][k] > 0.5)
                    hebbianInputToOutputW[i][k] += hebbianEta * hebbianInputLayerHistory[j][k] *
                                                   outputLayerHistory[j][i];
                    //else
                    //    hebbianInputToOutputW[i][k] -= hebbianEta * hebbianInputLayerHistory[j][k] *
                    //          outputLayerHistory[j][i];
                    totalWeightSumsExt[k] += Math.abs(hebbianInputToOutputW[i][k]);
                }
            }
            for (int k = 0; k < extHebbianInputDim; k++)
            {
                for (int i = 0; i < outputLayerDim; i++)
                    hebbianInputToOutputW[i][k] /= (totalWeightSumsExt[k] / 5.0);
            }

            // Calculate change in hidden to output layer weights
            for (int k = 0; k < outputLayerDim + recurrentOutputDim; k++)
            {
                for (int i = 0; i < hiddenLayerDim; i++)
                    hiddenToOutputTotalDW[k][i] += eta * outputErrorGradientHistory[j][k] * hiddenLayerHistory[j][i];
            }

            // if not at the end of the sequence
            if (j < L - 1)
            {
                // Calculate change in output to input layer weights
                for (int i = 0; i < recurrentInputDim; i++)
                {
                    for (int k = 0; k < recurrentOutputDim; k++)
                        recurrentOutputToInputTotalDW[i][k] += eta * recurrentInputErrorGradientHistory[j + 1][i] * outputLayerHistory[j][outputLayerDim + k];
                }
            }
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

    /**
     * Test all patterns
     *
     * @return - total output error over all patterns
     */
    public double testPattern()
    {
        int testSet[] = new int[patc];
        for (int i = 0; i < patc; i++)
            testSet[i] = i;
        return testPattern(false, testSet);
    }

    /**
     * Test patterns in the test set
     *
     * @param verbose - whether or not to output debug info
     * @param testSet - set of patterns to test
     * @return - total output error over all patterns
     */
    public double testPattern(final boolean verbose, final int testSet[])
    {
        // Total error
        double err = 0;
        // Total correct output
        correctSequences = 0;
        correctTimeSteps = 0;

        if (pat_indim != inputLayerDim || pat_exthebbindim != extHebbianInputDim)
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
        for (int i = 0; i < testSet.length; i++)
        {
            final int p = testSet[i];
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
        pat_exthebbindim = 0;
        pat_outdim = 0;
        pat_hiddim = 0;
        pat_rindim = 0;
        pat_routdim = 0;
        pat_eta = -1;
        pat_beta = -1;
        pat_etaUP = -1;
        pat_etaDOWN = -1;
        pat_hebb_eta = -1;

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
                        else if (setting.getNodeName().equals("HebbianInputDim"))
                            pat_exthebbindim = Integer.parseInt(setting.getChildNodes().item(0).getNodeValue());
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
                        else if (setting.getNodeName().equals("HebbianLearningRate"))
                            pat_hebb_eta = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
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
                    trainingInputSeq = new double[patc][MAX_seqLength][pat_indim + pat_exthebbindim];
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
                                        if (input.getNodeName().equals("Input") || input.getNodeName().equals("HebbianInput"))
                                        {
                                            final NodeList values = input.getChildNodes();
                                            int valIdx = 0;
                                            if (input.getNodeName().equals("HebbianInput"))
                                                valIdx = 7;
                                            for (int m = 0; m < values.getLength(); m++)
                                            {
                                                final Node value = values.item(m);
                                                if (value.getNodeName().equals("Value"))
                                                {
                                                    final double val = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                                    if (input.getNodeName().equals("Input"))
                                                        trainingInputSeq[seqIdx][inpIdx][valIdx++] = val;
                                                    else
                                                        trainingInputSeq[seqIdx][inpIdx][valIdx++] = val;
                                                }
                                            }
                                            if (input.getNodeName().equals("Input"))
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

        // Read parameters
        if (pat_beta > -1) beta = pat_beta;
        if (pat_eta > -1) eta = pat_eta;
        if (pat_etaUP > -1) etaUP = pat_etaUP;
        if (pat_etaDOWN > -1) etaDOWN = pat_etaDOWN;
        if (pat_hebb_eta > -1) hebbianEta = pat_hebb_eta;

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
        fileHebbianInputToOutputW = null;
        fileInputLayerDim = 0;
        fileExtHebbianInputDim = 0;
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
            if (in == null)
                return false;
            linec = 0;
            while (null != (s = in.readLine()))
            {
                linec++;
                if (s.equals(""))
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
                    if (tc == 0 && u.equals("hebbianinputdim"))
                    {
                        u = st.nextToken();
                        fileExtHebbianInputDim = Elib.toInt(u);
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
                    if (fileHebbianInputToOutputW == null)
                        fileHebbianInputToOutputW = new double[fileOutputLayerDim][fileExtHebbianInputDim];
                    if (fileRecurrentOutputToInputW == null)
                        fileRecurrentOutputToInputW = new double[fileRecurrentInputDim][fileRecurrentOutputDim];

                    if (row < fileHiddenLayerDim)
                        fileInputToHiddenW[row][tc] = Elib.toDouble(u);
                    else if (row < (fileHiddenLayerDim) + (fileOutputLayerDim + fileRecurrentOutputDim))
                        fileHiddenToOutputW[row - fileHiddenLayerDim][tc] = Elib.toDouble(u);
                    else if (row < (fileHiddenLayerDim) + (fileOutputLayerDim + fileRecurrentOutputDim) + (fileRecurrentInputDim))
                        fileRecurrentOutputToInputW[row - fileHiddenLayerDim - fileOutputLayerDim - fileRecurrentOutputDim][tc] = Elib.toDouble(u);
                    else
                        fileHebbianInputToOutputW[row - fileHiddenLayerDim - fileOutputLayerDim - fileRecurrentOutputDim - fileRecurrentInputDim][tc] = Elib.toDouble(u);
                    added = true;
                    tc++;
                }
                if (!added)
                    continue;
                if (tc != ((row < fileHiddenLayerDim) ? fileInputLayerDim + fileRecurrentInputDim :
                           ((row < fileHiddenLayerDim + fileOutputLayerDim + fileRecurrentOutputDim) ? fileHiddenLayerDim :
                            ((row < fileHiddenLayerDim + fileOutputLayerDim + fileRecurrentOutputDim + fileRecurrentInputDim) ? fileRecurrentOutputDim :
                             ((row < fileHiddenLayerDim + fileOutputLayerDim + fileRecurrentOutputDim + fileRecurrentInputDim + fileOutputLayerDim) ? fileExtHebbianInputDim :
                              0)))))
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
            out.writeBytes("# This weight file is generated by BPTTwithHebbian\n");
            out.writeBytes("# This file specfies the network size and the weight values\n\n");
            out.writeBytes("# Note: To train the network you need to load a pattern file\n");
            out.writeBytes("# Note: You can not specify learning parameters from this file\n");
            out.writeBytes("# Note: If you want to continue a learning session that you saved the\n");
            out.writeBytes("# weights from, use Make Network from Weight followed by Load Pattern then continue training.\n\n");
            out.writeBytes("# First matrix is the input(x)->hidden(y) weights(inputToHiddenW)\n");
            out.writeBytes("# Second matrix is the hidden(y)->output(z) weights(hiddenToOutputW)\n");
            out.writeBytes("# Third matrix is the recurrent output(z)->recurrent input(x) weights(recurrentOutputToInputW)\n");
            out.writeBytes("# Fourth matrix is the hebbian->output(z) weights (hebbianToOutputW)\n");

            out.writeBytes("outputdim  " + outputLayerDim + "\nhiddendim  " + hiddenLayerDim + "\ninputdim   " + inputLayerDim +
                           "\nhebbianinputdim   " + extHebbianInputDim + "\nrindim   " + recurrentInputDim + "\nroutdim   " +
                                                                                                                            recurrentOutputDim + "\n\n");
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

            out.writeBytes("\n#hebbian input -> output weights  hebbianInputToOutputW[" + outputLayerDim + "][" + extHebbianInputDim + "]:\n");
            for (int k = 0; k < outputLayerDim; k++)
            {
                for (int i = 0; i < extHebbianInputDim; i++)
                    out.writeBytes(hebbianInputToOutputW[k][i] + " ");
                out.writeBytes("\n");
            }

            out.close();
        }
        catch (IOException e)
        {
            Log.println("writeWeight() : EXCEPTION " + e);
        }
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
        wsc.setMaximum(101);
        wsc.setOrientation(1);
        wsc.setValue(100);
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
