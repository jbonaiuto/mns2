package mns2.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;


import mns2.comp.BPTT;
import mns2.comp.NetworkInterface;
import sim.motor.Graspable;

public class BPTTPanel extends BPPanel
{
    private static final long serialVersionUID = 4049641187256186676L;

    public BPTTPanel()
    {}

    public BPTTPanel(final BPTT owner)
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

        double thresh=wscValue/(BPTT.wscMAX/BPTT.wscrealMAX);

        if (!owner.validNet)
        {
            g.drawString("No network created",20,panelHeight /2);
            return;
        }
        else
            g.drawString("Weight display threshold:"+thresh,20,20);

        xrad=20; xgap=10;
        while ((xrad+xgap)*(owner.inputLayerDim+((BPTT)owner).recurrentInputDim+2) > panelWidth)
        {
            xrad--;
            xgap--;
        }
        if (xrad<=0)
            xrad=1;
        if (xgap<=0)
            xgap=1;

        yrad=20; ygap=10;
        while ((yrad+ygap)*(owner.hiddenLayerDim+2) > panelWidth)
        {
            yrad--;
            ygap--;
        }
        if (yrad<=0)
            yrad=1;
        if (ygap<=0)
            ygap=1;

        zrad=20; zgap=10;
        while ((zrad+zgap)*(owner.outputLayerDim+((BPTT)owner).recurrentOutputDim+2) > panelWidth)
        {
            zrad--;
            zgap--;
        }
        if (zrad<=0)
            zrad=1;
        if (zgap<=0)
            zgap=1;

