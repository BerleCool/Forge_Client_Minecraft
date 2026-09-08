#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text()


def write(path, content):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content)


def replace_once(path, old, new):
    p = ROOT / path
    s = p.read_text()
    if old not in s:
        raise SystemExit("Missing patch anchor in %s: %s" % (path, old[:100]))
    if s.count(old) != 1:
        raise SystemExit("Patch anchor is not unique in %s: %s" % (path, old[:100]))
    p.write_text(s.replace(old, new, 1))


def replace_range(path, start_token, end_token, replacement):
    p = ROOT / path
    s = p.read_text()
    start = s.find(start_token)
    if start < 0:
        raise SystemExit("Missing range start in %s: %s" % (path, start_token))
    end = s.find(end_token, start)
    if end < 0:
        raise SystemExit("Missing range end in %s: %s" % (path, end_token))
    p.write_text(s[:start] + replacement + s[end:])


write(Path('src/main/java/dev/forgeclient/core/QuickplayCatalog.java'), r'''package dev.forgeclient.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Small, deterministic Hypixel queue catalog for Forge Client's local selector.
 * The UI and control flow are independently implemented. Queue identifiers are
 * public server command identifiers, not copied Quickplay source code.
 */
public final class QuickplayCatalog {
    public static final class Entry {
        public final String id, category, name, description, command;
        private Entry(String id, String category, String name, String description, String command) {
            this.id=id;this.category=category;this.name=name;this.description=description;this.command=command;
        }
        public boolean matches(String query) {
            String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
            return q.isEmpty() || (id+" "+category+" "+name+" "+description+" "+command).toLowerCase(Locale.ROOT).contains(q);
        }
    }

    private static final List<Entry> ALL;
    private static final List<String> CATEGORIES;
    static {
        List<Entry> e=new ArrayList<Entry>();
        add(e,"bw_solo","BED WARS","Solo","8 teams / 1 player","/play bedwars_eight_one");
        add(e,"bw_doubles","BED WARS","Doubles","8 teams / 2 players","/play bedwars_eight_two");
        add(e,"bw_3v3v3v3","BED WARS","3v3v3v3","4 teams / 3 players","/play bedwars_four_three");
        add(e,"bw_4v4v4v4","BED WARS","4v4v4v4","4 teams / 4 players","/play bedwars_four_four");
        add(e,"sw_solo_normal","SKYWARS","Solo Normal","Solo SkyWars / Normal kits","/play solo_normal");
        add(e,"sw_solo_insane","SKYWARS","Solo Insane","Solo SkyWars / Insane kits","/play solo_insane");
        add(e,"sw_teams_normal","SKYWARS","Teams Normal","Teams SkyWars / Normal kits","/play teams_normal");
        add(e,"sw_teams_insane","SKYWARS","Teams Insane","Teams SkyWars / Insane kits","/play teams_insane");
        add(e,"duel_classic","DUELS","Classic Duel","Classic 1v1","/play duels_classic_duel");
        add(e,"duel_uhc","DUELS","UHC Duel","UHC 1v1","/play duels_uhc_duel");
        add(e,"duel_sumo","DUELS","Sumo Duel","Sumo 1v1","/play duels_sumo_duel");
        add(e,"duel_op","DUELS","OP Duel","OP 1v1","/play duels_op_duel");
        add(e,"duel_bridge","DUELS","Bridge Duel","Bridge 1v1","/play duels_bridge_duel");
        add(e,"duel_bow","DUELS","Bow Duel","Bow 1v1","/play duels_bow_duel");
        add(e,"lobby_main","LOBBIES","Main Lobby","Return to the Hypixel lobby","/lobby");
        add(e,"lobby_bedwars","LOBBIES","Bed Wars Lobby","Open the Bed Wars lobby","/lobby bedwars");
        add(e,"lobby_skywars","LOBBIES","SkyWars Lobby","Open the SkyWars lobby","/lobby skywars");
        add(e,"lobby_duels","LOBBIES","Duels Lobby","Open the Duels lobby","/lobby duels");
        add(e,"housing_home","OTHER","Housing Home","Go to your Housing home","/home");
        ALL=Collections.unmodifiableList(e);
        Set<String> cats=new LinkedHashSet<String>();
        cats.add("ALL");for(Entry entry:e)cats.add(entry.category);
        CATEGORIES=Collections.unmodifiableList(new ArrayList<String>(cats));
    }
    private QuickplayCatalog(){}
    private static void add(List<Entry> out,String id,String category,String name,String description,String command){out.add(new Entry(id,category,name,description,command));}
    public static List<Entry> all(){return ALL;}
    public static List<String> categories(){return CATEGORIES;}
    public static List<Entry> filter(String category,String query){
        List<Entry> out=new ArrayList<Entry>();String c=category==null?"ALL":category;
        for(Entry entry:ALL)if(("ALL".equals(c)||entry.category.equals(c))&&entry.matches(query))out.add(entry);
        return out;
    }
    public static int defaultIndex(List<Entry> entries,String legacyMode){
        if(entries==null||entries.isEmpty())return -1;
        String wanted="Bed Wars Solo".equals(legacyMode)?"bw_solo":"Bed Wars Doubles".equals(legacyMode)?"bw_doubles":"SkyWars Solo".equals(legacyMode)?"sw_solo_normal":"Duels".equals(legacyMode)?"duel_classic":"Lobby".equals(legacyMode)?"lobby_main":"bw_solo";
        for(int i=0;i<entries.size();i++)if(entries.get(i).id.equals(wanted))return i;
        return 0;
    }
}
''')

