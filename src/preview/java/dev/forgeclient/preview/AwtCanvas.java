package dev.forgeclient.preview;

import dev.forgeclient.core.Telemetry;
import dev.forgeclient.ui.Canvas;
import dev.forgeclient.ui.Rect;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;

/** Desktop-only renderer, intentionally excluded from the Minecraft mod JAR. */
public final class AwtCanvas implements Canvas {
    private final Graphics2D g;
    private final Deque<AffineTransform> transforms=new ArrayDeque<>();
    private final Deque<Shape> clips=new java.util.LinkedList<>();
    public AwtCanvas(Graphics2D graphics){g=graphics;g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,10));g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);}
    public void rect(int x,int y,int width,int height,int color){g.setColor(new Color(color,true));g.fillRect(x,y,width,height);}
    public void text(String text,int x,int y,int color,boolean shadow){if(text==null)return;if(shadow){g.setColor(new Color(0,0,0,180));g.drawString(text,x+1,y+9);}g.setColor(new Color(color,true));g.drawString(text,x,y+8);}
    public int textWidth(String text){return text==null?0:g.getFontMetrics().stringWidth(text);}
    public void push(double x,double y,double scale){transforms.push(g.getTransform());g.translate(x,y);g.scale(scale,scale);}
    public void pop(){g.setTransform(transforms.pop());}
    public void clip(Rect r){clips.push(g.getClip());g.clipRect(r.x,r.y,r.width,r.height);}
    public void unclip(){g.setClip(clips.pop());}
    public void item(Telemetry.Item item,int x,int y){rect(x+3,y+1,10,14,0xFF81B7C2);rect(x+5,y+3,6,9,0xFFB4DDE0);}
    public void grain(int x,int y,int width,int height,int opacity){
        for(int py=y;py<y+height;py+=8)for(int px=x;px<x+width;px+=8){int hash=(px*73856093)^(py*19349663);int level=80+(hash&31);rect(px,py,Math.min(8,x+width-px),Math.min(8,y+height-py),(opacity<<24)|(level<<16)|(level<<8)|level);}
    }
}
