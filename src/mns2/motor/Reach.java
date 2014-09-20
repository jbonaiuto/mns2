package mns2.motor;

import sim.motor.Trajectory;
import sim.util.Log;
import mns2.main.Main;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: May 18, 2006
 * Time: 1:02:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class Reach extends sim.motor.Reach
{
    public Reach(final mns2.motor.ArmHand armHand, final sim.motor.Graspable o, final sim.motor.Graspable obs,
                 final Trajectory t, final String com)
    {
        super(armHand,o,obs,t,com);
    }

    public void run()
    {
        // Go until told to stop
        while(!stopRequested)
        {
            // Visual search time step
            if(armHand.search_mode==ArmHand.VISUAL_SEARCH)
            {
                if(!armHand.tickVisual(object,obstacle,viaPoint))
                    stopRequested=true;
                Main.refreshDisplay();
            }
            // Execute reach time step
            else if(armHand.search_mode==ArmHand.EXECUTE)
            {
                // Load sound clip if not already loaded
                if(armHand.audible && armHand.rtime==0.0)
                    loadSound();

                if(!armHand.tickReachGesture(tr,object,obstacle))
                    stopRequested=true;

                ((mns2.motor.ArmHand)armHand).perceive((mns2.motor.ArmHand)armHand, object,false);

                // If this is a hidden grasp and the time step is over 5 - bring down the "screen" - make the object invisible
                if(((mns2.motor.ArmHand)armHand).hiddenGrasp && !Main.self.obsObj.visible &&
                        (int)(.5+mns2.motor.ArmHand.recognizeDelay+armHand.rtime/armHand.reach_deltat) > 5)
                {
                    //HV.self.setObjectVisibility(false);
                    Main.self.setObstacleVisibility(true);
                    if(Main.self.obsObj.objectCenter.x>Main.self.curObj.objectCenter.x)
                        Main.self.curObj.visible=false;
                }
                else
                    Main.refreshDisplay();
            }
            try
            {
                sleep(SLP);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        Log.println("Reach stopped.");
        // If the action is audible, the hand is contacting the object, and the sound is not finished
        Main.refreshDisplay();
        armHand.enablePanels();
        Main.setTrace(false);
        armHand.finalizeReach(com,object);
        armHand.kill_ifActive();
    }
}
