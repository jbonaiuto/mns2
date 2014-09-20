package mns2.main;

import mns2.comp.AuditoryProcessor;
import mns2.comp.BPTT;
import sim.util.VA;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Nov 14, 2005
 * Time: 5:46:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAuditory
{
    public static void main(String args[])
    {
        for(int k=1; k<args.length; k++)
        {
            BPTT net = new BPTT();
            net.netFromWeight(args[0]);
            net.plots = new String[]{"input","output","hidden","recurrentInput","recurrentOutput"};
            net.plot_output_files = new String[]{args[k]+"_input",args[k]+"_output",args[k]+"_hidden",args[k]+"_recurrentIn",args[k]+"_recurrentOut"};
            net.plot_labels=new String[5][];
            net.plot_labels[1] = new String[]{"paper", "wood", "slap"};
            net.plot_labels[3] = new String[]{"recurrentInput0","recurrentInput1","recurrentInput2","recurrentInput3","recurrentInput4"};
            net.plot_labels[4] = new String[]{"recurrentOutput0","recurrentOutput1","recurrentOutput2","recurrentOutput3","recurrentOutput4"};
            net.plot_dimensions = new int[]{2,2,2,2,2};
            net.plot_extra_command= new String[]{"set xlabel \"Time\";set ylabel \"Activation\";","set xlabel \"Time\";set ylabel \"Activation\";","set xlabel \"Time\";set ylabel \"Activation\";","set xlabel \"Time\";set ylabel \"Activation\";","set xlabel \"Time\";set ylabel \"Activation\";"};
            double[][] sequence = VA.transpose(AuditoryProcessor.getAuditoryPatternFromMatlab(args[k]));
            for(int i=0; i<sequence.length; i++)
                net.ask(sequence[i], sequence[i].length, false,false);
            for(int i=0; i<50; i++)
                net.ask(new double[net.inputLayerDim], net.inputLayerDim, i==49, true);
        }
        AuditoryProcessor.matlabEngine.engCloseAll();
    }
}
