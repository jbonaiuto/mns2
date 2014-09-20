package mns2.comp;

import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.*;
import javax.swing.event.ChangeEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import mns2.graphics.BPPanel;
import mns2.graphics.NetworkPanel;
import mns2.util.ParamsNode;
import sim.util.*;
import sim.motor.Graspable;

/**
 * @author Erhan Oztop, 2001-2002
 *         modified by James Bonaiuto (bonaiuto@usc.edu) 2005
 */

public class BP extends Network
{
    // training input patterns
    public double trainingInput[][];
    // training output patterns
    public double trainingOutput[][];

    // output layer error
    public double outputLayerErr[];
    // hidden layer error
    public double hiddenLayerErr[];
    // hidden to output delta weights (hiddenLayer->outputLayer weights)
    public double hiddenToOutputDW[][];
    // input to hidden delta weights  (inputLayer->hiddenLayer weights)
    public double inputToHiddenDW[][];
    // old hidden to output delta weights (hiddenLayer->outputLayer weights)
    public double hiddenToOutputOldDW[][];
    // old input to hidden delta weights  (inputLayer->hiddenLayer weights)
    public double inputToHiddenOldDw[][];

    // Pattern file learning parameters
    double pat_beta = 0, pat_etaUP = 0, pat_etaDOWN = 0;

    public JTextField pattern_filename, weight_filename, epochs, training_threshold;
    public JLabel patternFilenameLabel, weightFileLabel, trainingEpochsLabel, successThresholdLabel;
    JButton loadPattern, loadWeight, train, test, randomizeWeights, makeNetworkFromPattern, makeNetworkFromWeight,
            generateWeightFile, quit;
    public JSlider wsc;
    JPanel mainPanel, centerPanel, networkPanel, bottomPanel, topPanel;
    private BPPanel panel;

    /**
     * Constructor
     */
    public BP()
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

        setTitle("Back Propagation. - Erhan Oztop Dec'99");
        if (NetworkInterface.useSplines)
        {
            pattern_filename.setText("action_pattern_bp_spline.xml");
            weight_filename.setText("jjb_bp_spline.wgt");
        }
        else
        {
            pattern_filename.setText("action_pattern_bp.xml");
            weight_filename.setText("jjb_bp.wgt");
        }
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
     * Sets up the network display panel
     */
    public void setupCanvas()
    {
        panel = new BPPanel(this);
        panel.addMouseListener(this);
        panel.wscValue = wsc.getValue();
        networkPanel.add(panel);
    }

    /**
     * Constructor
     *
     * @param indim       - number of input units
     * @param hiddim      - number of hidden units
     * @param outdim      - number of output units
     * @param def_eta     - learning rate
     * @param def_beta    - momemtum
     * @param def_etaUP   - delta learning rate up coeff
     * @param def_etaDOWN - delta learning rate down coeff
     */
    public BP(final int indim, final int hiddim, final int outdim, final double def_eta, final double def_beta,
              final double def_etaUP, final double def_etaDOWN)
    {
        this();
        // Set learning rate
        beta = def_beta;
        eta = def_eta;
        etaUP = def_etaUP;
        etaDOWN = def_etaDOWN;

        //Create network
        createNet(indim, hiddim, outdim);
    }

    /**
     * Constructor
     *
     * @param s - pattern filename
     */
    public BP(final String s)
    {
        this(s, 0.05, 0.9, 0.01, 0.1);
    }

    /**
     * Constructor
     *
     * @param s           - pattern filename
     * @param def_eta     - learning rate
     * @param def_beta    - momentum
     * @param def_etaUP   - delta learning rate up coeff
     * @param def_etaDOWN - delta learning rate down coeff
     */
    public BP(final String s, final double def_eta, final double def_beta, final double def_etaUP,
              final double def_etaDOWN)
    {
        this();
        // Set learning rate
        beta = def_beta;
        eta = def_eta;
        etaUP = def_etaUP;
        etaDOWN = def_etaDOWN;

        //Create network from pattern file
        //System.out.println("Constructing the network with file:"+s);
        netFromPattern(s);
    }

