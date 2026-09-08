package dev.forgeclient.core;

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
