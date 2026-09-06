package dev.forgeclient.tests;

import dev.forgeclient.core.*;
import dev.forgeclient.ui.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free integration/property tests. NOT a replacement for a real Forge build or game test. */
public final class AllTests {
    private interface Check { void run()throws Exception; }
    private static int assertions,passed,failed;
    private static final List<String> results=new ArrayList<>();
    private static void test(String name,Check check){
        int start=assertions;
        try{check.run();passed++;results.add("{\"name\":\""+escape(name)+"\",\"passed\":true,\"assertions\":"+(assertions-start)+"}");System.out.println("PASS  "+name);}
        catch(Throwable e){failed++;results.add("{\"name\":\""+escape(name)+"\",\"passed\":false,\"error\":\""+escape(e.toString())+"\"}");System.err.println("FAIL  "+name);e.printStackTrace();}
    }
    private static void ok(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    private static void eq(Object expected,Object actual){ok(Objects.equals(expected,actual),"expected <"+expected+">; got <"+actual+">");}
    private static void near(double expected,double actual){ok(Math.abs(expected-actual)<1e-8,"expected "+expected+"; got "+actual);}
    private static void throwsType(Class<? extends Throwable> type,Check check)throws Exception{
        try{check.run();}catch(Throwable e){ok(type.isInstance(e),"wrong exception: "+e);return;}throw new AssertionError("Expected "+type.getName());
    }
    private static Properties values(String value){Properties p=new Properties();p.setProperty("schema","1");p.setProperty("test",value);return p;}
    public static void main(String[] args)throws Exception {
        long started=System.nanoTime();
        test("43 modules, unique ids, category sizes and conservative defaults",()->{
            ModuleRegistry r=ModuleCatalog.create();eq(43,r.all().size());eq(8,r.enabledCount());
            Set<String> ids=new HashSet<>();int[] categories=new int[4];
            for(ClientModule m:r.all()){
                ok(ids.add(m.id),"duplicate module");categories[m.category.ordinal()]++;
                Set<String> settings=new HashSet<>();for(Setting s:m.settings()){ok(settings.add(s.id),"duplicate setting");ok(s.set(s.raw()),"default must validate");}
                if(m.hud){eq(Setting.Kind.NUMBER,m.setting("scale").kind);ok(m.placement.x()>=0&&m.placement.x()<=1,"normalized x");}
            }
            eq(23,categories[0]);eq(10,categories[1]);eq(6,categories[2]);eq(4,categories[3]);
            ok(!r.enabled("fullbright")&&!r.enabled("no_fire")&&!r.enabled("distance_culling")&&!r.enabled("toggle_sprint"),"sensitive modules start off");
            throwsType(IllegalArgumentException.class,()->r.add(r.get("fps")));
        });
        test("Numeric validation, quantization, clamping and non-finite rejection",()->{
            Setting s=Setting.number("scale","Scale","",1,.5,2,.05);
            ok(s.set("1.234"),"valid input");near(1.25,s.number());s.set("100");near(2,s.number());s.set("-10");near(.5,s.number());
            for(String bad:new String[]{"NaN","Infinity","-Infinity","not a number",""})ok(!s.set(bad),"reject "+bad);
            near(.5,s.number());ok(!s.set(null),"reject null");s.fraction(Double.NaN);near(.5,s.number());s.fraction(.5);near(1.25,s.number());
            s.reset();near(1,s.number());eq("1",s.display());
            throwsType(IllegalArgumentException.class,()->Setting.number("n","N","",0,1,0,1));
            throwsType(IllegalArgumentException.class,()->Setting.number("n","N","",1,0,2,0));
        });
        test("Boolean and choice validation, reverse cycle, reset",()->{
            Setting b=Setting.bool("b","B","",true);ok(!b.set("yes"),"no coercion");ok(!b.set("TRUE"),"strict format");b.toggle();ok(!b.bool(),"toggle");b.reset();ok(b.bool(),"reset");
            Setting c=Setting.choice("c","C","","One","One","Two","Three");c.cycle(-1);eq("Three",c.raw());c.cycle(1);eq("One",c.raw());ok(!c.set("Other"),"reject unknown option");
            throwsType(IllegalArgumentException.class,()->Setting.choice("c","C","","Other","One"));
        });
        test("Module revision changes only for mutations",()->{
            ModuleRegistry r=ModuleCatalog.create();long n=r.revision();r.get("fps").enabled(true);eq(n,r.revision());r.get("fps").toggle();eq(n+1,r.revision());
            r.get("fps").setting("scale").set("1.2");eq(n+2,r.revision());r.get("fps").placement.set(.5,.5);eq(n+3,r.revision());
        });
        test("Bindings reserve Escape/Right Shift, ignore GUI and do not toggle hold modules",()->{
            ModuleRegistry r=ModuleCatalog.create();ClientModule fps=r.get("fps"),zoom=r.get("zoom");
            ok(!fps.bind(1)&&!fps.bind(54)&&!fps.bind(-1)&&!fps.bind(256),"reserved/bounds");ok(fps.bind(33),"letter binding");
            r.onKey(33,true);ok(fps.enabled(),"typing cannot toggle");r.onKey(33,false);ok(!fps.enabled(),"edge toggles");
            r.onKey(46,false);ok(zoom.enabled(),"hold stays enabled");ok(r.isKeyUsed(33,zoom),"conflict detection");ok(!r.isKeyUsed(0,zoom),"unbound not conflict");
        });
        test("Preset application and invalid preset transaction",()->{
            ModuleRegistry r=ModuleCatalog.create();r.preset("minimal");eq(3,r.enabledCount());Properties before=ConfigCodec.encode(r);
            throwsType(IllegalArgumentException.class,()->r.preset("invalid"));eq(before,ConfigCodec.encode(r));
            r.preset("explorer");ok(r.enabled("coordinates")&&r.enabled("waypoint"),"explorer modules");r.preset("pvp");ok(r.enabled("hit_distance"),"pvp readout");r.preset("default");eq(8,r.enabledCount());
        });
        test("HUD positions remain inside resized screens; NaN does not poison coordinates",()->{
            HudPlacement p=new HudPlacement(.75,.25);eq(675,p.pixelX(1000,100));eq(100,p.pixelY(500,100));
            p.move(999,-10,800,600,100,100);near(1,p.x());near(0,p.y());p.set(Double.NaN,1);near(1,p.x());near(0,p.y());
            p.move(100,100,50,50,100,100);eq(0,p.pixelX(50,100));eq(0,p.pixelY(50,100));p.reset();near(.75,p.x());near(.25,p.y());
        });
        test("Config round trips all metadata and settings",()->{
            ModuleRegistry r=ModuleCatalog.create();r.get("fps").enabled(false);r.get("cps").favorite(true);r.get("cps").bind(44);r.get("armor").setting("layout").set("Horizontal");r.get("fps").placement.set(.25,.89);
            Properties p=ConfigCodec.encode(r);ModuleRegistry copy=ModuleCatalog.create();ConfigCodec.decode(p,copy);eq(p,ConfigCodec.encode(copy));
        });
        test("Config envelope rejects future schema before mutating state",()->{
            ModuleRegistry r=ModuleCatalog.create();r.preset("minimal");Properties before=ConfigCodec.encode(r),bad=values("x");bad.setProperty("schema","2");
            throwsType(IllegalArgumentException.class,()->ConfigCodec.decode(bad,r));eq(before,ConfigCodec.encode(r));
        });
        test("Config ignores malformed individual values and unknown modules",()->{
            ModuleRegistry r=ModuleCatalog.create();Properties p=values("x");p.setProperty("module.fps.enabled","yes");p.setProperty("module.fps.key","54");p.setProperty("module.fps.x","NaN");p.setProperty("module.fps.setting.scale","Infinity");p.setProperty("module.unknown.enabled","true");
            ConfigCodec.decode(p,r);ok(r.enabled("fps"),"default enabled");eq(0,r.get("fps").key());near(.012,r.get("fps").placement.x());near(1,r.get("fps").setting("scale").number());
        });
        test("Seeded configuration property test: 100 complete round trips",()->{
            Random random=new Random(20260906);
            for(int trial=0;trial<100;trial++){
                ModuleRegistry r=ModuleCatalog.create();
                for(ClientModule m:r.all()){
                    m.enabled(random.nextBoolean());m.favorite(random.nextBoolean());m.placement.set(random.nextDouble(),random.nextDouble());
                    for(Setting s:m.settings())if(s.kind==Setting.Kind.BOOLEAN)s.set(Boolean.toString(random.nextBoolean()));else if(s.kind==Setting.Kind.NUMBER)s.fraction(random.nextDouble());else s.cycle(random.nextInt(10));
                }
                Properties p=ConfigCodec.encode(r);ModuleRegistry copy=ModuleCatalog.create();ConfigCodec.decode(p,copy);eq(p,ConfigCodec.encode(copy));
            }
        });
        test("Profile names reject traversal, absolute paths and Windows devices",()->{
            for(String bad:new String[]{"../x","a/b","a\\b","/tmp/x",".","..","","UPPER","con","nul","com1","lpt9",String.join("",Collections.nCopies(33,"x"))})ok(!ProfileStore.validName(bad),"rejected "+bad);
            for(String good:new String[]{"default","pvp-2","new_profile","a","0"})ok(ProfileStore.validName(good),"accepted "+good);
            Path dir=Files.createTempDirectory("forge-profile-");ProfileStore store=new ProfileStore(dir);throwsType(IOException.class,()->store.save("../escape",values("a")));
        });
        test("Atomic profile save, enumeration, last-good backup and corrupt-file preservation",()->{
            Path dir=Files.createTempDirectory("forge-profile-");ProfileStore store=new ProfileStore(dir);store.save("pvp",values("one"));store.save("pvp",values("two"));
            eq("two",store.load("pvp").getProperty("test"));eq("one",store.loadBackup("pvp").getProperty("test"));eq(Collections.singletonList("pvp"),store.names());
            Files.write(dir.resolve("pvp.properties"),"schema=1\nbad=\\uZZZZ\n".getBytes(StandardCharsets.UTF_8));
            throwsType(IOException.class,()->store.load("pvp"));store.save("pvp",values("three"));
            ok(Files.exists(dir.resolve("pvp.properties.damaged")),"corrupt original retained");eq("one",store.loadBackup("pvp").getProperty("test"));eq("three",store.load("pvp").getProperty("test"));
            try(java.util.stream.Stream<Path> files=Files.list(dir)){ok(files.noneMatch(x->x.toString().endsWith(".tmp")),"temporary files cleaned");}
        });
        test("Oversized and unsupported profiles fail without replacing good data",()->{
            Path dir=Files.createTempDirectory("forge-profile-");ProfileStore store=new ProfileStore(dir);store.save("good",values("saved"));
            Properties big=values(String.join("",Collections.nCopies(270000,"x")));throwsType(IOException.class,()->store.save("good",big));eq("saved",store.load("good").getProperty("test"));
            Files.write(dir.resolve("huge.properties"),new byte[270000]);throwsType(IOException.class,()->store.load("huge"));
            Properties future=values("x");future.setProperty("schema","2");throwsType(IOException.class,()->store.save("good",future));
        });
        test("Profile and backup symlinks are rejected",()->{
            Path dir=Files.createTempDirectory("forge-symlink-"),outside=Files.createTempFile("forge-external-",".txt");Files.write(outside,"unchanged".getBytes(StandardCharsets.UTF_8));
            ProfileStore store=new ProfileStore(dir);Files.createSymbolicLink(dir.resolve("link.properties"),outside);
            throwsType(IOException.class,()->store.save("link",values("x")));throwsType(IOException.class,()->store.load("link"));
            store.save("safe",values("a"));Files.createSymbolicLink(dir.resolve("safe.properties.bak"),outside);
            throwsType(IOException.class,()->store.save("safe",values("b")));eq("unchanged",new String(Files.readAllBytes(outside),StandardCharsets.UTF_8));
            eq("a",store.load("safe").getProperty("test"));
        });
        test("Single-writer debounce copies snapshots and explicit flush orders profile switches",()->{
            ProfileStore store=new ProfileStore(Files.createTempDirectory("forge-queue-"));AtomicInteger errors=new AtomicInteger();
            try(SaveQueue q=new SaveQueue(store,e->errors.incrementAndGet())){
                Properties p=values("old");q.submit("one",p);p.setProperty("test","mutated outside");q.flush();eq("old",store.load("one").getProperty("test"));
                for(int i=0;i<30;i++)q.submit("one",values(Integer.toString(i)));q.flush();eq("29",store.load("one").getProperty("test"));
                q.submit("two",values("second"));q.flush();eq("29",store.load("one").getProperty("test"));eq("second",store.load("two").getProperty("test"));
            }eq(0,errors.get());
        });
        test("Scheduled save runs without explicit flush; shutdown persists last change",()->{
            ProfileStore store=new ProfileStore(Files.createTempDirectory("forge-queue-"));AtomicInteger errors=new AtomicInteger();SaveQueue q=new SaveQueue(store,e->errors.incrementAndGet());
            q.submit("scheduled",values("first"));long deadline=System.nanoTime()+2_000_000_000L;
            while(!store.exists("scheduled")&&System.nanoTime()<deadline)Thread.sleep(20);
            eq("first",store.load("scheduled").getProperty("test"));q.submit("last",values("last"));q.close();eq("last",store.load("last").getProperty("test"));eq(0,errors.get());q.close();
        });
        test("Save failure is observable rather than reported as success",()->{
            ProfileStore store=new ProfileStore(Files.createTempDirectory("forge-queue-"));try(SaveQueue q=new SaveQueue(store,e->{})){
                q.submit("../unsafe",values("x"));throwsType(IOException.class,q::flush);
            }
        });
        test("CPS counts multiple events per tick, expires at one second, caps memory",()->{
            SlidingClickCounter c=new SlidingClickCounter(4);c.click(1);c.click(2);c.click(3);eq(3,c.count(5));eq(2,c.count(1_000_000_001L));
            c.clear();for(int i=0;i<100;i++)c.click(i);eq(4,c.count(100));eq(0,c.count(2_000_000_000L));
            throwsType(IllegalArgumentException.class,()->new SlidingClickCounter(0));
        });
        test("CPS randomized reference-model comparison and nanosecond wrap",()->{
            SlidingClickCounter c=new SlidingClickCounter(8);Deque<Long> model=new ArrayDeque<>();Random random=new Random(89);long now=0;
            for(int i=0;i<10000;i++){
                now+=random.nextInt(80_000_000);while(!model.isEmpty()&&now-model.peekFirst()>=1_000_000_000L)model.removeFirst();
                if(random.nextBoolean()){c.click(now);if(model.size()==8)model.removeFirst();model.addLast(now);}eq(model.size(),c.count(now));
            }
            c.clear();long start=Long.MAX_VALUE-10;c.click(start);eq(1,c.count(start+100));eq(0,c.count(start+1_000_000_000L));
        });
        test("Frame history bounds, chronological order, mean and p99",()->{
            FrameHistory f=new FrameHistory(4);for(int i=1;i<=5;i++)f.add(i);eq(4,f.size());near(2,f.sample(0));near(5,f.sample(3));near(3.5,f.mean());near(5,f.percentile(.99));near(2,f.percentile(0));
            f.add(Double.NaN);f.add(-1);f.add(10001);eq(4,f.size());throwsType(IndexOutOfBoundsException.class,()->f.sample(4));f.clear();eq(0,f.size());near(0,f.mean());near(0,f.percentile(.99));
            f.frame(1);f.frame(1_000_001);near(1,f.sample(0));
        });
        test("Option leases restore original values without redundant writes",()->{
            Integer[] state={3};AtomicInteger writes=new AtomicInteger();SettingLease<Integer> lease=new SettingLease<>(()->state[0],v->{state[0]=v;writes.incrementAndGet();});
            lease.apply(1);eq(1,state[0]);lease.apply(1);eq(1,writes.get());lease.apply(2);eq(2,state[0]);lease.release();eq(3,state[0]);ok(!lease.owned(),"released");lease.release();eq(3,writes.get());
        });
        test("Option leases preserve newer user/mod values on update and release",()->{
            Integer[] state={3};SettingLease<Integer> lease=new SettingLease<>(()->state[0],v->state[0]=v);
            lease.apply(1);state[0]=7;lease.apply(1);lease.release();eq(7,state[0]);lease.apply(1);state[0]=9;lease.release();eq(9,state[0]);
        });
        test("Waypoints isolate worlds/dimensions, select nearest, reject duplicates",()->{
            WaypointStore s=new WaypointStore();s.add(new Waypoint("Home","a",0,10,64,0));s.add(new Waypoint("Far","a",0,100,64,0));s.add(new Waypoint("Nether","a",-1,1,64,0));s.add(new Waypoint("Other","b",0,1,64,0));
            eq("Home",s.nearest("a",0,0,64,0).name);eq("Nether",s.nearest("a",-1,0,64,0).name);eq(null,s.nearest("c",0,0,0,0));
            throwsType(IllegalArgumentException.class,()->s.add(new Waypoint("home","a",0,1,2,3)));
            ok(!s.remove("Home","b",0),"cannot remove another world");ok(s.remove("HOME","a",0),"case-insensitive remove");eq("Far",s.nearest("a",0,0,64,0).name);
        });
        test("Waypoint limits, coordinates and transactional decoding",()->{
            for(String bad:new String[]{"", "   ","../home","!"})throwsType(IllegalArgumentException.class,()->new Waypoint(bad,"a",0,0,64,0));
            throwsType(IllegalArgumentException.class,()->new Waypoint("Home","a",0,Double.NaN,64,0));throwsType(IllegalArgumentException.class,()->new Waypoint("Home","a",0,30000001,64,0));
            WaypointStore s=new WaypointStore();for(int i=0;i<64;i++)s.add(new Waypoint("p"+i,"a",0,i,64,0));throwsType(IllegalStateException.class,()->s.add(new Waypoint("excess","a",0,0,0,0)));
            Properties before=s.encode(),bad=s.encode();bad.setProperty("point.32.x","NaN");throwsType(IOException.class,()->s.decode(bad));eq(before,s.encode());
            WaypointStore copy=new WaypointStore();copy.decode(before);eq(before,copy.encode());
        });
        test("Chat filter only removes consecutive repeats within the accepted-line window",()->{
            ChatDeduplicator d=new ChatDeduplicator();ok(!d.suppress("a",0,100),"first");ok(d.suppress("a",50,100),"repeat");ok(!d.suppress("a",100,100),"window expiry");ok(!d.suppress("b",101,100),"different");ok(!d.suppress("a",102,100),"not consecutive");d.clear();ok(!d.suppress("a",103,100),"reset");
        });
        test("Smart FPS never imposes a focused gameplay cap",()->{
            eq(0,ClientPolicies.frameCap(true,false,15,60,144));eq(15,ClientPolicies.frameCap(false,false,15,60,144));eq(60,ClientPolicies.frameCap(true,true,15,60,144));eq(30,ClientPolicies.frameCap(true,true,15,60,30));
        });
        test("Sprint intent respects hunger, collisions, menus, focus and vanilla constraints",()->{
            ok(ClientPolicies.maySprint(true,false,true,1,false,false,false,false,20,false),"normal sprint");
            ok(!ClientPolicies.maySprint(false,false,true,1,false,false,false,false,20,false),"focus");ok(!ClientPolicies.maySprint(true,true,true,1,false,false,false,false,20,false),"menu");
            ok(!ClientPolicies.maySprint(true,false,false,1,false,false,false,false,20,false),"world");ok(!ClientPolicies.maySprint(true,false,true,.5f,false,false,false,false,20,false),"forward");
            ok(!ClientPolicies.maySprint(true,false,true,1,true,false,false,false,20,false),"sneak");ok(!ClientPolicies.maySprint(true,false,true,1,false,true,false,false,20,false),"item");
            ok(!ClientPolicies.maySprint(true,false,true,1,false,false,true,false,20,false),"collision");ok(!ClientPolicies.maySprint(true,false,true,1,false,false,false,true,20,false),"blindness");
            ok(!ClientPolicies.maySprint(true,false,true,1,false,false,false,false,6,false),"hunger");ok(ClientPolicies.maySprint(true,false,true,1,false,false,false,false,0,true),"creative capability");
        });
        uiTests();
        long millis=(System.nanoTime()-started)/1_000_000L;
        String json="{\n  \"scope\": \"Core, shared UI, persistence and policies; not Minecraft runtime\",\n  \"minecraftRuntimeTested\": false,\n  \"java\": \""+escape(System.getProperty("java.version"))+"\",\n  \"groupsPassed\": "+passed+",\n  \"groupsFailed\": "+failed+",\n  \"assertions\": "+assertions+",\n  \"elapsedMillis\": "+millis+",\n  \"groups\": [\n    "+String.join(",\n    ",results)+"\n  ]\n}\n";
        Path output=Paths.get(args.length>0?args[0]:"build/reports/core-tests.json");if(output.getParent()!=null)Files.createDirectories(output.getParent());Files.write(output,json.getBytes(StandardCharsets.UTF_8));
        System.out.println("\n"+passed+" groups passed, "+failed+" failed; "+assertions+" assertions in "+millis+" ms.");
        System.out.println("Not a Forge compile, Minecraft launch, multiplayer compatibility test or FPS benchmark.");
        if(failed>0)System.exit(1);
    }
    private static void uiTests(){
        test("UI render passes balance transform/clip stacks at five effective resolutions",()->{
            int[][] sizes={{960,540},{640,480},{1280,720},{1920,540},{800,600}};
            for(int[] size:sizes)for(OverlayView.Tab tab:OverlayView.Tab.values()){
                Ui ui=new Ui(size[0],size[1]);ui.view.setTab(tab);ui.draw();eq(0,ui.canvas.depth);eq(0,ui.canvas.clips);
                for(OverlayView.Hit hit:ui.view.hitAreas())ok(hit.bounds.x>=0&&hit.bounds.y>=0&&hit.bounds.right()<=size[0]&&hit.bounds.bottom()<=size[1]&&!hit.bounds.empty(),"visible hit rectangle: "+hit.id);
            }
            near(.5,UiLayout.scaleFor(320,240));near(1,UiLayout.scaleFor(1920,1080));
        });
        test("Module card, independent toggle, favorite and category filter interactions",()->{
            Ui ui=new Ui(960,540);ui.draw();ui.click("toggle:fps");ok(!ui.registry.enabled("fps"),"toggle");
            ui.click("module:fps");eq("fps",ui.view.selection());ui.click("favorite");ok(ui.registry.get("fps").favorite(),"favorite");ui.click("filter:favorites");
            ok(ui.has("module:fps")&&!ui.has("module:cps"),"filtered favorites");ui.click("filter:PERFORMANCE");ok(!ui.has("module:fps")&&ui.has("module:smart_fps"),"category filter");
            ui.click("module:smart_fps");eq("smart_fps",ui.view.selection());
        });
        test("Search supports case-insensitive matches, empty state and clearing",()->{
            Ui ui=new Ui(960,540);ui.draw();ui.click("search");ui.type("ZoOm");ui.draw();ok(ui.has("module:zoom"),"zoom found");ok(!ui.has("module:fps"),"unrelated hidden");
            ui.click("clear-search");eq("",ui.view.query());ui.click("search");ui.type("not-a-module");ui.draw();ok(ui.view.hitAreas().stream().noneMatch(h->h.id.startsWith("module:")),"empty result");ui.view.key('a',30,true,false);ui.draw();eq("",ui.view.query());
        });
        test("UI binding capture rejects conflicts/reserved keys and supports clearing",()->{
            Ui ui=new Ui(960,540);ui.draw();ui.click("bind");ok(ui.view.capturingBinding(),"capture");ui.view.key('c',46,false,false);ok(ui.view.capturingBinding(),"zoom conflict");
            ui.view.key('\0',54,false,false);ok(ui.view.capturingBinding(),"reserved right shift");ui.view.key('f',33,false,false);ok(!ui.view.capturingBinding(),"bound");eq(33,ui.registry.get("keystrokes").key());
            ui.draw();ui.click("bind");ui.view.key('\0',211,false,false);eq(0,ui.registry.get("keystrokes").key());
            ui.draw();ui.click("bind");ui.view.key('\0',1,false,false);ok(!ui.view.capturingBinding(),"escape cancels capture");
        });
        test("Inspector slider dragging clamps and numeric settings persist",()->{
            Ui ui=new Ui(960,540);ui.draw();ui.click("filter:VISUAL");ui.click("module:zoom");
            OverlayView.Hit slider=ui.hit("setting:factor");ui.view.mouseDown(slider.bounds.x+slider.bounds.width/2,slider.bounds.y+4,0);ui.view.drag(100000,slider.bounds.y,false);ui.view.mouseUp();near(10,ui.registry.get("zoom").setting("factor").number());
            ui.draw();slider=ui.hit("setting:factor");ui.view.mouseDown(slider.bounds.x+2,slider.bounds.y+4,0);ui.view.drag(-1000,slider.bounds.y,false);ui.view.mouseUp();near(2,ui.registry.get("zoom").setting("factor").number());
        });
        test("Scrolling clips hidden controls and keeps inspector independent",()->{
            Ui ui=new Ui(960,540);ui.draw();ui.view.wheel(-200,350,400);ui.draw();ui.draw();ok(ui.view.scroll()>0,"list scroll");
            ok(!ui.has("toggle:fps"),"offscreen toggle not clickable");int before=ui.view.scroll();ui.view.wheel(-2,850,400);ui.draw();eq(before,ui.view.scroll());
            ui.view.wheel(200,350,400);ui.draw();ui.draw();eq(0,ui.view.scroll());ok(ui.has("toggle:fps"),"scroll back");
        });
        test("HUD editor can reach top-edge widgets and drag/scale at normalized positions",()->{
            Ui ui=new Ui(960,540);ui.view.setTab(OverlayView.Tab.HUD);ui.draw();OverlayView.Hit h=ui.hit("hud:fps");ok(h.bounds.y<52,"top widget is not covered by header");
            ui.view.mouseDown(h.bounds.x+2,h.bounds.y+2,0);ui.view.drag(400,300,true);ui.view.mouseUp();ui.draw();h=ui.hit("hud:fps");eq(398,h.bounds.x);eq(298,h.bounds.y);
            double x=ui.registry.get("fps").placement.x();ui.width=1280;ui.height=720;ui.draw();near(x,ui.registry.get("fps").placement.x());
            h=ui.hit("hud:fps");ui.view.wheel(1,h.bounds.x+2,h.bounds.y+2);near(1.05,ui.registry.get("fps").setting("scale").number());
            ui.draw();ui.view.key('\0',205,false,true);ui.draw();ok(ui.hit("hud:fps").bounds.right()<=1280,"keyboard nudge remains bounded");
        });
        test("HUD reset, edit-mode return and explicit close behavior",()->{
            Ui ui=new Ui(960,540);ui.view.setTab(OverlayView.Tab.HUD);ui.registry.get("fps").placement.set(.8,.8);ui.draw();ui.click("hud-reset");near(.012,ui.registry.get("fps").placement.x());
            ui.click("hud-done");eq(OverlayView.Tab.MODULES,ui.view.tab());ok(!ui.view.key('\0',54,false,false),"right shift delegated to native close");ui.click("close");eq(1,ui.host.closed);
        });
        test("HUD streamer privacy suppresses coordinates/waypoint only in Forge",()->{
            ModuleRegistry r=ModuleCatalog.create();HudRenderer h=new HudRenderer();r.get("streamer_mode").enabled(true);
            ok(h.hidden(r,r.get("coordinates"))&&h.hidden(r,r.get("waypoint")),"hide sensitive widgets");ok(!h.hidden(r,r.get("fps")),"retain fps");
            r.get("streamer_mode").setting("coordinates").set("false");ok(!h.hidden(r,r.get("coordinates")),"configurable");
        });
        test("Profiles apply presets, save/load snapshots and require overwrite confirmation",()->{
            Ui ui=new Ui(960,540);ui.view.setTab(OverlayView.Tab.PROFILES);ui.draw();ui.click("preset:minimal");eq(3,ui.registry.enabledCount());
            ui.click("profile-name");ui.type("minimal-1");ui.draw();ui.click("save-profile");eq("minimal-1",ui.host.active);eq(1,ui.host.saves);
            ui.registry.preset("explorer");ui.draw();ui.click("load-profile:minimal-1");eq(3,ui.registry.enabledCount());
            ui.click("save-profile");eq(1,ui.host.saves);ui.click("save-profile");eq(2,ui.host.saves);
        });
        test("All HUD widget bounds fit effective screen sizes including large scales",()->{
            ModuleRegistry r=ModuleCatalog.create();Telemetry t=PreviewData.create();RecordingCanvas c=new RecordingCanvas();HudRenderer h=new HudRenderer();
            for(int[] size:new int[][]{{640,480},{960,540},{1280,720}})for(ClientModule m:r.all())if(m.hud){
                m.setting("scale").set("2");for(double corner:new double[]{0,.5,1}){m.placement.set(corner,corner);Rect b=h.bounds(c,m,t,size[0],size[1]);ok(b.x>=0&&b.y>=0&&b.right()<=size[0]&&b.bottom()<=size[1],"HUD bounds "+m.id);}
            }
            h.render(c,r,t,960,540);eq(0,c.depth);eq(0,c.clips);
        });
        test("Telemetry world clear removes stale position, item, key and click data",()->{
            Telemetry t=PreviewData.create();t.left.click(10);t.clearWorld();ok(!t.world,"no world");eq(0,t.armor.length);eq(0,t.rows("coordinates").length);eq(0,t.left.count(10));eq(0,t.frames.size());for(boolean key:t.keys)ok(!key,"keys released");
        });
    }
    private static final class RecordingCanvas implements Canvas {
        int depth,clips;
        public void rect(int x,int y,int w,int h,int color){if(w<0||h<0)throw new AssertionError("negative drawing extent");}
        public void text(String value,int x,int y,int color,boolean shadow){if(value==null)throw new AssertionError("null text");}
        public int textWidth(String value){return value.length()*6;}
        public void push(double x,double y,double scale){if(!Double.isFinite(scale)||scale<=0)throw new AssertionError("invalid scale");depth++;}
        public void pop(){if(--depth<0)throw new AssertionError("transform underflow");}
        public void clip(Rect r){clips++;}
        public void unclip(){if(--clips<0)throw new AssertionError("clip underflow");}
        public void item(Telemetry.Item item,int x,int y){}
    }
    private static final class Host implements UiHost {
        final ModuleRegistry registry;final Map<String,Properties> data=new LinkedHashMap<>();String active="default";int saves,closed;
        Host(ModuleRegistry registry){this.registry=registry;data.put("default",ConfigCodec.encode(registry));}
        public String keyName(int code){return code==0?"NONE":Integer.toString(code);}
        public List<String> profiles(){return new ArrayList<>(data.keySet());}
        public String activeProfile(){return active;}
        public void saveProfile(String name){data.put(name,ConfigCodec.encode(registry));active=name;saves++;}
        public void loadProfile(String name){ConfigCodec.decode(data.get(name),registry);active=name;}
        public void closeScreen(){closed++;}
        public void message(String text){}
    }
    private static final class Ui {
        final ModuleRegistry registry=ModuleCatalog.create();final Host host=new Host(registry);final RecordingCanvas canvas=new RecordingCanvas();
        final OverlayView view=new OverlayView(registry,PreviewData.create(),host);int width,height;
        Ui(int width,int height){this.width=width;this.height=height;}
        void draw(){view.draw(canvas,width,height,-1,-1);}
        boolean has(String id){for(OverlayView.Hit h:view.hitAreas())if(id.equals(h.id))return true;return false;}
        OverlayView.Hit hit(String id){for(OverlayView.Hit h:view.hitAreas())if(id.equals(h.id))return h;throw new AssertionError("Hit not found: "+id);}
        void click(String id){OverlayView.Hit h=hit(id);view.mouseDown(h.bounds.x+Math.max(1,h.bounds.width/2),h.bounds.y+Math.max(1,h.bounds.height/2),0);view.mouseUp();draw();}
        void type(String text){for(char c:text.toCharArray())view.key(c,0,false,false);}
    }
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
}
