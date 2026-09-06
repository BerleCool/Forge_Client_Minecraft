#!/usr/bin/env python3
"""One-time/idempotent Forge Client 0.2 upgrade applied by CI before compilation."""
from __future__ import print_function
import base64, hashlib, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def path(rel): return ROOT / rel
def read(rel): return path(rel).read_text(encoding='utf-8')
def write(rel,text):
    f=path(rel); f.parent.mkdir(parents=True,exist_ok=True); f.write_text(text,encoding='utf-8')
def replace(rel,old,new,count=1):
    text=read(rel)
    if new in text and old not in text: return False
    found=text.count(old)
    if found!=count: raise RuntimeError('%s: expected %d occurrence(s), found %d for %r' % (rel,count,found,old[:120]))
    write(rel,text.replace(old,new,count)); return True
def regex_replace(rel,pattern,replacement,count=1,flags=0):
    text=read(rel); new,n=re.subn(pattern,replacement,text,count=count,flags=flags)
    if n!=count: raise RuntimeError('%s: regex expected %d replacement(s), got %d' % (rel,count,n))
    write(rel,new); return True
def reconstruct_asset(stem,out_rel,expected_sha):
    parts=sorted(path('build-support/assets').glob(stem+'.b64.part*')); out=path(out_rel)
    if parts:
        raw=base64.b64decode(''.join(x.read_text(encoding='ascii') for x in parts))
        actual=hashlib.sha256(raw).hexdigest()
        if actual!=expected_sha: raise RuntimeError('%s checksum mismatch: %s' % (stem,actual))
        out.parent.mkdir(parents=True,exist_ok=True); out.write_bytes(raw)
        for x in parts: x.unlink()
    if not out.exists(): raise RuntimeError('Missing reconstructed asset '+out_rel)
    if hashlib.sha256(out.read_bytes()).hexdigest()!=expected_sha: raise RuntimeError(out_rel+' checksum mismatch after reconstruction')

if path('src/main/java/dev/forgeclient/core/LunarParity.java').exists() and 'VERSION="0.2.0-alpha"' in read('src/main/java/dev/forgeclient/minecraft/ForgeClient.java'):
    reconstruct_asset('forge-logo','src/main/resources/assets/forgeclient/textures/gui/logo.jpg','0afedab5a03d9c0af1c5d7708bff52465ebe338fec0932ae20743abb09559c70')
    reconstruct_asset('forge-title','src/main/resources/assets/forgeclient/textures/gui/title.jpg','033e56fe9c860dd712c3a1db19e69e43021790d6550290b13c9379c3035380a5')
    print('Forge Client 0.2 source migration already applied; assets verified.'); raise SystemExit(0)

reconstruct_asset('forge-logo','src/main/resources/assets/forgeclient/textures/gui/logo.jpg','0afedab5a03d9c0af1c5d7708bff52465ebe338fec0932ae20743abb09559c70')
reconstruct_asset('forge-title','src/main/resources/assets/forgeclient/textures/gui/title.jpg','033e56fe9c860dd712c3a1db19e69e43021790d6550290b13c9379c3035380a5')

replace('src/main/java/dev/forgeclient/core/ClientModule.java',
'''    private boolean enabled,favorite;\n    private int key;''',
'''    private boolean enabled,favorite,available=true;\n    private int key;''')
replace('src/main/java/dev/forgeclient/core/ClientModule.java',
'''    public boolean enabled(){return enabled;}\n    public void enabled(boolean next){if(enabled!=next){enabled=next;revision++;}}\n    public void toggle(){enabled(!enabled);}''',
'''    public boolean enabled(){return enabled;}\n    public boolean available(){return available;}\n    /** Marks a catalog-parity entry as visible but non-toggleable until its native handler exists. */\n    public ClientModule unavailable(){available=false;enabled=false;return this;}\n    public void enabled(boolean next){if(next&&!available)return;if(enabled!=next){enabled=next;revision++;}}\n    public void toggle(){enabled(!enabled);}''')

write('src/main/java/dev/forgeclient/core/ModuleRegistry.java', '''package dev.forgeclient.core;

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
''')

