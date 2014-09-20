package mns2.main;
import java.io.DataOutputStream;

import mns2.comp.BP;
import mns2.comp.BPTT;
import mns2.comp.BPTTwithHebbian;
import mns2.comp.Hebbian;
import mns2.comp.Network;
import sim.util.Elib;

public class ConvertTrainingData
{
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String from=args[0];
        String to=args[2];

        Network net=new Network();
        if(from.equals("BPTT"))
            net = new BPTT();
        if(from.equals("BPTTwithHebbian"))
            net = new BPTTwithHebbian();
        if(from.startsWith("BP"))
            net = new BP();
        else if(from.startsWith("Hebbian"))
            net = new Hebbian();
        net.netFromPattern(args[1]);

        boolean encodeDerivative=false;
        if(to.endsWith("Delta"))
            encodeDerivative=true;
        boolean useSplines=false;
        if(to.endsWith("Spline"))
            useSplines=true;

        try
        {
            // Open file and write header
            DataOutputStream out = Elib.openfileWRITE(args[3]);
            if (out==null)
            {
                System.err.println("Error while creating file:"+args[3]);
                return;
            }

            if(to.equals("BPTT"))
                BPTT.convertPattern(from, net, out, encodeDerivative, useSplines);
            else if(to.startsWith("BP"))
                BP.convertPattern(from, net, out, encodeDerivative, useSplines);
            else if(to.startsWith("Hebbian"))
                Hebbian.convertPattern(from, net, out, encodeDerivative, useSplines);
            out.close();
            System.out.println("Done!");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}
