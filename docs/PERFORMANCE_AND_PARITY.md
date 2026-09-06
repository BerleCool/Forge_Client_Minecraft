# Forge Client 0.2 performance and Lunar parity baseline

## Hypixel latency diagnosis

Forge Client 0.1 did not inject packets or send background telemetry. Its Ping HUD only reads the `NetworkPlayerInfo` value vanilla already receives. Minecraft 1.8.9 already enables TCP_NODELAY, so Forge deliberately does not ship a placebo TcpNoDelay patch. Main-thread frame stalls can nevertheless make packet-driven game state feel late.

## 0.2 hot-path changes

- Stable cached module/HUD views replace a new `ArrayList` allocation on every `all()` call.
- HUD rendering iterates HUD entries only.
- `NativeCanvas` uses a fixed primitive transform stack instead of a new `double[]` per HUD widget per frame.
- Configuration revision scans are amortized to 4 Hz; closing Forge's UI snapshots immediately.
- Existing telemetry stays local and bounded; no server probes were added.
- Smart FPS only limits menus/unfocused windows and never focused gameplay.

## Lunar module parity

The current public Lunar module catalog is imported as a surface baseline from the MIT-licensed `LunarClient/Apollo` repository. Functional Forge modules with genuinely overlapping behavior satisfy their matching Lunar entry. Missing entries appear as `PORTING` targets and are intentionally non-toggleable until a real 1.8.9 handler exists. This is complete catalog visibility, **not yet 98/98 native behavior parity**.

## OptiFine

OptiFine is proprietary. Its official copyright terms prohibit public redistribution without advance written permission, so the public Forge Client JAR does not bundle it. Forge 0.2 detects and coexists with a separately installed OptiFine copy.