write('src/main/java/dev/forgeclient/core/LunarParity.java', '''package dev.forgeclient.core;

import java.util.*;

/** Current Lunar module catalog baseline sourced from the MIT-licensed LunarClient/Apollo list. */
public final class LunarParity {
    private LunarParity(){}
    public static final String[] REQUIRED_NAMES={
        "Replay Mod","1.7 Visuals","FPS","CPS","Toggle Sneak/Sprint","Zoom","Hypixel Mods","Hypixel Bedwars","Quickplay","Armor Status","Keystrokes","Coordinates","Day Counter","Crosshair","Attack Indicator","Potion Effects","Direction HUD","Waypoints","Hit Color","Scoreboard","Titles","Item Counter","Potion Counter","Ping","Motion Blur","Pack Organizer","Chat","Tab Editor","Nametag","Shulker Preview","Scrollable Tooltips","Particle Changer","Nick Hider","Cooldowns","WorldEdit CUI","Clock","Stopwatch","Playtime","Memory","Combo Counter","Reach Display","Time Changer","Server Address","Saturation","Color Saturation","Item Physics","TNT Countdown","Item Tracker","Shiny Pots","3D Skins","Glint Colorizer","Momentum","Block Outline","Screenshot","FOV","Fog","Auto Text Hotkey","Auto Text Actions","Mumble Link","Totem Counter","2D Items","Boss Bar","Freelook","PvP Info","Markers","Snaplook","Team View","Pack Display","Menu Blur","Minimap","Hitbox","Lighting","Weather Changer","Chunk Borders","Sound Changer","WAILA","Hurt Cam","Tier Tagger","Damage Tint","Mob Size","SkyBlock","Item Customizer","Horse Stats","Overlay Mod","Rewind","Audio Subtitles","Action Bar","Shields","Light Overlay","Kill Sounds","Inventory Mod","F3 Display","GUI Scale","Knockback Trainer","Radio","UHC Overlay","NotEnoughUpdates","SkyBlockAddons"
    };
    private static final Set<String> HUD=new HashSet<>(Arrays.asList("fps","cps","armorstatus","keystrokes","coordinates","daycounter","attackindicator","potioneffects","directionhud","scoreboard","itemcounter","potioncounter","ping","clock","stopwatch","playtime","memory","combocounter","reachdisplay","serveraddress","totemcounter","pvpinfo","teamview","packdisplay","minimap","knockbacktrainer"));
    private static final Set<String> VISUAL=new HashSet<>(Arrays.asList("17visuals","crosshair","hitcolor","motionblur","nametag","shulkerpreview","particlechanger","colorsaturation","itemphysics","shinypots","3dskins","glintcolorizer","blockoutline","fov","fog","2ditems","bossbar","freelook","snaplook","menublur","hitbox","lighting","weatherchanger","chunkborders","hurtcam","damagetint","mobsize","shields","lightoverlay"));
    public static void addMissing(ModuleRegistry registry){
        Set<String> existing=new HashSet<>();for(ClientModule m:registry.all())existing.add(normalize(m.name));
        for(String name:REQUIRED_NAMES){
            String normalized=normalize(name);if(existing.contains(normalized))continue;
            Category category=HUD.contains(normalized)?Category.HUD:VISUAL.contains(normalized)?Category.VISUAL:Category.UTILITY;
            ClientModule target=new ClientModule("lunar_"+slug(name),name,category,"PORTING - Lunar parity baseline","Catalog parity with Lunar Client's current public Apollo module list. Native Forge 1.8.9 behavior is still being implemented independently; this entry cannot be enabled yet.",false,false,false,0,0,0).unavailable();
            registry.add(target);existing.add(normalized);
        }
    }
    public static boolean covers(ModuleRegistry registry,String required){String n=normalize(required);for(ClientModule m:registry.all())if(normalize(m.name).equals(n))return true;return false;}
    public static int unavailableCount(ModuleRegistry registry){int n=0;for(ClientModule m:registry.all())if(!m.available())n++;return n;}
    private static String normalize(String value){return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static String slug(String value){String s=value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");return s.isEmpty()?"module":s;}
}
''')

for old,new in [
('"Compass","Find your heading"','"Direction HUD","Find your heading"'),('"Session time","Time in this world"','"Playtime","Time in this world"'),('"Server info","Where you are playing"','"Server Address","Where you are playing"'),('"World clock","Time under the Minecraft sun"','"Day Counter","Time under the Minecraft sun"'),('"Hit distance","A local hit-distance readout"','"Reach Display","A local hit-distance readout"'),('"Supplies","Arrows, pearls and blocks"','"Item Counter","Arrows, pearls and blocks"'),('"Resource packs","Your active pack"','"Pack Display","Your active pack"'),('"Brightness",Category.VISUAL','"Lighting",Category.VISUAL'),('"Stable FOV",Category.VISUAL','"FOV",Category.VISUAL'),('"Reduced particles",Category.PERFORMANCE','"Particle Changer",Category.PERFORMANCE'),('"Toggle sprint",Category.UTILITY','"Toggle Sneak/Sprint",Category.UTILITY'),('"Waypoint","A place worth remembering"','"Waypoints","A place worth remembering"'),('"Local clock","Real-world time"','"Clock","Real-world time"')]:
    replace('src/main/java/dev/forgeclient/core/ModuleCatalog.java',old,new)
