package mns2.comp;


import java.io.DataOutputStream;
import java.io.IOException;

import mns2.util.ParamsNode;
import mns2.main.Main;
import sim.motor.Graspable;
import sim.util.*;

/**
 * NetworkInterface
 *  - Runs network forward, interprets and returns output.
 *  - Writes grasp training set XML file
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */
public class NetworkInterface
{
    // Number of shuffled patterns to write for every normal one
    static public final int shuffledInputCount=1;

    // Maximum pattern length
    static public final int MAX_patternlength=150;

    // Network stats
    static public int indim=7;
    static public int hebbdim=3;
    static public int outdim= Graspable.grasps.length;
    static public int hiddim=10;
    static public int rindim=5;
    static public int routdim=5;

    static public boolean useSplines=false;
    static public int splineRepresentationRes=30;

    // Pattern file output stream
    static public DataOutputStream out=null;

    // Pattern file name
    static public String fname="action_pattern.xml";

    // Whether or not to copy output to console
    static final boolean COPYCON=true;

    // Learning rate for BPTT
    static public double learningrate    =   0.1;

    // Learning rate for Hebbian learning
    static public double hebbian_lr      =   0.01;

    // Momentum term for BPTT
    static public double momentum        =   0.0;

    // Change in BPTT learning rate when increased
    static public double learningincrease=   0.01;

    // Change in BPTT learning rate when decreased
    static public double learningdecrease=   0.1;

    // Number of grasps writtern to training set
    static int graspCount=0;

    // Last network output
    static public double[] lastout=null;

    // Display for output
    static public NDisp ndisp=null;

    // Network input parameters
    static public String[]  params={"aper1", "ang1","ang2", "speed","dist","axisdisp1","axisdisp2","sound0",
                                    "sound1", "sound2"};

    static public String netType="BPTTwithHebbian";

    // Parameter node containing pattern input parameters
    //static public ParamsNode parnode=new ParamsNode(NetworkInterface.MAX_patternlength,params);

    /***
     * Constructor
     */
    /*public NetworkInterface()
    {
    }*/

    /**
     * Clears inputs and resets status flag
     */
    static public void prepareForInput(String ntype)
    {
        netType=ntype;
        if(netType.equals("BPTTwithHebbian"))
        {
            params=new String[]{"aper1", "ang1","ang2", "speed","dist","axisdisp1","axisdisp2",
                                "sound0", "sound1", "sound2"};
        }
        else if(netType.startsWith("BP"))
        {
            params=new String[]{"aper1", "ang1","ang2", "speed","dist","axisdisp1","axisdisp2",
                                "d_aper1", "d_ang1","d_ang2", "d_speed","d_dist","d_axisdisp1","d_axisdisp2"};
        }
        else if(netType.equals("Hebbian"))
        {
            params=new String[]{"aper1", "ang1","ang2", "speed","dist","axisdisp1","axisdisp2",
                                "d_aper1", "d_ang1","d_ang2", "d_speed","d_dist","d_axisdisp1","d_axisdisp2"};
        }
        //parnode=new ParamsNode(NetworkInterface.MAX_patternlength,params);
        //parnode.reset();
    }

    /**
     * Writes string to output stream
     * @param out - DataOutputStream to write to
     * @param s - String to write
     * @throws IOException
     */
    static public void writeout(final DataOutputStream out, final String s) throws IOException
    {
        //Log.println("=====> "+ s);
        out.writeBytes(s);
    }



    /**
     * Write a negative example to the training set
     * @param obj - Object info
     * @param ss - Grasp info
     */
    static public void writeNegPattern(final Graspable obj, final String ss, final ParamsNode parNode)
    {
        final boolean saveHV= Main.negativeExample;
        Main.negativeExample=true;
        writePattern(obj,ss,parNode);
        Main.negativeExample=saveHV;
    }

    /**
     * Write pattern to file
     * @param obj - Object info
     * @param ss - Grasp info
     */
    static public void writePattern(final Graspable obj, final String ss, final ParamsNode parNode)
    {
        //Log.println("------> Writing pattern "+graspCount+". Object:"+obj.myname+" affords "+Graspable.grasps[obj.affordance]);

        try
        {
            // If this is the first pattern
            if (out==null)
            {
                // Open file and write header
                out = Elib.openfileWRITE(fname);
                if (out==null)
                {
                    //Log.println("Error while creating file:"+fname);
                    return;
                }
                writeHeader();
            }

            // Write input and output sequences for pattern
            writeInputOutputSequences(obj, ss, parNode);
        }
        catch (Exception e)
        {}
    }

