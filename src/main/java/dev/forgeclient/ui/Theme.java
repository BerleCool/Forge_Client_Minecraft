package dev.forgeclient.ui;
import java.util.*;
/** Original pixel-bevel drawing primitives; no third-party UI assets or fonts. */
public final class Theme {
    public static final int TEXT=0xFFECE3D9,MUTED=0xFFAAA0A0,DIM=0xFF726974,ORANGE=0xFFF19143,GOLD=0xFFFFC98E;
    private Theme(){}
    public static void outline(Canvas c,Rect r,int color){if(r.empty())return;c.rect(r.x,r.y,r.width,1,color);c.rect(r.x,r.bottom()-1,r.width,1,color);c.rect(r.x,r.y,1,r.height,color);c.rect(r.right()-1,r.y,1,r.height,color);}
    public static void panel(Canvas c,int x,int y,int w,int h,int background,boolean accent){
        if(w<=0||h<=0)return;c.rect(x,y,w,h,background);
        c.rect(x,y,w,1,accent?0xFFB57849:0xFF4A3E44);c.rect(x,y,1,h,accent?0xFF96613D:0xFF382F36);
        c.rect(x,y+h-1,w,1,0xFF0D0B10);c.rect(x+w-1,y,1,h,0xFF0D0B10);
        if(accent&&h>4)c.rect(x+1,y+1,2,h-2,ORANGE);
    }
    public static void logo(Canvas c,int x,int y,int scale){
        String[] pixels={"111111111","111111111","11       ","11       ","1111111  ","1111111  ","11       ","11       ","11       "};
        for(int py=0;py<pixels.length;py++)for(int px=0;px<pixels[py].length();px++)if(pixels[py].charAt(px)=='1')c.rect(x+px*scale,y+py*scale,scale,scale,py<2?GOLD:ORANGE);
    }
    public static String truncate(Canvas c,String text,int maxWidth){
        if(text==null||maxWidth<=0)return "";if(c.textWidth(text)<=maxWidth)return text;
        String suffix=c.textWidth("...")<=maxWidth?"...":"";int n=text.length();
        while(n>0&&c.textWidth(text.substring(0,n)+suffix)>maxWidth)n--;
        return text.substring(0,n)+suffix;
    }
    public static List<String> wrap(Canvas c,String text,int width){
        List<String> lines=new ArrayList<>();if(text==null||text.isEmpty())return lines;
        String line="";for(String word:text.split("\\s+")){
            String next=line.isEmpty()?word:line+" "+word;
            if(c.textWidth(next)>width&&!line.isEmpty()){lines.add(line);line=word;}else line=next;
        }
        if(!line.isEmpty())lines.add(line);return lines;
    }
    public static void wrapped(Canvas c,String text,int x,int y,int width,int color,int maxLines){List<String> lines=wrap(c,text,width);for(int i=0;i<Math.min(maxLines,lines.size());i++)c.text(truncate(c,lines.get(i),width),x,y+i*12,color,false);}
    public static void toggle(Canvas c,int x,int y,boolean on,boolean hover){
        panel(c,x,y,27,14,on?0xFF784421:0xFF17141A,false);
        c.rect(x+(on?15:3),y+3,9,8,on?ORANGE:0xFF6D6470);c.rect(x+(on?15:3),y+3,9,1,on?GOLD:0xFFABA0AD);
        if(hover)outline(c,new Rect(x,y,27,14),GOLD);
    }
    public static void rune(Canvas c,String letter,int x,int y,int size,int color){panel(c,x,y,size,size,0xFF2C211D,false);c.push(x+(size-c.textWidth(letter)*2)/2.0,y+(size-16)/2.0,2);c.text(letter,0,0,color,true);c.pop();}
    public static void scrollbar(Canvas c,Rect viewport,int contentHeight,int scroll){
        if(contentHeight<=viewport.height||viewport.height<=0)return;
        int h=Math.max(12,viewport.height*viewport.height/contentHeight);
        int y=viewport.y+(int)((viewport.height-h)*(scroll/(double)(contentHeight-viewport.height)));
        c.rect(viewport.right()-3,viewport.y,2,viewport.height,0x55100D13);c.rect(viewport.right()-3,y,2,h,0xFFB87B46);
    }
}