replace('src/main/java/dev/forgeclient/core/ModuleCatalog.java','        return r;\n    }','        LunarParity.addMissing(r);\n        return r;\n    }')

replace('src/main/java/dev/forgeclient/core/Telemetry.java','    public boolean world,sprinting;','    public boolean world,sprinting,toggleSprintEnabled,sprintToggled;')
replace('src/main/java/dev/forgeclient/core/Telemetry.java','    public void clearWorld(){world=sprinting=false;player=server="";fps=ping=leftCps=rightCps=0;Arrays.fill(keys,false);armor=new Item[0];held=new Item[0];values.clear();left.clear();right.clear();frames.clear();}','    public void clearWorld(){world=sprinting=toggleSprintEnabled=sprintToggled=false;player=server="";fps=ping=leftCps=rightCps=0;Arrays.fill(keys,false);armor=new Item[0];held=new Item[0];values.clear();left.clear();right.clear();frames.clear();}')

replace('src/main/java/dev/forgeclient/minecraft/ClientEvents.java','    private boolean sprintIntent,ownsSprint;','    private boolean sprintIntent,ownsSprint;private int sprintResumeTicks;')
replace('src/main/java/dev/forgeclient/minecraft/ClientEvents.java','''    private void sprint() {
        if(!play()||!Display.isActive()||!enabled("toggle_sprint"))sprintIntent=false;
        if(mc.thePlayer==null){ownsSprint=false;return;}
        boolean allowed=ClientPolicies.maySprint(Display.isActive(),mc.currentScreen!=null,mc.theWorld!=null,
            mc.thePlayer.movementInput.moveForward,mc.thePlayer.isSneaking(),mc.thePlayer.isUsingItem(),
            mc.thePlayer.isCollidedHorizontally,mc.thePlayer.isPotionActive(Potion.blindness),
            mc.thePlayer.getFoodStats().getFoodLevel(),mc.thePlayer.capabilities.allowFlying);
        if(sprintIntent&&allowed){if(!mc.thePlayer.isSprinting()){mc.thePlayer.setSprinting(true);ownsSprint=true;}}
        else if(ownsSprint){if(!mc.gameSettings.keyBindSprint.isKeyDown())mc.thePlayer.setSprinting(false);ownsSprint=false;}
    }''','''    private void sprint() {
        boolean module=enabled("toggle_sprint");client.telemetry.toggleSprintEnabled=module;
        if(!module){sprintIntent=false;sprintResumeTicks=0;if(mc.thePlayer!=null&&ownsSprint&&!mc.gameSettings.keyBindSprint.isKeyDown())mc.thePlayer.setSprinting(false);ownsSprint=false;client.telemetry.sprintToggled=false;return;}
        if(mc.theWorld==null||mc.thePlayer==null){sprintIntent=false;sprintResumeTicks=0;ownsSprint=false;client.telemetry.sprintToggled=false;return;}
        client.telemetry.sprintToggled=sprintIntent;
        if(!Display.isActive()||mc.currentScreen!=null){if(ownsSprint&&!mc.gameSettings.keyBindSprint.isKeyDown())mc.thePlayer.setSprinting(false);ownsSprint=false;return;}
        boolean allowed=ClientPolicies.maySprint(true,false,true,mc.thePlayer.movementInput.moveForward,mc.thePlayer.isSneaking(),mc.thePlayer.isUsingItem(),mc.thePlayer.isCollidedHorizontally,mc.thePlayer.isPotionActive(Potion.blindness),mc.thePlayer.getFoodStats().getFoodLevel(),mc.thePlayer.capabilities.allowFlying);
        if(sprintResumeTicks>0)sprintResumeTicks--;
        if(sprintIntent&&allowed){if(!mc.thePlayer.isSprinting())mc.thePlayer.setSprinting(true);ownsSprint=true;}
        else if(!sprintIntent&&ownsSprint){if(!mc.gameSettings.keyBindSprint.isKeyDown())mc.thePlayer.setSprinting(false);ownsSprint=false;}
    }''')
