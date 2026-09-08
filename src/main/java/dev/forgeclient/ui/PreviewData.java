package dev.forgeclient.ui;
import dev.forgeclient.core.*;
/** Explicitly synthetic telemetry, used only by UI previews and the offline tests. */
public final class PreviewData {
    private PreviewData(){}
    public static Telemetry create(){
        Telemetry t=new Telemetry();t.world=true;t.player="Forge Player";t.server="Preview world";t.fps=240;t.ping=28;t.leftCps=8;t.rightCps=2;
        t.keys[0]=true;t.keys[6]=true;
        String[][] values={{"fps","240 FPS"},{"cps","8 | 2 CPS"},{"ping","28 ms"},{"coordinates","X 128","Y 64","Z -256"},{"compass","NE / 225 deg"},{"potions","Speed II  1:24","Regeneration  0:18"},{"speed","5.61 blocks/s"},{"memory","512 / 2048 MB"},{"clock","19:42"},{"session","SESSION 00:24:12"},{"server","Preview world"},{"biome","Plains"},{"light","SKY 15 / BLOCK 0"},{"day_time","DAY 8 / 12:00"},{"hit_distance","LOCAL HIT 2.84 m"},{"inventory_counts","ARROWS 64 / PEARLS 8","BLOCKS 128"},{"pack_info","Default"},{"sprint_status","SPRINTING"},{"waypoint","Home / 120 m","AHEAD"},{"frame_graph","AVG 4.2 / P99 6.1 ms"}};
        for(String[] v:values){String[] rows=new String[v.length-1];System.arraycopy(v,1,rows,0,rows.length);t.put(v[0],rows);}
        t.armor=new Telemetry.Item[]{new Telemetry.Item("Helmet","90%",.9,null),new Telemetry.Item("Chestplate","84%",.84,null),new Telemetry.Item("Leggings","92%",.92,null),new Telemetry.Item("Boots","70%",.7,null)};
        t.held=new Telemetry.Item[]{new Telemetry.Item("Diamond sword","1384 / 1561",.89,null)};
        for(int i=0;i<100;i++)t.frames.add(4+Math.sin(i*.4)*1.5);return t;
    }
}