write(Path('src/main/java/dev/forgeclient/core/ModuleImplementationAudit.java'), r'''package dev.forgeclient.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Machine-readable provenance for every live module. Open-source does not mean
 * every license can be pasted into a combined client: MIT code may be adapted
 * with attribution; copyleft/restrictive projects are behavior references only.
 */
public final class ModuleImplementationAudit {
    public enum Use { NATIVE_FORGE, MIT_ADAPTED, CLEAN_ROOM_REFERENCE }
    public static final class Entry {
        public final String moduleId,source,license,note;
        public final Use use;
        private Entry(String moduleId,Use use,String source,String license,String note){this.moduleId=moduleId;this.use=use;this.source=source;this.license=license;this.note=note;}
    }
    private ModuleImplementationAudit(){}
    public static Map<String,Entry> build(ModuleRegistry registry){
        LinkedHashMap<String,Entry> out=new LinkedHashMap<String,Entry>();
        for(ClientModule m:registry.all())out.put(m.id,new Entry(m.id,Use.NATIVE_FORGE,"Minecraft 1.8.9 / Forge events","target APIs","Direct client-state implementation; no third-party source copied."));
        mark(out,"cps",Use.MIT_ADAPTED,"Marschi47/BasicHUD","MIT","Rolling one-second click window informed by BasicHUD.");
        String[] sba={"lunar_scoreboard","lunar_hypixel","lunar_bedwars","lunar_skyblock","lunar_sba"};
        for(String id:sba)mark(out,id,Use.MIT_ADAPTED,"BiscuitDevelopment/SkyblockAddons","MIT","Sidebar snapshot/filtering approach adapted to the Forge Client data model and UI.");
        mark(out,"lunar_quickplay",Use.CLEAN_ROOM_REFERENCE,"LunarClient/Apollo + QuickplayMod/quickplay","MIT semantics + restrictive Quickplay license","Forge selector is independently implemented; Quickplay source is not copied.");
        mark(out,"lunar_replay",Use.CLEAN_ROOM_REFERENCE,"ReplayMod/ReplayMod","GPL-3.0 reference only","Bounded local movement recorder; ReplayMod source is not copied or shaded.");
        mark(out,"lunar_rewind",Use.CLEAN_ROOM_REFERENCE,"ReplayMod/ReplayMod","GPL-3.0 reference only","Uses Forge Client's own local movement buffer.");
        mark(out,"lunar_neu",Use.CLEAN_ROOM_REFERENCE,"NotEnoughUpdates/NotEnoughUpdates","LGPL reference only","Reads vanilla item NBT/lore locally; NEU source is not copied.");
        mark(out,"toggle_sprint",Use.CLEAN_ROOM_REFERENCE,"My-Name-Is-Jeff/SimpleToggleSprint","AGPL-3.0 reference only","Forge Client state machine is independently implemented.");
        String[] perf={"smart_fps","particle_budget","fast_graphics","entity_shadows","clouds","distance_culling"};
        for(String id:perf)mark(out,id,Use.CLEAN_ROOM_REFERENCE,"Guichaguri/BetterFps + vanilla GameSettings","LGPL reference only / target APIs","Uses reversible vanilla/Forge controls; BetterFps source is not copied.");
        return Collections.unmodifiableMap(out);
    }
    private static void mark(Map<String,Entry> map,String id,Use use,String source,String license,String note){if(!map.containsKey(id))throw new IllegalArgumentException("Unknown audit module "+id);map.put(id,new Entry(id,use,source,license,note));}
    public static void validate(ModuleRegistry registry){
        Map<String,Entry> audit=build(registry);
        if(audit.size()!=registry.all().size())throw new IllegalStateException("Implementation audit does not cover the live module catalog");
        for(ClientModule m:registry.all())if(!audit.containsKey(m.id))throw new IllegalStateException("Missing implementation audit for "+m.id);
    }
}
''')

