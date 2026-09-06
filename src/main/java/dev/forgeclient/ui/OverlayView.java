package dev.forgeclient.ui;

import dev.forgeclient.core.Category;
import dev.forgeclient.core.ClientModule;
import dev.forgeclient.core.ModuleRegistry;
import dev.forgeclient.core.ProfileStore;
import dev.forgeclient.core.Setting;
import dev.forgeclient.core.Telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Actual application UI, shared by Minecraft and the desktop verification harness.
 * Luna-inspired hierarchy: icon rail -> navigation -> dense modules -> inspector.
 * Dawn-inspired treatment: pixel bevels, charcoal, inset controls and an orange seam.
 */
public final class OverlayView {
    public enum Tab { MODULES, HUD, PROFILES }
    public static final class Hit {
        public final String id;
        public final Rect bounds;
        private final Runnable left, right;
        private final Setting slider;
        private final Rect sliderTrack;
        private final ClientModule hud;
        Hit(String id,Rect bounds,Runnable left,Runnable right,Setting slider,Rect track,ClientModule hud) {
            this.id=id;this.bounds=bounds;this.left=left;this.right=right;this.slider=slider;this.sliderTrack=track;this.hud=hud;
        }
    }
    private final ModuleRegistry registry;
    private final Telemetry telemetry, sample=PreviewData.create();
    private final UiHost host;
    private final HudRenderer hudRenderer=new HudRenderer();
    private final List<Hit> hits=new ArrayList<>();
    private final List<ClientModule> filtered=new ArrayList<>();
    private long filterRevision=-1;
    private String filterCache="";
    private Rect clip=new Rect(0,0,0,0);
    private UiLayout layout;
    private Tab tab=Tab.MODULES;
    private String selection="keystrokes", filter="all", query="", profileName="", focus="";
    private ClientModule binding,draggingHud;
    private Setting draggingSlider;
    private Rect draggingTrack;
    private int dragOffsetX,dragOffsetY;
    private boolean searchFocused,profileFocused,snap=true;
    private int listScroll,inspectorScroll,profileScroll,listHeight,inspectorHeight,profileHeight;
    private int mouseX,mouseY;
    private String toast="";
    private long toastUntil;
    private String overwritePending="";
    private long overwriteUntil;
    private Canvas lastCanvas;
    private Rect moduleViewport=new Rect(0,0,0,0),profileViewport=new Rect(0,0,0,0);

