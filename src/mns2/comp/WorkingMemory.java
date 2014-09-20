package mns2.comp;

import sim.graphics.Point3d;

/**
 * Working memory implementation with random decay and dynamic remapping
 */
public class WorkingMemory
{
    // The time stamp for the working memory contents
    private int timeStamp=10000;

    // The number of time steps until the working memory trace is completely decayed
    private int decayTime=60;

    // The size of this working memory instance
    private int size;

    // Objects in working memory
    private Object[] memory;

    // For each object, whether or not it will be dynamically remapped
    private boolean[] dynamicRemapping;

    /**
     * Constructor
     */
    public WorkingMemory()
    {
    }

    /**
     * Constructor
     * @param s - size
     */
    public WorkingMemory(final int s)
    {
        size = s;
        memory = new Object[s];
        dynamicRemapping = new boolean[s];
    }

    /**
     * Clears contents of working memory
     */
    public void clear()
    {
        for(int i=0; i<size; i++)
        {
            if(memory[i] != null && memory[i].getClass().equals(Point3d.class))
                memory[i] = new Point3d((Math.random()-.5)*.01, (Math.random()-.5)*.01, (Math.random()-.5)*.01);
            else
                memory[i] = new Double((Math.random()-.5)*.01);

        }
        timeStamp=10000;
    }

    /**
     * Returns size
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Returns an item in working memory
     * @param i - index of the item
     */
    public Object get(final int i)
    {
        return memory[i];
    }

    /**
     * Sets the value of a working memory item
     * @param i - index of the item
     * @param val - value to set it to
     * @param dynRemap - whether or not to dynamically remap
     */
    public void set(final int i, final Object val, final boolean dynRemap)
    {
        memory[i] = val;
        dynamicRemapping[i] = dynRemap;
    }

    /**
     * Returns the time stamp
     */
    public int getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * Sets the time stamp
     * @param t
     */
    public void setTimeStamp(final int t)
    {
        timeStamp = t;
    }

    /**
     * Gets the decay time
     */
    public int getDecayTime()
    {
        return decayTime;
    }

    /**
     * Sets the decay time
     * @param dT - decay time
     */
    public void setDecayTime(final int dT)
    {
        decayTime = dT;
    }

    /**
     * Decays the working memory a little and performs dynamic remapping
     * @param t - current time step
     * @param displacement - displacement for dynamic remapping
     */
    public void decay(final int t, final Point3d displacement)
    {
        for(int i=0; i<size; i++)
        {
            // Memory completely decayed - clear memory
            if(t >= timeStamp+decayTime)
            {
                clear();
            }
            // Otherwise add small random values to items in memory
            else
            {
                if(memory[i].getClass().equals(Point3d.class))
                {
                    ((Point3d)memory[i]).x += (Math.random()-.5)*.001;
                    ((Point3d)memory[i]).y += (Math.random()-.5)*.001;
                    ((Point3d)memory[i]).z += (Math.random()-.5)*.001;
                }
                else
                    memory[i] = new Double(((Double)memory[i]).doubleValue() + (Math.random()-.5)*.001);
            }
            // Dynamic remapping
            if(dynamicRemapping[i] && memory[i].getClass().equals(Point3d.class))
            {
                ((Point3d)memory[i]).x += displacement.x;
                ((Point3d)memory[i]).y += displacement.y;
                ((Point3d)memory[i]).z += displacement.z;
            }
        }
    }
}
