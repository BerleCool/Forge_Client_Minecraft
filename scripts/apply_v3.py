#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def patch(path, fn):
    p=ROOT/path
    old=p.read_text()
    new=fn(old)
    if new!=old:p.write_text(new)

# Wire the functional catalog directly instead of the old placeholder injector.
patch(Path('src/main/java/dev/forgeclient/core/ModuleCatalog.java'),lambda s:s.replace('LunarParity.addMissing(r);','LunarFunctionalModules.add(r);'))
patch(Path('src/main/java/dev/forgeclient/core/LunarFunctionalModules.java'),lambda s:s.replace('roadmap/PORTING entries','roadmap placeholder entries'))

# Register the runtime exactly once and bump the mod version.
def forge(s):
    s=s.replace('VERSION="0.2.0-alpha"','VERSION="0.3.0-alpha"')
    needle='MinecraftForge.EVENT_BUS.register(new ClientEvents(this));'
    if 'new LunarRuntime(this)' not in s:s=s.replace(needle,needle+'\n        MinecraftForge.EVENT_BUS.register(new LunarRuntime(this));')
    return s
patch(Path('src/main/java/dev/forgeclient/minecraft/ForgeClient.java'),forge)
patch(Path('build.gradle.kts'),lambda s:s.replace('version = "0.2.0-alpha"','version = "0.3.0-alpha"'))

# Remove the obsolete placeholder label from the shared live UI and keep its displayed version current.
def overlay(s):
    s=s.replace('!m.available()?"PORTING":(m.enabled()?"ENABLED":"DISABLED")','!m.available()?"UNAVAILABLE":(m.enabled()?"ENABLED":"DISABLED")')
    s=s.replace('FORGE  /  0.1.0-ALPHA','FORGE  /  0.3.0-ALPHA')
    return s
patch(Path('src/main/java/dev/forgeclient/ui/OverlayView.java'),overlay)

# Upgrade the existing dependency-free test suite without removing its prior checks.
def tests(s):
    s=s.replace('eq(43,r.all().size());eq(8,r.enabledCount());','eq(89,r.all().size());eq(8,r.enabledCount());')
    s=s.replace('eq(23,categories[0]);eq(10,categories[1]);eq(6,categories[2]);eq(4,categories[3]);','eq(50,categories[0]);eq(22,categories[1]);eq(6,categories[2]);eq(11,categories[3]);')
    s=s.replace('"43 modules, unique ids, category sizes and conservative defaults"','"89 implemented modules, unique ids, category sizes and conservative defaults"')
    # Replace the v0.2 placeholder-parity test if present.
    idx=s.find('test("Lunar')
    if idx!=-1:
        start=s.rfind('        ',0,idx)
        nxt=s.find('\n        test("',idx+6)
        if nxt!=-1:s=s[:start]+s[nxt+1:]
    anchor='        test("Numeric validation, quantization, clamping and non-finite rejection",()->{'
    block='''        test("Lunar-inspired catalog contains only concrete toggleable 1.8.9 entries",()->{\n            ModuleRegistry r=ModuleCatalog.create();eq(89,r.all().size());\n            for(ClientModule m:r.all()){\n                ok(m.available(),"unavailable module leaked into live catalog: "+m.name);\n                ok(!m.summary.toUpperCase(Locale.ROOT).contains("PORTING"),"placeholder summary leaked: "+m.name);\n                ok(!m.description.toUpperCase(Locale.ROOT).contains("PORTING"),"placeholder description leaked: "+m.name);\n            }\n            String[] implemented={"Replay Mod","Hypixel Mods","Hypixel Bedwars","Quickplay","Attack Indicator","Potion Counter","Scoreboard","Chat","Tab Editor","Cooldowns","WorldEdit CUI","Stopwatch","Combo Counter","Time Changer","Item Physics","TNT Countdown","Item Tracker","Momentum","Screenshot","Fog","Boss Bar","PvP Info","Markers","Team View","Minimap","Hitbox","Weather Changer","Chunk Borders","WAILA","Hurt Cam","Tier Tagger","SkyBlock","Horse Stats","Overlay Mod","Rewind","Action Bar","Light Overlay","Kill Sounds","Inventory Mod","F3 Display","GUI Scale","Knockback Trainer","UHC Overlay","NotEnoughUpdates","SkyBlockAddons"};\n            for(String name:implemented)ok(LunarParity.containsName(r,name),"missing implemented Lunar baseline feature: "+name);\n            Set<String> missing=LunarParity.missingNames(r);\n            ok(missing.contains("Shulker Preview")&&missing.contains("Shields")&&missing.contains("Totem Counter"),"cross-version features must not be fake 1.8.9 toggles");\n        });\n'''
    if 'Lunar-inspired catalog contains only concrete' not in s:
        if anchor not in s:raise SystemExit('Could not locate test insertion anchor')
        s=s.replace(anchor,block+anchor,1)
    return s
patch(Path('src/test/java/dev/forgeclient/tests/AllTests.java'),tests)

# Strong source gate: PORTING text may exist in old authoring history/scripts, never in live src/main.
for p in (ROOT/'src/main').rglob('*'):
    if p.is_file() and p.suffix in {'.java','.md','.info'} and 'PORTING' in p.read_text(errors='ignore'):
        raise SystemExit('Live placeholder text remains in '+str(p.relative_to(ROOT)))
