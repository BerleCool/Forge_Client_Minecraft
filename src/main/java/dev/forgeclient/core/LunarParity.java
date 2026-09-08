package dev.forgeclient.core;

import java.util.*;

/**
 * Reference snapshot of Lunar/Apollo's public cross-version module names.
 * This class never injects disabled placeholders into the live client.
 */
public final class LunarParity {
    private LunarParity(){}
    public static final String[] REQUIRED_NAMES={
        "Replay Mod","1.7 Visuals","FPS","CPS","Toggle Sneak/Sprint","Zoom","Hypixel Mods","Hypixel Bedwars","Quickplay","Armor Status","Keystrokes","Coordinates","Day Counter","Crosshair","Attack Indicator","Potion Effects","Direction HUD","Waypoints","Hit Color","Scoreboard","Titles","Item Counter","Potion Counter","Ping","Motion Blur","Pack Organizer","Chat","Tab Editor","Nametag","Shulker Preview","Scrollable Tooltips","Particle Changer","Nick Hider","Cooldowns","WorldEdit CUI","Clock","Stopwatch","Playtime","Memory","Combo Counter","Reach Display","Time Changer","Server Address","Saturation","Color Saturation","Item Physics","TNT Countdown","Item Tracker","Shiny Pots","3D Skins","Glint Colorizer","Momentum","Block Outline","Screenshot","FOV","Fog","Auto Text Hotkey","Auto Text Actions","Mumble Link","Totem Counter","2D Items","Boss Bar","Freelook","PvP Info","Markers","Snaplook","Team View","Pack Display","Menu Blur","Minimap","Hitbox","Lighting","Weather Changer","Chunk Borders","Sound Changer","WAILA","Hurt Cam","Tier Tagger","Damage Tint","Mob Size","SkyBlock","Item Customizer","Horse Stats","Overlay Mod","Rewind","Audio Subtitles","Action Bar","Shields","Light Overlay","Kill Sounds","Inventory Mod","F3 Display","GUI Scale","Knockback Trainer","Radio","UHC Overlay","NotEnoughUpdates","SkyBlockAddons"
    };
    /** Backwards-compatible hook used by the 0.2 ModuleCatalog; now adds only real modules. */
    public static void addMissing(ModuleRegistry registry){LunarFunctionalModules.add(registry);}
    public static boolean containsName(ModuleRegistry registry,String required){String n=normalize(required);for(ClientModule m:registry.all())if(normalize(m.name).equals(n))return true;return false;}
    public static Set<String> missingNames(ModuleRegistry registry){Set<String> out=new LinkedHashSet<String>();for(String name:REQUIRED_NAMES)if(!containsName(registry,name))out.add(name);return out;}
    private static String normalize(String value){return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
}
