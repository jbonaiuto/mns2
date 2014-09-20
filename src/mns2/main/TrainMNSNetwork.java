package mns2.main;

import mns2.comp.BP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import sim.util.Resource;

/**
 * Created by IntelliJ IDEA.
 * User: jbonaiuto
 * Date: Dec 21, 2005
 * Time: 9:56:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class TrainMNSNetwork
{
    public static void main(String[] args)
    {
        Resource.read(Main.RESfile);
        if(args.length==4)
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            BP net = new BP(args[0]);
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