    /**
     * Create network
     *
     * @param indim  - number of input units
     * @param hiddim - number of hidden units = input units/2 + 1 if set to -1
     * @param outdim - number of output units
     */
    public void createNet(final int indim, int hiddim, final int outdim)
    {
        // Auto-calc number of hidden units
        if (hiddim <= 0) hiddim = indim / 2 + 1;

        // Initialize network layer dimensions
        inputLayerDim = indim;
        hiddenLayerDim = hiddim;
        outputLayerDim = outdim;

        // Initialize network layers
        inputLayer = new double[inputLayerDim];
        hiddenLayer = new double[hiddenLayerDim];
        hiddenLayerNet = new double[hiddenLayerDim];
        outputLayer = new double[outputLayerDim];
        outputLayerNet = new double[outputLayerDim];

        hiddenLayerErr = new double[hiddenLayerDim];
        outputLayerErr = new double[outputLayerDim];

        // Initialize network weights
        inputToHiddenW = new double[hiddenLayerDim][inputLayerDim];
        inputToHiddenDW = new double[hiddenLayerDim][inputLayerDim];
        inputToHiddenOldDw = new double[hiddenLayerDim][inputLayerDim];
        hiddenToOutputW = new double[outputLayerDim][hiddenLayerDim];
        hiddenToOutputDW = new double[outputLayerDim][hiddenLayerDim];
        hiddenToOutputOldDW = new double[outputLayerDim][hiddenLayerDim];
        initNet();

        validNet = true;
    }

    /**
     * Create net from pattern file
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

        // Read learning rate
        if (pat_beta > -1)
            beta = pat_beta;
        if (pat_eta > -1)
            eta = pat_eta;
        if (pat_etaUP > -1)
            etaUP = pat_etaUP;
        if (pat_etaDOWN > -1)
            etaDOWN = pat_etaDOWN;

        // Create the network
        createNet(pat_indim, pat_hiddim, pat_outdim);
    }

    /**
     * Create network from local weight file
     *
     * @param weightFilename - weight filename
     */
    public void netFromWeight(final String weightFilename)
    {
        netFromWeight(null, weightFilename);
    }

    /**
     * Create network from weight file
     *
     * @param base           - base URL, NULL if local file
     * @param weightFilename - weight file
     */
    public void netFromWeight(final URL base, final String weightFilename)
    {
        //panel.showAll();

        // read weight file
        if (!readWeight(base, weightFilename))
        {
            Log.println("Error occured reading weight file:" + weightFilename);
            return;
        }

        //create the network
        createNet(fileInputLayerDim, fileHiddenLayerDim, fileOutputLayerDim);

        //Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim);

        //Set hidden to output layer weights
        for (int k = 0; k < fileOutputLayerDim; k++)
            System.arraycopy(fileHiddenToOutputW[k], 0, hiddenToOutputW[k], 0, fileHiddenLayerDim);

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

        if (fileInputToHiddenW == null || fileHiddenToOutputW == null)
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
        if (fileOutputLayerDim != outputLayerDim)
        {
            Log.println("Mismatch in output dimension!");
            return;
        }

        // Set input to hidden layer weights
        for (int i = 0; i < fileHiddenLayerDim; i++)
            System.arraycopy(fileInputToHiddenW[i], 0, inputToHiddenW[i], 0, fileInputLayerDim);

        // Set hidden to output layer weights
        for (int k = 0; k < fileOutputLayerDim; k++)
            System.arraycopy(fileHiddenToOutputW[k], 0, hiddenToOutputW[k], 0, fileHiddenLayerDim);
    }

