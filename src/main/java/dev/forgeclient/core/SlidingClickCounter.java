package dev.forgeclient.core;
/** Bounded one-second event window, using monotonic nanoseconds (including wraparound). */
public final class SlidingClickCounter {
    private final long[] times;
    private int start,size;
    public SlidingClickCounter(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");times=new long[capacity];}
    public SlidingClickCounter(){this(256);}
    public void click(long now){expire(now);if(size==times.length){start=(start+1)%times.length;size--;}times[(start+size++)%times.length]=now;}
    private void expire(long now){while(size>0&&now-times[start]>=1_000_000_000L){start=(start+1)%times.length;size--;}}
    public int count(long now){expire(now);return size;}
    public void clear(){start=size=0;}
}