    public OverlayView(ModuleRegistry registry,Telemetry telemetry,UiHost host) {
        this.registry=registry;this.telemetry=telemetry;this.host=host;
    }
    public Tab tab() { return tab; }
    public String selection() { return selection; }
    public String query() { return query; }
    public int scroll() { return listScroll; }
    public List<Hit> hitAreas() { return Collections.unmodifiableList(hits); }
    public boolean capturingBinding() { return binding!=null; }
    public void showMessage(String message) { toast=message;toastUntil=System.nanoTime()+4_000_000_000L; }
    public void setTab(Tab next) {
        tab=next;searchFocused=profileFocused=false;binding=null;draggingHud=null;draggingSlider=null;focus="";
    }
    public void draw(Canvas c,int width,int height,int mx,int my) {
        lastCanvas=c;layout=new UiLayout(width,height);mouseX=mx;mouseY=my;
        hits.clear();clip=new Rect(0,0,width,height);
        if(tab==Tab.HUD) {
            c.rect(0,0,width,height,0x38100F13);drawHudEditor(c);
        } else {
            c.rect(0,0,width,height,0xBC100F14);
            c.grain(0,52,width,height-76,16);
            // Sparse, deterministic pixel seams, not random per-frame noise or expensive blur.
            for(int y=58;y<height-24;y+=32) c.rect(0,y,width,1,0x082F2720);
            rail(c);
            if(tab==Tab.MODULES) { navigation(c);modules(c);inspector(c); }
            else profiles(c);
        }
        if(tab!=Tab.HUD)header(c);footer(c);
        for(Hit hit:hits) if(hit.id.equals(focus)) Theme.outline(c,hit.bounds,Theme.GOLD);
        if(System.nanoTime()<toastUntil && !toast.isEmpty()) {
            int w=Math.min(width-24,c.textWidth(toast)+24), x=(width-w)/2;
            Theme.panel(c,x,height-59,w,25,0xF5232025,true);
            c.text(Theme.truncate(c,toast,w-16),x+8,height-50,Theme.GOLD,false);
        }
    }
    private void header(Canvas c) {
        c.rect(0,0,layout.width,52,0xF0141317);c.rect(0,51,layout.width,1,0xFF463226);
        Theme.logo(c,14,15,2);
        c.push(48,13,1.75);c.text("FORGE",0,0,Theme.TEXT,true);c.pop();
        c.text("CLIENT / 1.8.9",49,34,Theme.MUTED,false);
        int x=169;
        for(Tab value:Tab.values()) {
            String title=value==Tab.MODULES?"MODULES":(value==Tab.HUD?"HUD EDITOR":"PROFILES");
            final Tab target=value;Rect r=new Rect(x,12,87,29);
            button(c,"tab:"+value,r,title,tab==value,()->setTab(target));x+=92;
        }
        if(layout.width>=960) {
            boolean privacy=registry.enabled("streamer_mode");
            String name=privacy&&registry.get("streamer_mode").setting("player").bool()?"PLAYER":telemetry.player;
            String server=privacy&&registry.get("streamer_mode").setting("server").bool()?"PRIVATE SESSION":telemetry.server;
            c.text(Theme.truncate(c,name,120),layout.width-338,16,Theme.TEXT,false);
            c.text(Theme.truncate(c,server,120),layout.width-338,31,Theme.MUTED,false);
        }
        if(layout.width>=800) {
            int sx=layout.width-200;
            c.text(telemetry.world?telemetry.fps+" FPS":"NO WORLD",sx,16,Theme.GOLD,false);
            c.text(telemetry.world?telemetry.ping+" ms  /  LOCAL CONFIG":"CLIENT CONFIGURATION",sx,31,Theme.MUTED,false);
        }
        button(c,"close",new Rect(layout.width-39,12,27,29),"X",false,host::closeScreen);
    }
    private void footer(Canvas c) {
        c.rect(0,layout.height-24,layout.width,24,0xF5121116);
        c.rect(0,layout.height-24,layout.width,1,0xFF312830);
        c.text("FORGE  /  0.1.0-ALPHA",12,layout.height-15,Theme.MUTED,false);
        if(layout.width>690) c.text("PROFILE: "+host.activeProfile().toUpperCase(Locale.ROOT),layout.width/2-60,layout.height-15,Theme.MUTED,false);
        String hint=tab==Tab.HUD?"DRAG / SCROLL TO SCALE":"RSHIFT  CLOSE   /   TAB  NAVIGATE";
        c.text(hint,layout.width-c.textWidth(hint)-12,layout.height-15,Theme.MUTED,false);
    }
    private void rail(Canvas c) {
        c.rect(0,52,layout.rail,layout.height-76,0xE51B181E);
        c.rect(layout.rail-1,52,1,layout.height-76,0xFF3C3036);
        String[] glyphs={"M","H","P"};Tab[] tabs=Tab.values();
        for(int i=0;i<3;i++) {
            final Tab target=tabs[i];Rect r=new Rect(7,66+i*45,34,33);
            button(c,"rail:"+target,r,glyphs[i],tab==target,()->setTab(target));
        }
        if(layout.nav==0 && tab==Tab.MODULES) {
            int y=218;
            String[] names={"all","favorites","enabled","HUD","VISUAL","PERFORMANCE","UTILITY"};
            String[] labels={"ALL","*","ON","HUD","EYE","FPS","KIT"};
            for(int i=0;i<names.length;i++) {
                final String f=names[i];button(c,"filter:"+f,new Rect(5,y,38,25),labels[i],filter.equals(f),()->selectFilter(f));y+=29;
                if(y>layout.height-53) break;
            }
        }
        if(layout.height>420) {
            c.rect(14,layout.height-57,3,3,Theme.ORANGE);c.rect(21,layout.height-52,3,3,0xFF95512E);
            c.rect(28,layout.height-59,3,3,0xFFD28A44);
        }
    }
    private void navigation(Canvas c) {
        if(layout.nav==0) return;
        int x=layout.rail,y=74,w=layout.nav;
        c.rect(x,52,w,layout.height-76,0xC218171C);c.rect(x+w-1,62,1,layout.height-96,0xFF302B31);
        c.text("WORKSPACE",x+14,y,Theme.DIM,false);y+=24;
        String[] names={"all","favorites","enabled"},labels={"All modules","Favorites","Enabled"};
        for(int i=0;i<names.length;i++) {
            final String f=names[i];String count=i==0?Integer.toString(registry.all().size()):(i==2?Integer.toString(registry.enabledCount()):Integer.toString(favoriteCount()));
            nav(c,"filter:"+f,x+7,y,w-14,labels[i],count,filter.equals(f),()->selectFilter(f));y+=32;
        }
        y+=17;c.text("CATEGORIES",x+14,y,Theme.DIM,false);y+=20;
        for(Category category:Category.values()) {
            final String f=category.name();
            String label=category==Category.HUD?"HUD":(category==Category.VISUAL?"Visual":(category==Category.PERFORMANCE?"Performance":"Utility"));
            nav(c,"filter:"+f,x+7,y,w-14,label,Integer.toString(categoryCount(category)),filter.equals(f),()->selectFilter(f));y+=32;
        }
        if(layout.height>=485) {
            Theme.panel(c,x+10,layout.height-99,w-20,55,0xDB211C21,false);
            c.text("BUILT FOR",x+19,layout.height-88,Theme.DIM,false);
            c.text("MINECRAFT 1.8.9",x+19,layout.height-74,Theme.TEXT,false);
            c.text("FORGE MOD LOADER",x+19,layout.height-60,Theme.MUTED,false);
        }
    }
    private int favoriteCount() { int n=0;for(ClientModule m:registry.all())if(m.favorite())n++;return n; }
    private int categoryCount(Category cat) { int n=0;for(ClientModule m:registry.all())if(m.category==cat)n++;return n; }
    private void selectFilter(String f) { filter=f;listScroll=0;searchFocused=false;focus=""; }
    private void nav(Canvas c,String id,int x,int y,int w,String text,String count,boolean active,Runnable action) {
        Rect r=new Rect(x,y,w,27);boolean hover=r.contains(mouseX,mouseY);
        if(active||hover) c.rect(x,y,w,27,active?0xFF38291F:0xFF28232A);
        if(active)c.rect(x,y+3,2,21,Theme.ORANGE);
        c.text(text,x+10,y+9,active?Theme.GOLD:Theme.MUTED,false);
        c.text(count,x+w-c.textWidth(count)-8,y+9,active?Theme.ORANGE:Theme.DIM,false);
        hit(id,r,action,null);
    }
    private List<ClientModule> filtered() {
        String cache=filter+"\n"+query;
        if(filterRevision==registry.revision() && filterCache.equals(cache))return filtered;
        filtered.clear();
        for(ClientModule m:registry.all()) {
            boolean match=filter.equals("all")||(filter.equals("favorites")&&m.favorite())||(filter.equals("enabled")&&m.enabled())||filter.equals(m.category.name());
            if(match&&m.matches(query))filtered.add(m);
        }
        filterCache=cache;filterRevision=registry.revision();return filtered;
    }
    private void modules(Canvas c) {
        Rect area=layout.content;
        c.text("MODULE LIBRARY",area.x+2,area.y+4,Theme.TEXT,true);
        c.text(registry.all().size()+" modules. Make this client yours.",area.x+2,area.y+20,Theme.MUTED,false);
        Rect search=new Rect(area.x,area.y+42,area.width,28);
        Theme.panel(c,search.x,search.y,search.width,search.height,0xDA121116,searchFocused);
        c.text(">",search.x+9,search.y+10,Theme.ORANGE,false);
        String value=query.isEmpty()&&!searchFocused?"Search modules...":query+(searchFocused?"_":"");
        c.text(Theme.truncate(c,value,search.width-43),search.x+24,search.y+10,query.isEmpty()&&!searchFocused?Theme.DIM:Theme.TEXT,false);
        hit("search",search,()->{searchFocused=true;profileFocused=false;focus="search";},null);
        if(!query.isEmpty())button(c,"clear-search",new Rect(search.right()-25,search.y+4,20,20),"X",false,()->{query="";listScroll=0;});
        moduleViewport=new Rect(area.x,area.y+82,area.width,area.height-82);
        beginClip(c,moduleViewport);
        List<ClientModule> visible=filtered();
        int columns=layout.columns(),gap=8,cardWidth=(area.width-5-(columns-1)*gap)/columns;
        int y=moduleViewport.y-listScroll,start=y;
        for(Category category:Category.values()) {
            List<ClientModule> group=new ArrayList<>();for(ClientModule m:visible)if(m.category==category)group.add(m);
            if(group.isEmpty())continue;
            c.rect(area.x,y+4,3,8,Theme.ORANGE);
            c.text(category.title.toUpperCase(Locale.ROOT),area.x+10,y+4,Theme.MUTED,false);
            String num=Integer.toString(group.size());c.text(num,area.right()-c.textWidth(num)-9,y+4,Theme.DIM,false);y+=22;
            for(int i=0;i<group.size();i++) {
                ClientModule module=group.get(i);int row=i/columns,col=i%columns;
                card(c,module,new Rect(area.x+col*(cardWidth+gap),y+row*56,cardWidth,49));
            }
            y+=((group.size()+columns-1)/columns)*56+13;
        }
        listHeight=y-start;
        if(visible.isEmpty()) {
            c.text("NO MATCHING MODULES",area.x+12,moduleViewport.y+28,Theme.GOLD,false);
            Theme.wrapped(c,"Try another search or choose All modules.",area.x+12,moduleViewport.y+49,area.width-24,Theme.MUTED,3);
        }
        endClip(c);
        listScroll=clampScroll(listScroll,listHeight,moduleViewport.height);
        Theme.scrollbar(c,moduleViewport,listHeight,listScroll);
    }
    private void card(Canvas c,ClientModule m,Rect r) {
        if(r.bottom()<moduleViewport.y||r.y>moduleViewport.bottom())return;
        boolean active=selection.equals(m.id),hover=r.contains(mouseX,mouseY);
        Theme.panel(c,r.x,r.y,r.width,r.height,active?0xF3302722:(hover?0xE32D282E:0xCE211E25),active);
        hit("module:"+m.id,r,()->select(m),m.available()?m::toggle:null);
        c.rect(r.x+9,r.y+11,3,3,m.enabled()?Theme.ORANGE:Theme.DIM);
        c.text(Theme.truncate(c,m.name,r.width-63),r.x+18,r.y+9,active?Theme.GOLD:Theme.TEXT,false);
        c.text(Theme.truncate(c,m.summary,r.width-18),r.x+9,r.y+27,Theme.MUTED,false);
        Rect toggle=new Rect(r.right()-34,r.y+7,27,14);
        if(m.available()){Theme.toggle(c,toggle.x,toggle.y,m.enabled(),toggle.contains(mouseX,mouseY));hit("toggle:"+m.id,toggle,m::toggle,null);}
        else{c.rect(toggle.x,toggle.y,toggle.width,toggle.height,0xFF211D23);c.text("PORT",toggle.x+3,toggle.y+3,Theme.DIM,false);}
        if(m.favorite())c.text("*",r.right()-13,r.y+29,Theme.GOLD,false);
    }
    private void select(ClientModule m) { selection=m.id;inspectorScroll=0;binding=null;searchFocused=false;focus=""; }
    private void inspector(Canvas c) {
        ClientModule m=registry.get(selection);Rect area=layout.inspector;
        Theme.panel(c,area.x,area.y,area.width,area.height,0xE51B181D,false);
        c.rect(area.x,area.y,area.width,2,Theme.ORANGE);
        Rect viewport=new Rect(area.x+3,area.y+4,area.width-6,area.height-8);
        beginClip(c,viewport);
        int x=area.x+13,w=area.width-26,y=area.y+16-inspectorScroll,start=y;
        Theme.rune(c,m.name.substring(0,1).toUpperCase(Locale.ROOT),x,y,30,Theme.ORANGE);
        c.text(Theme.truncate(c,m.name,w-45),x+40,y+2,Theme.TEXT,true);
        c.text(m.category.name(),x+40,y+17,Theme.DIM,false);y+=42;
        int descLines=Math.min(4,Theme.wrap(c,m.description,w).size());
        Theme.wrapped(c,m.description,x,y,w,Theme.MUTED,4);y+=descLines*12+12;
        Theme.panel(c,x,y,w,29,0xFF242128,false);
        c.text(!m.available()?"PORTING":(m.enabled()?"ENABLED":"DISABLED"),x+9,y+10,m.enabled()?Theme.GOLD:Theme.MUTED,false);
        if(m.available()){Theme.toggle(c,x+w-35,y+7,m.enabled(),false);hit("inspector-toggle",new Rect(x,y,w,29),m::toggle,null);}
        else c.text("COMING SOON",x+w-83,y+10,Theme.DIM,false);y+=38;
        button(c,"favorite",new Rect(x,y,52,22),m.favorite()?"* PIN":"PIN",m.favorite(),()->m.favorite(!m.favorite()));
        String bindLabel=binding==m?"PRESS KEY":(m.holdBinding?"HOLD ":"KEY ")+host.keyName(m.key());
        button(c,"bind",new Rect(x+58,y,w-58,22),bindLabel,binding==m,()->{binding=m;searchFocused=false;focus="";});y+=32;
        if(m.hud) {
            c.text("LAYOUT PREVIEW",x,y,Theme.DIM,false);y+=15;
            int ph="keystrokes".equals(m.id)?91:64;
            Theme.panel(c,x,y,w,ph,0xFF131216,false);
            for(int gy=y+5;gy<y+ph-3;gy+=12)for(int gx=x+5;gx<x+w-3;gx+=12)c.rect(gx,gy,1,1,0xFF2A242B);
            HudRenderer.Size s=hudRenderer.measure(c,m,sample);
            double scale=Math.min(.85,Math.min((w-16)/(double)Math.max(1,s.width),(ph-10)/(double)Math.max(1,s.height)));
            hudRenderer.drawAt(c,m,sample,x+(w-(int)(s.width*scale))/2,y+(ph-(int)(s.height*scale))/2,scale);y+=ph+16;
        }
        c.text("CUSTOMIZE",x,y,Theme.DIM,false);y+=19;
        for(Setting setting:m.settings()) {
            if(setting.kind==Setting.Kind.BOOLEAN) {
                c.text(Theme.truncate(c,setting.name,w-40),x,y+4,Theme.TEXT,false);
                Theme.toggle(c,x+w-28,y,setting.bool(),false);
                hit("setting:"+setting.id,new Rect(x,y,w,19),setting::toggle,null);y+=28;
            } else if(setting.kind==Setting.Kind.NUMBER) {
                String number=setting.display();c.text(Theme.truncate(c,setting.name,w-c.textWidth(number)-12),x,y,Theme.TEXT,false);
                c.text(number,x+w-c.textWidth(number),y,Theme.ORANGE,false);
                Rect track=new Rect(x,y+19,w,4);
                c.rect(track.x,track.y,track.width,track.height,0xFF0D0C10);
                double ratio=(setting.number()-setting.min)/(setting.max-setting.min);
                int length=(int)Math.round((w-5)*ratio);
                c.rect(x,track.y+1,length,2,0xFFA65F32);c.rect(x+length,track.y-3,5,10,Theme.ORANGE);c.rect(x+length,track.y-3,5,1,Theme.GOLD);
                slider("setting:"+setting.id,new Rect(x,y+12,w,17),setting,track);y+=37;
            } else {
                c.text(setting.name,x,y,Theme.TEXT,false);y+=14;
                button(c,"setting:"+setting.id,new Rect(x,y,w,23),setting.raw()+"  >",false,()->setting.cycle(1));y+=32;
            }
        }
        y+=9;button(c,"reset-settings",new Rect(x,y,w,25),"RESET THIS MODULE",false,m::resetSettings);y+=36;
        inspectorHeight=y-start+20;
        endClip(c);inspectorScroll=clampScroll(inspectorScroll,inspectorHeight,viewport.height);
        Theme.scrollbar(c,viewport,inspectorHeight,inspectorScroll);
    }
    private void profiles(Canvas c) {
        int x=layout.rail+20,w=layout.width-x-24,y=73;
        c.text("YOUR LOADOUTS",x,y,Theme.TEXT,true);
        c.text("Local profiles. No accounts. No cloud dependency.",x,y+20,Theme.MUTED,false);y+=50;
        int gap=12,cw=(w-gap*2)/3;
        String[] preset={"minimal","pvp","explorer"};
        String[] text={"Just the essentials.","Your match-day HUD.","Know your world."};
        for(int i=0;i<3;i++) {
            final String name=preset[i];int bx=x+i*(cw+gap);
            Theme.panel(c,bx,y,cw,79,0xDF252026,false);
            c.text(name.toUpperCase(Locale.ROOT),bx+12,y+12,Theme.GOLD,false);
            c.text(Theme.truncate(c,text[i],cw-24),bx+12,y+30,Theme.MUTED,false);
            button(c,"preset:"+name,new Rect(bx+10,y+48,cw-20,22),"APPLY PRESET",false,()->{registry.preset(name);showMessage("Preset applied; save it as a named profile below.");});
        }
        y+=102;
        Rect field=new Rect(x,y,Math.max(125,w-135),28);
        Theme.panel(c,field.x,field.y,field.width,field.height,0xDC151219,profileFocused);
        String label=profileName.isEmpty()&&!profileFocused?"new-profile-name":profileName+(profileFocused?"_":"");
        c.text(Theme.truncate(c,label,field.width-18),x+9,y+10,profileName.isEmpty()?Theme.DIM:Theme.TEXT,false);
        hit("profile-name",field,()->{profileFocused=true;searchFocused=false;focus="profile-name";},null);
        boolean confirm=profileName.equals(overwritePending)&&System.nanoTime()<overwriteUntil;
        button(c,"save-profile",new Rect(x+w-125,y,125,28),confirm?"CONFIRM OVERWRITE":"SAVE AS PROFILE",false,this::saveProfile);y+=46;
        c.text("SAVED PROFILES",x,y,Theme.DIM,false);y+=20;
        Rect viewport=new Rect(x,y,w,layout.height-39-y);
        beginClip(c,viewport);int yy=y-profileScroll;
        for(String name:host.profiles()) {
            Theme.panel(c,x,yy,w,37,0xD5242027,name.equals(host.activeProfile()));
            c.text(Theme.truncate(c,name,w-130),x+12,yy+14,Theme.TEXT,false);
            button(c,"load-profile:"+name,new Rect(x+w-95,yy+7,85,23),"LOAD",false,()->{host.loadProfile(name);inspectorScroll=listScroll=0;});yy+=44;
        }
        profileHeight=yy-(y-profileScroll);
        if(host.profiles().isEmpty())c.text("No named profiles yet. Save one above.",x+10,y+14,Theme.MUTED,false);
        endClip(c);profileScroll=clampScroll(profileScroll,profileHeight,viewport.height);
        profileViewport=viewport;Theme.scrollbar(c,viewport,profileHeight,profileScroll);
    }
    private void saveProfile() {
        if(!ProfileStore.validName(profileName)) { showMessage("Use 1-32 lowercase letters, numbers, _ or -.");return; }
        long now=System.nanoTime();
        if(host.profiles().contains(profileName)&&(!overwritePending.equals(profileName)||now>overwriteUntil)) {
            overwritePending=profileName;overwriteUntil=now+5_000_000_000L;showMessage("This profile exists. Press CONFIRM OVERWRITE to replace it.");return;
        }
        host.saveProfile(profileName);overwritePending="";profileFocused=false;
    }
    private void drawHudEditor(Canvas c) {
        Telemetry data=telemetry.world?telemetry:sample;
        for(int x=0;x<layout.width;x+=16)c.rect(x,0,1,layout.height-24,0x173C3A40);
        for(int y=0;y<layout.height-24;y+=16)c.rect(0,y,layout.width,1,0x173C3A40);
        c.rect(layout.width/2,0,1,layout.height-24,0x506E5140);c.rect(0,layout.height/2,layout.width,1,0x506E5140);
        hudRenderer.render(c,registry,data,layout.width,layout.height);
        for(ClientModule m:registry.all()) if(m.hud&&m.enabled()&&!hudRenderer.hidden(registry,m)) {
            Rect bounds=hudRenderer.bounds(c,m,data,layout.width,layout.height);
            if(bounds.empty())continue;
            boolean selected=selection.equals(m.id),hover=bounds.contains(mouseX,mouseY);
            if(selected||hover) {
                Theme.outline(c,bounds,selected?Theme.ORANGE:Theme.MUTED);
                int labelY=bounds.y>68?bounds.y-12:bounds.bottom()+4;
                c.text(m.name,bounds.x,labelY,Theme.GOLD,true);
            }
            Rect hit=bounds.intersect(new Rect(0,0,layout.width,layout.height-24));
            if(!hit.empty())hits.add(new Hit("hud:"+m.id,hit,()->selection=m.id,null,null,null,m));
        }
        int y=layout.height-60;
        button(c,"hud-snap",new Rect(12,y,98,25),snap?"SNAP: ON":"SNAP: OFF",snap,()->snap=!snap);
        button(c,"hud-reset",new Rect(118,y,119,25),"RESET POSITIONS",false,()->{for(ClientModule m:registry.all())if(m.hud)m.placement.reset();});
        button(c,"hud-done",new Rect(layout.width-90,y,78,25),"DONE",true,()->setTab(Tab.MODULES));
        if(!telemetry.world)c.text("SAMPLE DATA - JOIN A WORLD FOR LIVE VALUES",layout.width/2-126,66,Theme.GOLD,true);
    }
    private void button(Canvas c,String id,Rect r,String text,boolean active,Runnable action) {
        boolean hover=r.contains(mouseX,mouseY);
        Theme.panel(c,r.x,r.y,r.width,r.height,active?0xF4432C20:(hover?0xF0342D33:0xE9252229),active||hover);
        String label=Theme.truncate(c,text,r.width-10);
        c.text(label,r.x+(r.width-c.textWidth(label))/2,r.y+(r.height-8)/2,active?Theme.GOLD:(hover?Theme.TEXT:Theme.MUTED),false);
        hit(id,r,action,null);
    }
    private void hit(String id,Rect r,Runnable left,Runnable right) {
        Rect visible=r.intersect(clip);if(!visible.empty())hits.add(new Hit(id,visible,left,right,null,null,null));
    }
    private void slider(String id,Rect r,Setting s,Rect track) {
        Rect visible=r.intersect(clip);if(!visible.empty())hits.add(new Hit(id,visible,null,null,s,track,null));
    }
    private void beginClip(Canvas c,Rect r) { clip=r.intersect(new Rect(0,0,layout.width,layout.height));c.clip(clip); }
    private void endClip(Canvas c) { c.unclip();clip=new Rect(0,0,layout.width,layout.height); }
    private static int clampScroll(int scroll,int content,int height) { return Math.max(0,Math.min(scroll,Math.max(0,content-height))); }

