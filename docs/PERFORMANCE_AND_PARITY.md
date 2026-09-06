# Forge Client 0.3 - functional Lunar baseline and performance notes

## No placeholder modules

Forge Client 0.2 exposed Lunar/Apollo names that were deliberately marked `PORTING`. That was useful as a roadmap, but it was a bad client experience.

0.3 removes that model entirely. **The live module catalog contains only toggleable modules with concrete Minecraft 1.8.9 behavior.** The public Lunar/Apollo 98-name list remains in source only as reference metadata; it does not inject fake entries into the client.

Forge 0.3 has **89 live modules**. The new Lunar-inspired set covers practical 1.8.9 features such as Hypixel/Bed Wars/SkyBlock HUDs, Quickplay, attack/potion/combo/cooldown HUDs, scoreboard mirror, local chat controls, WorldEdit CUI, stopwatch, time/weather/fog controls, item physics, TNT countdown, item tracking, momentum, boss bar control, PvP/team/UHC overlays, markers, minimap, hitbox/chunk/light overlays, WAILA, hurt-cam suppression, horse stats, replay/rewind trails, movable action bar, inventory/F3/GUI-scale helpers, knockback trainer and lightweight NEU/SBA-style SkyBlock inspectors.

Some names in Lunar's current cross-version catalog do not make sense as honest standalone 1.8.9 toggles (for example Shulker Preview, Totem Counter and Shields), while others are entire third-party products rather than a small module. Forge does **not** show those as dead switches.

## Replay / NEU / SBA scope

The 1.8.9 `Replay Mod` entry in Forge 0.3 is a bounded **local movement recorder/trail**, not a claim that we reimplemented ReplayMod's packet recording/export pipeline.

`NotEnoughUpdates` and `SkyBlockAddons` are lightweight client-received-data inspectors/status HUDs in this alpha. They are functional, but they are not claims of feature-for-feature copies of the separate NEU/SBA projects.

## Performance / Hypixel latency

Forge Client does not inject packets, ping servers in the background or run telemetry. Ping widgets read the latency value Minecraft already receives.

0.2/0.3 keep the hot-path changes that:
- cache module/HUD views instead of allocating lists every frame,
- iterate HUD entries directly,
- avoid per-widget transform-array allocations,
- amortize configuration revision work,
- budget world scans used by minimap/light overlays,
- keep all new server-aware features on data already present in the client.

This can reduce main-thread stalls that make multiplayer *feel* late. It cannot lower physical network RTT. Hypixel responsiveness still needs controlled in-game A/B testing.

## OptiFine

OptiFine remains optional and separately installed. Forge detects it for coexistence but does not redistribute proprietary OptiFine files.