replace('src/main/java/dev/forgeclient/minecraft/ClientEvents.java','    @SubscribeEvent public void attack(AttackEntityEvent event){if(event.entityPlayer==mc.thePlayer)client.sampler.attack(event.target);}','    @SubscribeEvent public void attack(AttackEntityEvent event){if(event.entityPlayer==mc.thePlayer){client.sampler.attack(event.target);if(enabled("toggle_sprint")&&sprintIntent)sprintResumeTicks=3;}}')
replace('src/main/java/dev/forgeclient/minecraft/ClientEvents.java','    @SubscribeEvent public void overlayPost(RenderGameOverlayEvent.Post event) {','    @SubscribeEvent(priority=EventPriority.LOWEST) public void overlayPost(RenderGameOverlayEvent.Post event) {')
replace('src/main/java/dev/forgeclient/minecraft/ClientEvents.java','''    @SubscribeEvent public void gui(GuiOpenEvent event) {
        // Release before a vanilla/mod settings screen has a chance to save GameSettings.
        if(event.gui!=null&&!(event.gui instanceof ForgeScreen))overrides.releaseAll();
    }''','''    @SubscribeEvent public void gui(GuiOpenEvent event) {
        if(event.gui!=null&&event.gui.getClass()==net.minecraft.client.gui.GuiMainMenu.class){event.gui=new ForgeMainMenu();return;}
        // Release before a vanilla/mod settings screen has a chance to save GameSettings.
        if(event.gui!=null&&!(event.gui instanceof ForgeScreen)&&!(event.gui instanceof ForgeMainMenu))overrides.releaseAll();
    }''')

replace('src/main/java/dev/forgeclient/minecraft/TelemetrySampler.java','        EntityPlayer p=mc.thePlayer;data.player=mc.getSession().getUsername();data.server=mc.isSingleplayer()?"Singleplayer":(mc.getCurrentServerData()==null?"Multiplayer":mc.getCurrentServerData().serverName);','        EntityPlayer p=mc.thePlayer;data.player=mc.getSession().getUsername();data.server=mc.isSingleplayer()?"Singleplayer":(mc.getCurrentServerData()==null?"Multiplayer":mc.getCurrentServerData().serverIP);')
replace('src/main/java/dev/forgeclient/minecraft/TelemetrySampler.java','        if(enabled("sprint_status"))data.put("sprint_status",p.isSprinting()?"SPRINTING":"WALKING");','        if(enabled("sprint_status")){boolean module=modules.enabled("toggle_sprint");data.put("sprint_status",module?"TOGGLE SPRINT  ENABLED":"TOGGLE SPRINT  DISABLED",module?(data.sprintToggled?"LOCKED ON":"READY"):(p.isSprinting()?"SPRINTING":"WALKING"));}')

replace('src/main/java/dev/forgeclient/ui/HudRenderer.java','        for(ClientModule m:registry.all())if(m.hud&&m.enabled()&&!hidden(registry,m)){','        for(ClientModule m:registry.hud())if(m.enabled()&&!hidden(registry,m)){')
replace('src/main/java/dev/forgeclient/ui/HudRenderer.java','''            if(m.setting("background").bool()){
                int alpha=(int)Math.round(m.setting("opacity").number()*255/100);
                c.rect(0,0,size.width,size.height,(alpha<<24)|0x17131A);
            }
            if(m.setting("accent").bool())c.rect(0,0,2,size.height,Theme.ORANGE);''','''            boolean sprintHighlight=m.id.equals("sprint_status")&&t.toggleSprintEnabled;
            if(m.setting("background").bool()){
                int alpha=(int)Math.round(m.setting("opacity").number()*255/100);
                c.rect(0,0,size.width,size.height,(alpha<<24)|(sprintHighlight?0x2A1A10:0x17131A));
            }
            if(m.setting("accent").bool())c.rect(0,0,sprintHighlight?3:2,size.height,sprintHighlight?Theme.GOLD:Theme.ORANGE);''')