    public boolean mouseDown(int x,int y,int button) {
        searchFocused=profileFocused=false;
        for(int i=hits.size()-1;i>=0;i--) {
            Hit hit=hits.get(i);if(!hit.bounds.contains(x,y))continue;
            focus=hit.id;
            if(button==0 && hit.hud!=null) {
                selection=hit.hud.id;draggingHud=hit.hud;
                Telemetry data=telemetry.world?telemetry:sample;
                Rect actual=hudRenderer.bounds(lastCanvas,draggingHud,data,layout.width,layout.height);
                dragOffsetX=x-actual.x;dragOffsetY=y-actual.y;return true;
            }
            if(button==0 && hit.slider!=null) {
                draggingSlider=hit.slider;draggingTrack=hit.sliderTrack;drag(x,y,false);return true;
            }
            Runnable action=button==1?hit.right:hit.left;
            if(action!=null) {action.run();return true;}
            return false;
        }
        focus="";return false;
    }
    public void drag(int x,int y,boolean shift) {
        if(draggingSlider!=null)draggingSlider.fraction((x-draggingTrack.x)/(double)Math.max(1,draggingTrack.width-5));
        if(draggingHud!=null&&layout!=null&&lastCanvas!=null) {
            Telemetry data=telemetry.world?telemetry:sample;
            Rect actual=hudRenderer.bounds(lastCanvas,draggingHud,data,layout.width,layout.height);
            int nx=x-dragOffsetX,ny=y-dragOffsetY;
            if(snap&&!shift) { nx=Math.round(nx/4f)*4;ny=Math.round(ny/4f)*4; }
            draggingHud.placement.move(nx,ny,layout.width,layout.height,actual.width,actual.height);
        }
    }
    public void mouseUp() { draggingHud=null;draggingSlider=null;draggingTrack=null; }
    public void wheel(int notches,int x,int y) {
        if(layout==null||notches==0)return;
        if(tab==Tab.HUD) {
            for(int i=hits.size()-1;i>=0;i--) {
                Hit h=hits.get(i);if(h.hud!=null&&h.bounds.contains(x,y)) {
                    Setting s=h.hud.setting("scale");s.set(Double.toString(s.number()+Integer.signum(notches)*.05));selection=h.hud.id;return;
                }
            }
        } else if(tab==Tab.PROFILES) profileScroll=clampScroll(profileScroll-notches*25,profileHeight,Math.max(1,profileViewport.height));
        else if(layout.inspector.contains(x,y))inspectorScroll=clampScroll(inspectorScroll-notches*27,inspectorHeight,layout.inspector.height-8);
        else if(layout.content.contains(x,y))listScroll=clampScroll(listScroll-notches*28,listHeight,moduleViewport.height);
    }
    /** Keyboard codes are LWJGL 2's 1.8.9 codes, translated by the desktop preview. */
    public boolean key(char character,int code,boolean ctrl,boolean shift) {
        if(binding!=null) {
            if(code==0)return true;
            if(code==1){binding=null;return true;}
            int key=(code==14||code==211)?0:code;
            if(!host.keyAvailable(key)){showMessage("That key opens Forge Client and is reserved.");return true;}
            if(registry.isKeyUsed(key,binding)){showMessage("That key is already used by another module.");return true;}
            if(!binding.bind(key)){showMessage("Escape and Right Shift are reserved.");return true;}
            binding=null;showMessage("Key binding updated.");return true;
        }
        if(code==54)return false;
        if(code==15) {
            searchFocused=profileFocused=false;
            if(hits.isEmpty())return true;
            int index=-1;for(int i=0;i<hits.size();i++)if(hits.get(i).id.equals(focus))index=i;
            int next=Math.floorMod(index+(shift?-1:1),hits.size());focus=hits.get(next).id;return true;
        }
        if(code==1) {
            if(searchFocused||profileFocused){searchFocused=profileFocused=false;focus="";return true;}
            if(tab!=Tab.MODULES){setTab(Tab.MODULES);return true;}
            return false;
        }
        if(searchFocused||profileFocused) {
            if(ctrl&&code==30){if(searchFocused)query="";else profileName="";listScroll=0;return true;}
            if(code==14){if(searchFocused&&!query.isEmpty())query=query.substring(0,query.length()-1);if(profileFocused&&!profileName.isEmpty())profileName=profileName.substring(0,profileName.length()-1);listScroll=0;return true;}
            if(code==28){if(profileFocused)saveProfile();else if(!filtered().isEmpty())select(filtered().get(0));return true;}
            if(character>=32&&character!=127) {
                if(searchFocused&&query.length()<64){query+=character;listScroll=0;}
                if(profileFocused&&profileName.length()<32&&((character>='a'&&character<='z')||(character>='0'&&character<='9')||character=='_'||character=='-'))profileName+=character;
            }
            return true;
        }
        if(code==203||code==205)for(Hit hit:hits)if(hit.id.equals(focus)&&hit.slider!=null){
            hit.slider.set(Double.toString(hit.slider.number()+(code==205?1:-1)*hit.slider.step));return true;
        }
        if(code==28||code==57)for(Hit hit:hits)if(hit.id.equals(focus)&&hit.left!=null){hit.left.run();return true;}
        if(tab==Tab.HUD&&(code==203||code==205||code==200||code==208)&&lastCanvas!=null) {
            ClientModule m=registry.get(selection);if(!m.hud)return true;
            Rect bounds=hudRenderer.bounds(lastCanvas,m,telemetry.world?telemetry:sample,layout.width,layout.height);
            int step=shift?10:1,dx=code==203?-step:(code==205?step:0),dy=code==200?-step:(code==208?step:0);
            m.placement.move(bounds.x+dx,bounds.y+dy,layout.width,layout.height,bounds.width,bounds.height);return true;
        }
        if(code==53&&tab==Tab.MODULES){searchFocused=true;focus="search";return true;}
        return false;
    }
}
