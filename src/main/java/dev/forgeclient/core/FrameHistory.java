package dev.forgeclient.core;
import java.util.Arrays;
public final class FrameHistory {
    private final double[] values;
    private int start,size;
    private long previous;
    private boolean started;
    public FrameHistory(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");values=new double[capacity];}
    public FrameHistory(){this(120);}
    public void add(double ms){if(!Double.isFinite(ms)||ms<0||ms>10000)return;if(size==values.length){start=(start+1)%values.length;size--;}values[(start+size++)%values.length]=ms;}
    public void frame(long now){if(started)add((now-previous)/1_000_000.0);previous=now;started=true;}
    public int size(){return size;}
    public double sample(int index){if(index<0||index>=size)throw new IndexOutOfBoundsException();return values[(start+index)%values.length];}
    public double mean(){double total=0;for(int i=0;i<size;i++)total+=sample(i);return size==0?0:total/size;}
    public double percentile(double quantile){if(size==0)return 0;double[] sorted=new double[size];for(int i=0;i<size;i++)sorted[i]=sample(i);Arrays.sort(sorted);double q=Double.isFinite(quantile)?Math.max(0,Math.min(1,quantile)):0;return sorted[Math.max(0,(int)Math.ceil(q*size)-1)];}
    public void clear(){start=size=0;started=false;previous=0;}
}