    /**
     * Write pattern file header
     * @throws IOException
     */
    static private void writeHeader() throws IOException
    {
        //System.out.println("Writing header");
        writeout(out,"<!-- This pattern file is generated by reach& grasp simulator HV (by Erhan Oztop -April'00)-->\n");
        writeout(out, "<PatternSet>\n");
        writeout(out, "<RequiredNetworkSettings>\n");
        writeout(out,"<OutputDim>"+outdim+"</OutputDim>\n");
        writeout(out, "<HiddenDim>"+hiddim+"</HiddenDim>\n");
        if(netType.startsWith("Hebbian"))
            writeout(out, "<TeacherDim>"+hiddim+"</TeacherDim>\n");
        if(((mns2.comp.VisualProcessor)((mns2.main.Main)Main.self).selfArmHand.visualProcessor).encodeDerivative)
            writeout(out, "<InputDim>"+(indim*2)+"</InputDim>\n");
        else
            writeout(out, "<InputDim>"+indim+"</InputDim>\n");    
        if(netType.equals("BPTTwithHebbian"))
            writeout(out, "<HebbianInputDim>"+hebbdim+"</HebbianInputDim>\n");
        if(netType.startsWith("BPTT"))
            writeout(out, "<RecurrentInputDim>"+rindim+"</RecurrentInputDim>\n<RecurrentOutputDim>"+routdim+"</RecurrentOutputDim>\n");
        writeout(out, "</RequiredNetworkSettings>\n");
        writeout(out, "<OptionalNetworkSettings>\n");
        writeout(out,"<!--these are optional network settings. If not supplied defaults will be used-->\n");
        if(netType.startsWith("BPTT"))
            writeout(out,"<BPTTLearningRate>"+learningrate+"</BPTTLearningRate>\n");
        else if(netType.equals("Hebbian"))
            writeout(out,"<LearningRate>"+learningrate+"</LearningRate>\n");
        if(netType.equals("BPTTwithHebbian"))
            writeout(out,"<HebbianLearningRate>"+hebbian_lr+"</HebbianLearningRate>\n");
        if(netType.startsWith("BPTT"))
        {
            writeout(out,"<Momentum>"+momentum+"</Momentum>\n");
            writeout(out,"<LearningIncrease>"+learningincrease+"</LearningIncrease>\n");
            writeout(out,"<LearningDecrease>"+learningdecrease+"</LearningDecrease>\n");
        }
        writeout(out, "</OptionalNetworkSettings>\n");
        writeout(out,"<!-- The parameters coding follows this order:\n#");
        for (int i=0;i<params.length;i++)
            out.writeBytes(params[i]+" ");
        writeout(out,"\nThe parameters are in temporal sequences -->\n");
        writeout(out,"<!-- For each correct pattern there are ("+shuffledInputCount+") shuffled -wrong- pattern. -->\n");
        writeout(out,"<Sequences>\n");
    }

    /**
     * Closes pattern file output stream
     */
    static public void close()
    {
        try
        {
            if (out!=null)
            {
                writeout(out, "</Sequences>\n</PatternSet>\n");
                out.close();
            }
        }
        catch(IOException e) {}
        out=null;
    }

