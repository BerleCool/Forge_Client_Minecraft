# Third-party notices

Forge Client is an independent Minecraft Forge 1.8.9 client mod. The 0.4 pass deliberately reuses permissively licensed implementation ideas where that improves correctness, while keeping copyleft or restrictive projects as behavior references unless their licensing is compatible with this combined distribution.

## BasicHUD

The rolling one-second click-counting approach used by Forge Client's CPS telemetry was informed by BasicHUD by Marschi47. BasicHUD is licensed under the MIT License. The license text is bundled at `licenses/BasicHUD-MIT.txt`.

Source: https://github.com/Marschi47/BasicHUD

## Lunar Client Apollo

Forge Client uses the public module catalog and public setting semantics in `LunarClient/Apollo` as a compatibility/parity reference. Apollo is MIT-licensed; the applicable license is bundled at `licenses/Lunar-Apollo-MIT.txt`. No proprietary Lunar Client runtime, assets, or optimization code is included.

Source: https://github.com/LunarClient/Apollo

## SkyblockAddons

Forge Client's immutable sidebar-scoreboard snapshot/filtering pipeline is adapted from the MIT-licensed `ScoreboardManager` design in BiscuitDevelopment/SkyblockAddons and rewritten for Forge Client without its globals, Guava/Lombok dependencies, web services, or GUI. The applicable MIT license is bundled at `licenses/SkyblockAddons-MIT.txt`.

Source: https://github.com/BiscuitDevelopment/SkyblockAddons

## Reference-only projects

The following open-source projects were studied for expected behavior or implementation tradeoffs, but their source is **not copied or shaded into Forge Client 0.4** because their licenses are copyleft or otherwise unsuitable for direct incorporation into this combined client without additional obligations: QuickplayMod/quickplay (custom/restrictive license), HyperiumClient/Hyperium (LGPL-3.0), Guichaguri/BetterFps (LGPL-2.1), ReplayMod/ReplayMod (GPL-3.0), NotEnoughUpdates/NotEnoughUpdates (LGPL), My-Name-Is-Jeff/SimpleToggleSprint (AGPL-3.0), and Polyfrost/VanillaHUD (GPL-3.0).

The Forge-styled Quickplay selector is independently implemented. Public Hypixel `/play` queue identifiers are server command identifiers; QuickplayMod source and UI code are not copied.

## Minecraft Forge and Minecraft

Forge Client targets Minecraft Forge 1.8.9 (`11.15.1.2318-1.8.9`) and is built against Minecraft 1.8.9 mappings. Minecraft, Minecraft Forge, and their respective marks are owned by their respective rights holders. Forge Client is not affiliated with or endorsed by Mojang, Microsoft, or the Minecraft Forge project.

## OptiFine

OptiFine is **not bundled or redistributed**. Its official copyright terms prohibit public redistribution without advance written permission. Forge Client only detects a user-installed OptiFine copy and remains compatible with it.

Copyright / redistribution terms: https://optifine.net/copyright
