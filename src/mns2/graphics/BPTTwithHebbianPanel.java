package mns2.graphics;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import mns2.comp.BPTTwithHebbian;

public class BPTTwithHebbianPanel extends BPTTPanel
{
	private static final long serialVersionUID = 3616729370279031606L;

    public BPTTwithHebbianPanel()
	{}
	
	public BPTTwithHebbianPanel(final BPTTwithHebbian owner)
    {
		this.owner=owner;
    }

    public void paint(final Graphics g)
    {
        final Dimension d=getSize();
        panelHeight =d.height;
        panelWidth =d.width;
        panelMidWidth = panelWidth /2;

        hiddenY=panelHeight/2;
        if(panelHeight/4<40)
            inputY=3*panelHeight/4;
        else
            inputY=panelHeight-40;
        if(panelHeight/4<40)
            outputY=panelHeight/4;
        else
            outputY=40;

		double thresh=wscValue/(BPTTwithHebbian.wscMAX/BPTTwithHebbian.wscrealMAX);

        if (!owner.validNet)
        {
            g.drawString("No network created",20, panelHeight /2);
            return;
        }
        else
            g.drawString("Weight display threshold:"+thresh,20,20);

        xrad=20; xgap=10;
        while ((xrad+xgap)*(owner.inputLayerDim+((BPTTwithHebbian)owner).recurrentInputDim+2) > panelWidth)
        {
            xrad--;
            xgap--;
        }
        if (xrad<=0)
            xrad=1;
        if (xgap<=0)
            xgap=1;

        yrad=20; ygap=10;
        while ((yrad+ygap)*(owner.hiddenLayerDim+((BPTTwithHebbian)owner).extHebbianInputDim+2) > panelWidth)
        {
            yrad--;
            ygap--;
        }
        if (yrad<=0)
            yrad=1;
        if (ygap<=0)
            ygap=1;

        zrad=20; zgap=10;
        while ((zrad+zgap)*(owner.outputLayerDim+((BPTTwithHebbian)owner).recurrentOutputDim+2) > panelWidth)
        {
            zrad--;
            zgap--;
        }
        if (zrad<=0)
            zrad=1;
        if (zgap<=0)
            zgap=1;

        final double[] totalToOutputLayer = new double[owner.hiddenLayerDim+((BPTTwithHebbian)owner).extHebbianInputDim];
        System.arraycopy(((BPTTwithHebbian)owner).hebbianInputLayer, 0, totalToOutputLayer, 0, ((BPTTwithHebbian)owner).extHebbianInputDim);
        System.arraycopy(owner.hiddenLayer, 0, totalToOutputLayer, ((BPTTwithHebbian)owner).extHebbianInputDim,
                         owner.hiddenLayerDim);

        final double[][] totalToOutputW = new double[owner.outputLayerDim+((BPTTwithHebbian)owner).recurrentOutputDim][owner.hiddenLayerDim+((BPTTwithHebbian)owner).extHebbianInputDim];
        for(int i=0; i<owner.outputLayerDim+((BPTTwithHebbian)owner).recurrentOutputDim; i++)
        {
            if(i<owner.outputLayerDim)
                System.arraycopy(((BPTTwithHebbian)owner).hebbianInputToOutputW[i], 0, totalToOutputW[i], 0,
                                 ((BPTTwithHebbian)owner).extHebbianInputDim);
            System.arraycopy(owner.hiddenToOutputW[i], 0, totalToOutputW[i], ((BPTTwithHebbian)owner).extHebbianInputDim,
                             owner.hiddenLayerDim);
        }
        final int inputPos[][] = drawInputLayer(g,20,inputY,xrad,xgap,owner.inputLayer,owner.inputLayerDim+((BPTTwithHebbian)owner).recurrentInputDim);
        final int hiddenPos[][] = drawHiddenLayer(g,20,hiddenY,yrad,ygap,totalToOutputLayer,
                                                  owner.hiddenLayerDim+((BPTTwithHebbian)owner).extHebbianInputDim);
        final int outputPos[][] = drawOutputLayer(g,20,outputY,zrad,zgap,owner.outputLayer,owner.outputLayerDim+((BPTTwithHebbian)owner).recurrentOutputDim);
        lastInputPos=inputPos;
        lastHiddenPos=hiddenPos;
        lastOutputPos=outputPos;
        drawingfrom=0;
        drawInputToHiddenConn(g,inputPos,hiddenPos,owner.inputLayerDim+((BPTTwithHebbian)owner).recurrentInputDim,
                owner.hiddenLayerDim,owner.inputToHiddenW,thresh,((BPTTwithHebbian)owner).extHebbianInputDim);
        drawingfrom=1;
        drawconn(g,hiddenPos,outputPos,owner.hiddenLayerDim+((BPTTwithHebbian)owner).extHebbianInputDim,owner.outputLayerDim+
                ((BPTTwithHebbian)owner).recurrentOutputDim, totalToOutputW,thresh);
        drawingfrom=2;
        final int recurrentOutputPos[][] = new int[((BPTTwithHebbian)owner).recurrentOutputDim][3];
        final int recurrentInputPos[][] = new int[((BPTTwithHebbian)owner).recurrentInputDim][3];
        for(int i=0; i<((BPTTwithHebbian)owner).recurrentOutputDim; i++)
        {
            System.arraycopy(outputPos[owner.outputLayerDim+i],0,recurrentOutputPos[i],0,3);
        }
        for(int i=0; i<((BPTTwithHebbian)owner).recurrentInputDim; i++)
        {
            System.arraycopy(inputPos[owner.inputLayerDim+i],0,recurrentInputPos[i],0,3);
        }
        int lastOutputX=outputPos[owner.outputLayerDim+((BPTTwithHebbian)owner).recurrentOutputDim-1][0];
        int lastHiddenX=hiddenPos[owner.hiddenLayerDim+((BPTTwithHebbian)owner).extHebbianInputDim-1][0];
        int lastInputX=inputPos[owner.inputLayerDim+((BPTTwithHebbian)owner).recurrentInputDim-1][0];
        int recurrentX=20;
        if(lastOutputX>lastHiddenX && lastOutputX>lastInputX)
            recurrentX+=lastOutputX;
        else if(lastHiddenX>lastOutputX && lastHiddenX>lastInputX)
            recurrentX+=lastHiddenX;
        else if(lastInputX>lastOutputX && lastInputX>lastHiddenX)
            recurrentX+=lastInputX;
        drawRconn(g, recurrentOutputPos, recurrentInputPos, ((BPTTwithHebbian)owner).recurrentOutputDim,
                ((BPTTwithHebbian)owner).recurrentInputDim, ((BPTTwithHebbian)owner).recurrentOutputToInputW,
                recurrentX,thresh);
        drawingfrom=-1;
    }

