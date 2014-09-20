package mns2.test.comp;

import mns2.comp.BPTT;

import java.util.Vector;

/**
 * Test BPTTHebbian
 */
public class TestBPTT
{
    public static void main(final String[] argv)
    {
        final boolean testForward = testForward();
        if(testForward)
            System.out.println("*** Test forward passed ****");
        else
            System.out.println("### Test forward failed ###");

        final boolean testLearn = testLearn();
        if(testLearn)
            System.out.println("*** Test learn passed ***");
        else
            System.out.println("### Test learn failed ###");
    }

    public static boolean testForward()
    {
        System.out.println("**** test Forward started ****");
        final BPTT bptt=new BPTT();
        bptt.createNet(2,1,1,1,1,2);
        bptt.inputToHiddenW[0][0] = 1.0;
        bptt.inputToHiddenW[0][1] = 0.0;
        bptt.inputToHiddenW[0][2] = 0.5;
        bptt.hiddenToOutputW[0][0] = 0.5;
        bptt.hiddenToOutputW[1][0] = 1.0;
        bptt.recurrentOutputToInputW[0][0] = 1.0;

        bptt.presentPattern(new double[]{0.0, 1.0});
        bptt.forward(false, false);
        if(bptt.hiddenLayer[0] != 0.5)
            return false;
        if(bptt.outputLayer[0] != 0.5621765008857981)
            return false;
        if(bptt.outputLayer[1] != 0.6224593312018546)
            return false;

        bptt.forward(false, false);
        if(bptt.inputLayer[2] != 0.6507776782147005)
            return false;

        return true;
    }

    public static boolean testLearn()
    {
        boolean passed = false;
        System.out.println("**** test Learn started ****");
        final BPTT bptt=new BPTT();
        bptt.createNet(2,8,2,6,6,2);
        bptt.eta = 1.0;
        bptt.patc=2;
        bptt.pat_indim=2;
        bptt.pat_outdim=2;
        bptt.trainingInputSeq = new double[bptt.patc][2][2];
        bptt.trainingOutputSeq = new double[bptt.patc][2][2];
        bptt.trainingSeqLength = new int[bptt.patc];

        bptt.trainingSeqLength[0] = 2;
        bptt.trainingInputSeq[0][0][0] = 0.0;
        bptt.trainingInputSeq[0][0][1] = 1.0;
        bptt.trainingInputSeq[0][1][0] = 1.0;
        bptt.trainingInputSeq[0][1][1] = 1.0;
        bptt.trainingOutputSeq[0][0][0] = 0.0;
        bptt.trainingOutputSeq[0][0][1] = 1.0;
        bptt.trainingOutputSeq[0][1][0] = 0.0;
        bptt.trainingOutputSeq[0][1][1] = 1.0;

        bptt.trainingSeqLength[1] = 2;
        bptt.trainingInputSeq[1][0][0] = 1.0;
        bptt.trainingInputSeq[1][0][1] = 0.0;
        bptt.trainingInputSeq[1][1][0] = 1.0;
        bptt.trainingInputSeq[1][1][1] = 1.0;
        bptt.trainingOutputSeq[1][0][0] = 1.0;
        bptt.trainingOutputSeq[1][0][1] = 0.0;
        bptt.trainingOutputSeq[1][1][0] = 1.0;
        bptt.trainingOutputSeq[1][1][0] = 0.0;

        // Train until it gets it right
        for(int i=0; i<300 && !passed; i++)
        {
            bptt.train(0, 1.1, true);
            if(bptt.correctSequences==bptt.patc)
                passed=true;
        }
        bptt.dumpNet();

        System.out.println("Lesion Recurrent Test:");
        bptt.lesionedConnection = new Vector();
        bptt.lesionedConnection.add("recurrent");
        bptt.lesionTime=new Vector();
        bptt.lesionTime.add("preGrasp");
        bptt.testPattern(true, new int[]{0,1});

        return passed;
    }
}
