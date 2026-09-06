package dev.forgeclient.core;

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