write(Path('src/main/java/dev/forgeclient/minecraft/SkyblockScoreboardSnapshot.java'), r'''package dev.forgeclient.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Immutable sidebar snapshot. The filter/cap/format pipeline is adapted from
 * BiscuitDevelopment/SkyblockAddons' MIT-licensed ScoreboardManager, rewritten
 * without its globals, Guava, Lombok, streams, or background data services.
 */
public final class SkyblockScoreboardSnapshot {
    private static final SkyblockScoreboardSnapshot EMPTY=new SkyblockScoreboardSnapshot("","",Collections.<String>emptyList(),Collections.<String>emptyList());
    public final String formattedTitle,plainTitle;
    private final List<String> formatted,plain;
    private SkyblockScoreboardSnapshot(String formattedTitle,String plainTitle,List<String> formatted,List<String> plain){this.formattedTitle=formattedTitle;this.plainTitle=plainTitle;this.formatted=formatted;this.plain=plain;}
    public static SkyblockScoreboardSnapshot empty(){return EMPTY;}
    public static SkyblockScoreboardSnapshot capture(Minecraft mc){
        if(mc==null||mc.theWorld==null)return EMPTY;
        Scoreboard board=mc.theWorld.getScoreboard();if(board==null)return EMPTY;
        ScoreObjective objective=board.getObjectiveInDisplaySlot(1);if(objective==null)return EMPTY;
        List<Score> filtered=new ArrayList<Score>();Collection<Score> scores=board.getSortedScores(objective);
        for(Score score:scores){String name=score.getPlayerName();if(name!=null&&!name.startsWith("#"))filtered.add(score);}
        if(filtered.size()>15)filtered=new ArrayList<Score>(filtered.subList(filtered.size()-15,filtered.size()));
        // Vanilla lays ascending scores from bottom to top. Reverse for a top-down HUD list.
        Collections.reverse(filtered);
        List<String> formatted=new ArrayList<String>(filtered.size()),plain=new ArrayList<String>(filtered.size());
        for(Score score:filtered){
            String name=score.getPlayerName();String line=ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name),name).trim();
            formatted.add(line);plain.add(strip(line));
        }
        return new SkyblockScoreboardSnapshot(objective.getDisplayName(),strip(objective.getDisplayName()),Collections.unmodifiableList(formatted),Collections.unmodifiableList(plain));
    }
    private static String strip(String text){String out=EnumChatFormatting.getTextWithoutFormattingCodes(text==null?"":text);if(out==null)return "";StringBuilder clean=new StringBuilder(out.length());for(int i=0;i<out.length();i++){char c=out.charAt(i);if(c>=32&&c!=127)clean.append(c);}return clean.toString().trim();}
    public boolean present(){return !formattedTitle.isEmpty()||!formatted.isEmpty();}
    public List<String> formattedLines(){return formatted;}
    public List<String> plainLines(){return plain;}
    public String[] rows(int maxLines){
        if(!present())return new String[]{"NO SCOREBOARD"};int max=Math.max(1,maxLines),count=Math.min(max,formatted.size());List<String> out=new ArrayList<String>(count+1);if(!formattedTitle.isEmpty())out.add(formattedTitle);for(int i=0;i<count;i++)out.add(formatted.get(i));return out.toArray(new String[out.size()]);
    }
    public String plainJoined(){StringBuilder out=new StringBuilder(plainTitle);for(String line:plain)out.append('\n').append(line);return out.toString();}
    public boolean containsIgnoreCase(String needle){return plainJoined().toLowerCase(java.util.Locale.ROOT).contains(needle.toLowerCase(java.util.Locale.ROOT));}
}
''')

write(Path('src/main/java/dev/forgeclient/minecraft/ForgeQuickplayScreen.java'), r'''package dev.forgeclient.minecraft;

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
''')

write(Path('src/test/java/dev/forgeclient/tests/V4ContractTests.java'), r'''package dev.forgeclient.tests;

import dev.forgeclient.core.ClientModule;
import dev.forgeclient.core.ModuleCatalog;
import dev.forgeclient.core.ModuleImplementationAudit;
import dev.forgeclient.core.ModuleRegistry;
import dev.forgeclient.core.QuickplayCatalog;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class V4ContractTests {
    private V4ContractTests(){}
    public static void main(String[] args){
        ModuleRegistry registry=ModuleCatalog.create();
        if(registry.all().size()!=89)throw new AssertionError("Expected 89 live modules, got "+registry.all().size());
        ModuleImplementationAudit.validate(registry);Map<String,ModuleImplementationAudit.Entry> audit=ModuleImplementationAudit.build(registry);
        if(audit.size()!=89)throw new AssertionError("Audit coverage mismatch: "+audit.size());
        ClientModule quickplay=registry.get("lunar_quickplay");if(!quickplay.available()||!quickplay.holdBinding)throw new AssertionError("Quickplay must be a live action binding");
        List<QuickplayCatalog.Entry> entries=QuickplayCatalog.all();if(entries.size()<18)throw new AssertionError("Quickplay catalog unexpectedly small");
        Set<String> ids=new HashSet<String>(),commands=new HashSet<String>();for(QuickplayCatalog.Entry entry:entries){if(!ids.add(entry.id))throw new AssertionError("Duplicate Quickplay id "+entry.id);if(!entry.command.startsWith("/"))throw new AssertionError("Unsafe Quickplay command "+entry.command);commands.add(entry.command);}
        if(!commands.contains("/play bedwars_eight_one")||!commands.contains("/play bedwars_eight_two")||!commands.contains("/play solo_insane")||!commands.contains("/play duels_classic_duel"))throw new AssertionError("Core Hypixel queues missing");
        if(QuickplayCatalog.filter("BED WARS","double").size()!=1)throw new AssertionError("Quickplay search/category filter broken");
        System.out.println("Forge Client 0.4 contracts: 89/89 implementation audit, "+entries.size()+" Quickplay destinations, no duplicate ids.");
    }
}
''')

# Version and metadata wiring.
replace_once(Path('src/main/java/dev/forgeclient/minecraft/ForgeClient.java'),'public static final String MOD_ID="forgeclient",VERSION="0.3.0-alpha";','public static final String MOD_ID="forgeclient",VERSION="0.4.0-alpha";')
replace_once(Path('build.gradle.kts'),'version = "0.3.0-alpha"','version = "0.4.0-alpha"')
replace_once(Path('src/main/java/dev/forgeclient/ui/OverlayView.java'),'FORGE  /  0.3.0-ALPHA','FORGE  /  0.4.0-ALPHA')
replace_once(Path('src/main/java/dev/forgeclient/ui/OverlayView.java'),'(m.holdBinding?"HOLD ":"KEY ")','(m.holdBinding?"ACTION ":"KEY ")')