replace('src/main/java/dev/forgeclient/ui/HudRenderer.java','            for(int i=0;i<lines.length;i++)c.text(Theme.truncate(c,lines[i],size.width-16),8,6+i*12,Theme.TEXT,shadow);','            for(int i=0;i<lines.length;i++)c.text(Theme.truncate(c,lines[i],size.width-16),8,6+i*12,(m.id.equals("sprint_status")&&t.toggleSprintEnabled&&i==0)?Theme.GOLD:Theme.TEXT,shadow);')
replace('src/main/java/dev/forgeclient/ui/HudRenderer.java','''    private double scale(Canvas c,ClientModule m,Telemetry t,int w,int h){
        Size size=measure(c,m,t);return Math.min(m.setting("scale").number(),Math.min(w/(double)Math.max(1,size.width),h/(double)Math.max(1,size.height)));
    }
    public Rect bounds(Canvas c,ClientModule m,Telemetry t,int width,int height){
        Size size=measure(c,m,t);double scale=scale(c,m,t,width,height);
        int w=Math.min(width,(int)Math.ceil(size.width*scale)),h=Math.min(height,(int)Math.ceil(size.height*scale));
        return new Rect(m.placement.pixelX(width,w),m.placement.pixelY(height,h),w,h);
    }
    public void render(Canvas c,ModuleRegistry registry,Telemetry t,int width,int height){
        for(ClientModule m:registry.hud())if(m.enabled()&&!hidden(registry,m)){
            Rect r=bounds(c,m,t,width,height);drawAt(c,m,t,r.x,r.y,scale(c,m,t,width,height));
        }
    }
    public void drawAt(Canvas c,ClientModule m,Telemetry t,int x,int y,double scale){
        Size size=measure(c,m,t);boolean shadow=m.setting("shadow").bool();''','''    private double scale(Size size,ClientModule m,int w,int h){return Math.min(m.setting("scale").number(),Math.min(w/(double)Math.max(1,size.width),h/(double)Math.max(1,size.height)));}
    public Rect bounds(Canvas c,ClientModule m,Telemetry t,int width,int height){
        Size size=measure(c,m,t);double scale=scale(size,m,width,height);
        int w=Math.min(width,(int)Math.ceil(size.width*scale)),h=Math.min(height,(int)Math.ceil(size.height*scale));
        return new Rect(m.placement.pixelX(width,w),m.placement.pixelY(height,h),w,h);
    }
    public void render(Canvas c,ModuleRegistry registry,Telemetry t,int width,int height){
        for(ClientModule m:registry.hud())if(m.enabled()&&!hidden(registry,m)){
            Size size=measure(c,m,t);double scale=scale(size,m,width,height);
            int w=Math.min(width,(int)Math.ceil(size.width*scale)),h=Math.min(height,(int)Math.ceil(size.height*scale));
            Rect r=new Rect(m.placement.pixelX(width,w),m.placement.pixelY(height,h),w,h);drawMeasured(c,m,t,r.x,r.y,scale,size);
        }
    }
    public void drawAt(Canvas c,ClientModule m,Telemetry t,int x,int y,double scale){drawMeasured(c,m,t,x,y,scale,measure(c,m,t));}
    private void drawMeasured(Canvas c,ClientModule m,Telemetry t,int x,int y,double scale,Size size){
        boolean shadow=m.setting("shadow").bool();''')

replace('src/main/java/dev/forgeclient/ui/OverlayView.java','''        hit("module:"+m.id,r,()->select(m),m::toggle);
        c.rect(r.x+9,r.y+11,3,3,m.enabled()?Theme.ORANGE:Theme.DIM);''','''        hit("module:"+m.id,r,()->select(m),m.available()?m::toggle:null);
        c.rect(r.x+9,r.y+11,3,3,m.enabled()?Theme.ORANGE:Theme.DIM);''')
replace('src/main/java/dev/forgeclient/ui/OverlayView.java','''        Theme.toggle(c,toggle.x,toggle.y,m.enabled(),toggle.contains(mouseX,mouseY));hit("toggle:"+m.id,toggle,m::toggle,null);''','''        if(m.available()){Theme.toggle(c,toggle.x,toggle.y,m.enabled(),toggle.contains(mouseX,mouseY));hit("toggle:"+m.id,toggle,m::toggle,null);}
        else{c.rect(toggle.x,toggle.y,toggle.width,toggle.height,0xFF211D23);c.text("PORT",toggle.x+3,toggle.y+3,Theme.DIM,false);}''')
replace('src/main/java/dev/forgeclient/ui/OverlayView.java','''        c.text(m.enabled()?"ENABLED":"DISABLED",x+9,y+10,m.enabled()?Theme.GOLD:Theme.MUTED,false);
        Theme.toggle(c,x+w-35,y+7,m.enabled(),false);hit("inspector-toggle",new Rect(x,y,w,29),m::toggle,null);y+=38;''','''        c.text(!m.available()?"PORTING":(m.enabled()?"ENABLED":"DISABLED"),x+9,y+10,m.enabled()?Theme.GOLD:Theme.MUTED,false);
        if(m.available()){Theme.toggle(c,x+w-35,y+7,m.enabled(),false);hit("inspector-toggle",new Rect(x,y,w,29),m::toggle,null);}
        else c.text("COMING SOON",x+w-83,y+10,Theme.DIM,false);y+=38;''')
