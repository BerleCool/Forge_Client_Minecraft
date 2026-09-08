package dev.forgeclient.ui;
public final class Rect {
    public final int x,y,width,height;
    public Rect(int x,int y,int width,int height){this.x=x;this.y=y;this.width=Math.max(0,width);this.height=Math.max(0,height);}
    public int right(){return x+width;} public int bottom(){return y+height;}
    public boolean contains(int px,int py){return px>=x&&py>=y&&px<right()&&py<bottom();}
    public boolean empty(){return width==0||height==0;}
    public Rect intersect(Rect r){int nx=Math.max(x,r.x),ny=Math.max(y,r.y);return new Rect(nx,ny,Math.min(right(),r.right())-nx,Math.min(bottom(),r.bottom())-ny);}
}