        final int inputPos[][] = drawInputLayer(g,20,inputY,xrad,xgap,owner.inputLayer,owner.inputLayerDim+((BPTT)owner).recurrentInputDim);
        final int hiddenPos[][] = drawHiddenLayer(g,20,hiddenY,yrad,ygap,owner.hiddenLayer,owner.hiddenLayerDim);
        final int outputPos[][] = drawOutputLayer(g,20,outputY,zrad,zgap,owner.outputLayer,owner.outputLayerDim+((BPTT)owner).recurrentOutputDim);
        lastInputPos=inputPos;
        lastHiddenPos=hiddenPos;
        lastOutputPos=outputPos;
        drawingfrom=0;
        drawconn(g,inputPos,hiddenPos,owner.inputLayerDim+((BPTT)owner).recurrentInputDim,owner.hiddenLayerDim,
                owner.inputToHiddenW,thresh);
        drawingfrom=1;
        drawconn(g,hiddenPos,outputPos,owner.hiddenLayerDim,owner.outputLayerDim+((BPTT)owner).recurrentOutputDim,
                owner.hiddenToOutputW,thresh);
        drawingfrom=2;
        final int recurrentOutputPos[][] = new int[((BPTT)owner).recurrentOutputDim][3];
        final int recurrentInputPos[][] = new int[((BPTT)owner).recurrentInputDim][3];
        for(int i=0; i<((BPTT)owner).recurrentOutputDim; i++)
        {
            System.arraycopy(outputPos[owner.outputLayerDim+i],0,recurrentOutputPos[i],0,3);
        }
        for(int i=0; i<((BPTT)owner).recurrentInputDim; i++)
        {
            System.arraycopy(inputPos[owner.inputLayerDim+i],0,recurrentInputPos[i],0,3);
        }
        int lastOutputX=outputPos[owner.outputLayerDim+((BPTT)owner).recurrentOutputDim-1][0];
        int lastHiddenX=hiddenPos[owner.hiddenLayerDim-1][0];
        int lastInputX=inputPos[owner.inputLayerDim+((BPTT)owner).recurrentInputDim-1][0];
        int recurrentX=20;
        if(lastOutputX>lastHiddenX && lastOutputX>lastInputX)
            recurrentX+=lastOutputX;
        else if(lastHiddenX>lastOutputX && lastHiddenX>lastInputX)
            recurrentX+=lastHiddenX;
        else if(lastInputX>lastOutputX && lastInputX>lastHiddenX)
            recurrentX+=lastInputX;
        drawRconn(g, recurrentOutputPos, recurrentInputPos, ((BPTT)owner).recurrentOutputDim, ((BPTT)owner).recurrentInputDim,
                ((BPTT)owner).recurrentOutputToInputW, recurrentX,thresh);
        drawingfrom=-1;
    }

    protected void drawRconn(final Graphics g, final int[][] X, final int[][] Y, final int xd, final int yd,
                             final double[][] w, final int recX, final double thresh)
    {
        for (int j=0;j<yd;j++)
        {
            for (int i=0;i<xd;i++)
            {
                if (!showOK(i,j))
                    continue;
                final double st=Math.abs(w[j][i]);
                final double tt;
                if (st==0)
                    continue;
                if (w[j][i]<0)
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
                {
                    g.drawLine(X[i][0]+t,X[i][1],X[i][0]+t,X[i][1]-(5+2*i));
                    g.drawLine(X[i][0]+t,X[i][1]-(5+2*i),recX+(2*i),X[i][1]-(5+2*i));
                    g.drawLine(recX+(2*i),X[i][1]-(5+2*i),recX+(2*i),Y[j][2]+(5+2*i));
                    g.drawLine(recX+(2*i),Y[j][2]+(5+2*i),Y[j][0]+t,Y[j][2]+(5+2*i));
                    g.drawLine(Y[j][0]+t,Y[j][2]+(5+2*i),Y[j][0]+t,Y[j][2]);
                }
            }
        }
    }

    protected int[][] drawInputLayer(final Graphics g,int x, int y, final int radius, final int gap, final double v[],
                                     final int size)
    {
        final int originalX=x;
        int offset=(panelWidth -(size*(radius +gap)+owner.inputLayerDim*28)-gap)/2;
        if (offset<0)
        {
            offset=0;
            System.err.println("Overflow occured during drawlayer...");
        }
        final int[][] p=new int[size][3];
        for (int i=0;i<size;i++)
        {
            p[i][0]=x+offset+radius /2;
            p[i][1]=y;
            p[i][2]=y+radius;

            g.drawArc(x+offset,y,radius,radius,0,360);
            g.fillArc(x+offset,y,radius,radius,0,(int)(0.5+360*v[i]));
            if(i<owner.inputLayerDim)
                g.drawString(NetworkInterface.params[i],(int)(x+offset-(NetworkInterface.params[i].length()*1.5)),y+radius +10);
            x+=radius +gap;
            if(i<owner.inputLayerDim)
                x+=28;
            if ((i+1)%30==0)
                x+=radius +gap;
            if (p[i][0]>panelWidth -radius -radius /2)
            {
                x=originalX;
                y+=5*radius;
            }
        }
        return p;
    }

    protected int[][] drawHiddenLayer(final Graphics g, int x, int y, final int diameter, final int gap,
                                      final double v[], final int size)
    {
        final int X=x;
        int offset=(panelWidth -size*(diameter +gap)-gap)/2;
        if (offset<0)
        {
            offset=0;
            System.err.println("Overflow occured during drawlayer...");
        }
        final int[][] p=new int[size][3];
        for (int i=0;i<size;i++)
        {
            p[i][0]=x+offset+diameter /2;
            p[i][1]=y;
            p[i][2]=y+diameter;
            g.setColor(Color.BLACK);
            g.drawArc(x+offset,y,diameter,diameter,0,360);
            g.fillArc(x+offset,y,diameter,diameter,0,(int)(0.5+360*v[i]));
            x+=diameter +gap;
            if ((i+1)%30==0)
                x+=diameter +gap;
            if (p[i][0]>panelWidth -diameter -diameter /2)
            {
                x=X;
                y+=5*diameter;
            }
        }
        return p;
    }

    protected int[][] drawOutputLayer(final Graphics g,int x, int y, final int R, final int gap, final double v[],
                                      final int size)
    {
        final int X=x;
        int offset=(panelWidth - (size*(R+gap)+owner.outputLayerDim*28)-gap)/2;
        if (offset<0)
        {
            offset=0;
            System.err.println("Overflow occured during drawlayer...");
        }
        final int[][] p=new int[size][3];
        for (int i=0;i<size;i++)
        {
            p[i][0]=x+offset+(R/2);
            p[i][1]=y;
            p[i][2]=y+R;
            g.drawArc(x+offset,y,R,R,0,360);
            g.fillArc(x+offset,y,R,R,0,(int)(0.5+360*v[i]));
            if(i<owner.outputLayerDim)
                g.drawString(Graspable.grasps[i],(int)(x+offset-(Graspable.grasps[i].length()*1.5)),y+R-20);
            x+=R+gap;
            if(i<owner.outputLayerDim)
                x+=28;
            if ((i+1)%30==0)
                x+=R+gap;
            if (p[i][0]>panelWidth -R-(R/2))
            {
                x=X;
                y+=5*R;
            }
        }
        return p;
    }

    protected boolean showOK(final int from, final int to)
    {
        if (super.showexc==null)
            return true;
        if (drawingfrom==0)   // from=inputLayer to=hiddenLayer
        {
            if (super.showexc.x==0 && from==super.showexc.y)
                return true; //showthis
            if (super.showexc.x==1 && to==super.showexc.y)
                return true; //showthis
        }
        else
        {
            if (drawingfrom==1)   // from=hiddenLayer to=outputLayer
            {
                if (super.showexc.x==1 && from==super.showexc.y)
                    return true; //showthis
                if (super.showexc.x==2 &&   to==super.showexc.y)
                    return true; //showthis
            }
            else if (drawingfrom==2)   // from=outputLayer to=inputLayer
            {
                if (super.showexc.x==0 && to+owner.inputLayerDim==super.showexc.y)
                    return true;  //showthis
                if (super.showexc.x==2 && from+owner.outputLayerDim==super.showexc.y)
                    return true;
            }
        }
        return false; //don't show
    }
}