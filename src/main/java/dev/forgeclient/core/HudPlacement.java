package dev.forgeclient.core;
/** Positions are fractions of the available travel, not of raw screen size. */
public final class HudPlacement {
    private final double initialX,initialY;
    private double x,y;
    private Runnable changed=()->{};
    public HudPlacement(double x,double y){initialX=clamp(x);initialY=clamp(y);this.x=initialX;this.y=initialY;}
    private static double clamp(double n){return Double.isFinite(n)?Math.max(0,Math.min(1,n)):0;}
    void onChange(Runnable listener){changed=listener;}
    public double x(){return x;} public double y(){return y;}
    public void set(double nx,double ny){
        if(!Double.isFinite(nx)||!Double.isFinite(ny))return;
        nx=clamp(nx);ny=clamp(ny);
        if(x!=nx||y!=ny){x=nx;y=ny;changed.run();}
    }
    public int pixelX(int width,int widget){return (int)Math.round(Math.max(0,width-widget)*x);}
    public int pixelY(int height,int widget){return (int)Math.round(Math.max(0,height-widget)*y);}
    public void move(int px,int py,int width,int height,int widgetWidth,int widgetHeight){
        set(width>widgetWidth?px/(double)(width-widgetWidth):0,height>widgetHeight?py/(double)(height-widgetHeight):0);
    }
    public void reset(){set(initialX,initialY);}
}