# Make the second Java-8 contract suite part of `check`.
build=read(Path('build.gradle.kts'))
anchor='tasks.check { dependsOn(coreTests) }\n'
if anchor not in build:raise SystemExit('Missing Gradle test anchor')
block='''val v4ContractTests by tasks.registering(JavaExec::class) {\n    dependsOn(tasks.testClasses)\n    classpath = sourceSets.test.get().runtimeClasspath\n    mainClass.set("dev.forgeclient.tests.V4ContractTests")\n    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })\n    enableAssertions = true\n}\ntasks.check { dependsOn(coreTests, v4ContractTests) }\n'''
write(Path('build.gradle.kts'),build.replace(anchor,block,1))

# Make Quickplay a selector action and clarify the improved local implementations.
mods=read(Path('src/main/java/dev/forgeclient/core/LunarFunctionalModules.java'))
mods=mods.replace('action(r,"lunar_quickplay","Quickplay","One-key Hypixel queue","When enabled and bound, sends the selected /play command only when you press its key.")\n            .add(Setting.choice("mode","Queue","Hypixel game queue used by the hotkey.","Bed Wars Solo","Bed Wars Solo","Bed Wars Doubles","SkyWars Solo","Duels","Lobby"));',
'''action(r,"lunar_quickplay","Quickplay","Forge-styled Hypixel queue selector","Press the bound action key to open a native Forge selector with categories, search and keyboard navigation. Commands are sent only after you explicitly choose Join and only on hypixel.net.")\n            .add(Setting.choice("mode","Default selection","Initial destination highlighted when the selector opens.","Bed Wars Solo","Bed Wars Solo","Bed Wars Doubles","SkyWars Solo","Duels","Lobby"));''')
mods=mods.replace('r.add(module("lunar_item_physics","Item Physics",Category.VISUAL,"Smoother dropped-item motion","Applies a bounded client-only rotation to loaded dropped-item entities while enabled.",false)',
'r.add(module("lunar_item_physics","Item Physics",Category.VISUAL,"Dropped-item spin control","Advances the vanilla 1.8.9 EntityItem hover phase used by its renderer, producing a visible client-only spin without touching item physics or packets.",false)')
mods=mods.replace('hud(r,"lunar_minimap","Minimap","Local block minimap","Builds a tiny text-grid map from already-loaded blocks around you. It never loads chunks or shows hidden entities.",false,.02,.30)',
'hud(r,"lunar_minimap","Minimap","Loaded-block minimap","Builds a bounded top-down map from already-loaded blocks around you and renders it as a Forge-styled tile HUD. It never loads chunks or reveals hidden entities.",false,.02,.30)')
mods=mods.replace('hud(r,"lunar_neu","NotEnoughUpdates","SkyBlock item inspector","Provides a lightweight 1.8.9 SkyBlock held-item/lore inspector using the item data already present on the client.",false,.70,.76);',
'hud(r,"lunar_neu","NotEnoughUpdates","SkyBlock item inspector","License-safe NEU-style inspector using local vanilla NBT/lore: display name, ExtraAttributes id and lore. The LGPL NEU codebase is behavior reference only and is not copied.",false,.70,.76);')
mods=mods.replace('hud(r,"lunar_sba","SkyBlockAddons","SkyBlock status HUD","Provides a lightweight SkyBlock action-bar/scoreboard status panel using client-received data.",false,.70,.82);',
'hud(r,"lunar_sba","SkyBlockAddons","SkyBlock status HUD","Uses an MIT-adapted SkyblockAddons-style sidebar snapshot plus the local action bar to build a clean SkyBlock status panel from client-received data.",false,.70,.82);')
write(Path('src/main/java/dev/forgeclient/core/LunarFunctionalModules.java'),mods)

# Telemetry gains a typed hotbar snapshot for the Inventory Mod HUD.
tele=read(Path('src/main/java/dev/forgeclient/core/Telemetry.java'))
tele=tele.replace('public Item[] armor=new Item[0],held=new Item[0];','public Item[] armor=new Item[0],held=new Item[0],hotbar=new Item[9];\n    public int hotbarSlot;')
tele=tele.replace('armor=new Item[0];held=new Item[0];values.clear();','armor=new Item[0];held=new Item[0];hotbar=new Item[9];hotbarSlot=0;values.clear();')
write(Path('src/main/java/dev/forgeclient/core/Telemetry.java'),tele)

sampler=read(Path('src/main/java/dev/forgeclient/minecraft/TelemetrySampler.java'))
needle='''        if(enabled("held_item")){ItemStack stack=p.getHeldItem();data.held=stack==null?new Telemetry.Item[0]:new Telemetry.Item[]{item(stack,modules.get("held_item").setting("percent").bool())};}\n'''
addition=needle+'''        if(enabled("lunar_inventory")){Telemetry.Item[] hotbar=new Telemetry.Item[9];for(int i=0;i<9;i++){ItemStack stack=p.inventory.mainInventory[i];if(stack!=null)hotbar[i]=item(stack,false);}data.hotbar=hotbar;data.hotbarSlot=Math.max(0,Math.min(8,p.inventory.currentItem));}\n'''
if needle not in sampler:raise SystemExit('Missing TelemetrySampler held-item anchor')
sampler=sampler.replace(needle,addition,1)
write(Path('src/main/java/dev/forgeclient/minecraft/TelemetrySampler.java'),sampler)

