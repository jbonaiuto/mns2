package mns2.graphics;

import java.awt.*;

import mns2.comp.Network;

import javax.swing.*;

public class NetworkPanel extends Panel
{
    private static final long serialVersionUID = 3834315042081681464L;

    protected Network owner;
    protected Point showexc=null;

    public double wscValue;

    public NetworkPanel()
    {
    }

    public NetworkPanel(final Network o)
    {
        owner=o;
    }

    public void showThis(final Point p)
    {
        showexc=p;
        repaint();
    }

    public void showAll()
    {
        showexc=null;
        repaint();
    }

    public Point which(final int x, final int y)
    {
        return new Point();
    }
}