replace('src/main/java/dev/forgeclient/minecraft/ForgeClient.java','    public static final String MOD_ID="forgeclient",VERSION="0.1.0-alpha";','    public static final String MOD_ID="forgeclient",VERSION="0.2.0-alpha";')
replace('src/main/java/dev/forgeclient/minecraft/ForgeClient.java','    private long savedRevision;\n    private volatile String saveError;','    private long savedRevision;private int savePoll;\n    private volatile String saveError;')
replace('src/main/java/dev/forgeclient/minecraft/ForgeClient.java','    public void snapshotIfChanged() {\n        if(modules.revision()!=savedRevision)snapshot();','    public void snapshotIfChanged() {\n        if(++savePoll>=5){savePoll=0;if(modules.revision()!=savedRevision)snapshot();}')
replace('src/main/java/dev/forgeclient/minecraft/ForgeClient.java','        log.info("Forge Client {} initialized with {} modules. No telemetry or remote services.",VERSION,modules.all().size());','        log.info("Forge Client {} initialized with {} module entries ({} currently native). No telemetry or remote services.",VERSION,modules.all().size(),modules.availableCount());\n        log.info("OptiFine {}. Forge Client never redistributes OptiFine; a user-installed copy is detected and left compatible.",OptiFineCompatibility.present()?"detected":"not detected");')

replace('src/main/java/dev/forgeclient/minecraft/NativeCanvas.java','    private final Deque<double[]> transforms=new ArrayDeque<>();\n    private final Deque<Rect> clips=new ArrayDeque<>();','    private final double[] stackX=new double[128],stackY=new double[128],stackScale=new double[128];private int transformDepth;\n    private final Deque<Rect> clips=new ArrayDeque<>();')
replace('src/main/java/dev/forgeclient/minecraft/NativeCanvas.java','        began=true;transforms.clear();clips.clear();offsetX=offsetY=0;localScale=1;','        began=true;transformDepth=0;clips.clear();offsetX=offsetY=0;localScale=1;')
replace('src/main/java/dev/forgeclient/minecraft/NativeCanvas.java','            while(!transforms.isEmpty())pop();','            while(transformDepth>0)pop();')
replace('src/main/java/dev/forgeclient/minecraft/NativeCanvas.java','''    public void push(double x,double y,double scale){
        transforms.push(new double[]{offsetX,offsetY,localScale});offsetX+=x*localScale;offsetY+=y*localScale;localScale*=scale;
        GlStateManager.pushMatrix();GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
    }
    public void pop(){if(transforms.isEmpty())throw new IllegalStateException("Unbalanced canvas transform");double[] v=transforms.pop();offsetX=v[0];offsetY=v[1];localScale=v[2];GlStateManager.popMatrix();}''','''    public void push(double x,double y,double scale){
        if(transformDepth>=stackX.length)throw new IllegalStateException("Canvas transform stack overflow");stackX[transformDepth]=offsetX;stackY[transformDepth]=offsetY;stackScale[transformDepth]=localScale;transformDepth++;
        offsetX+=x*localScale;offsetY+=y*localScale;localScale*=scale;GlStateManager.pushMatrix();GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
    }
    public void pop(){if(transformDepth<=0)throw new IllegalStateException("Unbalanced canvas transform");transformDepth--;offsetX=stackX[transformDepth];offsetY=stackY[transformDepth];localScale=stackScale[transformDepth];GlStateManager.popMatrix();}''')

write('src/main/java/dev/forgeclient/minecraft/OptiFineCompatibility.java','''package dev.forgeclient.minecraft;

/** Detection only. OptiFine is proprietary and is intentionally not redistributed in Forge Client. */
public final class OptiFineCompatibility {
    private static final boolean PRESENT=detect();
    private OptiFineCompatibility(){}
    public static boolean present(){return PRESENT;}
    private static boolean detect(){ClassLoader loader=OptiFineCompatibility.class.getClassLoader();for(String name:new String[]{"Config","optifine.OptiFineForgeTweaker"})try{Class.forName(name,false,loader);return true;}catch(Throwable ignored){}return false;}
}
''')