# Render item overlays (stack counts/durability) and add polished inventory/minimap widgets.
native=read(Path('src/main/java/dev/forgeclient/minecraft/NativeCanvas.java'))
replace='mc.getRenderItem().renderItemAndEffectIntoGUI((ItemStack)item.stack,x,y);'
if replace not in native:raise SystemExit('Missing NativeCanvas item renderer anchor')
native=native.replace(replace,replace+'mc.getRenderItem().renderItemOverlays(mc.fontRendererObj,(ItemStack)item.stack,x,y);',1)
write(Path('src/main/java/dev/forgeclient/minecraft/NativeCanvas.java'),native)

hud=r'''package dev.forgeclient.ui;
import dev.forgeclient.core.*;
/** Stateless layout and drawing shared by the native mod and verification harness. */
public final class HudRenderer {
    public static final class Size { public final int width,height;public Size(int w,int h){width=w;height=h;} }
    public boolean hidden(ModuleRegistry registry,ClientModule module){return registry.enabled("streamer_mode")&&registry.get("streamer_mode").setting("coordinates").bool()&&(module.id.equals("coordinates")||module.id.equals("waypoint"));}
    public Size measure(Canvas c,ClientModule m,Telemetry t){
        if(m.id.equals("keystrokes"))return new Size(86,88+(m.setting("mouse").bool()?24:0)+(m.setting("jump").bool()?17:0));
        if(m.id.equals("lunar_inventory"))return new Size(174,28);
        if(m.id.equals("lunar_minimap")){int r=m.setting("radius").integer(),d=r*2+1;return new Size(Math.max(58,d*5+14),d*5+19);}
        if(m.id.equals("armor")||m.id.equals("held_item")){Telemetry.Item[] items=m.id.equals("armor")?t.armor:t.held;boolean horizontal=m.id.equals("armor")&&m.setting("layout").raw().equals("Horizontal");if(items.length==0)return new Size(96,22);return horizontal?new Size(items.length*55+8,36):new Size(m.id.equals("armor")?100:160,items.length*23+8);}
        int width=48;String[] lines=t.rows(m.id);for(String line:lines)width=Math.max(width,c.textWidth(line)+16);if(m.id.equals("frame_graph"))return new Size(Math.max(180,Math.min(260,width)),68);return new Size(Math.min(280,width),Math.max(1,lines.length)*12+10);
    }
    private double scale(Size size,ClientModule m,int w,int h){return Math.min(m.setting("scale").number(),Math.min(w/(double)Math.max(1,size.width),h/(double)Math.max(1,size.height)));}
    public Rect bounds(Canvas c,ClientModule m,Telemetry t,int width,int height){Size size=measure(c,m,t);double scale=scale(size,m,width,height);int w=Math.min(width,(int)Math.ceil(size.width*scale)),h=Math.min(height,(int)Math.ceil(size.height*scale));return new Rect(m.placement.pixelX(width,w),m.placement.pixelY(height,h),w,h);}
    public void render(Canvas c,ModuleRegistry registry,Telemetry t,int width,int height){for(ClientModule m:registry.hud())if(m.enabled()&&!hidden(registry,m)){Size size=measure(c,m,t);double scale=scale(size,m,width,height);int w=Math.min(width,(int)Math.ceil(size.width*scale)),h=Math.min(height,(int)Math.ceil(size.height*scale));Rect r=new Rect(m.placement.pixelX(width,w),m.placement.pixelY(height,h),w,h);drawMeasured(c,m,t,r.x,r.y,scale,size);}}
    public void drawAt(Canvas c,ClientModule m,Telemetry t,int x,int y,double scale){drawMeasured(c,m,t,x,y,scale,measure(c,m,t));}
    private void drawMeasured(Canvas c,ClientModule m,Telemetry t,int x,int y,double scale,Size size){boolean shadow=m.setting("shadow").bool();c.push(x,y,scale);try{boolean sprintHighlight=m.id.equals("sprint_status")&&t.toggleSprintEnabled;if(m.setting("background").bool()){int alpha=(int)Math.round(m.setting("opacity").number()*255/100);c.rect(0,0,size.width,size.height,(alpha<<24)|(sprintHighlight?0x2A1A10:0x17131A));}if(m.setting("accent").bool())c.rect(0,0,sprintHighlight?3:2,size.height,sprintHighlight?Theme.GOLD:Theme.ORANGE);
        if(m.id.equals("keystrokes")){keys(c,m,t);return;}if(m.id.equals("lunar_inventory")){inventory(c,t);return;}if(m.id.equals("lunar_minimap")){minimap(c,m,t,size,shadow);return;}
        if(m.id.equals("armor")||m.id.equals("held_item")){Telemetry.Item[] items=m.id.equals("armor")?t.armor:t.held;boolean horizontal=m.id.equals("armor")&&m.setting("layout").raw().equals("Horizontal");if(items.length==0){c.text("NO EQUIPMENT",8,7,Theme.MUTED,shadow);return;}for(int i=0;i<items.length;i++){Telemetry.Item item=items[i];int ix=horizontal?8+i*55:8,iy=horizontal?4:5+i*23;c.item(item,ix,iy);int tx=horizontal?ix+19:ix+22;c.text(Theme.truncate(c,item.label,horizontal?32:size.width-35),tx,iy+4,Theme.TEXT,shadow);int w=horizontal?44:size.width-18;c.rect(ix,iy+18,w,2,0xFF40323A);c.rect(ix,iy+18,(int)(w*Math.max(0,Math.min(1,item.fraction))),2,Theme.ORANGE);}return;}
        String[] lines=t.rows(m.id);if(lines.length==0)c.text(m.name.toUpperCase(java.util.Locale.ROOT)+" --",8,6,Theme.MUTED,shadow);for(int i=0;i<lines.length;i++)c.text(Theme.truncate(c,lines[i],size.width-16),8,6+i*12,(m.id.equals("sprint_status")&&t.toggleSprintEnabled&&i==0)?Theme.GOLD:Theme.TEXT,shadow);if(m.id.equals("frame_graph")){int n=t.frames.size(),available=size.width-16;for(int i=0;i<n;i++){int bx=8+i*available/Math.max(1,n),next=8+(i+1)*available/Math.max(1,n);int bh=(int)Math.round(Math.min(40,t.frames.sample(i)*2));c.rect(bx,size.height-6-bh,Math.max(1,next-bx),bh,Theme.ORANGE);}}
    }finally{c.pop();}}
    private void inventory(Canvas c,Telemetry t){for(int i=0;i<9;i++){int x=6+i*18;boolean selected=i==t.hotbarSlot;c.rect(x,5,18,18,selected?0xFF6B4328:0xC928242B);if(selected)c.rect(x,5,18,2,Theme.GOLD);Telemetry.Item item=i<t.hotbar.length?t.hotbar[i]:null;if(item!=null)c.item(item,x+1,6);}}
    private void minimap(Canvas c,ClientModule m,Telemetry t,Size size,boolean shadow){String[] rows=t.rows("lunar_minimap");int r=m.setting("radius").integer(),d=r*2+1,ox=(size.width-d*5)/2,oy=14;c.text("N",(size.width-c.textWidth("N"))/2,4,Theme.GOLD,shadow);for(int z=0;z<d;z++){String row=z+1<rows.length?rows[z+1]:"";for(int x=0;x<d;x++){char ch=x<row.length()?row.charAt(x):'?';int color;switch(ch){case '~':color=0xFF355F7A;break;case '!':color=0xFFB5522F;break;case '.':color=0xFF242129;break;case '?':color=0xFF151318;break;case '^':case '>':case 'v':case '<':case '@':color=Theme.GOLD;break;default:color=0xFF685B50;}c.rect(ox+x*5,oy+z*5,4,4,color);}}}
    private void keys(Canvas c,ClientModule m,Telemetry t){int[][] positions={{31,5},{5,31},{31,31},{57,31}};for(int i=0;i<4;i++)key(c,positions[i][0],positions[i][1],24,24,t.keyNames[i],t.keys[i]);key(c,5,57,76,20,t.keyNames[5],t.keys[5]);int y=81;if(m.setting("mouse").bool()){key(c,5,y,36,20,"L "+t.leftCps,t.keys[6]);key(c,45,y,36,20,"R "+t.rightCps,t.keys[7]);y+=24;}if(m.setting("jump").bool())key(c,5,y,76,13,t.keyNames[4],t.keys[4]);}
    private void key(Canvas c,int x,int y,int w,int h,String name,boolean down){Theme.panel(c,x,y,w,h,down?0xFF7D492C:0xFF27222A,down);String label=Theme.truncate(c,name,w-4);c.text(label,x+(w-c.textWidth(label))/2,y+(h-8)/2,down?Theme.GOLD:Theme.TEXT,false);}
}
'''
write(Path('src/main/java/dev/forgeclient/ui/HudRenderer.java'),hud)

