package dev.forgeclient.ui;
/** The shared UI keeps a 640x480 logical minimum even at large Minecraft GUI scales. */
public final class UiLayout {
    public final int width,height,rail=48,nav;
    public final Rect content,inspector;
    public UiLayout(int width,int height){
        this.width=width;this.height=height;nav=width>=850?154:0;
        int inspectorWidth=width>=850?264:220;
        int x=rail+nav+16,inspectorX=width-inspectorWidth-14;
        content=new Rect(x,70,inspectorX-x-16,height-108);
        inspector=new Rect(inspectorX,66,inspectorWidth,height-104);
    }
    public int columns(){return content.width>=390?2:1;}
    public static double scaleFor(int width,int height){return Math.min(1,Math.min(width/640.0,height/480.0));}
}