    /**
     * Writes pattern input and output sequences to file
     * @param obj - Object info
     * @param info - Grasp info
     * @throws IOException
     */
    static public void writeInputOutputSequences(final Graspable obj, final String info, final ParamsNode parNode) throws IOException
    {
        // Write sequence header
        writeout(out, "<Sequence>\n");
        final String comment = "<!-- Grasp [" + graspCount + "] for " + obj.myname + " with " + Graspable.grasps[obj.lastPlan-1] +
                ", obj-aper-size:" + obj.objsize + "[" + info + "] " + "obj-location:("+obj.objectCenter.x+", "+
                obj.objectCenter.y+", "+obj.objectCenter.z+")-->\n";
        writeout(out, comment);

        // Length of sequence
        int len=0;

        // Get input values sequence
        final double[][] input = parNode.getAll();

        // Compute sequence length - Look for when all inputs go to zero
        for(int i=1; i<100; i++)
        {
            boolean allZero = true;
            for(int j=0; j<params.length; j++)
            {
                if(input[j][i] > 0.0)
                {
                    allZero = false;
                    break;
                }
            }
            // Increment computed length of input sequence as long as all inputs are not 0
            if(!allZero)
                len++;
            else
                break;
        }
        writeout(out, "<SequenceLength>"+(len)+"</SequenceLength>\n");

        // Write input sequence
        final String seq = formInputSequencePattern(len, parNode);
        writeout(out, seq);

        // Create positive and negative example output sequences
        final String mir = formMirrorPattern(len, obj.lastPlan-1);
        final String zero= formMirrorPattern(len, -11);

        // Decide which one to write
        if (!Main.negativeExample)
        {
            writeout(out,mir);
        }
        else
        {
            Log.println("Writing negative example...");
            writeout(out, zero);
        }

        // End sequence and increment number of grasp sequences written
        writeout(out, "</Sequence>\n");
        graspCount++;

        //Let's shuffle the hand state trajectory
        if (!Main.negativeExample)
        {
            for(int i=0; i<shuffledInputCount; i++)
            {
                Log.println("Writing shuffled example...");
                writeout(out, "<Sequence>\n");
                writeout(out, "<SequenceLength>"+len+"</SequenceLength>\n");
                final String shuffled = formShuffledPattern(len,parNode);
                writeout(out, shuffled);
                writeout(out, zero);
                writeout(out, "</Sequence>\n");
            }
        }
    }

    /**
     * Constructs input sequence in XML form
     * @param len
     */
    static public String formInputSequencePattern(final int len, final ParamsNode parNode)
    {
        String s="";
        if(netType.startsWith("BPTT"))
        {
            s="<InputSequence>\n";
            final double[][] input = parNode.getAll();
            for(int i=0; i<len; i++)
            {
                if(netType.equals("BPTTwithHebbian"))
                {
                    s += "<Input>";
                    for(int j=0; j<7; j++)
                        s += "<Value>" + input[j][i] + "</Value>";
                    s += "</Input>\n";
                    s += "<HebbianInput>";
                    for(int j=7; j<params.length; j++)
                        s += "<Value>" + input[j][i] + "</Value>";
                    s += "</HebbianInput>\n";
                }
                else
                {
                    s += "<Input>";
                    for(int j=0; j<params.length; j++)
                        s += "<Value>" + input[j][i] + "</Value>";
                    s += "</Input>\n";
                }
            }
            s += "</InputSequence>\n";
        }
        else if(netType.equals("BP") || netType.equals("Hebbian"))
        {
            if(useSplines)
            {
                s="<InputPattern>\n";
                Spline sp[]=parNode.getSplines();
                double step=1.0/(splineRepresentationRes-1);
                for (int i=0;i<sp.length;i++)
                {
                    for (int j=0;j<splineRepresentationRes;j++)
                    {
                        double v=sp[i].eval(j*step);
                        s+="<Value>"+v+"</Value>";
                    }
                }
                s+="</InputPattern>\n";
            }
        }
        return s;
    }

    /**
     * The mirror response coding - Constructs positive example output sequence in XML format
     * @param len - Length of output sequence
     * @param affordance - Affordance for the current grasp
     */
    static public String formMirrorPattern(final int len, final int affordance)
    {
        String s="<OutputSequence>\n";
        if(netType.equals("Hebbian"))
            s="<TeacherPattern>\n";
        else if(netType.equals("BP"))
            s="<OutputPattern>\n";
        for(int j=0; j<len; j++)
        {
            if(netType.startsWith("BPTT"))
                s += "<Output>";
            for (int i=0;i<outdim;i++)
            {
                s+="<Value>";
                if (affordance==i && j >= 5)
                    s+=1;
                else
                    s+=0;
                s+="</Value>";
            }
            if(netType.startsWith("BPTT"))
                s += "</Output>\n";
        }
        if(netType.equals("Hebbian"))
            s += "</TeacherPattern>\n";
        else if(netType.equals("BP"))
            s += "</OutputPattern>\n";
        else
            s += "</OutputSequence>\n";

        return s;
    }