# Runtime upgrades: cached scoreboard/inventory data, real item-spin phase, better minimap/NBT inspector, Quickplay GUI.
runtime_path=Path('src/main/java/dev/forgeclient/minecraft/LunarRuntime.java')
runtime=read(runtime_path)
runtime=runtime.replace('private Map<String,Integer> previousInventory=Collections.emptyMap();','private Map<String,Integer> previousInventory=Collections.emptyMap();\n    private final Map<net.minecraft.item.Item,Integer> itemCounts=new IdentityHashMap<net.minecraft.item.Item,Integer>();\n    private SkyblockScoreboardSnapshot sidebar=SkyblockScoreboardSnapshot.empty();\n    private String[] minimapCache=new String[]{"N","?"};')
runtime=runtime.replace('''        if(enabled("lunar_item_physics")){\n            float spin=(float)mod("lunar_item_physics").setting("speed").number();int touched=0;\n            for(Object raw:mc.theWorld.loadedEntityList)if(raw instanceof EntityItem){EntityItem item=(EntityItem)raw;item.rotationYaw=(item.rotationYaw+spin)%360F;if(++touched>=96)break;}\n        }''','''        if(enabled("lunar_item_physics")){\n            float spin=(float)Math.toRadians(mod("lunar_item_physics").setting("speed").number());int touched=0;\n            for(Object raw:mc.theWorld.loadedEntityList)if(raw instanceof EntityItem){EntityItem item=(EntityItem)raw;item.hoverStart=(item.hoverStart+spin)%(float)(Math.PI*2D);if(++touched>=96)break;}\n        }''')
runtime=runtime.replace('''        if((ticks%10)==0){if(enabled("lunar_light_overlay"))sampleLowLight();else lowLight.clear();if(enabled("lunar_item_tracker"))sampleInventoryDelta();}\n''','''        if((ticks%10)==0){if(enabled("lunar_light_overlay"))sampleLowLight();else lowLight.clear();if(enabled("lunar_item_tracker"))sampleInventoryDelta();if(enabled("lunar_minimap"))minimapCache=buildMinimapRows();else minimapCache=new String[]{"N","?"};}\n''')
runtime=runtime.replace('lastActionBar="";previousInventory=Collections.emptyMap();lastInventoryDelta="NO RECENT CHANGE";lastHurtTime=0;latestKnockback=0;lastX=lastZ=lastSpeed=0;','lastActionBar="";previousInventory=Collections.emptyMap();itemCounts.clear();sidebar=SkyblockScoreboardSnapshot.empty();minimapCache=new String[]{"N","?"};lastInventoryDelta="NO RECENT CHANGE";lastHurtTime=0;latestKnockback=0;lastX=lastZ=lastSpeed=0;')
runtime=runtime.replace('''    private void sampleHud(long n){\n        EntityPlayer p=mc.thePlayer;''','''    private void sampleHud(long n){\n        EntityPlayer p=mc.thePlayer;sidebar=SkyblockScoreboardSnapshot.capture(mc);refreshItemCounts();''')
runtime=runtime.replace('if(enabled("lunar_potion_counter")){int potions=0;for(ItemStack s:p.inventory.mainInventory)if(s!=null&&s.getItem()==Items.potionitem)potions+=s.stackSize;client.telemetry.put("lunar_potion_counter",potions+" POTIONS");}','if(enabled("lunar_potion_counter"))client.telemetry.put("lunar_potion_counter",countItem(Items.potionitem)+" POTIONS");')
runtime=runtime.replace('if(enabled("lunar_minimap"))client.telemetry.put("lunar_minimap",minimapRows());','if(enabled("lunar_minimap"))client.telemetry.put("lunar_minimap",minimapCache);')
runtime=runtime.replace('''    private int countItem(net.minecraft.item.Item item){int n=0;for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null&&s.getItem()==item)n+=s.stackSize;return n;}''','''    private void refreshItemCounts(){itemCounts.clear();for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null){net.minecraft.item.Item item=s.getItem();Integer count=itemCounts.get(item);itemCounts.put(item,(count==null?0:count)+s.stackSize);}}\n    private int countItem(net.minecraft.item.Item item){Integer count=itemCounts.get(item);return count==null?0:count;}''')

