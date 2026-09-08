# Forge Client

Forge Client is a client-side Minecraft **1.8.9** mod for **Minecraft Forge 11.15.1.2318**. The current 0.4.1 alpha combines the Right-Shift client UI, the high-resolution Dawn-inspired title screen, an open-source-audited 89-module catalog, and a new native Forge-styled Hypixel Quickplay selector.

> Forge Client is our product name and is independent of the Minecraft Forge project.

## Current alpha

- Minecraft: **1.8.9 only**
- Loader: **Minecraft Forge 1.8.9**
- Open client: **Right Shift**
- Live catalog: **89 implemented modules**
- Placeholder modules: **0**
- Open-source implementation audit: **89 / 89 live modules covered**
- Networking: no Forge Client telemetry, HTTP module traffic, or background ping probes

The compiled development alpha is published by GitHub Actions to:

`dist/Forge-Client-1.8.9-0.4.1-alpha.jar`

A matching SHA-256 file and build/test receipts are generated beside it.

## 0.4.1 title-screen restoration

- Restored the exact first Dawn-style menu composition and baked-control layout instead of the later native-control redesign.
- Rebuilt the title artwork resource at **3840x2160** using a high-quality Lanczos + restrained sharpening pass, preserving the original composition rather than changing the design.
- Added explicit linear texture sampling for clean downscaling at 1080p/1440p/4K while keeping the original 800x450 logical hitbox map.
- No module/runtime behavior was changed from 0.4.

## 0.4 changes

- Added a Forge-styled Quickplay selector: press the module's action bind to open categories + search, select a destination, and explicitly Join. Forge refuses to send `/play` commands off Hypixel.
- Reworked the module implementation strategy around license-compatible open source. SkyblockAddons' MIT scoreboard snapshot approach is adapted directly; Apollo/BasicHUD remain MIT references; GPL/LGPL/AGPL/custom-restricted projects are behavior references only unless their obligations are deliberately adopted.
- Added machine-readable provenance through `ModuleImplementationAudit`, covering all 89 live module IDs, plus a Java 8 CI contract suite.
- Fixed Item Physics to change the actual 1.8.9 dropped-item render phase (`EntityItem.hoverStart`) instead of `rotationYaw`.
- Rebuilt Inventory Mod as a real nine-slot hotbar HUD with item renders, stack overlays and selected-slot highlighting.
- Upgraded Minimap from raw text to a terrain-following, loaded-block tile map with a facing marker.
- Consolidated sidebar/Hypixel/Bed Wars/SkyBlock/SBA parsing into one cached immutable scoreboard snapshot to reduce repeated main-thread work.
- Improved the NEU-style inspector using local `ExtraAttributes.id` and item lore without copying NEU's LGPL implementation.
- Made inventory delta reporting deterministic and cached repeated item counts per HUD sample.

See [`docs/MODULE_IMPLEMENTATION_AUDIT.md`](docs/MODULE_IMPLEMENTATION_AUDIT.md) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for source/licensing decisions.

## Build

```bash
./gradlew clean check remapJar
```

The workflow compiles the real Minecraft adapter, runs the original core/UI suite plus the 0.4 module/Quickplay contract suite on Java 8, remaps the Forge JAR and verifies required classes/resources/licenses before publishing.

## Alpha status

A green build proves compilation, Java 8 tests, remapping, source-audit contracts and packaging. It does **not** replace authenticated in-game QA. Before calling this stable, exercise the modules you actually use in a real client, A/B Hypixel responsiveness against vanilla Forge/Lunar under identical conditions, and benchmark frame pacing with identical video settings.