    /**
     * Forms a randomly shuffled version of the input sequence in XML format
     * @param len - Length of input sequence
     */
    static public String formShuffledPattern(final int len, final ParamsNode parNode)
    {
        String s="<InputSequence>\n";

        final double[][] input = parNode.getAll();
        for(int i=0; i<len; i++)
        {
            // Calculate 2 random indices
            final int i1 = (int)(Math.random() * (len-1));
            final int i2 = (int)(Math.random() * (len-1));

            // Swap the rows at these indices
            final double[] temp = new double[params.length];
            for(int j=0; j<params.length; j++)
                temp[j] = input[j][i1];
            for(int j=0; j<params.length; j++)
                input[j][i1] = input[j][i2];
            for(int j=0; j<params.length; j++)
                input[j][i2] = temp[j];
        }

        // Put scrambled input sequence in XML formatted string
        for(int i=0; i<len; i++)
        {
            s += "<Input>";
            for(int j=0; j<params.length; j++)
                s += "<Value>" + input[j][i] + "</Value>";
            s += "</Input>\n";
        }
        s += "</InputSequence>\n";
        return s;
    }

    /**
     * Apply current time step's input to the network and compute its output
     * @param network - Network to use
     * @param plot - Whether or not to plot network activity
     */
    static public String recognize(final Network network, final ParamsNode parNode, final boolean plot,
                                   final boolean staticAction)
    {
        if(network.getClass().equals(BPTTwithHebbian.class))
        {
            // Get input for network
            final double[] inp=formInputArray(((BPTTwithHebbian)network).t,parNode);
            // Apply input to network and get output
            final double[] out=((BPTTwithHebbian)network).ask(inp,inp.length,plot,staticAction);
            // Get values of hidden layer units
            final double[] hidden=((BPTTwithHebbian)network).hiddenLayer;

            // Create new output display
            if (ndisp==null || !ndisp.isShowing())
            {
                ndisp=new NDisp(network,"mirror output");
                ndisp.setBounds(100,100,400,400);
                ndisp.setVisible(true);
            }

            // Present network output
            ndisp.drawArray(out,0,1,null,out.length,1);
            ((BPTTwithHebbian)network).panel.repaint();

            // Set last network output
            lastout=out;

            // Dump network state to console
            String s="   OOOOOO [";
            for (int i=0;i<out.length;i++)
                s+=Elib.nice(out[i],1e4)+"   ";
            Log.println(s+"]");
            s="   HHHHHH [ ";
            for (int i=0;i<hidden.length;i++)
                s+=Elib.nice(hidden[i],1e4)+"   ";
            Log.println(s+"]");
            s="   IIIIII [ ";
            for (int i=0; i<inp.length; i++)
                s+=Elib.nice(inp[i],1e4)+"    ";
            Log.println(s+"]");

            // Interpret network output and return
            return interpret(out);
        }
        else if(network.getClass().equals(BPTT.class))
        {
            // Get input for network
            final double[] inp=formInputArray(((BPTT)network).t,parNode);
            // Apply input to network and get output
            final double[] out=((BPTT)network).ask(inp,inp.length,plot,staticAction);
            // Get values of hidden layer units
            final double[] hidden=((BPTT)network).hiddenLayer;

            // Create new output display
            if (ndisp==null || !ndisp.isShowing())
            {
                ndisp=new NDisp(network,"mirror output");
                ndisp.setBounds(100,100,400,400);
                ndisp.setVisible(true);
            }

            // Present network output
            ndisp.drawArray(out,0,1,null,out.length,1);

            // Set last network output
            lastout=out;

            // Dump network state to console
            String s="   OOOOOO [";
            for (int i=0;i<out.length;i++)
                s+=Elib.nice(out[i],1e4)+"   ";
            Log.println(s+"]");
            s="   HHHHHH [ ";
            for (int i=0;i<hidden.length;i++)
                s+=Elib.nice(hidden[i],1e4)+"   ";
            Log.println(s+"]");
            s="   IIIIII [ ";
            for (int i=0; i<inp.length; i++)
                s+=Elib.nice(inp[i],1e4)+"    ";
            Log.println(s+"]");

            // Interpret network output and return
            return interpret(out);
        }
        else if(network.getClass().equals(BP.class))
        {
            // Get input for network
            final double[] inp=formInputArray(((BP)network).t,parNode);
            // Apply input to network and get output
            final double[] out=((BP)network).ask(inp,inp.length,plot,staticAction);
            // Get values of hidden layer units
            final double[] hidden=((BP)network).hiddenLayer;

            // Create new output display
            if (ndisp==null || !ndisp.isShowing())
            {
                ndisp=new NDisp(network,"mirror output");
                ndisp.setBounds(100,100,400,400);
                ndisp.setVisible(true);
            }

            // Present network output
            ndisp.drawArray(out,0,1,null,out.length,1);

            // Set last network output
            lastout=out;

            // Dump network state to console
            String s="   OOOOOO [";
            for (int i=0;i<out.length;i++)
                s+=Elib.nice(out[i],1e4)+"   ";
            Log.println(s+"]");
            s="   HHHHHH [ ";
            for (int i=0;i<hidden.length;i++)
                s+=Elib.nice(hidden[i],1e4)+"   ";
            Log.println(s+"]");
            s="   IIIIII [ ";
            for (int i=0; i<inp.length; i++)
                s+=Elib.nice(inp[i],1e4)+"    ";
            Log.println(s+"]");

            // Interpret network output and return
            return interpret(out);
        }
        else if(network.getClass().equals(Hebbian.class))
        {
            // Get input for network
            final double[] inp=formInputArray(((Hebbian)network).t,parNode);
            // Apply input to network and get output
            final double[] out=((Hebbian)network).ask(inp,inp.length,plot,staticAction);

            // Set last network output
            lastout=out;

            // Dump network state to console
            String s="   OOOOOO [";
            for (int i=0;i<out.length;i++)
                s+=Elib.nice(out[i],1e4)+"   ";
            Log.println(s+"]");
            s="   IIIIII [ ";
            for (int i=0; i<inp.length; i++)
                s+=Elib.nice(inp[i],1e4)+"    ";
            Log.println(s+"]");

            // Interpret network output and return
            return interpret_hebbian(out, ((Hebbian)network).teacherLayer, ((Hebbian)network).teacherToOutputW);
        }
        return "";
    }