    /**
     * Run network forward
     */
    public void forward(final boolean plot, final boolean staticAction)
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
     * Learn pattern p
     *
     * @param p    - pattern index
     * @param step -  number of steps to run
     * @return - output error averaged over runs
     */
    public double learn(final int p, final int step)
    {
        double err = 0;
        presentPattern(p);
        for (int runs = 0; runs < step; runs++)
        {
            t = 0;
            // Calc forward net dynamics
            forward(false, false);

            //Calculate output layer error
            for (int k = 0; k < outputLayerDim; k++)
            {
                // difference between desired output and produced output
                final double ee = (trainingOutput[p][k] - outputLayer[k]);
                // this part equals G'(outputLayerNet) for b=0.5
                outputLayerErr[k] = ee * (1 - outputLayer[k]) * outputLayer[k];
                // increment output error
                err += ee * ee;
            }

            //Calculate hidden layer error
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                hiddenLayerErr[i] = 0;
                for (int k = 0; k < outputLayerDim; k++)
                    //each hiddenLayer[i]'s error share of output error
                    hiddenLayerErr[i] += hiddenToOutputW[k][i] * outputLayerErr[k];
                // this part equals G'(hiddenLayerNet) for b=0.5
                hiddenLayerErr[i] = hiddenLayerErr[i] * (1 - hiddenLayer[i]) * hiddenLayer[i];

            }

            // Calculate change in input to hidden layer weights
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                for (int j = 0; j < inputLayerDim; j++)
                {
                    inputToHiddenDW[i][j] = eta * hiddenLayerErr[i] * inputLayer[j] + beta * inputToHiddenOldDw[i][j];
                    inputToHiddenOldDw[i][j] = inputToHiddenDW[i][j];
                }
            }

            // Calculate change in hidden to output layer weights
            for (int k = 0; k < outputLayerDim; k++)
            {
                for (int i = 0; i < hiddenLayerDim; i++)
                {
                    hiddenToOutputDW[k][i] = eta * outputLayerErr[k] * hiddenLayer[i] + beta * hiddenToOutputOldDW[k][i];
                    hiddenToOutputOldDW[k][i] = hiddenToOutputDW[k][i];
                }
            }

