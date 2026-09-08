package dev.forgeclient.core;

/**
 * Lunar-inspired modules that have concrete Forge 1.8.9 behavior in LunarRuntime.
 * There are deliberately no roadmap placeholder entries here: if it is in this catalog,
 * it is toggleable and has a runtime implementation.
 */
public final class LunarFunctionalModules {
    private LunarFunctionalModules() {}

    public static void add(ModuleRegistry r) {
        hud(r,"lunar_replay","Replay Mod","Local replay trail","Records a bounded local movement timeline and can render the recorded trail. It never records packets, voices, or other players.",false,.80,.72)
            .add(Setting.number("seconds","History seconds","How much local movement history to keep.",15,5,60,5))
            .add(Setting.bool("trail","Render trail","Draw the recorded movement path in-world.",true));
        hud(r,"lunar_hypixel","Hypixel Mods","Hypixel session HUD","Detects Hypixel from the connected server address and summarizes the current scoreboard locally. No Hypixel API requests.",false,.02,.58);
        hud(r,"lunar_bedwars","Hypixel Bedwars","Bed Wars scoreboard","Extracts useful Bed Wars lines from the scoreboard you already receive from the server.",false,.02,.64);
        action(r,"lunar_quickplay","Quickplay","One-key Hypixel queue","When enabled and bound, sends the selected /play command only when you press its key.")
            .add(Setting.choice("mode","Queue","Hypixel game queue used by the hotkey.","Bed Wars Solo","Bed Wars Solo","Bed Wars Doubles","SkyWars Solo","Duels","Lobby"));
        hud(r,"lunar_attack","Attack Indicator","Attack feedback","Shows a short local hit pulse and the last targeted entity. It does not change attack timing or reach.",false,.50,.86);
        hud(r,"lunar_potion_counter","Potion Counter","Potion inventory count","Counts potion stacks in your own inventory on a throttled client tick.",false,.86,.34);
        hud(r,"lunar_scoreboard","Scoreboard","Movable scoreboard mirror","Mirrors the server-provided sidebar scoreboard into a movable Forge HUD widget.",false,.80,.20)
            .add(Setting.number("lines","Max lines","Maximum mirrored scoreboard lines.",10,3,15,1));
        r.add(module("lunar_chat","Chat",Category.UTILITY,"Chat readability controls","Adjusts local vanilla chat opacity while enabled and restores your previous value when disabled.",false)
            .add(Setting.number("opacity","Chat opacity","Local vanilla chat opacity percentage.",85,20,100,5)));
        r.add(module("lunar_tab","Tab Editor",Category.UTILITY,"Forge tab header","Adds a small local Forge status header to the vanilla player list without replacing the server header.",false));
        hud(r,"lunar_cooldowns","Cooldowns","Pearl cooldown helper","Tracks your own ender-pearl use locally and displays a configurable cooldown timer.",false,.50,.92)
            .add(Setting.number("pearl","Pearl cooldown","Seconds displayed after a local pearl use.",15,1,30,1));
        hud(r,"lunar_stopwatch","Stopwatch","Session stopwatch","Runs while this module is enabled. Disabling pauses it; resetting the module resets the timer.",false,.02,.70);
        hud(r,"lunar_combo","Combo Counter","Local combo counter","Counts consecutive local attack events against the same target and resets after a timeout or when you take damage.",false,.50,.80)
            .add(Setting.number("timeout","Combo timeout","Seconds before a combo expires.",2,1,5,.25));
        r.add(module("lunar_time","Time Changer",Category.VISUAL,"Local world time","Overrides the client-side visual world time while enabled. It never sends a time command to the server.",false)
            .add(Setting.choice("time","Visual time","Local visual time preset.","Day","Day","Sunset","Night","Sunrise")));
        r.add(module("lunar_item_physics","Item Physics",Category.VISUAL,"Smoother dropped-item motion","Applies a bounded client-only rotation to loaded dropped-item entities while enabled.",false)
            .add(Setting.number("speed","Spin speed","Degrees of local visual rotation per tick.",2,.5,8,.5)));
        hud(r,"lunar_tnt","TNT Countdown","Nearest TNT fuse","Displays the nearest loaded primed TNT and its remaining fuse time.",false,.50,.74);
        hud(r,"lunar_item_tracker","Item Tracker","Inventory delta tracker","Tracks local inventory count changes and displays the latest item gain/loss without network requests.",false,.02,.76);
        hud(r,"lunar_momentum","Momentum","Movement momentum","Shows horizontal blocks/second and horizontal velocity from the local player state.",false,.02,.82);
        r.add(module("lunar_screenshot","Screenshot",Category.UTILITY,"Screenshot confirmation","Adds a Forge confirmation line when the vanilla F2 screenshot key is pressed. Minecraft still owns the actual screenshot.",false));
        r.add(module("lunar_fog","Fog",Category.VISUAL,"Custom local fog density","Overrides render fog density through Forge's render event. It does not reveal unloaded geometry.",false)
            .add(Setting.number("density","Fog density","Requested local render fog density.",.02,0,.15,.005)));
        action(r,"lunar_auto_text","Auto Text Hotkey","One-key text action","When enabled and bound, sends the selected text only on your explicit key press.")
            .add(Setting.choice("text","Message","Text sent by the hotkey.","gg","gg","Good game!","/hub","/lobby","/spawn"));
        r.add(module("lunar_bossbar","Boss Bar",Category.VISUAL,"Boss-bar visibility","Locally hides the vanilla boss-health overlay while enabled.",false));
        hud(r,"lunar_pvp_info","PvP Info","Compact PvP readout","Shows health, armor durability, arrows, golden apples and potion count from your own inventory.",false,.86,.42);
        r.add(module("lunar_markers","Markers",Category.VISUAL,"Nearest waypoint beacon","Draws a depth-tested vertical marker at your nearest Forge waypoint.",false));
        hud(r,"lunar_team_view","Team View","Nearby teammates","Counts loaded nearby players on your scoreboard team and shows the nearest distance.",false,.86,.50)
            .add(Setting.number("range","Range","Maximum loaded-player range in blocks.",64,16,160,8));
        hud(r,"lunar_minimap","Minimap","Local block minimap","Builds a tiny text-grid map from already-loaded blocks around you. It never loads chunks or shows hidden entities.",false,.02,.30)
            .add(Setting.number("radius","Radius","Loaded-block radius sampled around the player.",4,2,6,1));
        r.add(module("lunar_hitbox","Hitbox",Category.VISUAL,"Target hitbox","Draws the normal bounding box of the entity currently under your crosshair with depth testing preserved.",false));
        r.add(module("lunar_weather","Weather Changer",Category.VISUAL,"Local clear weather","Suppresses rain/thunder strength on the client while enabled. It does not change server weather.",false));
        r.add(module("lunar_chunk_borders","Chunk Borders",Category.VISUAL,"Current chunk outline","Draws a depth-tested outline around your current 16x16 chunk near the player's Y level.",false));
        hud(r,"lunar_waila","WAILA","What am I looking at?","Shows the localized name and metadata of the loaded block under your crosshair.",false,.50,.16);
        r.add(module("lunar_hurt_cam","Hurt Cam",Category.VISUAL,"Disable hurt-camera shake","Temporarily suppresses hurt-time only during rendering and restores it immediately after the frame.",false));
        hud(r,"lunar_tier","Tier Tagger","Scoreboard team tag","Shows the local scoreboard team prefix/suffix for the entity under your crosshair.",false,.50,.22);
        hud(r,"lunar_skyblock","SkyBlock","SkyBlock scoreboard helper","Detects Hypixel SkyBlock and mirrors useful purse/bits/location lines from data already sent to your client.",false,.86,.58);
        hud(r,"lunar_horse","Horse Stats","Mounted horse stats","Shows health and current horizontal speed while you are riding a horse.",false,.50,.28);
        hud(r,"lunar_overlay","Overlay Mod","Compact session overlay","Combines FPS, ping, server and coordinates into one lightweight movable widget.",false,.02,.88);
        r.add(module("lunar_rewind","Rewind",Category.VISUAL,"Recent movement trail","Renders the newest section of the same bounded local movement history used by Replay Mod.",false));
        hud(r,"lunar_actionbar","Action Bar","Movable action bar","Captures the action-bar component Forge already receives, suppresses the vanilla copy, and renders it as a movable HUD widget.",false,.50,.68);
        r.add(module("lunar_light_overlay","Light Overlay",Category.VISUAL,"Spawn-light helper","Samples a small bounded set of already-loaded nearby floor blocks and marks low block-light positions.",false)
            .add(Setting.number("radius","Radius","Horizontal sample radius.",5,2,8,1))
            .add(Setting.number("threshold","Light threshold","Mark positions at or below this block-light level.",7,0,15,1)));
        r.add(module("lunar_kill_sounds","Kill Sounds",Category.UTILITY,"Local kill feedback","Plays a local orb sound when common server kill messages indicate that you made a kill.",false));
        hud(r,"lunar_inventory","Inventory Mod","Inventory summary","Shows hotbar stacks plus arrows, pearls and golden apples from your own inventory.",false,.70,.90);
        hud(r,"lunar_f3","F3 Display","Compact debug display","Shows FPS, chunk coordinates, facing and memory without forcing the vanilla F3 screen open.",false,.02,.94);
        r.add(module("lunar_gui_scale","GUI Scale",Category.UTILITY,"Temporary GUI scale","Applies the chosen vanilla GUI scale while enabled and restores the previous value when disabled.",false)
            .add(Setting.choice("scale","GUI scale","Vanilla GUI scale preset.","Auto","Auto","Small","Normal","Large")));
        hud(r,"lunar_knockback","Knockback Trainer","Local knockback meter","Samples your local horizontal velocity when hurt time starts and displays the latest magnitude.",false,.50,.34);
        hud(r,"lunar_uhc","UHC Overlay","UHC essentials","Shows health, absorption, coordinates, arrows and golden apples from your local state.",false,.86,.66);
        hud(r,"lunar_neu","NotEnoughUpdates","SkyBlock item inspector","Provides a lightweight 1.8.9 SkyBlock held-item/lore inspector using the item data already present on the client.",false,.70,.76);
        hud(r,"lunar_sba","SkyBlockAddons","SkyBlock status HUD","Provides a lightweight SkyBlock action-bar/scoreboard status panel using client-received data.",false,.70,.82);
        r.add(module("lunar_worldedit","WorldEdit CUI",Category.VISUAL,"Wooden-axe selection CUI","While holding a wooden axe, left/right clicks on loaded blocks set local CUI corners and render the selection box.",false));
    }

    private static ClientModule hud(ModuleRegistry r,String id,String name,String summary,String description,boolean enabled,double x,double y) {
        ClientModule m=new ClientModule(id,name,Category.HUD,summary,description,enabled,true,false,0,x,y)
            .add(Setting.number("scale","HUD scale","Size of this widget.",1,.5,2,.05))
            .add(Setting.bool("background","Background","Draw a translucent charcoal panel behind the widget.",true))
            .add(Setting.number("opacity","Panel opacity","Percent opacity of the widget background.",70,0,100,5))
            .add(Setting.bool("accent","Orange accent","Draw the orange edge on this widget.",true))
            .add(Setting.bool("shadow","Text shadow","Use a dark pixel shadow behind text.",true));
        r.add(m);return m;
    }
    private static ClientModule module(String id,String name,Category c,String summary,String description,boolean enabled) {
        return new ClientModule(id,name,c,summary,description,enabled,false,false,0,0,0);
    }
    private static ClientModule action(ModuleRegistry r,String id,String name,String summary,String description) {
        ClientModule m=new ClientModule(id,name,Category.UTILITY,summary,description,false,false,true,0,0,0);
        r.add(m);return m;
    }
}
