package dev.forgeclient.ui;
import dev.forgeclient.core.*;
/** Stateless layout and drawing shared by the native mod and verification harness. */
public final class HudRenderer {
    public static final class Size { public final int width,height;public Size(int w,int h){width=w;height=h;} }
    public boolean hidden(ModuleRegistry registry,ClientModule module){
        return registry.enabled("streamer_mode")&&registry.get("streamer_mode").setting("coordinates").bool()&&(module.id.equals("coordinates")||module.id.equals("waypoint"));
    }
    public Size measure(Canvas c,ClientModule m,Telemetry t){
        if(m.id.equals("keystrokes"))return new Size(86,88+(m.setting("mouse").bool()?24:0)+(m.setting("jump").bool()?17:0));
        if(m.id.equals("armor")||m.id.equals("held_item")){
            Telemetry.Item[] items=m.id.equals("armor")?t.armor:t.held;
            boolean horizontal=m.id.equals("armor")&&m.setting("layout").raw().equals("Horizontal");
            if(items.length==0)return new Size(96,22);
            return horizontal?new Size(items.length*55+8,36):new Size(m.id.equals("armor")?100:160,items.length*23+8);
        }
        int width=48;String[] lines=t.rows(m.id);
        for(String line:lines)width=Math.max(width,c.textWidth(line)+16);
        if(m.id.equals("frame_graph"))return new Size(Math.max(180,Math.min(260,width)),68);
        return new Size(Math.min(280,width),Math.max(1,lines.length)*12+10);
    }
    private double scale(Canvas c,ClientModule m,Telemetry t,int w,int h){
        Size size=measure(c,m,t);return Math.min(m.setting("scale").number(),Math.min(w/(double)Math.max(1,size.width),h/(double)Math.max(1,size.height)));
    }
    public Rect bounds(Canvas c,ClientModule m,Telemetry t,int width,int height){
        Size size=measure(c,m,t);double scale=scale(c,m,t,width,height);
        int w=Math.min(width,(int)Math.ceil(size.width*scale)),h=Math.min(height,(int)Math.ceil(size.height*scale));
        return new Rect(m.placement.pixelX(width,w),m.placement.pixelY(height,h),w,h);
    }
    public void render(Canvas c,ModuleRegistry registry,Telemetry t,int width,int height){
        for(ClientModule m:registry.all())if(m.hud&&m.enabled()&&!hidden(registry,m)){
            Rect r=bounds(c,m,t,width,height);drawAt(c,m,t,r.x,r.y,scale(c,m,t,width,height));
        }
    }
    public void drawAt(Canvas c,ClientModule m,Telemetry t,int x,int y,double scale){
        Size size=measure(c,m,t);boolean shadow=m.setting("shadow").bool();
        c.push(x,y,scale);
        try{
            if(m.setting("background").bool()){
                int alpha=(int)Math.round(m.setting("opacity").number()*255/100);
                c.rect(0,0,size.width,size.height,(alpha<<24)|0x17131A);
            }
            if(m.setting("accent").bool())c.rect(0,0,2,size.height,Theme.ORANGE);
            if(m.id.equals("keystrokes")){keys(c,m,t);return;}
            if(m.id.equals("armor")||m.id.equals("held_item")){
                Telemetry.Item[] items=m.id.equals("armor")?t.armor:t.held;
                boolean horizontal=m.id.equals("armor")&&m.setting("layout").raw().equals("Horizontal");
                if(items.length==0){c.text("NO EQUIPMENT",8,7,Theme.MUTED,shadow);return;}
                for(int i=0;i<items.length;i++){
                    Telemetry.Item item=items[i];int ix=horizontal?8+i*55:8,iy=horizontal?4:5+i*23;
                    c.item(item,ix,iy);int tx=horizontal?ix+19:ix+22;
                    c.text(Theme.truncate(c,item.label,horizontal?32:size.width-35),tx,iy+4,Theme.TEXT,shadow);
                    int w=horizontal?44:size.width-18;c.rect(ix,iy+18,w,2,0xFF40323A);
                    c.rect(ix,iy+18,(int)(w*Math.max(0,Math.min(1,item.fraction))),2,Theme.ORANGE);
                }return;
            }
            String[] lines=t.rows(m.id);
            if(lines.length==0)c.text(m.name.toUpperCase(java.util.Locale.ROOT)+" --",8,6,Theme.MUTED,shadow);
            for(int i=0;i<lines.length;i++)c.text(Theme.truncate(c,lines[i],size.width-16),8,6+i*12,Theme.TEXT,shadow);
            if(m.id.equals("frame_graph")){
                int n=t.frames.size(),available=size.width-16;
                for(int i=0;i<n;i++){
                    int bx=8+i*available/Math.max(1,n),next=8+(i+1)*available/Math.max(1,n);
                    int bh=(int)Math.round(Math.min(40,t.frames.sample(i)*2));
                    c.rect(bx,size.height-6-bh,Math.max(1,next-bx),bh,Theme.ORANGE);
                }
            }
        }finally{c.pop();}
    }
    private void keys(Canvas c,ClientModule m,Telemetry t){
        int[][] positions={{31,5},{5,31},{31,31},{57,31}};
        for(int i=0;i<4;i++)key(c,positions[i][0],positions[i][1],24,24,t.keyNames[i],t.keys[i]);
        key(c,5,57,76,20,t.keyNames[5],t.keys[5]);int y=81;
        if(m.setting("mouse").bool()){
            key(c,5,y,36,20,"L "+t.leftCps,t.keys[6]);key(c,45,y,36,20,"R "+t.rightCps,t.keys[7]);y+=24;
        }
        if(m.setting("jump").bool())key(c,5,y,76,13,t.keyNames[4],t.keys[4]);
    }
    private void key(Canvas c,int x,int y,int w,int h,String name,boolean down){
        Theme.panel(c,x,y,w,h,down?0xFF7D492C:0xFF27222A,down);
        String label=Theme.truncate(c,name,w-4);c.text(label,x+(w-c.textWidth(label))/2,y+(h-8)/2,down?Theme.GOLD:Theme.TEXT,false);
    }
}