write('src/main/java/dev/forgeclient/minecraft/ForgeMainMenu.java','''package dev.forgeclient.minecraft;

import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import java.io.IOException;

/** Dawn-inspired title screen backed by Forge Client's original generated artwork. */
public final class ForgeMainMenu extends GuiScreen {
    private static final ResourceLocation ART=new ResourceLocation("forgeclient","textures/gui/title.jpg");
    private static final int ART_W=800,ART_H=450;
    private float artScale;private int artX,artY;
    @Override public void initGui(){layout();}
    private void layout(){artScale=Math.max(width/(float)ART_W,height/(float)ART_H);artX=Math.round((width-ART_W*artScale)/2F);artY=Math.round((height-ART_H*artScale)/2F);}
    private int sx(int x){return artX+Math.round(x*artScale);}private int sy(int y){return artY+Math.round(y*artScale);}
    private boolean hit(int mx,int my,int x1,int y1,int x2,int y2){return mx>=sx(x1)&&mx<=sx(x2)&&my>=sy(y1)&&my<=sy(y2);}
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){layout();GlStateManager.disableLighting();GlStateManager.disableFog();GlStateManager.color(1,1,1,1);mc.getTextureManager().bindTexture(ART);drawModalRectWithCustomSizedTexture(artX,artY,0,0,Math.round(ART_W*artScale),Math.round(ART_H*artScale),Math.round(ART_W*artScale),Math.round(ART_H*artScale));drawRect(sx(11),sy(392),sx(88),sy(428),0xC9121114);fontRendererObj.drawString("v"+ForgeClient.VERSION,sx(17),sy(399),0xFFFFA24A,true);fontRendererObj.drawString("Forge Client",sx(17),sy(407),0xFFE6D8D0,false);hover(mouseX,mouseY,294,248,508,282);hover(mouseX,mouseY,294,286,508,318);hover(mouseX,mouseY,294,323,508,355);hover(mouseX,mouseY,294,361,508,393);hover(mouseX,mouseY,6,94,90,127);hover(mouseX,mouseY,6,127,90,160);super.drawScreen(mouseX,mouseY,partialTicks);}
    private void hover(int mx,int my,int x1,int y1,int x2,int y2){if(hit(mx,my,x1,y1,x2,y2))drawRect(sx(x1),sy(y1),sx(x2),sy(y2),0x24FF8A28);}
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{if(button!=0)return;if(hit(x,y,294,248,508,282)){mc.displayGuiScreen(new GuiSelectWorld(this));return;}if(hit(x,y,294,286,508,318)){mc.displayGuiScreen(new GuiMultiplayer(this));return;}if(hit(x,y,294,323,508,355)||hit(x,y,6,127,90,160)){mc.displayGuiScreen(new GuiOptions(this,mc.gameSettings));return;}if(hit(x,y,294,361,508,393)){mc.shutdown();return;}if(hit(x,y,6,94,90,127)){mc.displayGuiScreen(new ForgeScreen(this));return;}super.mouseClicked(x,y,button);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
''')

pattern=r'        test\("43 modules, unique ids, category sizes and conservative defaults",\(\)->\{.*?\n        \}\);\n        test\("Numeric validation'
replacement='''        test("Lunar catalog parity, unique ids and conservative defaults",()->{\n            ModuleRegistry r=ModuleCatalog.create();eq(98,LunarParity.REQUIRED_NAMES.length);eq(8,r.enabledCount());\n            Set<String> ids=new HashSet<>();\n            for(ClientModule m:r.all()){\n                ok(ids.add(m.id),"duplicate module");\n                Set<String> settings=new HashSet<>();for(Setting s:m.settings()){ok(settings.add(s.id),"duplicate setting");ok(s.set(s.raw()),"default must validate");}\n                if(m.hud){eq(Setting.Kind.NUMBER,m.setting("scale").kind);ok(m.placement.x()>=0&&m.placement.x()<=1,"normalized x");}\n            }\n            for(String name:LunarParity.REQUIRED_NAMES)ok(LunarParity.covers(r,name),"missing Lunar parity entry: "+name);\n            ok(r.all().size()>=98,"catalog must include the full Lunar baseline plus Forge extras");ok(LunarParity.unavailableCount(r)>0,"unfinished parity entries stay visibly non-toggleable rather than faking behavior");\n            ok(!r.enabled("fullbright")&&!r.enabled("no_fire")&&!r.enabled("distance_culling")&&!r.enabled("toggle_sprint"),"sensitive modules start off");\n            throwsType(IllegalArgumentException.class,()->r.add(r.get("fps")));\n        });\n        test("Numeric validation'''
regex_replace('src/test/java/dev/forgeclient/tests/AllTests.java',pattern,replacement,flags=re.S)

replace('build.gradle.kts','version = "0.1.0-alpha"','version = "0.2.0-alpha"')
replace('scripts/verify_jar.py',"        'dev/forgeclient/minecraft/ForgeScreen.class',\n        'dev/forgeclient/ui/OverlayView.class',","        'dev/forgeclient/minecraft/ForgeScreen.class',\n        'dev/forgeclient/minecraft/ForgeMainMenu.class',\n        'dev/forgeclient/core/LunarParity.class',\n        'dev/forgeclient/ui/OverlayView.class',")
replace('scripts/verify_jar.py',"        'assets/forgeclient/textures/gui/stone.png',\n        'assets/forgeclient/lang/en_US.lang',","        'assets/forgeclient/textures/gui/stone.png',\n        'assets/forgeclient/textures/gui/logo.jpg',\n        'assets/forgeclient/textures/gui/title.jpg',\n        'assets/forgeclient/lang/en_US.lang',")
replace('scripts/verify_jar.py',"        'META-INF/licenses/BasicHUD-MIT.txt',","        'META-INF/licenses/BasicHUD-MIT.txt',\n        'META-INF/licenses/Lunar-Apollo-MIT.txt',")
replace('scripts/verify_jar.py',"('forgeclient', '1.8.9', '0.1.0-alpha')","('forgeclient', '1.8.9', '0.2.0-alpha')")

