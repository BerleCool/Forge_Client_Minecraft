package dev.forgeclient.core;

import java.util.*;

public final class ModuleRegistry {
    private final LinkedHashMap<String,ClientModule> modules=new LinkedHashMap<>();
    public void add(ClientModule module){if(modules.containsKey(module.id))throw new IllegalArgumentException("Duplicate module "+module.id);modules.put(module.id,module);}
    public List<ClientModule> all(){return Collections.unmodifiableList(new ArrayList<>(modules.values()));}
    public ClientModule get(String id){ClientModule m=modules.get(id);if(m==null)throw new IllegalArgumentException("Unknown module "+id);return m;}
    public boolean enabled(String id){ClientModule m=modules.get(id);return m!=null&&m.enabled();}
    public int enabledCount(){int n=0;for(ClientModule m:modules.values())if(m.enabled())n++;return n;}
    public long revision(){long n=0;for(ClientModule m:modules.values())n+=m.revision();return n;}
    public boolean isKeyUsed(int key,ClientModule except){if(key==0)return false;for(ClientModule m:modules.values())if(m!=except&&m.key()==key)return true;return false;}
    public void onKey(int key,boolean guiOpen){if(guiOpen||key==0)return;for(ClientModule m:modules.values())if(m.key()==key&&!m.holdBinding)m.toggle();}
    public void preset(String name){
        Set<String> enabled=new HashSet<>();
        switch(name){
            case "default":for(ClientModule m:modules.values())if(m.defaultEnabled)enabled.add(m.id);break;
            case "minimal":Collections.addAll(enabled,"fps","ping","zoom");break;
            case "pvp":Collections.addAll(enabled,"fps","cps","keystrokes","ping","armor","potions","zoom","smart_fps","hit_distance","sprint_status");break;
            case "explorer":Collections.addAll(enabled,"fps","ping","zoom","coordinates","compass","biome","light","day_time","waypoint","armor","smart_fps");break;
            default:throw new IllegalArgumentException("Unknown preset "+name);
        }
        for(ClientModule m:modules.values())m.enabled(enabled.contains(m.id));
    }
}
