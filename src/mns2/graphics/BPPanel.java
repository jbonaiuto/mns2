package mns2.graphics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import mns2.comp.BP;

public class BPPanel extends NetworkPanel
{
	private static final long serialVersionUID = 3546919195272295472L;
	public int panelWidth,panelHeight;
    public int panelMidWidth;
    public int xgap,ygap,zgap,xrad,yrad,zrad;
    public int inputY,hiddenY,outputY;

    protected int drawingfrom=-1;
    
    protected int[][] lastInputPos=null;
    protected int[][] lastHiddenPos=null;
    protected int[][] lastOutputPos=null;

	public BPPanel()
	{}
	
	public BPPanel(BP owner)
	{
		this.owner=owner;
    }

    public void paint(final Graphics g)
    {
        final Dimension d=getSize();
        panelHeight=d.height;
        panelWidth=d.width;
        panelMidWidth =panelWidth /2;

        hiddenY=panelHeight/2;
        if(panelHeight/4<40)
            inputY=3*panelHeight/4;
        else
            inputY=panelHeight-40;
        if(panelHeight/4<40)
            outputY=panelHeight/4;
        else
            outputY=40;

        double thresh=wscValue/(BP.wscMAX/BP.wscrealMAX);

        if (!owner.validNet)
        {
            g.drawString("No network created",20,panelHeight /2);
            return;
        }
        else
            g.drawString("Weight display threshold:"+thresh,20,20);

        xrad=20; xgap=10;
        while ((xrad+xgap)*(owner.inputLayerDim+2) > panelWidth)
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
        while ((zrad+zgap)*(owner.outputLayerDim+2) > panelWidth)
        {
            zrad--;
            zgap--;
        }
        if (zrad<=0)
            zrad=1;
        if (zgap<=0)
            zgap=1;
        final int xpos[][] = drawlayer(g,20,inputY,xrad,xgap,owner.inputLayer,owner.inputLayerDim);
        final int ypos[][] = drawlayer(g,20,hiddenY,yrad,ygap,owner.hiddenLayer,owner.hiddenLayerDim);
        final int zpos[][] = drawlayer(g,20,outputY,zrad,zgap,owner.outputLayer,owner.outputLayerDim);
        lastInputPos=xpos;
        lastHiddenPos=ypos;
        lastOutputPos=zpos;
        drawingfrom=0;
        drawconn(g,xpos,ypos,owner.inputLayerDim,owner.hiddenLayerDim,owner.inputToHiddenW,thresh);
        drawingfrom=1;
        drawconn(g,ypos,zpos,owner.hiddenLayerDim,owner.outputLayerDim,owner.hiddenToOutputW,thresh);
        drawingfrom=-1;
    }

    protected int[][] drawlayer(final Graphics g,int x, int y, final int R, final int gap, final double v[],
                                final int size)
    {
        final int xOriginal=x;
        int offset=(panelWidth -size*(R+gap)-gap)/2;
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
            x+=R+gap;
            if ((i+1)%30==0)
                x+=R+gap;
            if (p[i][0]>panelWidth -R-(R/2))
            {
                x=xOriginal;
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
        }
        return false; //don't show
    }

    protected void drawconn(final Graphics g, final int[][] X, final int[][] Y, final int xd, final int yd,
                            final double[][] w, final double thresh)
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
                    g.drawLine(X[i][0]+t,X[i][1],Y[j][0]+t,Y[j][2]);
            }
        }
    }

    public Point which(final int x, final int y)
    {
        if (lastInputPos==null)
            return null;
        for (int i=0;i<lastInputPos.length;i++)
        {
            if (inunit(x,y,lastInputPos[i][0],lastInputPos[i][1],lastInputPos[i][2]))
                return new Point(0,i);
        }
        for (int i=0;i<lastHiddenPos.length;i++)
        {
            if (inunit(x,y,lastHiddenPos[i][0],lastHiddenPos[i][1],lastHiddenPos[i][2]))
                return new Point(1,i);
        }
        for (int i=0;i<lastOutputPos.length;i++)
        {
            if (inunit(x,y,lastOutputPos[i][0],lastOutputPos[i][1],lastOutputPos[i][2]))
                return new Point(2,i);
        }

        return null;
    }

    private static boolean inunit(final int X, final int Y, final int x, final int yt, final int yb)
    {
        final double R=(yb-yt)/2;
        final double y=(yt+yb)/2;
        return (X - x) * (X - x) + (Y - y) * (Y - y) <= R * R;
    }
}
