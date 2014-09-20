package mns2.main;

import mns2.comp.BPTT;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: JABONA
 * Date: Nov 8, 2005
 * Time: 10:09:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrainAudioNetwork
{
    public static void main(String[] args)
    {
        if(args.length==4)
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            BPTT net = new BPTT(args[0]);
            int epochs = Integer.parseInt(args[1]);
            double thresh = Double.parseDouble(args[2]);
            boolean cont=true;
            while(cont)
            {
                net.train(epochs, thresh, true);
                System.out.println("Save weights? (y/n)");
                String answer="";
                try
                {
                    answer = in.readLine();
                }
                catch(IOException e)
                {}
                if(answer.equals("y"))
                    net.writeWeight(args[3]);
                System.out.println("Continue training? (y/n)");
                answer="";
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
                    System.out.println("Success threshold: ");
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
