package dev.forgeclient.core;

/** Every entry has a corresponding runtime handler or HUD renderer; no placeholder toggles. */
public final class ModuleCatalog {
    private ModuleCatalog() { }
    public static ModuleRegistry create() {
        ModuleRegistry r = new ModuleRegistry();
        hud(r,"fps","FPS","Your live frame rate","Minecraft's measured frames per second. Updates four times per second without formatting text in every frame.",true,.012,.018);
        hud(r,"cps","CPS","Left and right click counters","Counts physical mouse-down events over a rolling one-second window, including multiple clicks between ticks.",true,.012,.068)
            .add(Setting.choice("mode","Mouse buttons","Choose which counters to display.","Both","Both","Left","Right"));
        hud(r,"keystrokes","Keystrokes","Every movement, in miniature","Live movement and mouse keys. Labels follow your actual Minecraft controls, not hard-coded WASD.",true,.014,.92)
            .add(Setting.bool("mouse","Mouse buttons","Display both mouse buttons and CPS.",true))
            .add(Setting.bool("jump","Jump bar","Display the jump key beneath the movement keys.",true));
        hud(r,"ping","Ping","Server latency at a glance","Uses latency already supplied by the server's player list. Does not send additional pings or network requests.",true,.988,.018);
        hud(r,"coordinates","Coordinates","Know exactly where you are","Your own position only. Streamer mode can hide this widget without changing Minecraft's debug screen.",false,.012,.13)
            .add(Setting.number("decimals","Decimal places","Precision of the position display.",0,0,2,1));
        hud(r,"compass","Compass","Find your heading","An eight-direction heading and your yaw. No entity scanning or hidden player information.",false,.5,.018);
        hud(r,"armor","Armor status","Durability before disaster","The armor you are wearing, with native item icons and remaining durability. Drag and resize it in the HUD editor.",true,.985,.87)
            .add(Setting.bool("percent","Percentage","Show percent instead of raw remaining durability.",true))
            .add(Setting.choice("layout","Layout","Arrange armor horizontally or vertically.","Vertical","Vertical","Horizontal"));
        hud(r,"held_item","Held item","Keep an eye on your tool","Your currently held item, stack size and durability. Empty hands do not create an empty HUD panel.",false,.70,.96)
            .add(Setting.bool("percent","Percentage","Show remaining durability as a percentage.",false));
        hud(r,"potions","Potion effects","Effects and their timers","Your active potion effects and remaining durations, sampled from the local player.",true,.988,.25);
        hud(r,"speed","Movement speed","Your pace in blocks per second","Horizontal movement per elapsed tick, smoothed over five ticks. Teleports and world transitions reset the sample.",false,.012,.24);
        hud(r,"memory","Memory","Keep tabs on the JVM","Used and maximum Java heap. This is not system RAM or an FPS improvement claim.",false,.988,.085);
        hud(r,"clock","Local clock","Real-world time","Uses your device's local time zone, sampled four times per second.",false,.988,.15)
            .add(Setting.bool("twenty_four","24-hour time","Use HH:mm instead of a 12-hour clock.",true));
        hud(r,"session","Session time","Time in this world","A monotonic session timer, reset on world changes. Does not depend on the system clock remaining unchanged.",false,.012,.30);
        hud(r,"server","Server info","Where you are playing","Shows the current server name, not its resolved IP. Streamer mode replaces identifying text.",false,.5,.075);
        hud(r,"biome","Biome","Read the landscape","The biome of your current loaded block. Does not load or generate additional chunks.",false,.012,.36);
        hud(r,"light","Light level","Check your surroundings","Sky and block light at your current loaded position. No world scanning or extra chunk requests.",false,.012,.42);
        hud(r,"day_time","World clock","Time under the Minecraft sun","The world's day number and time of day. Display only; never changes server time.",false,.5,.13);
        hud(r,"hit_distance","Hit distance","A local hit-distance readout","Distance from your eyes to the selected hit point on a local attack. This is NOT server-confirmed reach and does not extend attacks.",false,.5,.79)
            .add(Setting.number("timeout","Display seconds","Hide the reading after this many seconds.",3,1,10,1));
        hud(r,"inventory_counts","Supplies","Arrows, pearls and blocks","Counts your own inventory every five ticks. Useful for PvP and building without constantly opening your inventory.",false,.30,.94);
        hud(r,"pack_info","Resource packs","Your active pack","The names of your enabled resource packs. Uses Minecraft's existing repository; never reads arbitrary files.",false,.012,.50);
        hud(r,"sprint_status","Sprint status","Know when you're sprinting","Displays your actual local sprint state, not simply whether a sprint key is being held.",false,.012,.82);
        hud(r,"waypoint","Waypoint","A place worth remembering","Distance and heading to your nearest user-created waypoint in this world and dimension. Manage points with /forgeclient waypoint.",false,.5,.20);
        hud(r,"frame_graph","Frame graph","Frame pacing, not just FPS","A rolling 240-frame history. Mean and 99th-percentile frame times use bounded storage and are computed outside the HUD draw path.",false,.988,.50);

        r.add(module("zoom","Zoom",Category.VISUAL,"Get a closer look","Hold the bound key to zoom. FOV is changed for rendering; sensitivity is leased only while zoom is active, then restored. Compatible with vanilla rendering; test alongside other zoom mods.",true,true,46)
            .add(Setting.number("factor","Magnification","The FOV divisor while the zoom key is held.",4,2,10,.5))
            .add(Setting.bool("smooth","Smooth camera","Use Minecraft's cinematic camera during zoom.",false)));
        r.add(module("fullbright","Brightness",Category.VISUAL,"Lighten the darkest corners","A reversible local gamma override. Disabled by default; check the rules of the server you play on.",false)
            .add(Setting.number("gamma","Gamma level","Restores the previous gamma when disabled.",10,1,16,.5)));
        r.add(module("fov_stabilizer","Stable FOV",Category.VISUAL,"Keep the view consistent","Removes movement- and bow-induced FOV changes through Forge's FOV event. Does not change movement speed or attack mechanics.",false));
        r.add(module("crosshair","Crosshair",Category.VISUAL,"An orange point of focus","A custom pixel crosshair in first person. Hidden with F1, in debug view, and when spectator crosshair rules apply.",false)
            .add(Setting.number("gap","Center gap","Pixels between the center and each arm.",3,0,8,1))
            .add(Setting.number("length","Arm length","Length of the four arms in GUI pixels.",5,1,12,1))
            .add(Setting.number("thickness","Thickness","Crosshair stroke width.",1,1,3,1))
            .add(Setting.bool("dot","Center dot","Display an additional center pixel.",true)));
        r.add(module("block_outline","Block outline",Category.VISUAL,"Make your selection clear","An orange outline around the block you are already targeting. Uses vanilla selection bounds; no through-wall block scanner.",false)
            .add(Setting.number("width","Line width","Requested OpenGL line width; GPU support varies.",2,1,5,.5)));
        r.add(module("no_fire","Clean fire overlay",Category.VISUAL,"Keep the center of view clear","Hides the first-person fire texture. Does not remove fire damage. Disabled by default; server rules still apply.",false));
        r.add(module("no_pumpkin","Clean pumpkin overlay",Category.VISUAL,"A less obstructed helmet view","Hides the helmet overlay while wearing a pumpkin. The pumpkin and its gameplay behavior are unchanged.",false));
        r.add(module("no_portal","Clean portal overlay",Category.VISUAL,"Lose the purple overlay","Hides the portal HUD texture only. Does not skip portal travel time or alter teleportation.",false));
        r.add(module("no_water","Clean water overlay",Category.VISUAL,"Remove the water screen texture","Hides the first-person water texture through Forge. Does not remove fog, change swimming or reveal entities.",false));
        r.add(module("steady_camera","Steady walking",Category.VISUAL,"Disable walk bobbing","Temporarily disables view bobbing and restores the previous setting when switched off.",false));

        r.add(module("smart_fps","Smart FPS",Category.PERFORMANCE,"Save work when you're away","Caps rendering while Minecraft is unfocused or displaying a menu. Never caps focused, unobstructed gameplay and never changes the saved vanilla FPS limit.",true)
            .add(Setting.number("background","Background FPS","Frame limit while the window is not active.",15,5,60,5))
            .add(Setting.number("menu","Menu FPS","Frame limit while a screen is open. HUD editing is included.",60,30,144,1)));
        r.add(module("particle_budget","Reduced particles",Category.PERFORMANCE,"Less particle work","Leases vanilla's particle setting. This is a real density reduction, not a custom GPU particle engine.",false)
            .add(Setting.choice("density","Particle density","The vanilla level to use while enabled.","Decreased","Decreased","Minimal")));
        r.add(module("fast_graphics","Fast graphics",Category.PERFORMANCE,"Favor simpler rendering","Leases the vanilla fast-graphics mode. Chunk rebuilding is requested only when the mode changes, never every tick.",false));
        r.add(module("entity_shadows","Disable shadows",Category.PERFORMANCE,"Skip entity shadow rendering","Turns off vanilla entity shadows while enabled and restores your setting on release.",false));
        r.add(module("clouds","Disable clouds",Category.PERFORMANCE,"Spend frames on the world","Turns off cloud rendering while enabled. Does not change world weather or server state.",false));
        r.add(module("distance_culling","Entity distance",Category.PERFORMANCE,"Skip distant living mobs","Distance-only render suppression for unnamed, non-boss, non-player living entities. NOT occlusion culling. Entities continue ticking; players are never hidden.",false)
            .add(Setting.number("distance","Render distance","Maximum distance in blocks. Off by default.",96,16,192,8)));

        r.add(module("toggle_sprint","Toggle sprint",Category.UTILITY,"One tap, keep moving","While enabled, tap your vanilla sprint key to toggle sprint intent. Respects hunger, collisions, sneaking and item use. Stops on focus loss, menus and world changes.",false));
        r.add(module("chat_timestamps","Chat timestamps",Category.UTILITY,"Give conversations context","Prepends a local timestamp to ordinary chat. Preserves click/hover components and leaves action-bar messages untouched.",false)
            .add(Setting.bool("seconds","Show seconds","Include seconds in the timestamp.",false)));
        r.add(module("chat_filter","Repeated chat filter",Category.UTILITY,"Keep repeated lines quiet","Suppresses immediately repeated chat text inside a short window. Never suppresses action-bar messages; does not send anything to the server.",false)
            .add(Setting.number("window","Window seconds","How long an identical consecutive line is considered a repeat.",3,1,10,1)));
        r.add(module("streamer_mode","Streamer mode",Category.UTILITY,"Privacy for Forge's own HUD","Redacts identifying fields in this client's widgets. Does NOT cover vanilla chat, F3, the player list or other mods; check your whole screen before streaming.",false)
            .add(Setting.bool("coordinates","Hide coordinates","Hide coordinates and waypoint names in Forge's HUD.",true))
            .add(Setting.bool("server","Hide server","Replace the displayed server name.",true))
            .add(Setting.bool("player","Hide username","Replace your name in Forge's overlay.",true)));
        return r;
    }
    private static ClientModule hud(ModuleRegistry r, String id, String name, String summary, String description, boolean enabled, double x, double y) {
        ClientModule m = new ClientModule(id,name,Category.HUD,summary,description,enabled,true,false,0,x,y)
            .add(Setting.number("scale","HUD scale","Size of this widget. Mouse wheel also changes this in the HUD editor.",1,.5,2,.05))
            .add(Setting.bool("background","Background","Draw a translucent charcoal panel behind the widget.",true))
            .add(Setting.number("opacity","Panel opacity","Percent opacity of the widget's background.",70,0,100,5))
            .add(Setting.bool("accent","Orange accent","Draw the orange edge on this widget.",true))
            .add(Setting.bool("shadow","Text shadow","Use a dark pixel shadow behind text.",true));
        r.add(m); return m;
    }
    private static ClientModule module(String id,String name,Category c,String summary,String description,boolean enabled) {
        return module(id,name,c,summary,description,enabled,false,0);
    }
    private static ClientModule module(String id,String name,Category c,String summary,String description,boolean enabled,boolean hold,int key) {
        return new ClientModule(id,name,c,summary,description,enabled,false,hold,key,0,0);
    }
}
