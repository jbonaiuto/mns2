package mns2.comp;

import mns2.motor.ArmHand;
import mns2.util.ParamsNode;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;

import sim.util.Elib;
import sim.util.Log;
import sim.util.Resource;
import sim.util.VA;
import sim.motor.Graspable;

/**
 * Processes auditory information
 */
public class AuditoryProcessor
{
    // Time step for sound input
    private static int soundT=0;

    public static jmatlink.JMatLink matlabEngine=null;

    public static BPTT network;

    private static double[][] soundPattern;

    private static String soundFilename ="";

    public static String soundWeightFile="sound_bptt.wgt";

    //Number of steps in the audio network per visual network time step
    private static int AV_TIME_RATIO=10;

    /**
     * Constructor
     */
    public static void reset()
    {
        soundT=0;
        if(network==null)
            network=new BPTT();
        network.clearHistory();
        network.t=0;
        network.netFromWeight(soundWeightFile);
    }

    static public ParamsNode collectAuditoryInput(final ArmHand handInfo, final Network recognizeNet, final Graspable obj,
                                                  ParamsNode parnode, final boolean congruentSound)
    {
        String soundFile=getSoundFileName(obj, congruentSound);
        if(!soundFile.equals(soundFilename) || soundPattern==null || soundPattern.length==0)
        {
            // Get the sound and audio pattern
            soundFilename =soundFile;
            if(Resource.getString("useMatlab").equals("true"))
                soundPattern = getAuditoryPatternFromMatlab(soundFilename);
            else
            {
                soundPattern = getAuditoryPatternFromFile(soundFilename.replace("wav","txt"));
            }
            network.clearHistory();
        }

        // If the action is audible, the hand is contacting the object, and the sound is not finished
        double averagedOutput[] = new double[network.outputLayerDim];
        int i;
        for(i=0; i<AV_TIME_RATIO && (soundT*AV_TIME_RATIO)+i<soundPattern.length; i++)
        {
            if(handInfo.audible && handInfo.contact && soundT < (soundPattern.length/AV_TIME_RATIO))
            {
                double pattern[] = new double[network.inputLayerDim];
                if(handInfo.audible)
                    System.arraycopy(soundPattern[(soundT*AV_TIME_RATIO)+i], 0, pattern, 0, network.inputLayerDim);
                //double out[] = network.ask(pattern,pattern.length,((soundT*AV_TIME_RATIO)+i==soundPattern.length-1);
                double out[] = network.ask(pattern,pattern.length,false,false);
                VA.addto(averagedOutput,network.outputLayerDim,out);
            }
            if(recognizeNet!=null && network.recordNetwork)
            {
                //network.plotNetworkActivity();
                // Dump network state to console
                String s="a   OOOOOO [";
                for (int j=0;j<network.outputLayerDim;j++)
                    s+= Elib.nice(network.outputLayer[j], 1e4)+"   ";
                Log.println(s + "]");
                /*s="a   HHHHHH [ ";
                for (int i=0;i<network.hiddenLayerDim;i++)
                    s+=Elib.nice(network.hiddenLayer[i],1e4)+"   ";
                Log.println(s+"]");
                s="a   IIIIII [ ";
                for (int i=0; i<network.inputLayerDim; i++)
                    s+=Elib.nice(network.inputLayer[i],1e4)+"    ";
                Log.println(s+"]");*/
            }
        }
        if(handInfo.audible && handInfo.contact && soundT < (soundPattern.length/AV_TIME_RATIO))
        {
            VA.divideBy(averagedOutput, network.outputLayerDim, (i+1));
            // Collect audio input pattern
            for(int j=0; j<network.outputLayerDim; j++)
                parnode.put("sound"+j, averagedOutput[j]);
            // Increment sound time step
            soundT++;
        }
        else
        {
            for(int j=0; j<network.outputLayerDim; j++)
                parnode.put("sound"+j, 0.0);
        }
        // Otherwise collect audio input as 0's
        return parnode;
    }

    public static String getSoundFileName(final Graspable obj, final boolean congruentSound)
    {
        String soundFile="";
        if((obj.lastPlan == Graspable.POWER && congruentSound) || (obj.lastPlan == Graspable.PRECISION && obj.myname.equals("objects/box.seg") && !congruentSound))
        {
            soundFile="sounds/slap.wav";
        }
        else if((obj.myname.equals("objects/box.seg") && obj.lastPlan == Graspable.PRECISION  && congruentSound) || (obj.lastPlan == Graspable.POWER && !congruentSound))
        {
            soundFile="sounds/wood.wav";
        }
        return soundFile;
    }

    /**
     * Returns pattern of auditory neuron activity associated with action (pre-computed using the Lyon Passive Ear model
     * @param soundFile - Name of the file containing the auditory nerve firing probabilities for the given sound
     */
    public static double[][] getAuditoryPatternFromFile(final String soundFile)
    {
        double[][] sound=null;

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(soundFile)));
            Vector matrix = new Vector();
            while(reader.ready())
            {
                String s = reader.readLine();
                StringTokenizer t = new StringTokenizer(s);
                Vector row = new Vector();
                while(t.hasMoreElements())
                {
                    String elem = t.nextElement().toString();
                    row.add(new Double(Double.parseDouble(elem)));
                }
                matrix.add(row);
            }
            if(matrix.size()>0)
            {
                sound = new double[((Vector)matrix.get(0)).size()][matrix.size()];
                for(int i=0; i<matrix.size(); i++)
                {
                    Vector row = (Vector)matrix.get(i);
                    for(int j=0; j<row.size(); j++)
                    {
                        sound[j][i] = ((Double)row.get(j)).doubleValue();
                    }
                }
            }
        }
        catch(FileNotFoundException e)
        {}
        catch(IOException e)
        {}

        return sound;
    }

    /**
     * Returns pattern of auditory neuron activity associated with action (computed in real time from the Lyon
     * Passive Ear model implemented in Matlab
     * @param soundFile - Name of the wav file containing the sound
     */
    public static double[][] getAuditoryPatternFromMatlab(final String soundFile)
    {
        try
        {
            if(matlabEngine==null)
            {
                matlabEngine = new jmatlink.JMatLink();
                matlabEngine.engOpen();
            }
            matlabEngine.engEvalString("cd "+getCWD()+"/lib/auditoryToolbox");
            matlabEngine.engEvalString("y=LyonPassiveEar(wavread('../../"+soundFile+"'),10000,100);");
            matlabEngine.engEvalString("y=y/max(max(y));");
            return VA.transpose(matlabEngine.engGetArray("y"));
        }
        catch(Exception e)
        {

        }

        return new double[0][];
    }

    public static String getCWD()
    {
         //	 Get the "." file present in all directories..
         java.io.File f = new java.io.File(".");
         //	 Get the absolute path to the "." file..
         String cwd = f.getAbsolutePath();
         //	 Return the absolute path minus the "."..
         return cwd.substring(0, cwd.length() - 1);
    }
}
