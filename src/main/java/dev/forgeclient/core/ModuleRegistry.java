package dev.forgeclient.core;

import java.util.*;

/** Stable ordered registry. Read views are cached so the HUD does not allocate every frame. */
public final class ModuleRegistry {
    private final LinkedHashMap<String,ClientModule> modules=new LinkedHashMap<>();
    private final ArrayList<ClientModule> ordered=new ArrayList<>(),hudModules=new ArrayList<>();
    private final List<ClientModule> orderedView=Collections.unmodifiableList(ordered),hudView=Collections.unmodifiableList(hudModules);
    public void add(ClientModule module){
        if(modules.containsKey(module.id))throw new IllegalArgumentException("Duplicate module "+module.id);
        modules.put(module.id,module);ordered.add(module);if(module.hud)hudModules.add(module);
    }
    public List<ClientModule> all(){return orderedView;}
    public List<ClientModule> hud(){return hudView;}
    public ClientModule get(String id){ClientModule m=modules.get(id);if(m==null)throw new IllegalArgumentException("Unknown module "+id);return m;}
    public boolean enabled(String id){ClientModule m=modules.get(id);return m!=null&&m.enabled();}
    public int enabledCount(){int n=0;for(ClientModule m:ordered)if(m.enabled())n++;return n;}
    public int availableCount(){int n=0;for(ClientModule m:ordered)if(m.available())n++;return n;}
    public long revision(){long n=0;for(ClientModule m:ordered)n+=m.revision();return n;}
    public boolean isKeyUsed(int key,ClientModule except){if(key==0)return false;for(ClientModule m:ordered)if(m!=except&&m.key()==key)return true;return false;}
    public void onKey(int key,boolean guiOpen){if(guiOpen||key==0)return;for(ClientModule m:ordered)if(m.available()&&m.key()==key&&!m.holdBinding)m.toggle();}
    public void preset(String name){
        Set<String> enabled=new HashSet<>();
        switch(name){
            case "default":for(ClientModule m:ordered)if(m.defaultEnabled)enabled.add(m.id);break;
            case "minimal":Collections.addAll(enabled,"fps","ping","zoom");break;
            case "pvp":Collections.addAll(enabled,"fps","cps","keystrokes","ping","armor","potions","zoom","smart_fps","hit_distance","sprint_status");break;
            case "explorer":Collections.addAll(enabled,"fps","ping","zoom","coordinates","compass","biome","light","day_time","waypoint","armor","smart_fps");break;
            default:throw new IllegalArgumentException("Unknown preset "+name);
        }
        for(ClientModule m:ordered)m.enabled(enabled.contains(m.id));
    }
}
