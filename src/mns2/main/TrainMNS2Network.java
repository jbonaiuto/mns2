package mns2.main;

import mns2.comp.BPTTwithHebbian;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Nov 7, 2005
 * Time: 9:18:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrainMNS2Network
{
    public static void main(String[] args)
    {
        if(args.length==4)
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            BPTTwithHebbian net = new BPTTwithHebbian(args[0]);
            int epochs = Integer.parseInt(args[1]);
            double thresh = Double.parseDouble(args[2]);
            boolean cont=true;
            while(cont)
            {
                net.train(epochs, thresh, true);
                net.writeWeight(args[3]);
                System.out.println("Continue training? (y/n)");
                String answer="";
                try
                {
                    answer = in.readLine();
                }
                catch(IOException e)
                {}
                if(answer.equals("n"))
                    cont=false;
                else
                {
                    System.out.println("New success threshold: ");
                    answer="";
                    try
                    {
                        answer = in.readLine();
                    }
                    catch(IOException e)
                    {}
                    if(answer.length()>0)
                        thresh=Double.parseDouble(answer);
                }
            }
        }
    }
}
