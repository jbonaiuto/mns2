package mns2.main;

import mns2.comp.BPTT;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Nov 9, 2005
 * Time: 2:56:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAudioNetwork
{
    public static void main(String[] args)
    {
        BPTT net = new BPTT();
        net.netFromWeight(args[0]);
        net.readPattern(args[1]);
        for(int i=0; i<net.patc; i++)
        {
            net.t = 0;
            while(net.t < net.trainingSeqLength[i])
            {
                // Input pattern and compute network output
                net.presentPattern(i);
                net.forward(net.t==net.trainingSeqLength[i]-1,false);
            }
        }
    }
}
