package dev.forgeclient.minecraft;

import dev.forgeclient.core.QuickplayCatalog;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Forge-styled, local-only Hypixel Quickplay selector. */
public final class ForgeQuickplayScreen extends GuiScreen {
    private static final int BG=0xF00F0D12,PANEL=0xE818151B,PANEL_HOVER=0xEF2B211E,EDGE=0xFFFF862D,GOLD=0xFFFFBA69,TEXT=0xFFE9E0DB,MUTED=0xFF9B918F,DIM=0xFF665E61;
    private final ForgeClient client;
    private final String legacyDefault;
    private String category="ALL",query="",status="";
    private long statusUntil;
    private List<QuickplayCatalog.Entry> visible=Collections.emptyList();
    private int selected,scroll;
    private boolean searchFocused;

    public ForgeQuickplayScreen(ForgeClient client,String legacyDefault){this.client=client;this.legacyDefault=legacyDefault;}
    @Override public void initGui(){Keyboard.enableRepeatEvents(true);rebuild(true);}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    @Override public boolean doesGuiPauseGame(){return false;}
    private int contentX(){return width>=720?154:18;}
    private int contentW(){return width-contentX()-18;}
    private int listTop(){return 111;}
    private int listBottom(){return height-34;}
    private int rowH(){return 46;}
    private void rebuild(boolean initial){
        visible=QuickplayCatalog.filter(category,query);
        if(initial)selected=Math.max(0,QuickplayCatalog.defaultIndex(visible,legacyDefault));
        if(selected>=visible.size())selected=Math.max(0,visible.size()-1);if(selected<0)selected=0;clampScroll();
    }
    private void clampScroll(){int rows=Math.max(0,visible.size()*rowH()),view=Math.max(1,listBottom()-listTop());scroll=Math.max(0,Math.min(scroll,Math.max(0,rows-view)));}
    private boolean inside(int mx,int my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private void panel(int x,int y,int w,int h,int color){Gui.drawRect(x,y,x+w,y+h,color);Gui.drawRect(x,y,x+2,y+h,EDGE);}
    private String clip(String text,int max){if(fontRendererObj.getStringWidth(text)<=max)return text;String s=text;while(s.length()>1&&fontRendererObj.getStringWidth(s+"...")>max)s=s.substring(0,s.length()-1);return s+"...";}

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        Gui.drawRect(0,0,width,height,BG);Gui.drawRect(0,0,width,2,EDGE);Gui.drawRect(0,52,width,1,0xFF443027);
        fontRendererObj.drawStringWithShadow("FORGE",18,18,GOLD);fontRendererObj.drawStringWithShadow("QUICKPLAY // HYPIXEL",18,34,MUTED);
        String network=isHypixel()?"HYPIXEL CONNECTED":"NOT ON HYPIXEL";int networkColor=isHypixel()?0xFF78D58B:0xFFE47C68;
        fontRendererObj.drawStringWithShadow(network,width-fontRendererObj.getStringWidth(network)-18,25,networkColor);
        if(width>=720)drawCategories(mouseX,mouseY);drawSearch(mouseX,mouseY);drawEntries(mouseX,mouseY);
        String hint="UP/DOWN SELECT   ENTER JOIN   TAB CATEGORY   CTRL+F SEARCH   ESC CLOSE";fontRendererObj.drawStringWithShadow(clip(hint,width-28),14,height-18,DIM);
        if(System.nanoTime()<statusUntil&&!status.isEmpty()){int w=Math.min(width-36,fontRendererObj.getStringWidth(status)+20);int x=(width-w)/2;panel(x,62,w,28,0xF0241C1C);fontRendererObj.drawStringWithShadow(status,x+10,72,GOLD);}
    }
    private void drawCategories(int mx,int my){
        int y=72;fontRendererObj.drawStringWithShadow("QUEUE GROUP",18,62,DIM);
        for(final String c:QuickplayCatalog.categories()){
            boolean active=c.equals(category),hover=inside(mx,my,14,y,126,27);Gui.drawRect(14,y,140,y+27,active?0xFF35271F:(hover?0xFF262126:0xB817151A));if(active)Gui.drawRect(14,y,17,y+27,EDGE);
            fontRendererObj.drawStringWithShadow(c,24,y+9,active?GOLD:MUTED);y+=31;
        }
    }
    private void drawSearch(int mx,int my){
        int x=contentX(),w=contentW(),y=68;boolean hover=inside(mx,my,x,y,w,30);panel(x,y,w,30,searchFocused?0xF0251D1A:(hover?0xED211B1C:PANEL));
        String value=query.isEmpty()&&!searchFocused?"Search queues...":query+(searchFocused&&((System.currentTimeMillis()/500)%2==0)?"_":"");
        fontRendererObj.drawStringWithShadow(">",x+10,y+11,EDGE);fontRendererObj.drawStringWithShadow(clip(value,w-42),x+26,y+11,query.isEmpty()&&!searchFocused?DIM:TEXT);
        String count=visible.size()+" RESULTS";fontRendererObj.drawStringWithShadow(count,x+w-fontRendererObj.getStringWidth(count)-10,y+11,DIM);
    }
    private void drawEntries(int mx,int my){
        int x=contentX(),w=contentW(),top=listTop(),bottom=listBottom(),index=0;
        for(QuickplayCatalog.Entry entry:visible){int y=top+index*rowH()-scroll;if(y+40>=top&&y<=bottom){boolean sel=index==selected,hover=inside(mx,my,x,y,w,40);panel(x,y,w,40,sel?0xF03B2A21:(hover?PANEL_HOVER:PANEL));
            fontRendererObj.drawStringWithShadow(clip(entry.name,w-144),x+12,y+8,sel?GOLD:TEXT);fontRendererObj.drawStringWithShadow(clip(entry.category+"  //  "+entry.description,w-150),x+12,y+23,MUTED);
            int jw=58,jx=x+w-jw-8;boolean join=inside(mx,my,jx,y+7,jw,26);Gui.drawRect(jx,y+7,jx+jw,y+33,join?0xFF6C4329:0xFF3D2B22);Gui.drawRect(jx,y+7,jx+2,y+33,EDGE);String label="JOIN";fontRendererObj.drawStringWithShadow(label,jx+(jw-fontRendererObj.getStringWidth(label))/2,y+16,join?GOLD:TEXT);
        }index++;}
        if(visible.isEmpty())fontRendererObj.drawStringWithShadow("NO QUEUES MATCH THIS FILTER",x+12,top+18,MUTED);
    }

    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{
        if(button!=0){super.mouseClicked(mx,my,button);return;}
        int x=contentX(),w=contentW();searchFocused=inside(mx,my,x,68,w,30);
        if(width>=720){int y=72;for(String c:QuickplayCatalog.categories()){if(inside(mx,my,14,y,126,27)){category=c;selected=0;scroll=0;rebuild(false);return;}y+=31;}}
        if(my>=listTop()&&my<listBottom()){int idx=(my-listTop()+scroll)/rowH();if(idx>=0&&idx<visible.size()){selected=idx;int rowY=listTop()+idx*rowH()-scroll;if(inside(mx,my,x+w-66,rowY+7,58,26))joinSelected();return;}}
        super.mouseClicked(mx,my,button);
    }
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0){scroll-=Integer.signum(wheel)*rowH()*2;clampScroll();}}
    @Override protected void keyTyped(char character,int code)throws IOException{
        if(code==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(null);return;}
        if(isCtrlKeyDown()&&code==Keyboard.KEY_F){searchFocused=true;return;}
        if(code==Keyboard.KEY_TAB){cycleCategory(isShiftKeyDown()?-1:1);return;}
        if(code==Keyboard.KEY_UP){move(-1);return;}if(code==Keyboard.KEY_DOWN){move(1);return;}if(code==Keyboard.KEY_PRIOR){move(-5);return;}if(code==Keyboard.KEY_NEXT){move(5);return;}
        if(code==Keyboard.KEY_RETURN||code==Keyboard.KEY_NUMPADENTER){joinSelected();return;}
        if(searchFocused){if(code==Keyboard.KEY_BACK){if(!query.isEmpty()){query=query.substring(0,query.length()-1);selected=0;scroll=0;rebuild(false);}return;}if(code==Keyboard.KEY_DELETE){query="";selected=0;scroll=0;rebuild(false);return;}if(character>=32&&character!=127&&query.length()<48){query+=character;selected=0;scroll=0;rebuild(false);return;}}
        super.keyTyped(character,code);
    }
    private void move(int delta){if(visible.isEmpty())return;selected=Math.max(0,Math.min(visible.size()-1,selected+delta));int y=selected*rowH();int view=Math.max(1,listBottom()-listTop());if(y<scroll)scroll=y;else if(y+rowH()>scroll+view)scroll=y+rowH()-view;clampScroll();}
    private void cycleCategory(int direction){List<String> cats=QuickplayCatalog.categories();int i=cats.indexOf(category);category=cats.get(Math.floorMod(i+direction,cats.size()));selected=0;scroll=0;rebuild(false);}
    private boolean isHypixel(){
        if(mc.getCurrentServerData()==null||mc.getCurrentServerData().serverIP==null)return false;String host=mc.getCurrentServerData().serverIP.trim().toLowerCase(Locale.ROOT);int slash=host.lastIndexOf('/');if(slash>=0)host=host.substring(slash+1);int colon=host.indexOf(':');if(colon>0)host=host.substring(0,colon);return host.equals("hypixel.net")||host.endsWith(".hypixel.net");
    }
    private void joinSelected(){
        if(visible.isEmpty()||selected<0||selected>=visible.size())return;if(mc.thePlayer==null){show("Join a world before using Quickplay.");return;}if(!isHypixel()){show("Quickplay is locked to Hypixel so Forge never sends /play on another server.");return;}
        QuickplayCatalog.Entry entry=visible.get(selected);mc.thePlayer.sendChatMessage(entry.command);mc.displayGuiScreen(null);
    }
    private void show(String message){status=EnumChatFormatting.getTextWithoutFormattingCodes(message);statusUntil=System.nanoTime()+3_000_000_000L;}
}
