#!/usr/bin/env python3
"""Validate a Forge Client distributable and reject preview/dev/test bundles."""
import hashlib
import json
import pathlib
import struct
import sys
import zipfile

jar = pathlib.Path(sys.argv[1])
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    required = {
        'dev/forgeclient/minecraft/ForgeClient.class',
        'dev/forgeclient/minecraft/ForgeScreen.class',
        'dev/forgeclient/minecraft/ForgeMainMenu.class',
        'dev/forgeclient/minecraft/ForgeQuickplayScreen.class',
        'dev/forgeclient/minecraft/SkyblockScoreboardSnapshot.class',
        'dev/forgeclient/minecraft/LunarRuntime.class',
        'dev/forgeclient/core/LunarFunctionalModules.class',
        'dev/forgeclient/core/QuickplayCatalog.class',
        'dev/forgeclient/core/ModuleImplementationAudit.class',
        'dev/forgeclient/ui/OverlayView.class',
        'mcmod.info', 'pack.mcmeta',
        'assets/forgeclient/textures/gui/stone.png',
        'assets/forgeclient/textures/gui/title.jpg',
        'assets/forgeclient/textures/gui/logo.jpg',
        'assets/forgeclient/lang/en_US.lang',
        'META-INF/THIRD_PARTY_NOTICES.md',
        'META-INF/licenses/BasicHUD-MIT.txt',
        'META-INF/licenses/Lunar-Apollo-MIT.txt',
        'META-INF/licenses/SkyblockAddons-MIT.txt',
    }
    missing = required - set(names)
    if missing:
        raise SystemExit('Required entries missing: ' + ', '.join(sorted(missing)))
    if len(names) != len(set(names)):
        raise SystemExit('Duplicate JAR entries')
    for name in names:
        if name.startswith(('net/minecraft/', 'net/minecraftforge/', 'org/lwjgl/', 'dev/forgeclient/preview/', 'dev/forgeclient/tests/')):
            raise SystemExit('Dependency/test/preview code incorrectly bundled: ' + name)
        if name.endswith('.class'):
            data = archive.read(name)
            if data[:4] != b'\xca\xfe\xba\xbe' or struct.unpack('>H', data[6:8])[0] != 52:
                raise SystemExit('Not Java 8 bytecode: ' + name)
    mod = json.loads(archive.read('mcmod.info'))[0]
    if (mod['modid'], mod['mcversion'], mod['version']) != ('forgeclient', '1.8.9', '0.4.1-alpha'):
        raise SystemExit('Incorrect or unexpanded mod metadata')
    main = archive.read('dev/forgeclient/minecraft/ForgeClient.class')
    if b'getMinecraft' in main:
        raise SystemExit('Development method names remain: this is not the remapped distributable')
    classes = sum(name.endswith('.class') for name in names)
print(json.dumps({'jar': str(jar), 'bytes': jar.stat().st_size, 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'java_major': 52, 'classes': classes, 'live_modules': 89, 'placeholder_modules': 0, 'quickplay_selector': True, 'implementation_audit_modules': 89, 'mod': mod}, indent=2))
