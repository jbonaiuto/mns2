package mns2.test.comp;

import mns2.comp.BPTTwithHebbian;

import java.util.Vector;

/**
 * Test BPTTHebbian
 */
public class TestBPTTwithHebbian
{
    public static void main(final String[] argv)
    {
        final boolean testLearn = testLearn();
        if(testLearn)
            System.out.println("*** Test learn passed ***");
        else
            System.out.println("### Test learn failed ###");

        final boolean testLearnFF = testLearnFF();
        if(testLearnFF)
            System.out.println("*** FF net successfully learned sequences ***");
        else
            System.out.println("### FF net could not learn sequences ###");
    }

    public static boolean testLearn()
    {
        boolean passed = false;
        System.out.println("**** test Learn3 started ****");
        final BPTTwithHebbian bptt=new BPTTwithHebbian();
        bptt.netFromPattern("action_pattern.xml");
        // Train until it gets it right
        for(int i=0; i<300 && !passed; i++)
        {
            bptt.train(1000, 1.0, false);
            if(bptt.correctSequences>=bptt.patc*.9)
                passed=true;
        }
        final int testSet[] = new int[bptt.patc];
        for(int i=0; i<bptt.patc; i++)
            testSet[i] = i;
        System.out.println("Lesion Recurrent Test:");
        bptt.lesionedConnection=new Vector();
        bptt.lesionedConnection.add("recurrent");
        bptt.lesionTime=new Vector();
        bptt.lesionTime.add("preGrasp");
        bptt.testPattern(true, testSet);

        bptt.writeWeight("jjb_test.wgt");

        return passed;
    }

    public static boolean testLearnFF()
    {
        boolean passed = false;
        System.out.println("**** test LearnFF started ****");
        final BPTTwithHebbian bptt=new BPTTwithHebbian();
        bptt.netFromPattern("action_pattern.xml");
        int extaudim=bptt.extHebbianInputDim;
        int inputdim=bptt.inputLayerDim;
        int hiddim=bptt.hiddenLayerDim*4;
        int extoutdim=bptt.outputLayerDim;
        int L = bptt.L;
        bptt.createNet(inputdim, extaudim, hiddim, extoutdim, 0, 0, L);
        // Train until it gets it right
        for(int i=0; i<500 && !passed; i++)
        {
            bptt.train(1000, 1.0, false);
            if(bptt.correctSequences>=bptt.patc*.9)
                passed=true;
        }
        final int testSet[] = new int[bptt.patc];
        for(int i=0; i<bptt.patc; i++)
            testSet[i] = i;
        bptt.writeWeight("jjb_test_FF.wgt");

        return passed;
    }
}