# Replace repeated sidebar walks with one MIT-adapted snapshot captured per sample pass.
start=runtime.find('    private String[] scoreboardLines(int max)')
end=runtime.find('    private void sampleHypixel()',start)
if start<0 or end<0:raise SystemExit('Could not locate scoreboard helpers in LunarRuntime')
new_helpers='''    private String[] scoreboardLines(int max){return sidebar.rows(max);}\n    private String scoreboardPlain(){return sidebar.plainJoined();}\n    private boolean hypixel(){if(mc.getCurrentServerData()==null||mc.getCurrentServerData().serverIP==null)return false;String host=mc.getCurrentServerData().serverIP.trim().toLowerCase(Locale.ROOT);int slash=host.lastIndexOf('/');if(slash>=0)host=host.substring(slash+1);int colon=host.indexOf(':');if(colon>0)host=host.substring(0,colon);return host.equals("hypixel.net")||host.endsWith(".hypixel.net");}\n'''
runtime=runtime[:start]+new_helpers+runtime[end:]

# Stronger SkyBlock inspectors without importing NEU's LGPL implementation.
start=runtime.find('    private void sampleSkyblockAddons()')
end=runtime.find('    private void sampleTier()',start)
if start<0 or end<0:raise SystemExit('Could not locate SkyBlock inspector helpers')
new_inspectors='''    private void sampleSkyblockAddons(){if(!hypixel()||!scoreboardPlain().toLowerCase(Locale.ROOT).contains("skyblock")){client.telemetry.put("lunar_sba","SKYBLOCK NOT DETECTED");return;}String action=EnumChatFormatting.getTextWithoutFormattingCodes(lastActionBar);if(action==null||action.trim().isEmpty())action="SKYBLOCK ACTIVE";List<String> rows=new ArrayList<String>();rows.add(action.trim());for(String line:sidebar.plainLines()){String l=line.toLowerCase(Locale.ROOT);if((l.contains("purse")||l.contains("bits")||l.contains("location"))&&rows.size()<4)rows.add(line);}client.telemetry.put("lunar_sba",rows.toArray(new String[rows.size()]));}\n    private void sampleNeu(){ItemStack held=mc.thePlayer.getHeldItem();if(held==null){client.telemetry.put("lunar_neu","NO HELD ITEM");return;}List<String> rows=new ArrayList<String>();rows.add(held.getDisplayName());net.minecraft.nbt.NBTTagCompound tag=held.getTagCompound();if(tag!=null&&tag.hasKey("ExtraAttributes",10)){net.minecraft.nbt.NBTTagCompound extra=tag.getCompoundTag("ExtraAttributes");if(extra.hasKey("id",8))rows.add("ID "+extra.getString("id"));}List<String> tip=held.getTooltip(mc.thePlayer,false);for(int i=1;i<tip.size()&&rows.size()<5;i++){String line=tip.get(i);if(line!=null&&!line.trim().isEmpty())rows.add(line);}client.telemetry.put("lunar_neu",rows.toArray(new String[rows.size()]));}\n'''
runtime=runtime[:start]+new_inspectors+runtime[end:]

