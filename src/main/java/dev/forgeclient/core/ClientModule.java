package dev.forgeclient.core;

import java.util.*;

public final class ClientModule {
    public final String id,name,summary,description;
    public final Category category;
    public final boolean hud,holdBinding,defaultEnabled;
    public final HudPlacement placement;
    private final int defaultKey;
    private final LinkedHashMap<String,Setting> settings=new LinkedHashMap<>();
    private boolean enabled,favorite,available=true;
    private int key;
    private long revision;
    public ClientModule(String id,String name,Category category,String summary,String description,
                        boolean enabled,boolean hud,boolean holdBinding,int key,double x,double y) {
        this.id=Objects.requireNonNull(id);this.name=name;this.category=category;this.summary=summary;this.description=description;
        this.enabled=this.defaultEnabled=enabled;this.hud=hud;this.holdBinding=holdBinding;this.key=this.defaultKey=key;
        placement=new HudPlacement(x,y);placement.onChange(()->revision++);
    }
    public ClientModule add(Setting setting){
        if(settings.containsKey(setting.id))throw new IllegalArgumentException("Duplicate setting "+setting.id);
        settings.put(setting.id,setting);setting.onChange(()->revision++);return this;
    }
    public Collection<Setting> settings(){return Collections.unmodifiableCollection(settings.values());}
    public Setting setting(String id){Setting s=settings.get(id);if(s==null)throw new IllegalArgumentException("Unknown setting "+id);return s;}
    public boolean enabled(){return enabled;}
    public boolean available(){return available;}
    /** Marks a catalog-parity entry as visible but non-toggleable until its native handler exists. */
    public ClientModule unavailable(){available=false;enabled=false;return this;}
    public void enabled(boolean next){if(next&&!available)return;if(enabled!=next){enabled=next;revision++;}}
    public void toggle(){enabled(!enabled);}
    public boolean favorite(){return favorite;}
    public void favorite(boolean next){if(favorite!=next){favorite=next;revision++;}}
    public int key(){return key;}
    public boolean bind(int next){if(next<0||next>255||next==1||next==54)return false;if(key!=next){key=next;revision++;}return true;}
    public long revision(){return revision;}
    public void resetSettings(){for(Setting s:settings.values())s.reset();}
    public void reset(){enabled(defaultEnabled);favorite(false);bind(defaultKey);placement.reset();resetSettings();}
    public boolean matches(String query){String q=query.toLowerCase(Locale.ROOT);return (id+" "+name+" "+summary+" "+description+" "+category.title).toLowerCase(Locale.ROOT).contains(q);}
}