mcmod=read('src/main/resources/mcmod.info')
if '"logoFile"' not in mcmod:
    mcmod=mcmod.replace('    "url": "https://github.com/BerleCool/Forge_Client_Minecraft",','    "url": "https://github.com/BerleCool/Forge_Client_Minecraft",\n    "logoFile": "assets/forgeclient/textures/gui/logo.jpg",')
    write('src/main/resources/mcmod.info',mcmod)

write('licenses/Lunar-Apollo-MIT.txt','''MIT License\n\nCopyright (c) 2026 Moonsworth\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n''')

notices=read('THIRD_PARTY_NOTICES.md')
if '## Lunar Client Apollo' not in notices:
    notices+='''\n\n## Lunar Client Apollo\n\nForge Client uses the public module catalog in `LunarClient/Apollo` as a compatibility/parity reference. Apollo is MIT-licensed; the applicable license is bundled at `licenses/Lunar-Apollo-MIT.txt`. No proprietary Lunar Client runtime, assets, or optimization code is included.\n\nSource: https://github.com/LunarClient/Apollo\n\n## OptiFine\n\nOptiFine is **not bundled or redistributed**. Its official copyright terms prohibit public redistribution without advance written permission. Forge Client only detects a user-installed OptiFine copy and remains compatible with it.\n\nCopyright / redistribution terms: https://optifine.net/copyright\n'''
    write('THIRD_PARTY_NOTICES.md',notices)

write('docs/PERFORMANCE_AND_PARITY.md','''# Forge Client 0.2 performance and Lunar parity baseline\n\n## Hypixel latency diagnosis\n\nForge Client 0.1 did not inject packets or send background telemetry. Its Ping HUD only reads the `NetworkPlayerInfo` value vanilla already receives. Minecraft 1.8.9 already enables TCP_NODELAY, so Forge deliberately does not ship a placebo TcpNoDelay patch. Main-thread frame stalls can nevertheless make packet-driven game state feel late.\n\n## 0.2 hot-path changes\n\n- Stable cached module/HUD views replace a new `ArrayList` allocation on every `all()` call.\n- HUD rendering iterates HUD entries only.\n- `NativeCanvas` uses a fixed primitive transform stack instead of a new `double[]` per HUD widget per frame.\n- Configuration revision scans are amortized to 4 Hz; closing Forge's UI snapshots immediately.\n- Existing telemetry stays local and bounded; no server probes were added.\n- Smart FPS only limits menus/unfocused windows and never focused gameplay.\n\n## Lunar module parity\n\nThe current public Lunar module catalog is imported as a surface baseline from the MIT-licensed `LunarClient/Apollo` repository. Functional Forge modules with genuinely overlapping behavior satisfy their matching Lunar entry. Missing entries appear as `PORTING` targets and are intentionally non-toggleable until a real 1.8.9 handler exists. This is complete catalog visibility, **not yet 98/98 native behavior parity**.\n\n## OptiFine\n\nOptiFine is proprietary. Its official copyright terms prohibit public redistribution without advance written permission, so the public Forge Client JAR does not bundle it. Forge 0.2 detects and coexists with a separately installed OptiFine copy.\n''')

readme=read('README.md')
readme=readme.replace('The current alpha combines a full-screen, Right-Shift module interface with HUD, visual, utility and performance modules in a dark Minecraft-inspired orange theme.','The 0.2 alpha combines a full-screen, Right-Shift module interface with a Lunar-derived module catalog baseline, a Dawn-inspired custom title screen, local HUD/visual utilities, and a lower-allocation gameplay path in a dark Minecraft-inspired orange theme.')
readme=readme.replace('- Modules: **43** HUD / visual / performance / utility modules','- Catalog: **current 98-module Lunar Apollo baseline** plus Forge-specific extras; only entries with real 1.8.9 handlers are toggleable')
readme=readme.replace('`dist/Forge-Client-1.8.9-0.1.0-alpha.jar`','`dist/Forge-Client-1.8.9-0.2.0-alpha.jar`')
if '## OptiFine compatibility' not in readme:
    readme+='''\n\n## OptiFine compatibility\n\nOptiFine is not bundled in this public repository because its official copyright terms prohibit public redistribution without advance written permission. Forge Client 0.2 detects and coexists with a user-installed Minecraft 1.8.9 OptiFine JAR.\n\nSee `docs/PERFORMANCE_AND_PARITY.md` for the performance work and exactly what Lunar parity means in this alpha.\n'''
write('README.md',readme)
print('Forge Client 0.2 source migration applied successfully.')
