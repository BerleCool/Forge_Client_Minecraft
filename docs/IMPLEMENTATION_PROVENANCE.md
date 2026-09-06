# Source handoff and verification

The previous authoring workspace did not retain a complete checkout. Five source
files survived as conversation attachments and were reused, including the original
module catalog, overlay UI, native event handlers, entry point and test suite.
Missing helper classes, native rendering adapters, preview harness and resource
files were reconstructed around those retained interfaces. This is **not** a claim
that every helper is byte-for-byte identical to the previous temporary workspace.

## Retained originals

- `src/main/java/dev/forgeclient/core/ModuleCatalog.java`: SHA-256 `c77adb87b393da66f3208713dc07da71423a343e8390208d721a3d94c1801dbd`
- `src/main/java/dev/forgeclient/minecraft/ClientEvents.java`: SHA-256 `29b4d41c88ee516ff049aa802e7ed86483d9487a8477109112b70ee79a66df45`
- `src/main/java/dev/forgeclient/minecraft/ForgeClient.java`: SHA-256 `d67a03fcbe0694cf43b49067c85b711e299bba6362e139774c0afde8916597fb`
- `src/main/java/dev/forgeclient/ui/OverlayView.java`: SHA-256 `fd1425a9d98bf52652a04ca9d1da8494304527f8c78180b0f7f3aa767162a1eb`
- `src/test/java/dev/forgeclient/tests/AllTests.java`: SHA-256 `1602071604d6413fbd8ed1abd1d8e1777c654cbf825be4c035e74f978a043d52`

The original `AllTests.java` is not weakened or replaced. Assertion totals can
vary with rendering hit-area counts; the 40 test-group names and checks remain
unchanged. `scripts/test.sh` tests the core/shared UI without Minecraft. The Actions
build additionally compiles every native adapter against actual Minecraft Forge
1.8.9 dependencies, runs the same suite with Java 8 and remaps the distributable.

## Import transport

A one-time, checksum-verified source archive is used to transfer the complete local
source through the connected repository API. The runner validates relative paths,
rejects symlinks and unpacked size excesses, writes only source/resources/scripts/docs,
commits those ordinary files to this PR branch and removes the archive before
compilation. Subsequent builds use the committed source directly. No code is fetched
from third-party clients at runtime or during import.

## Alpha limitations

A green build is not an in-game launch test, multiplayer approval, or FPS benchmark.
Performance modules are reversible vanilla graphics controls, background/menu frame
limits and distance-based living-entity suppression, not an imported occlusion
culling engine. Check each server's rules before enabling optional visual modules.
No remote telemetry, anti-cheat evasion, packet injection or copied Luna/Dawn assets
are included.
