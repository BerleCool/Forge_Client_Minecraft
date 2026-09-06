# Forge Client

Forge Client is a client-side Minecraft 1.8.9 mod for **Minecraft Forge 11.15.1.2318**. The 0.2 alpha combines a full-screen, Right-Shift module interface with a Lunar-derived module catalog baseline, a Dawn-inspired custom title screen, local HUD/visual utilities, and a lower-allocation gameplay path in a dark Minecraft-inspired orange theme.

> Forge Client is our product name and is independent of the Minecraft Forge project.

## Current alpha

- Minecraft: **1.8.9 only**
- Loader: **Minecraft Forge 1.8.9**
- Open client: **Right Shift**
- Catalog: **current 98-module Lunar Apollo baseline** plus Forge-specific extras; only entries with real 1.8.9 handlers are toggleable
- Configuration: local profiles and persistent HUD placement
- Networking: no Forge Client telemetry or remote service

The compiled development alpha is published by GitHub Actions to:

`dist/Forge-Client-1.8.9-0.2.0-alpha.jar`

A matching `.sha256` file is generated beside it. The same files are also retained as a GitHub Actions artifact for each successful build.

## Build

The GitHub workflow compiles the real Minecraft adapter, runs the dependency-free core/UI test suite, remaps the Forge JAR, validates required entries inside the JAR, and only then publishes it to `dist/`.

Locally, with internet access:

```bash
./gradlew clean check remapJar
```

The repository also contains `scripts/test.sh`, which can validate the dependency-free core and shared UI without downloading Minecraft/Forge dependencies. It does **not** replace a full Forge build.

## Alpha status

A successful build proves that the source compiles against the targeted Forge/Minecraft APIs and produces a remapped mod JAR. It does not replace manual in-game QA. Before calling an alpha release stable, verify at minimum:

- Right Shift opens and closes the overlay in a real 1.8.9 Forge client.
- Every module can be toggled without crashing or corrupting vanilla settings.
- HUD dragging/scaling behaves at multiple GUI scales and resolutions.
- Profiles survive restart and malformed profile recovery works.
- Visual overrides restore the player's original settings after disable, world changes and GUI transitions.
- Multiplayer testing confirms the client remains client-side and does not send unintended commands or packets.
- Performance modules are benchmarked rather than advertised from compile-time assumptions.

## Third-party work

The Luna Mod Menu and Dawn / Feather Client were used only as visual/design references; their code and assets are not bundled. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for the open-source attribution used by the implementation.

---

_I wish you luck, Forge. I know you can do this._


## OptiFine compatibility

OptiFine is not bundled in this public repository because its official copyright terms prohibit public redistribution without advance written permission. Forge Client 0.2 detects and coexists with a user-installed Minecraft 1.8.9 OptiFine JAR.

See `docs/PERFORMANCE_AND_PARITY.md` for the performance work and exactly what Lunar parity means in this alpha.
