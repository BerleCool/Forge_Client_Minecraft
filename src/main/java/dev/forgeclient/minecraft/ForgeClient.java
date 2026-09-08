package dev.forgeclient.minecraft;

import dev.forgeclient.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Mod(modid=ForgeClient.MOD_ID,name="Forge Client",version=ForgeClient.VERSION,
        acceptedMinecraftVersions="[1.8.9]",clientSideOnly=true,acceptableRemoteVersions="*",
        guiFactory="dev.forgeclient.minecraft.ForgeGuiFactory")
public final class ForgeClient {
    public static final String MOD_ID="forgeclient",VERSION="0.4.1-alpha";
    @Mod.Instance(MOD_ID) private static ForgeClient INSTANCE;
    public static ForgeClient instance(){if(INSTANCE==null)throw new IllegalStateException("Forge Client is not initialized");return INSTANCE;}
    public final ModuleRegistry modules=ModuleCatalog.create();
    public final Telemetry telemetry=new Telemetry();
    public final WaypointStore waypoints=new WaypointStore();
    public final KeyBinding openKey=new KeyBinding("key.forgeclient.open",Keyboard.KEY_RSHIFT,"key.categories.forgeclient");
    public TelemetrySampler sampler;
    private Logger log;
    private ProfileStore profiles,data;
    private SaveQueue saves;
    private String active="default";
    private List<String> names=Collections.singletonList("default");
    private long savedRevision;private int savePoll;
    private volatile String saveError;
    private boolean openRequested;

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        INSTANCE=this;log=event.getModLog();
        try {
            Path root=event.getModConfigurationDirectory().toPath().resolve("forgeclient");
            profiles=new ProfileStore(root.resolve("profiles"));data=new ProfileStore(root.resolve("data"));
            if(data.exists("workspace")) {
                try {
                    Properties workspace=recover(data,"workspace");String name=workspace.getProperty("active","default");
                    if(ProfileStore.validName(name))active=name;
                } catch(IOException error){log.warn("Active-profile selector is unreadable; using default",error);}
            }
            if(profiles.exists(active)) {
                try{ConfigCodec.decode(recover(profiles,active),modules);}
                catch(IOException|IllegalArgumentException error){
                    log.warn("Keeping unreadable profile intact; using a new recovery profile",error);
                    active="recovered-"+System.currentTimeMillis();
                }
            }
            if(data.exists("waypoints")) {
                try{waypoints.decode(recover(data,"waypoints"));}
                catch(IOException error){log.warn("Waypoint file was not loaded; original remains on disk",error);}
            }
            saves=new SaveQueue(profiles,error->{log.warn("Configuration write failed",error);saveError="Config save failed: "+error.getMessage();});
            refreshProfiles();savedRevision=modules.revision();
            if(!profiles.exists(active))snapshot();
            rememberActive();
            Runtime.getRuntime().addShutdownHook(new Thread(()->saves.close(),"Forge-Config-Shutdown"));
        } catch(IOException error){
            log.error("Configuration is unavailable; Forge Client remains usable in memory",error);
            saveError="Configuration unavailable. Changes are not persistent.";
        }
    }
    private Properties recover(ProfileStore store,String name)throws IOException {
        try{return store.load(name);}
        catch(IOException original){
            try{Properties backup=store.loadBackup(name);log.warn("Using last-good backup for {}",name);return backup;}
            catch(IOException ignored){throw original;}
        }
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        sampler=new TelemetrySampler(this);ClientRegistry.registerKeyBinding(openKey);
        MinecraftForge.EVENT_BUS.register(new ClientEvents(this));
        MinecraftForge.EVENT_BUS.register(new LunarRuntime(this));
        ClientCommandHandler.instance.registerCommand(new ForgeCommand(this));
        log.info("Forge Client {} initialized with {} module entries ({} currently native). No telemetry or remote services.",VERSION,modules.all().size(),modules.availableCount());
        log.info("OptiFine {}. Forge Client never redistributes OptiFine; a user-installed copy is detected and left compatible.",OptiFineCompatibility.present()?"detected":"not detected");
    }
    public void snapshotIfChanged() {
        if(++savePoll>=5){savePoll=0;if(modules.revision()!=savedRevision)snapshot();}
        if(saveError!=null){String error=saveError;saveError=null;message(error);}
        if(openRequested&&Minecraft.getMinecraft().currentScreen==null){openRequested=false;Minecraft.getMinecraft().displayGuiScreen(new ForgeScreen(null));}
    }
    public void requestOpen(){openRequested=true;}
    public void snapshot() {
        if(saves!=null){saves.submit(active,ConfigCodec.encode(modules));savedRevision=modules.revision();}
    }
    public String activeProfile(){return active;}
    public List<String> profileNames(){return names;}
    public void refreshProfiles() {
        if(profiles==null)return;
        try{
            List<String> next=new ArrayList<>(profiles.names());if(!next.contains(active))next.add(active);Collections.sort(next);
            names=Collections.unmodifiableList(next);
        }catch(IOException error){log.warn("Could not refresh profiles",error);}
    }
    public void saveProfile(String name) {
        if(profiles==null||saves==null){message("Profile storage is unavailable.");return;}
        try {
            snapshot();saves.flush();profiles.save(name,ConfigCodec.encode(modules));active=name;
            savedRevision=modules.revision();refreshProfiles();
            message("Saved profile '"+name+"'"+(rememberActive()?".":"; active selection could not be saved."));
        }catch(IOException error){log.warn("Profile save failed",error);message("Could not save profile: "+error.getMessage());}
    }
    public void loadProfile(String name) {
        if(profiles==null||saves==null){message("Profile storage is unavailable.");return;}
        try {
            snapshot();saves.flush();Properties snapshot=recover(profiles,name);ConfigCodec.decode(snapshot,modules);
            active=name;savedRevision=modules.revision();refreshProfiles();
            message("Loaded profile '"+name+"'"+(rememberActive()?".":"; active selection could not be saved."));
        }catch(IOException|IllegalArgumentException error){log.warn("Profile load failed",error);message("Could not load profile: "+error.getMessage());}
    }
    private boolean rememberActive() {
        if(data==null)return false;Properties workspace=new Properties();workspace.setProperty("schema","1");workspace.setProperty("active",active);
        try{data.save("workspace",workspace);return true;}catch(IOException error){log.warn("Could not save active profile",error);return false;}
    }
    public void saveWaypoints()throws IOException {
        if(data==null)throw new IOException("Waypoint storage is unavailable");data.save("waypoints",waypoints.encode());
    }
    public String keyName(int code){String name=code==0?null:Keyboard.getKeyName(code);return name==null?"NONE":name;}
    public void message(String text) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.currentScreen instanceof ForgeScreen)((ForgeScreen)mc.currentScreen).message(text);
        else if(mc.thePlayer!=null)mc.thePlayer.addChatMessage(new ChatComponentText("\u00a76[Forge] \u00a7r"+text));
        else log.info(text);
    }
}
