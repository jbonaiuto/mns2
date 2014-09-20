package mns2.util;

import sim.util.Spline;

import java.util.Hashtable;

/**
 *
 * @author Erhan Oztop, 2001-2002 <br>
 * <br>
 * Source code by Erhan Oztop (erhan@atr.co.jp) <br>
 * Copyright August 2002 via <br>
 * University of Southern California Ph.D. publication copyright <br>
 */

public class ParamsNode
{
    private int parc;
    private Hashtable parhash;
    private double par[][];
    private int slot=0;
    private String names[];

    public ParamsNode(int maxsample, String[] params)
    {
        parhash=new Hashtable();
        parc=params.length;
        for (int i=0;i<parc;i++)
            parhash.put(params[i],new Integer(i));
        par=new double[parc][maxsample];
        names=new String[parc];
        System.arraycopy(params, 0, names, 0, parc);
    }

    private int getIndex(String s)
    {
        return ((Integer)parhash.get(s)).intValue();
    }

    public void advance()
    {
        slot++;
    }

    public void put(String s, double value)
    {
        int k=getIndex(s);
        par[k][slot]=value;
    }

    public double[][] getAll()
    {
        return par;
    }

    public double getRel(String s,int relpos)
    {
        int k=getIndex(s);
        return par[k][slot+relpos];
    }

    public double getAbs(String s,int pos)
    {
        int k=getIndex(s);
        return par[k][pos];
    }

    public int getSlot()
    {
        return slot;
    }

	public Spline[] getSplines()
	{
		Spline[] sp=new Spline[parc];
		for (int k=0;k<parc;k++)
		{
			sp[k]=new Spline(slot,par[k]);
			sp[k].setLabel(names[k]);
		}
		return sp;
	}

    public void reset()
    {
        slot=0;
        par=new double[parc][par[0].length];
    }
}
/*
*
* Erhan Oztop, 2000-2002  <br>
* Source code by Erhan Oztop (erhan@atr.co.jp) <br>
* Copyright August 2002 under <br>
* University of Southern California Ph.D. publication copyright <br>
*/

