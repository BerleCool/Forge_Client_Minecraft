package dev.forgeclient.minecraft;

import dev.forgeclient.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Samples existing client state only; never probes a server or starts a telemetry connection. */
public final class TelemetrySampler {
    private final ForgeClient client;private final Minecraft mc;private final ModuleRegistry modules;private final Telemetry data;
    private World previousWorld;private int ticks;
    private long sessionStarted,speedAt,attackAt;private double previousX,previousZ,lastDistance;
    private String worldKey="none",packs="Default";
    private final DateTimeFormatter clock24=DateTimeFormatter.ofPattern("HH:mm"),clock12=DateTimeFormatter.ofPattern("h:mm a",Locale.ROOT);
    public TelemetrySampler(ForgeClient client){this.client=client;mc=Minecraft.getMinecraft();modules=client.modules;data=client.telemetry;}
    public String worldKey(){return worldKey;}
    public boolean tick(long now){
        boolean changed=previousWorld!=mc.theWorld;
        if(changed){previousWorld=mc.theWorld;data.clearWorld();sessionStarted=now;speedAt=attackAt=0;ticks=0;worldKey=mc.theWorld==null?"none":identifyWorld();}
        if(mc.theWorld==null||mc.thePlayer==null){data.world=false;return changed;}
        data.world=true;keys();data.fps=Minecraft.getDebugFPS();data.leftCps=data.left.count(now);data.rightCps=data.right.count(now);data.sprinting=mc.thePlayer.isSprinting();
        if(ticks++%5==0)sample(now);return changed;
    }
    private void keys(){
        GameSettings s=mc.gameSettings;boolean active=mc.currentScreen==null;
        data.keys[0]=active&&s.keyBindForward.isKeyDown();data.keys[1]=active&&s.keyBindLeft.isKeyDown();data.keys[2]=active&&s.keyBindBack.isKeyDown();data.keys[3]=active&&s.keyBindRight.isKeyDown();
        data.keys[4]=active&&s.keyBindJump.isKeyDown();data.keys[5]=active&&s.keyBindSneak.isKeyDown();data.keys[6]=active&&Mouse.isButtonDown(0);data.keys[7]=active&&Mouse.isButtonDown(1);
    }
    private String identifyWorld(){
        String identity=mc.isSingleplayer()&&mc.getIntegratedServer()!=null?"local:"+mc.getIntegratedServer().getFolderName():"server:"+(mc.getCurrentServerData()==null?"unknown":mc.getCurrentServerData().serverIP.toLowerCase(Locale.ROOT));
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }
    private boolean enabled(String id){return modules.enabled(id);}
    private void sample(long now){
        EntityPlayer p=mc.thePlayer;data.player=mc.getSession().getUsername();data.server=mc.isSingleplayer()?"Singleplayer":(mc.getCurrentServerData()==null?"Multiplayer":mc.getCurrentServerData().serverName);
        NetworkPlayerInfo info=mc.getNetHandler()==null?null:mc.getNetHandler().getPlayerInfo(p.getUniqueID());data.ping=info==null?0:Math.max(0,info.getResponseTime());
        if(enabled("fps"))data.put("fps",data.fps+" FPS");
        if(enabled("ping"))data.put("ping",mc.isSingleplayer()?"LOCAL SESSION":(info==null?"PING --":data.ping+" ms"));
        if(enabled("cps")){String mode=modules.get("cps").setting("mode").raw();data.put("cps","Left".equals(mode)?data.leftCps+" CPS":"Right".equals(mode)?data.rightCps+" CPS":data.leftCps+" | "+data.rightCps+" CPS");}
        if(enabled("keystrokes")){GameSettings s=mc.gameSettings;int[] codes={s.keyBindForward.getKeyCode(),s.keyBindLeft.getKeyCode(),s.keyBindBack.getKeyCode(),s.keyBindRight.getKeyCode(),s.keyBindJump.getKeyCode(),s.keyBindSneak.getKeyCode()};for(int i=0;i<codes.length;i++)data.keyNames[i]=GameSettings.getKeyDisplayString(codes[i]);}
        if(enabled("coordinates")){String format="%."+modules.get("coordinates").setting("decimals").integer()+"f";data.put("coordinates","X "+fmt(format,p.posX),"Y "+fmt(format,p.posY),"Z "+fmt(format,p.posZ));}
        double yaw=((p.rotationYaw%360)+360)%360;
        if(enabled("compass")){String[] direction={"S","SW","W","NW","N","NE","E","SE"};data.put("compass",direction[((int)Math.floor(yaw/45+.5))%8]+" / "+fmt("%.0f",yaw)+" deg");}
        if(enabled("armor")){List<Telemetry.Item> armor=new ArrayList<>();for(int i=3;i>=0;i--){ItemStack stack=p.inventory.armorInventory[i];if(stack!=null)armor.add(item(stack,modules.get("armor").setting("percent").bool()));}data.armor=armor.toArray(new Telemetry.Item[0]);}
        if(enabled("held_item")){ItemStack stack=p.getHeldItem();data.held=stack==null?new Telemetry.Item[0]:new Telemetry.Item[]{item(stack,modules.get("held_item").setting("percent").bool())};}
        if(enabled("potions")){List<String> effects=new ArrayList<>();for(PotionEffect effect:p.getActivePotionEffects()){int id=effect.getPotionID();Potion potion=id>=0&&id<Potion.potionTypes.length?Potion.potionTypes[id]:null;if(potion!=null)effects.add(I18n.format(potion.getName())+(effect.getAmplifier()>0?" "+(effect.getAmplifier()+1):"")+"  "+Potion.getDurationString(effect));}data.put("potions",effects.toArray(new String[0]));}
        if(enabled("speed")){double seconds=(now-speedAt)/1_000_000_000.0,delta=Math.hypot(p.posX-previousX,p.posZ-previousZ);double speed=speedAt!=0&&seconds>0&&seconds<1&&delta<8?delta/seconds:0;speedAt=now;previousX=p.posX;previousZ=p.posZ;data.put("speed",fmt("%.2f",speed)+" blocks/s");}else speedAt=0;
        if(enabled("memory")){Runtime rt=Runtime.getRuntime();data.put("memory",(rt.totalMemory()-rt.freeMemory())/1048576+" / "+rt.maxMemory()/1048576+" MB");}
        if(enabled("clock"))data.put("clock",LocalTime.now().format(modules.get("clock").setting("twenty_four").bool()?clock24:clock12));
        if(enabled("session")){long seconds=Math.max(0,(now-sessionStarted)/1_000_000_000L);data.put("session",String.format(Locale.ROOT,"SESSION %02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60));}
        if(enabled("server"))data.put("server",enabled("streamer_mode")&&modules.get("streamer_mode").setting("server").bool()?"PRIVATE SESSION":data.server);
        if(enabled("biome")||enabled("light")){BlockPos pos=new BlockPos(p.posX,p.posY,p.posZ);if(mc.theWorld.isBlockLoaded(pos)){if(enabled("biome"))data.put("biome",mc.theWorld.getBiomeGenForCoords(pos).biomeName);if(enabled("light"))data.put("light","SKY "+mc.theWorld.getLightFor(EnumSkyBlock.SKY,pos)+" / BLOCK "+mc.theWorld.getLightFor(EnumSkyBlock.BLOCK,pos));}else{if(enabled("biome"))data.put("biome","BIOME --");if(enabled("light"))data.put("light","LIGHT --");}}
        if(enabled("day_time")){long time=mc.theWorld.getWorldTime(),day=Math.floorDiv(time,24000)+1,clock=Math.floorMod(time+6000,24000);data.put("day_time",String.format(Locale.ROOT,"DAY %d / %02d:%02d",day,clock/1000,clock%1000*60/1000));}
        if(enabled("hit_distance"))data.put("hit_distance",attackAt!=0&&now-attackAt<modules.get("hit_distance").setting("timeout").number()*1_000_000_000L?"LOCAL HIT "+fmt("%.2f",lastDistance)+" m":"LOCAL HIT --");
        if(enabled("inventory_counts")){int arrows=0,pearls=0,blocks=0;for(ItemStack stack:p.inventory.mainInventory)if(stack!=null){if(stack.getItem()==Items.arrow)arrows+=stack.stackSize;if(stack.getItem()==Items.ender_pearl)pearls+=stack.stackSize;if(stack.getItem() instanceof ItemBlock)blocks+=stack.stackSize;}data.put("inventory_counts","ARROWS "+arrows+" / PEARLS "+pearls,"BLOCKS "+blocks);}
        if(enabled("pack_info")){if(ticks%40<5){List<String> names=new ArrayList<>();for(ResourcePackRepository.Entry entry:mc.getResourcePackRepository().getRepositoryEntries())names.add(entry.getResourcePackName());packs=names.isEmpty()?"Default":String.join(", ",names);}data.put("pack_info",packs.length()>80?packs.substring(0,77)+"...":packs);}
        if(enabled("sprint_status"))data.put("sprint_status",p.isSprinting()?"SPRINTING":"WALKING");
        if(enabled("waypoint")){Waypoint point=client.waypoints.nearest(worldKey,p.dimension,p.posX,p.posY,p.posZ);if(point==null)data.put("waypoint","NO WAYPOINT / /forgeclient waypoint");else{double bearing=Math.toDegrees(Math.atan2(-(point.x-p.posX),point.z-p.posZ));double relative=((bearing-yaw+540)%360)-180;data.put("waypoint",point.name+" / "+fmt("%.0f",point.distance(p.posX,p.posY,p.posZ))+" m",Math.abs(relative)<12?"AHEAD":relative<0?"LEFT "+fmt("%.0f",-relative)+" deg":"RIGHT "+fmt("%.0f",relative)+" deg");}}
        if(enabled("frame_graph"))data.put("frame_graph","AVG "+fmt("%.1f",data.frames.mean())+" / P99 "+fmt("%.1f",data.frames.percentile(.99))+" ms");
    }
    public void attack(Entity target){MovingObjectPosition hit=mc.objectMouseOver;if(!enabled("hit_distance")||mc.thePlayer==null||hit==null||hit.typeOfHit!=MovingObjectPosition.MovingObjectType.ENTITY||hit.entityHit!=target||hit.hitVec==null)return;lastDistance=mc.thePlayer.getPositionEyes(1).distanceTo(hit.hitVec);attackAt=System.nanoTime();}
    private static Telemetry.Item item(ItemStack stack,boolean percent){int max=stack.getMaxDamage(),remaining=Math.max(0,max-stack.getItemDamage());double fraction=max>0?remaining/(double)max:1;String label=max>0?(percent?Math.round(fraction*100)+"%":remaining+" / "+max):stack.stackSize+"x";return new Telemetry.Item(stack.getDisplayName(),label,fraction,stack.copy());}
    private static String fmt(String pattern,double value){return String.format(Locale.ROOT,pattern,value);}
}