    protected void drawInputToHiddenConn(final Graphics g, final int[][] X, final int[][] Y, final int xd, final int yd,
                                         final double[][] w, final double thresh, final int noA)
    {
        for (int j=noA;j<yd+noA;j++)
        {
            for (int i=0;i<xd;i++)
            {
                if (!showOK(i,j))
                    continue;
                final double st=Math.abs(w[j-noA][i]);
                final double tt;
                if (st==0)
                    continue;
                if (w[j-noA][i]<0)
                    g.setColor(Color.blue);
                else
                    g.setColor(Color.red);
                if (thresh==0)
                    tt=Math.log(st+1)/Math.log(Math.exp(1));
                else
                    tt=Math.log(st/thresh)/Math.log(Math.exp(1)); // log2 of st
                int htness=(int)(tt+.5);
                if (tt<0)
                    htness=-1;
                if (tt>5)
                    htness=5;

                for (int t=-1;t<htness;t++)
                    g.drawLine(X[i][0]+t,X[i][1],Y[j][0]+t,Y[j][2]);
            }
        }
    }

    protected int[][] drawHiddenLayer(final Graphics g, int x, int y, final int R, final int gap,
                                      final double v[], final int size)
    {
        final int originalX=x;
        int offset= (panelWidth - (size * (R + gap)+((BPTTwithHebbian)owner).extHebbianInputDim*gap*2) - gap)/2;
        if (offset<0)
        {
            offset=0;
        }
        final int[][] p=new int[size][3];
        for (int i=0;i<size;i++)
        {
            p[i][0]=x+offset+(R/2);
            p[i][1]=y;
            p[i][2]=y+R;
            if(i<((BPTTwithHebbian)owner).extHebbianInputDim)
                g.setColor(Color.MAGENTA);
            else
                g.setColor(Color.BLACK);
            g.drawArc(x+offset,y,R,R,0,360);
            g.fillArc(x+offset,y,R,R,0,(int)(0.5+360*v[i]));
            if(i<((BPTTwithHebbian)owner).extHebbianInputDim)
            {
                g.setColor(Color.BLACK);
                g.drawString("audio"+i,x+offset-5,y+R+10);
            }
            if(i<((BPTTwithHebbian)owner).extHebbianInputDim)
                x+=R+(gap*3);
            else
                x+=R+gap;
            if ((i+1)%30==0)
                x+=R+gap;
            if (p[i][0]>panelWidth -R-(R/2))
            {
                x=originalX;
                y+=5*R;
            }
        }
        return p;
    }
}