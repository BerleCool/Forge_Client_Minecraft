# Forge Client

Forge Client is a client-side Minecraft **1.8.9** mod for **Minecraft Forge 11.15.1.2318**. The current 0.3 alpha combines a full-screen Right-Shift module UI, a resolution-independent Dawn-inspired title screen, performance-conscious HUD/rendering code and a large set of practical Lunar-inspired 1.8.9 features.

> Forge Client is our product name and is independent of the Minecraft Forge project.

## Current alpha

- Minecraft: **1.8.9 only**
- Loader: **Minecraft Forge 1.8.9**
- Open client: **Right Shift**
- Live catalog: **89 implemented modules**
- Placeholder / `PORTING` toggles: **0**
- Configuration: local profiles and persistent HUD placement
- Networking: no Forge Client telemetry or background server probes

The compiled development alpha is published by GitHub Actions to:

`dist/Forge-Client-1.8.9-0.3.0-alpha.jar`

A matching SHA-256 file and build/test receipts are generated beside it.

## 0.3 changes

- Removed the 0.2 Lunar roadmap placeholders. If a module appears in Forge's live UI, it has concrete 1.8.9 behavior.
- Added functional Lunar-inspired Hypixel, Bed Wars, SkyBlock, Quickplay, scoreboard, PvP, team, minimap, hitbox, chunk/light overlay, WAILA, WorldEdit CUI, replay/rewind trail, item tracking, cooldown, combo, stopwatch, action-bar, inventory, F3, GUI-scale and other modules.
- Kept Toggle Sprint intent across attack interruption and highlighted the enabled state in its HUD.
- Rebuilt the title menu so artwork is only a smoothly filtered atmosphere layer; buttons, labels and wordmark are rendered natively at the current resolution instead of being baked into a scaled 800x450 bitmap.
- Preserved the lower-allocation rendering/config path from 0.2.

See [`docs/PERFORMANCE_AND_PARITY.md`](docs/PERFORMANCE_AND_PARITY.md) for exact scope notes.

## Build

```bash
./gradlew clean check remapJar
```

The workflow compiles the real Minecraft adapter, runs the core/UI suite on Java 8, remaps the Forge JAR and verifies required classes/resources before publishing.

## Alpha status

A green build proves compilation, tests, remapping and packaging. It does **not** replace manual game QA. Before calling this stable, test the JAR in a real client across several GUI scales, exercise every module you intend to use, compare Hypixel responsiveness against vanilla/Lunar under the same network conditions, and benchmark frame pacing with identical video settings.

## Third-party work

Luna Mod Menu and Dawn / Feather Client were visual references only; their code/assets are not bundled. Lunar/Apollo's public MIT-licensed module catalog is used as reference metadata. OptiFine is not redistributed.

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