# Better terrain-following loaded-only minimap and deterministic inventory deltas.
start=runtime.find('    private String[] minimapRows()')
end=runtime.find('    private String cleanItemKey',start)
if start<0 or end<0:raise SystemExit('Could not locate minimap/inventory helpers')
new_map='''    private String[] buildMinimapRows(){int r=mod("lunar_minimap").setting("radius").integer();List<String> rows=new ArrayList<String>();rows.add("N");int centerY=(int)Math.floor(mc.thePlayer.posY)-1;char player=facingChar(mc.thePlayer.rotationYaw);for(int dz=-r;dz<=r;dz++){StringBuilder line=new StringBuilder();for(int dx=-r;dx<=r;dx++){if(dx==0&&dz==0){line.append(player);continue;}line.append(minimapCell(dx,dz,centerY));}rows.add(line.toString());}return rows.toArray(new String[rows.size()]);}\n    private char facingChar(float yaw){double y=((yaw%360)+360)%360;int q=((int)Math.floor(y/90D+.5D))&3;return q==0?'v':q==1?'<':q==2?'^':'>'; }\n    private char minimapCell(int dx,int dz,int centerY){for(int dy=4;dy>=-5;dy--){BlockPos pos=new BlockPos(mc.thePlayer.posX+dx,centerY+dy,mc.thePlayer.posZ+dz);if(!mc.theWorld.isBlockLoaded(pos))return '?';Material material=mc.theWorld.getBlockState(pos).getBlock().getMaterial();if(material==Material.air)continue;if(material==Material.water)return '~';if(material==Material.lava)return '!';return '#';}return '.';}\n    private void sampleInventoryDelta(){Map<String,Integer> current=new TreeMap<String,Integer>();for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null){String key=s.getItem().getUnlocalizedName();Integer old=current.get(key);current.put(key,(old==null?0:old)+s.stackSize);}if(!previousInventory.isEmpty()){TreeSet<String> keys=new TreeSet<String>();keys.addAll(previousInventory.keySet());keys.addAll(current.keySet());List<String> changes=new ArrayList<String>();for(String key:keys){int before=previousInventory.containsKey(key)?previousInventory.get(key):0,after=current.containsKey(key)?current.get(key):0;if(after!=before)changes.add((after>before?"+":"")+(after-before)+" "+cleanItemKey(key));if(changes.size()>=3)break;}if(!changes.isEmpty())lastInventoryDelta=join(changes," / ");}previousInventory=current;}\n    private String join(List<String> values,String separator){StringBuilder out=new StringBuilder();for(String value:values){if(out.length()>0)out.append(separator);out.append(value);}return out.toString();}\n'''
runtime=runtime[:start]+new_map+runtime[end:]

old_quick='''if(enabled("lunar_quickplay")&&mod("lunar_quickplay").key()!=0&&key==mod("lunar_quickplay").key()){String mode=mod("lunar_quickplay").setting("mode").raw();String command="Bed Wars Doubles".equals(mode)?"/play bedwars_eight_two":"SkyWars Solo".equals(mode)?"/play solo_normal":"Duels".equals(mode)?"/play duels_classic_duel":"Lobby".equals(mode)?"/lobby":"/play bedwars_eight_one";mc.thePlayer.sendChatMessage(command);return;}'''
new_quick='''if(enabled("lunar_quickplay")&&mod("lunar_quickplay").key()!=0&&key==mod("lunar_quickplay").key()){mc.displayGuiScreen(new ForgeQuickplayScreen(client,mod("lunar_quickplay").setting("mode").raw()));return;}'''
if old_quick not in runtime:raise SystemExit('Could not locate old Quickplay key handler')
runtime=runtime.replace(old_quick,new_quick,1)
write(runtime_path,runtime)

# Sanity gates before Gradle gets involved.
for p in (ROOT/'src/main/java').rglob('*.java'):
    text=p.read_text(errors='ignore')
    if '0.3.0-alpha' in text or '0.3.0-ALPHA' in text:raise SystemExit('Stale 0.3 version in '+str(p.relative_to(ROOT)))
if 'new ForgeQuickplayScreen' not in read(runtime_path):raise SystemExit('Quickplay selector is not wired')
if '.hoverStart=' not in read(runtime_path):raise SystemExit('Item Physics is not using the 1.8.9 render phase')
if 'SkyblockScoreboardSnapshot.capture' not in read(runtime_path):raise SystemExit('Sidebar snapshot is not wired')