            // Add delta weight to hidden to output layer weights
            VA.addto(hiddenToOutputW, outputLayerDim, hiddenLayerDim, hiddenToOutputDW);
            // Add delta weight to input to hidden layer weights
            VA.addto(inputToHiddenW, hiddenLayerDim, inputLayerDim, inputToHiddenDW);
        }
        return err / step;
    }

    /**
     * Set input units to pattern
     *
     * @param p - pattern index
     */
    public void presentPattern(final int p)
    {
        // Set input
        for (int j = 0; j < inputLayerDim; j++)
        {
            if (p == patc)
                inputLayer[j] = Math.random();
            else
                inputLayer[j] = trainingInput[p][j];
        }
    }

    /**
     * Get output for given input
     *
     * @param inp  - input pattern
     * @param size - input pattern size - must match input layer size
     * @return - network output
     */
    double[] ask(final double[] inp, final int size, final boolean plot, final boolean staticAction)
    {
        if (size != inputLayerDim)
        {
            Log.println("Pattern input dim does not match network input dim!!");
            return null;
        }
        presentPattern(inp);
        forward(plot, staticAction);

        return outputLayer;
    }

    /**
     * Test all patterns
     *
     * @return - total output error over all patterns
     */
    public double testPattern()
    {
        return testPattern(false);
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
        final double eta_original = eta;
        final Vector<Double> errHistory = new Vector<Double>();
        final Vector<Double> maxErrHistory = new Vector<Double>();
        final Vector<Double> correctHistory = new Vector<Double>();
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
        // if maxiter is greater than zero, keep going until min error, otherwise train for maxiter iterations
        for (int it = 0; (maxiter <= 0 || it < maxiter); it++)
        {
            totalit++;

            p = (int) (Math.random() * patc);

            // Clear history
            clearHistory();
            // Learn pattern and get error
            // old error
            olderr = err;
            err = learn(p, 1);
            // direction of change in error
            dE += err - olderr;

            // Time to change the learning rate?
            avc++;
            if (avc == 20 * patc)
            {
                // If the error is decreasing, increase the learning rate
                if (dE < 0)
                    eta += etaUP;
                    // If the error is staying the same or increasing, decrease the learning rate
                else
                    eta -= etaDOWN * eta;

                // reset countdown and direction of change in error
                avc = 0;
                dE = 0;
            }

            // check if error is small enough to stop train
            if (it % 100 == 0)
            {
                errHistory.add(testPattern(verbose));
                maxErrHistory.add(maxErr);
                correctHistory.add((double) correctPatterns);
                if (errHistory.get(it / 100) < 0.001)
                {
                    Log.println("At " + it + " iterations, error is less than 0.001, stopping training.");
                    break;
                }
            }
            if (correctPatterns >= (training_thresh * patc))
                break;
        }
        errHistory.add(testPattern(true));
        maxErrHistory.add(maxErr);
        correctHistory.add((double) correctPatterns);
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
     * @param verbose
     * @return - total output error over all patterns
     */
    public double testPattern(final boolean verbose)
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
        if (pat_outdim != outputLayerDim)
        {
            Log.println("Pattern output dim does not match network output dim!!");
            return -1;
        }

        // Max output unit error
        maxErr = -1;

        // For all patterns
        for (int i = 0; i < patc; i++)
        {
            t = 0;
            double patt_err = 0.0;
            // Input pattern and compute network output
            presentPattern(i);
            forward(false, false);

            // Number of units with correct output
            int oo = 0;
            // Output layer error
            double thiserr = 0;

            // Compute error for all output units
            for (int k = 0; k < outputLayerDim; k++)
            {
                // error squared for this unit
                final double ee = (trainingOutput[i][k] - outputLayer[k]) * (trainingOutput[i][k] - outputLayer[k]);

                // if error small enough, output is correct
                if (ee < 0.05)
                    oo++;
                // update max output unit error
                if ((ee * .5) > maxErr)
                    maxErr = ee * .5;
                // update layer error
                thiserr += ee * .5;
            }

            // accumulate error
            patt_err += thiserr;

            // If all units have correct output, increment number of correct outputs
            if (oo == outputLayerDim)
                correctPatterns++;

            err += patt_err;
        }
        final String rep = totalit + ":Total error over patterns:" + err + " # Correct patterns:" + correctPatterns + "/" + patc +
                           " MAX (unit) err:" + maxErr + " [L.rate:" + eta + "]";

        if (verbose)
        {
            Log.println(rep);
            setTitle(rep);
        }
        return err;
    }

    /**
     * Initialize network weights randomly
     */
    public void initNet()
    {
        totalit = 0;

        for (int i = 0; i < hiddenLayerDim; i++)
        {
            for (int j = 0; j < inputLayerDim; j++)
            {
                inputToHiddenW[i][j] = (Math.random() - 0.5) * 0.1;
                inputToHiddenOldDw[i][j] = 0;
            }
        }
        for (int k = 0; k < outputLayerDim; k++)
        {
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                hiddenToOutputW[k][i] = (Math.random() - 0.5) * 0.1;
                hiddenToOutputOldDW[k][i] = 0;
            }
        }
    }

    /**
     * Clear weight change history
     */
    public void clearHistory()
    {
        inputLayerHistory = new double[MAX_seqLength][inputLayerDim];
        hiddenLayerHistory = new double[MAX_seqLength][hiddenLayerDim];
        outputLayerHistory = new double[MAX_seqLength][outputLayerDim];
        inputToHiddenOldDw = new double[hiddenLayerDim][inputLayerDim];
        hiddenToOutputOldDW = new double[outputLayerDim][hiddenLayerDim];
    }

    /**
     * Print network profile
     */
    public void dumpNet()
    {
        Log.println("** Below data reflects the current Network **");
        Log.println("(L.rate)eta          :" + eta);
        Log.println("(L.rate+)etaUP       :" + etaUP);
        Log.println("(L.rate-)etaDOWN     :" + etaDOWN);
        Log.println("(momentum)beta       :" + eta);
        Log.println("-------------------------------------");
        Log.println("(input)   inputLayerDim:" + inputLayerDim);
        Log.println("(hidden)  hiddenLayerDim:" + hiddenLayerDim);
        Log.println("(output)  outputLayerDim:" + outputLayerDim);
        Log.println("** Above data reflects the current Network **");
        Elib.dumpMatrix("Input->Hidden layer weights:", inputToHiddenW, hiddenLayerDim, inputLayerDim);
        Elib.dumpMatrix("Hidden->Output layer weights:", hiddenToOutputW, outputLayerDim, hiddenLayerDim);
    }

    /**
     * Print pattern file profile
     */
    public void dumpPattern()
    {
        Log.println("** Below data reflects the current PATTERN FILE **");
        Log.println("(L.rate)pat_eta          :" + pat_eta);
        Log.println("(L.rate+)pat_etaUP       :" + pat_etaUP);
        Log.println("(L.rate-)pat_etaDOWN     :" + pat_etaDOWN);
        Log.println("(momentum)pat_beta       :" + pat_eta);
        Log.println("-------------------------------------");
        Log.println("(input)   inputLayerDim:" + pat_indim);
        Log.println("(hidden)  hiddenLayerDim:" + pat_hiddim);
        Log.println("(output)  outputLayerDim:" + pat_outdim);
        Log.println("# Patterns loaded:" + patc);
        for (int i = 0; i < patc; i++)
        {
            String s = "";
            for (int j = 0; j < pat_indim; j++)
                s += trainingInput[i][j] + " ";
            s += "     ";
            for (int j = 0; j < pat_outdim; j++)
                s += trainingOutput[i][j] + " ";
            Log.println(s);
        }
        Log.println("** Above data reflects the current PATTERN FILE **");
    }

    /**
     * Reads a network pattern from a file
     *
     * @param patternFilename - pattern filename
     * @return - successful
     */
    public boolean readXMLPattern(final String patternFilename)
    {
        pat_indim = 0;
        pat_outdim = 0;
        pat_hiddim = 0;
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
                        else if (setting.getNodeName().equals("Momentum"))
                            pat_beta = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("LearningIncrease"))
                            pat_etaUP = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                        else if (setting.getNodeName().equals("LearningDecrease"))
                            pat_etaDOWN = Double.parseDouble(setting.getChildNodes().item(0).getNodeValue());
                    }
                }
                else if (rootChild.getNodeName().equals("Patterns"))
                {
                    final NodeList patterns = rootChild.getChildNodes();
                    patc = 0;
                    for (int j = 0; j < patterns.getLength(); j++)
                    {
                        if (patterns.item(j).getNodeName().equals("Pattern"))
                            patc++;
                    }
                    trainingInput = new double[patc][pat_indim];
                    trainingOutput = new double[patc][pat_outdim];

                    int patIdx = 0;
                    for (int j = 0; j < patterns.getLength(); j++)
                    {
                        if (patterns.item(j).getNodeName().equals("Pattern"))
                        {
                            final NodeList patternNodes = patterns.item(j).getChildNodes();
                            for (int k = 0; k < patternNodes.getLength(); k++)
                            {
                                final Node patternNode = patternNodes.item(k);
                                if (patternNode.getNodeName().equals("InputPattern"))
                                {
                                    final NodeList inputs = patternNode.getChildNodes();
                                    int inpIdx = 0;
                                    for (int l = 0; l < inputs.getLength(); l++)
                                    {
                                        final Node value = inputs.item(l);
                                        if (value.getNodeName().equals("Value"))
                                        {
                                            trainingInput[patIdx][inpIdx++] = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                        }
                                    }
                                }
                                else if (patternNode.getNodeName().equals("OutputPattern"))
                                {
                                    final NodeList outputs = patternNode.getChildNodes();
                                    int outIdx = 0;
                                    for (int l = 0; l < outputs.getLength(); l++)
                                    {
                                        final Node value = outputs.item(l);
                                        if (value.getNodeName().equals("Value"))
                                        {
                                            trainingOutput[patIdx][outIdx++] = Double.parseDouble(value.getChildNodes().item(0).getNodeValue());
                                        }
                                    }
                                }
                            }
                            patIdx++;
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

        return true;
    }

    /**
     * Reads a network pattern from a file
     *
     * @param patternFilename - pattern filename
     * @return - successful
     */
    public boolean readPattern(final String patternFilename)
    {
        if (patternFilename.endsWith(".xml"))
            return readXMLPattern(patternFilename);

        final Vector vpi = new Vector(40), vpo = new Vector(40);
        ArrayCover bufi = null, bufo = null;
        int tc, linec;
        String s, u;
        boolean added;
        pat_indim = 0;
        pat_outdim = 0;
        pat_hiddim = 0;
        pat_eta = -1;
        pat_beta = -1;
        pat_etaUP = -1;
        pat_etaDOWN = -1;

        try
        {
            final BufferedReader in = new BufferedReader(new InputStreamReader(Elib.openfileREAD(patternFilename)));
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
                        pat_indim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("outputdim"))
                    {
                        u = st.nextToken();
                        pat_outdim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("hiddendim"))
                    {
                        u = st.nextToken();
                        pat_hiddim = Elib.toInt(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("learningrate"))
                    {
                        u = st.nextToken();
                        pat_eta = Elib.toDouble(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("momentum"))
                    {
                        u = st.nextToken();
                        pat_beta = Elib.toDouble(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("learningincrease"))
                    {
                        u = st.nextToken();
                        pat_etaUP = Elib.toDouble(u);
                        continue;
                    }
                    if (tc == 0 && u.equals("learningdecrease"))
                    {
                        u = st.nextToken();
                        pat_etaDOWN = Elib.toDouble(u);
                        continue;
                    }
                    if (bufi == null)
                    {
                        bufi = new ArrayCover(pat_indim);
                        bufo = new ArrayCover(pat_outdim);
                    }

                    if (tc < pat_indim)
                    {
                        bufi.val[tc] = Elib.toDouble(u);
                    }
                    else
                    {
                        bufo.val[tc - pat_indim] = Elib.toDouble(u);
                    }
                    added = true;
                    tc++;
                }
                if (!added)
                    continue;
                vpi.addElement(bufi);
                vpo.addElement(bufo);
                bufi = null;
                bufo = null;
                patc++;
                if (tc != pat_indim + pat_outdim)
                {
                    Log.println("File format Error in " + patternFilename + " line " + linec);
                    Log.println("indim+outdim:" + (pat_indim + pat_outdim) + " BUT token count:" + tc);
                    return false;
                }
            }
            in.close();
        }
        catch (IOException e)
        {
            Log.println("BP.readPattern() : EXCEPTION " + e);
        }

        trainingInput = new double[patc][pat_indim];
        trainingOutput = new double[patc][pat_outdim];

        for (int i = 0; i < pat_outdim; i++)
            trainingOutput[patc][i] = 0;

        ArrayCover r;

        int i = 0;
        final Enumeration e = vpi.elements();
        while (e.hasMoreElements())
        {
            r = (ArrayCover) e.nextElement();
            System.arraycopy(r.val, 0, trainingInput[i], 0, pat_indim);
            i++;
        }

        i = 0;
        final Enumeration f = vpo.elements();
        while (f.hasMoreElements())
        {
            r = (ArrayCover) f.nextElement();
            System.arraycopy(r.val, 0, trainingOutput[i], 0, pat_outdim);
            i++;
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
        out.writeBytes("<OutputDim>" + fromNetwork.outputLayerDim + "</OutputDim>\n");
        out.writeBytes("<HiddenDim>" + fromNetwork.hiddenLayerDim + "</HiddenDim>\n");
        if (useSplines)
            out.writeBytes("<InputDim>" + (params.length * NetworkInterface.splineRepresentationRes) + "</InputDim>\n");
        else
            out.writeBytes("<InputDim>" + params.length + "</InputDim>\n");
        out.writeBytes("</RequiredNetworkSettings>\n");
        out.writeBytes("<OptionalNetworkSettings>\n");
        out.writeBytes("<!--these are optional network settings. If not supplied defaults will be used-->\n");
        out.writeBytes("<LearningRate>" + fromNetwork.eta + "</LearningRate>\n");
        out.writeBytes("<Momentum>" + ((BP) fromNetwork).beta + "</Momentum>\n");
        out.writeBytes("<LearningIncrease>" + ((BP) fromNetwork).etaUP + "</LearningIncrease>\n");
        out.writeBytes("<LearningDecrease>" + ((BP) fromNetwork).etaDOWN + "</LearningDecrease>\n");
        out.writeBytes("</OptionalNetworkSettings>\n");
        out.writeBytes("<Patterns>\n");
        for (int i = 0; i < fromNetwork.patc; i++)
        {
            if (useSplines)
            {
                ParamsNode parnode = new ParamsNode(NetworkInterface.MAX_patternlength, params);
                out.writeBytes("<Pattern>\n");

                if (fromNetworkType.equals("HebbianSpline"))
                {
                    out.writeBytes("<InputPattern>\n");
                    for (int k = 0; k < NetworkInterface.splineRepresentationRes * params.length; k++)
                    {
                        out.writeBytes("<Value>" + ((Hebbian) fromNetwork).trainingInput[i][k] + "</Value>");
                    }
                    out.writeBytes("</InputPattern>\n");
                    out.writeBytes("<OutputPattern>");
                    for (int k = 0; k < ((Hebbian) fromNetwork).teacherDim; k++)
                    {
                        out.writeBytes("<Value>" + ((Hebbian) fromNetwork).trainingTeacher[i][k] + "</Value>");
                    }
                    out.writeBytes("</OutputPattern>\n");
                    out.writeBytes("</Pattern>\n");
                }
                else if (fromNetworkType.startsWith("BPTT"))
                {
                    for (int j = 0; j < ((BPTT) fromNetwork).trainingSeqLength[i]; j++)
                    {
                        for (int k = 0; k < params.length; k++)
                        {
                            parnode.put(params[k], ((BPTT) fromNetwork).trainingInputSeq[i][j][k]);
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
                    out.writeBytes("<OutputPattern>");
                    for (int k = 0; k < ((BPTT) fromNetwork).outputLayerDim; k++)
                    {
                        out.writeBytes("<Value>" + ((BPTT) fromNetwork).trainingOutputSeq[i][10][k] + "</Value>");
                    }
                    out.writeBytes("</OutputPattern>\n");
                    out.writeBytes("</Pattern>\n");
                }
            }
            else
            {
                if (fromNetworkType.equals("Hebbian"))
                {
                    out.writeBytes("<Pattern>\n");
                    out.writeBytes("<InputPattern>");
                    for (int k = 0; k < params.length; k++)
                    {
                        out.writeBytes("<Value>" + ((Hebbian) fromNetwork).trainingInput[i][k] + "</Value>");
                    }
                    out.writeBytes("</InputPattern>\n");
                    out.writeBytes("<OutputPattern>");
                    for (int k = 0; k < ((Hebbian) fromNetwork).teacherDim; k++)
                    {
                        out.writeBytes("<Value>" + ((Hebbian) fromNetwork).trainingTeacher[i][k] + "</Value>");
                    }
                    out.writeBytes("</OutputPattern>\n");
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
                        out.writeBytes("<OutputPattern>");
                        for (int k = 0; k < ((BPTT) fromNetwork).outputLayerDim; k++)
                        {
                            out.writeBytes("<Value>" + ((BPTT) fromNetwork).trainingOutputSeq[i][j][k] + "</Value>");
                        }
                        out.writeBytes("</OutputPattern>\n");
                        out.writeBytes("</Pattern>\n");
                    }
                }
            }
        }
        out.writeBytes("</Patterns>\n");
        out.writeBytes("</PatternSet>\n");
    }

    /**
     * Reads network weights from a local file
     *
     * @param weightFilename - weight file name
     * @return - successful
     */
    public boolean readWeight(final String weightFilename)
    {
        return readWeight(null, weightFilename);
    }

    /**
     * Reads network weights from a file
     *
     * @param base           - base URL, NULL if local file
     * @param weightFilename - filename
     * @return - successful
     */
    public boolean readWeight(final URL base, final String weightFilename)
    {
        fileInputToHiddenW = null;
        fileHiddenToOutputW = null;
        fileInputLayerDim = 0;
        fileHiddenLayerDim = 0;
        fileOutputLayerDim = 0;

        int tc, linec, row = 0;
        String s, u;
        boolean added;
        try
        {
            final BufferedReader in;
            if (base == null)
                in = new BufferedReader(new InputStreamReader(Elib.openfileREAD(weightFilename)));
            else
                in = new BufferedReader(new InputStreamReader(Elib.openfileREAD(weightFilename)));
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
                    if (fileInputLayerDim == 0 || fileHiddenLayerDim == 0 || fileOutputLayerDim == 0)
                    {
                        Log.println("The weight file doesn't specify the net size properly!");
                    }

                    if (fileInputToHiddenW == null)
                        fileInputToHiddenW = new double[fileHiddenLayerDim][fileInputLayerDim];
                    if (fileHiddenToOutputW == null)
                        fileHiddenToOutputW = new double[fileOutputLayerDim][fileHiddenLayerDim];

                    if (row < fileHiddenLayerDim)
                        fileInputToHiddenW[row][tc] = Elib.toDouble(u);
                    else
                        fileHiddenToOutputW[row - fileHiddenLayerDim][tc] = Elib.toDouble(u);
                    added = true;
                    tc++;
                }
                if (!added)
                    continue;
                if (tc != ((row < fileHiddenLayerDim) ? fileInputLayerDim : fileHiddenLayerDim))
                {
                    Log.println("File format Error in " + weightFilename + " line " + linec);
                    return false;
                }
                row++;
            }
            in.close();
        }
        catch (IOException e)
        {
            Log.println("BP.readPattern() : EXCEPTION " + e);
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
            out.writeBytes("# The network computes  sgn(hiddenToOutputW.sgn(inputToHiddenW.x)) where sgn(t)=1/(1+exp(-t))\n\n");

            out.writeBytes("outputdim  " + outputLayerDim + "\nhiddendim  " + hiddenLayerDim + "\ninputdim   " + inputLayerDim + "\n\n");
            out.writeBytes("#input  -> hidden weights  inputToHiddenW[" + (hiddenLayerDim) + "][" + (inputLayerDim) + "]\n");
            for (int i = 0; i < hiddenLayerDim; i++)
            {
                for (int j = 0; j < inputLayerDim; j++)
                    out.writeBytes(inputToHiddenW[i][j] + " ");
                out.writeBytes("\n");
            }

            out.writeBytes("\n#hidden -> output weights  hiddenToOutputW[" + (outputLayerDim) + "][" + (hiddenLayerDim) + "]:\n");
            for (int k = 0; k < outputLayerDim; k++)
            {
                for (int i = 0; i < hiddenLayerDim; i++)
                    out.writeBytes(hiddenToOutputW[k][i] + " ");
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
        epochs.setInheritsPopupMenu(false);
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
        training_threshold.setToolTipText("1.0");
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
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright
*/
