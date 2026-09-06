package dev.forgeclient.core;
import java.util.*;
/** Main-thread snapshot. World objects never leave the client thread. */
public final class Telemetry {
    public static final class Item {
        public final String name,label;public final double fraction;public final Object stack;
        public Item(String name,String label,double fraction,Object stack){this.name=name;this.label=label;this.fraction=fraction;this.stack=stack;}
    }
    public boolean world,sprinting,toggleSprintEnabled,sprintToggled;
    public String player="",server="";
    public int fps,ping,leftCps,rightCps;
    public final boolean[] keys=new boolean[8];
    public final String[] keyNames={"W","A","S","D","SPACE","SHIFT"};
    public Item[] armor=new Item[0],held=new Item[0];
    public final SlidingClickCounter left=new SlidingClickCounter(),right=new SlidingClickCounter();
    public final FrameHistory frames=new FrameHistory();
    private final Map<String,String[]> values=new HashMap<>();
    public void put(String id,String... rows){values.put(id,rows.clone());}
    public String[] rows(String id){String[] rows=values.get(id);return rows==null?new String[0]:rows;}
    public void clearWorld(){world=sprinting=toggleSprintEnabled=sprintToggled=false;player=server="";fps=ping=leftCps=rightCps=0;Arrays.fill(keys,false);armor=new Item[0];held=new Item[0];values.clear();left.clear();right.clear();frames.clear();}
}
