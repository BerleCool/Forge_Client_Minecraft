# Forge Client 0.4 module implementation audit

This pass changes the rule from “there is a toggle with a Lunar-like name” to “every live toggle has an implementation owner and a licensing decision.” `ModuleImplementationAudit` covers all 89 live module IDs at runtime, and CI fails if the live registry and audit diverge.

## Reuse policy

Permissive code can be adapted when doing so is materially better than rewriting it. MIT attribution/license text stays in the JAR. Copyleft or custom-restricted projects can still be excellent behavioral references, but Forge Client does not paste them into a combined client and pretend the license disappeared.

The direct/adapted sources in 0.4 are BasicHUD (MIT, CPS rolling window), LunarClient/Apollo (MIT, public catalog/semantics), and BiscuitDevelopment/SkyblockAddons (MIT, sidebar snapshot/filter pipeline). QuickplayMod, Hyperium, BetterFps, ReplayMod, NEU, SimpleToggleSprint, and VanillaHUD were researched but are reference-only for this release because of their respective licenses.

## Every live module

The core 43 modules continue to use direct Minecraft 1.8.9 / Forge event and `GameSettings` implementations, except CPS (BasicHUD MIT-informed) and the performance/toggle-sprint modules which also have license-safe behavior references:

`fps`, `cps`, `keystrokes`, `ping`, `coordinates`, `compass`, `armor`, `held_item`, `potions`, `speed`, `memory`, `clock`, `session`, `server`, `biome`, `light`, `day_time`, `hit_distance`, `inventory_counts`, `pack_info`, `sprint_status`, `waypoint`, `frame_graph`, `zoom`, `fullbright`, `fov_stabilizer`, `crosshair`, `block_outline`, `no_fire`, `no_pumpkin`, `no_portal`, `no_water`, `steady_camera`, `smart_fps`, `particle_budget`, `fast_graphics`, `entity_shadows`, `clouds`, `distance_culling`, `toggle_sprint`, `chat_timestamps`, `chat_filter`, `streamer_mode`.

The 46 Lunar-baseline modules remain live and are all mapped in `ModuleImplementationAudit`:

`lunar_replay`, `lunar_hypixel`, `lunar_bedwars`, `lunar_quickplay`, `lunar_attack`, `lunar_potion_counter`, `lunar_scoreboard`, `lunar_chat`, `lunar_tab`, `lunar_cooldowns`, `lunar_stopwatch`, `lunar_combo`, `lunar_time`, `lunar_item_physics`, `lunar_tnt`, `lunar_item_tracker`, `lunar_momentum`, `lunar_screenshot`, `lunar_fog`, `lunar_auto_text`, `lunar_bossbar`, `lunar_pvp_info`, `lunar_markers`, `lunar_team_view`, `lunar_minimap`, `lunar_hitbox`, `lunar_weather`, `lunar_chunk_borders`, `lunar_waila`, `lunar_hurt_cam`, `lunar_tier`, `lunar_skyblock`, `lunar_horse`, `lunar_overlay`, `lunar_rewind`, `lunar_actionbar`, `lunar_light_overlay`, `lunar_kill_sounds`, `lunar_inventory`, `lunar_f3`, `lunar_gui_scale`, `lunar_knockback`, `lunar_uhc`, `lunar_neu`, `lunar_sba`, `lunar_worldedit`.

## 0.4 implementation upgrades

Quickplay no longer fires one hard-coded queue immediately. Its action bind opens `ForgeQuickplayScreen`: a native charcoal/orange selector with queue groups, search, mouse controls, keyboard navigation, explicit Join buttons, and a safety gate that refuses to send `/play` commands unless the connected host is `hypixel.net` or a subdomain. No HTTP calls, API keys, telemetry, or background probes are involved.

Scoreboard/Hypixel/Bed Wars/SkyBlock/SBA modules now share one immutable sidebar snapshot per sample interval instead of each re-walking the scoreboard. The cap/filter/team-format flow is adapted from SkyblockAddons' MIT `ScoreboardManager`, then simplified for Java 8 and Forge Client. This both improves correctness and cuts repeated main-thread work.

Item Physics now advances `EntityItem.hoverStart`, the 1.8.9 field actually used by vanilla dropped-item rendering, instead of changing `rotationYaw` (which did not produce the intended dropped-item spin). Inventory Mod now renders the actual nine hotbar items with stack overlays and selected-slot highlighting. Minimap now follows nearby terrain vertically and gets a native tile renderer rather than a raw text grid. NEU-style item inspection now reads local `ExtraAttributes.id` plus lore without importing NEU's LGPL code. Inventory counters are cached per HUD sample and item-delta output is deterministic.

Replay/Rewind remain Forge Client's bounded local movement recorder/trail, not a disguised copy of ReplayMod's GPL packet/video recorder. The audit labels that distinction explicitly. The same principle applies to NEU and other large third-party products: a live Forge module must do something real, but its name is not a claim that the entire upstream product has been cloned feature-for-feature.