    /**
     * Form network input array for given time step
     * @param t - Time step
     */
    static public double[] formInputArray(int t, ParamsNode parNode)
    {
        if((netType.equals("BP") || netType.equals("Hebbian")) && useSplines)
        {
            Spline sp[]=parNode.getSplines();
            int size=splineRepresentationRes*sp.length;
            double[] inpattern=new double[size];
            //System.out.println("Input size:"+size);
            double step=1.0/(splineRepresentationRes-1);
            int k=0;
            for (int i=0;i<sp.length;i++)
            {
                for (int j=0;j<splineRepresentationRes;j++)
                {
                    inpattern[k++]=sp[i].eval(j*step);
                }
            }
            return inpattern;
        }
        else
        {
            // Get the input sequence
            final double[][] input = parNode.getAll();
            final int size=params.length;
            final double[] insequence=new double[size];
            int t_1=t;
            for(int i=0; i<size; i++)
            {
                //if(i>=7)
                //    t_1=t+2;
                insequence[i] = input[i][t_1];
            }

            // Check its dimensions
            if (insequence.length!=params.length)
                Log.println("WARNING: input size mismatch. Put in "+params.length+" values...");

            return insequence;
        }
    }

    /**
     * Interpret the network's pattern of output activity
     * @param ans - Network output
     */
    static public String interpret(final double[] ans)
    {
        // Min and Max unit activity
        double min=1e10;
        double max=-1e10;

        // The index of the maximally active unit
        int maxix=-1;

        // Find the min and max units
        for (int i=0;i<ans.length;i++)
        {
            if (ans[i]<min)
            {
                min=ans[i];
            }
            if (ans[i]>max)
            {
                maxix=i;
                max=ans[i];
            }
        }

        // Get the name of the grasp that the max unit codes for
        final String s;
        if ((max-min)>0.1)
            s=Graspable.getGrasp(maxix);
          else
            s="[not confident] "+Graspable.getGrasp(maxix);
        return s;
    }

    static public String interpret_hebbian(final double[] out, final double[] teacher, final double[][] teacherToOutputW)
    {

        // Min and Max unit activity
        double min=1e10;
        double max=-1e10;

        // The index of the teacher unit connected to the maximally active output unit
        int maxix=-1;

        double[] totals = new double[teacher.length];
        for(int i=0; i<teacher.length; i++)
        {
            for(int j=0; j<out.length; j++)
            {
                if(teacherToOutputW[j][i]>0)
                    totals[i] += out[j];
            }
        }
        // Find the min and max units
        for (int i=0;i<totals.length;i++)
        {
            if (totals[i]<min)
            {
                min=totals[i];
            }
            if (totals[i]>max)
            {
                maxix=i;
                max=totals[i];
            }
        }

        // Get the name of the grasp that the max unit codes for
        final String s;
        if ((max-min)>0.1)
            s=Graspable.getGrasp(maxix);
        else
            s="[not confident] "+Graspable.getGrasp(maxix);
        return s;
    }
  }
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/


