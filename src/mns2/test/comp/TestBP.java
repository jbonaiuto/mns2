package mns2.test.comp;

import mns2.comp.BP;

/**
 * Test BPTTHebbian
 */
public class TestBP
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

        if(testForward && testLearn)
        {
            System.out.println("**** All tests passed ****");
        }
    }

    public static boolean testForward()
    {
        System.out.println("**** test Forward started ****");
        final BP bptt=new BP();
        bptt.createNet(2,1,1);
        bptt.inputToHiddenW[0][0] = 1.0;
        bptt.inputToHiddenW[0][1] = 0.0;
        bptt.hiddenToOutputW[0][0] = 0.5;

        bptt.presentPattern(new double[]{0.0, 1.0});
        bptt.forward(false, false);
        if(bptt.hiddenLayer[0] != 0.5)
            return false;
        if(bptt.outputLayer[0] != 0.5621765008857981)
            return false;
        return true;
    }

    public static boolean testLearn()
    {
        boolean passed = false;
        System.out.println("**** test Learn started ****");
        final BP bp=new BP();
        bp.netFromPattern("joint_coord_to_angle.xml");
        // Train until it gets it right
        for(int i=0; i<300 && !passed; i++)
        {
            bp.train(10000, 1.0, false);
            if(bp.correctPatterns==bp.patc)
                passed=true;
        }

        return passed;
    }
}
