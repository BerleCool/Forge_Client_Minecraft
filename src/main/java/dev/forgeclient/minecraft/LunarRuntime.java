package dev.forgeclient.minecraft;

import dev.forgeclient.core.ClientModule;
import dev.forgeclient.core.Waypoint;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * Runtime for the Lunar-inspired 1.8.9 modules exposed by LunarFunctionalModules.
 * No feature here starts background networking or injects packets.
 */
public final class LunarRuntime {
    private static final class TrailPoint { final double x,y,z; TrailPoint(double x,double y,double z){this.x=x;this.y=y;this.z=z;} }
    private final ForgeClient client;
    private final Minecraft mc=Minecraft.getMinecraft();
    private int ticks;
    private Object previousWorld;
    private long lastAttackNs,lastPearlNs,comboAtNs,stopwatchStartedNs,stopwatchAccumulatedNs;
    private Entity comboTarget;
    private int combo;
    private boolean stopwatchWasEnabled,chatWasEnabled,scaleWasEnabled,weatherWasEnabled;
    private float previousChatOpacity,previousRain,previousThunder;
    private int previousGuiScale,previousHurtTime=-1,previousMaxHurtTime=-1,lastHurtTime;
    private boolean hurtThisTick;
    private double lastX,lastZ,lastSpeed,latestKnockback;
    private String lastActionBar="",lastInventoryDelta="NO RECENT CHANGE";
    private Map<String,Integer> previousInventory=Collections.emptyMap();
    private final Map<net.minecraft.item.Item,Integer> itemCounts=new IdentityHashMap<net.minecraft.item.Item,Integer>();
    private SkyblockScoreboardSnapshot sidebar=SkyblockScoreboardSnapshot.empty();
    private String[] minimapCache=new String[]{"N","?"};
    private final Deque<TrailPoint> trail=new ArrayDeque<TrailPoint>();
    private final List<BlockPos> lowLight=new ArrayList<BlockPos>();
    private BlockPos cuiOne,cuiTwo;

    public LunarRuntime(ForgeClient client){this.client=client;}
    private boolean enabled(String id){return client.modules.enabled(id);}
    private ClientModule mod(String id){return client.modules.get(id);}
    private boolean play(){return mc.theWorld!=null&&mc.thePlayer!=null;}
    private long now(){return System.nanoTime();}

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        if(previousWorld!=mc.theWorld){previousWorld=mc.theWorld;resetWorldState();}
        applyLeasedSettings();
        if(!play())return;
        ticks++;EntityPlayer p=mc.thePlayer;long n=now();
        if(enabled("lunar_replay")||enabled("lunar_rewind")){
            if((ticks&1)==0){trail.addLast(new TrailPoint(p.posX,p.posY,p.posZ));int seconds=enabled("lunar_replay")?mod("lunar_replay").setting("seconds").integer():8;int cap=Math.max(25,seconds*10);while(trail.size()>cap)trail.removeFirst();}
        }else trail.clear();
        if(enabled("lunar_item_physics")){
            float spin=(float)Math.toRadians(mod("lunar_item_physics").setting("speed").number());int touched=0;
            for(Object raw:mc.theWorld.loadedEntityList)if(raw instanceof EntityItem){EntityItem item=(EntityItem)raw;item.hoverStart=(item.hoverStart+spin)%(float)(Math.PI*2D);if(++touched>=96)break;}
        }
        if(enabled("lunar_time")){String value=mod("lunar_time").setting("time").raw();long time="Sunset".equals(value)?12000L:"Night".equals(value)?18000L:"Sunrise".equals(value)?23000L:6000L;long day=mc.theWorld.getWorldTime()/24000L;mc.theWorld.setWorldTime(day*24000L+time);}
        if(enabled("lunar_weather")){mc.theWorld.setRainStrength(0F);mc.theWorld.setThunderStrength(0F);}
        if((ticks%5)==0)sampleHud(n);
        if((ticks%10)==0){if(enabled("lunar_light_overlay"))sampleLowLight();else lowLight.clear();if(enabled("lunar_item_tracker"))sampleInventoryDelta();if(enabled("lunar_minimap"))minimapCache=buildMinimapRows();else minimapCache=new String[]{"N","?"};}
        if((ticks%5)==0){double dx=p.posX-lastX,dz=p.posZ-lastZ;lastSpeed=Math.hypot(dx,dz)/.25D;lastX=p.posX;lastZ=p.posZ;}
        if(p.hurtTime>0&&lastHurtTime<=0){latestKnockback=Math.hypot(p.motionX,p.motionZ);if(enabled("lunar_combo"))combo=0;}lastHurtTime=p.hurtTime;
        if(combo>0&&n-comboAtNs>(long)(mod("lunar_combo").setting("timeout").number()*1_000_000_000L))combo=0;
    }

    private void resetWorldState(){trail.clear();lowLight.clear();cuiOne=cuiTwo=null;combo=0;comboTarget=null;lastAttackNs=lastPearlNs=comboAtNs=0;lastActionBar="";previousInventory=Collections.emptyMap();itemCounts.clear();sidebar=SkyblockScoreboardSnapshot.empty();minimapCache=new String[]{"N","?"};lastInventoryDelta="NO RECENT CHANGE";lastHurtTime=0;latestKnockback=0;lastX=lastZ=lastSpeed=0;}
    private void applyLeasedSettings(){
        boolean chat=enabled("lunar_chat");if(chat&&!chatWasEnabled)previousChatOpacity=mc.gameSettings.chatOpacity;if(chat)mc.gameSettings.chatOpacity=(float)(mod("lunar_chat").setting("opacity").number()/100D);else if(chatWasEnabled)mc.gameSettings.chatOpacity=previousChatOpacity;chatWasEnabled=chat;
        boolean scale=enabled("lunar_gui_scale");if(scale&&!scaleWasEnabled)previousGuiScale=mc.gameSettings.guiScale;if(scale){String v=mod("lunar_gui_scale").setting("scale").raw();mc.gameSettings.guiScale="Small".equals(v)?1:"Normal".equals(v)?2:"Large".equals(v)?3:0;}else if(scaleWasEnabled)mc.gameSettings.guiScale=previousGuiScale;scaleWasEnabled=scale;
        boolean weather=enabled("lunar_weather");if(weather&&play()&&!weatherWasEnabled){previousRain=mc.theWorld.getRainStrength(1F);previousThunder=mc.theWorld.getThunderStrength(1F);}else if(!weather&&weatherWasEnabled&&play()){mc.theWorld.setRainStrength(previousRain);mc.theWorld.setThunderStrength(previousThunder);}weatherWasEnabled=weather;
        boolean sw=enabled("lunar_stopwatch");long n=now();if(sw&&!stopwatchWasEnabled)stopwatchStartedNs=n;if(!sw&&stopwatchWasEnabled&&stopwatchStartedNs!=0)stopwatchAccumulatedNs+=Math.max(0,n-stopwatchStartedNs);stopwatchWasEnabled=sw;
    }

    private void sampleHud(long n){
        EntityPlayer p=mc.thePlayer;sidebar=SkyblockScoreboardSnapshot.capture(mc);refreshItemCounts();
        if(enabled("lunar_replay"))client.telemetry.put("lunar_replay","REC "+trail.size()+" SAMPLES",String.format(Locale.ROOT,"%.1f s BUFFER",trail.size()/10D));
        if(enabled("lunar_attack")){double age=lastAttackNs==0?99:(n-lastAttackNs)/1_000_000_000D;client.telemetry.put("lunar_attack",age>.45?"ATTACK READY":String.format(Locale.ROOT,"HIT %.2f s",age),comboTarget==null?"NO TARGET":comboTarget.getName());}
        if(enabled("lunar_potion_counter"))client.telemetry.put("lunar_potion_counter",countItem(Items.potionitem)+" POTIONS");
        if(enabled("lunar_stopwatch")){long elapsed=stopwatchAccumulatedNs+(stopwatchStartedNs==0?0:n-stopwatchStartedNs),sec=Math.max(0,elapsed/1_000_000_000L);client.telemetry.put("lunar_stopwatch",String.format(Locale.ROOT,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60));}
        if(enabled("lunar_combo"))client.telemetry.put("lunar_combo",combo+" COMBO");
        if(enabled("lunar_cooldowns")){double total=mod("lunar_cooldowns").setting("pearl").number(),left=lastPearlNs==0?0:Math.max(0,total-(n-lastPearlNs)/1_000_000_000D);client.telemetry.put("lunar_cooldowns",left<=0?"PEARL READY":String.format(Locale.ROOT,"PEARL %.1f s",left));}
        if(enabled("lunar_pvp_info"))client.telemetry.put("lunar_pvp_info",String.format(Locale.ROOT,"HP %.1f / %.1f",p.getHealth(),p.getMaxHealth()),"ARROWS "+countItem(Items.arrow)+" / GAPS "+countItem(Items.golden_apple),"POTS "+countItem(Items.potionitem)+" / ARMOR "+armorPercent()+"%");
        if(enabled("lunar_team_view"))sampleTeamView();if(enabled("lunar_tnt"))sampleTnt();
        if(enabled("lunar_horse")){if(p.ridingEntity instanceof EntityHorse){EntityHorse h=(EntityHorse)p.ridingEntity;client.telemetry.put("lunar_horse",String.format(Locale.ROOT,"HP %.1f / %.1f",h.getHealth(),h.getMaxHealth()),String.format(Locale.ROOT,"SPEED %.2f",Math.hypot(h.motionX,h.motionZ)*20D));}else client.telemetry.put("lunar_horse","NOT MOUNTED");}
        if(enabled("lunar_inventory"))client.telemetry.put("lunar_inventory",hotbarSummary(),"ARROWS "+countItem(Items.arrow)+" / PEARLS "+countItem(Items.ender_pearl),"GAPS "+countItem(Items.golden_apple));
        if(enabled("lunar_uhc"))client.telemetry.put("lunar_uhc",String.format(Locale.ROOT,"HP %.1f + %.1f",p.getHealth(),p.getAbsorptionAmount()),String.format(Locale.ROOT,"XYZ %.0f %.0f %.0f",p.posX,p.posY,p.posZ),"ARROWS "+countItem(Items.arrow)+" / GAPS "+countItem(Items.golden_apple));
        if(enabled("lunar_waila"))sampleWaila();if(enabled("lunar_f3"))client.telemetry.put("lunar_f3",client.telemetry.fps+" FPS / "+facing(p.rotationYaw),String.format(Locale.ROOT,"CHUNK %d %d",((int)Math.floor(p.posX))>>4,((int)Math.floor(p.posZ))>>4),memoryLine());
        if(enabled("lunar_knockback"))client.telemetry.put("lunar_knockback",String.format(Locale.ROOT,"KB %.3f blocks/tick",latestKnockback));
        if(enabled("lunar_momentum"))client.telemetry.put("lunar_momentum",String.format(Locale.ROOT,"%.2f blocks/s",lastSpeed),String.format(Locale.ROOT,"VEL %.3f",Math.hypot(p.motionX,p.motionZ)));
        if(enabled("lunar_item_tracker"))client.telemetry.put("lunar_item_tracker",lastInventoryDelta);
        if(enabled("lunar_overlay"))client.telemetry.put("lunar_overlay",client.telemetry.fps+" FPS / "+client.telemetry.ping+" ms",client.telemetry.server,String.format(Locale.ROOT,"%.0f %.0f %.0f",p.posX,p.posY,p.posZ));
        if(enabled("lunar_scoreboard"))client.telemetry.put("lunar_scoreboard",scoreboardLines(mod("lunar_scoreboard").setting("lines").integer()));
        if(enabled("lunar_hypixel"))sampleHypixel();if(enabled("lunar_bedwars"))sampleBedwars();if(enabled("lunar_skyblock"))sampleSkyblock("lunar_skyblock");if(enabled("lunar_sba"))sampleSkyblockAddons();if(enabled("lunar_neu"))sampleNeu();
        if(enabled("lunar_minimap"))client.telemetry.put("lunar_minimap",minimapCache);if(enabled("lunar_tier"))sampleTier();if(enabled("lunar_actionbar"))client.telemetry.put("lunar_actionbar",lastActionBar.isEmpty()?"ACTION BAR --":lastActionBar);
    }
    private void refreshItemCounts(){itemCounts.clear();for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null){net.minecraft.item.Item item=s.getItem();Integer count=itemCounts.get(item);itemCounts.put(item,(count==null?0:count)+s.stackSize);}}
    private int countItem(net.minecraft.item.Item item){Integer count=itemCounts.get(item);return count==null?0:count;}
    private int armorPercent(){int max=0,left=0;for(ItemStack s:mc.thePlayer.inventory.armorInventory)if(s!=null&&s.isItemStackDamageable()){max+=s.getMaxDamage();left+=Math.max(0,s.getMaxDamage()-s.getItemDamage());}return max==0?0:(int)Math.round(left*100D/max);}
    private String hotbarSummary(){StringBuilder out=new StringBuilder();for(int i=0;i<9;i++){ItemStack s=mc.thePlayer.inventory.mainInventory[i];if(s==null)continue;if(out.length()>0)out.append(" | ");out.append(i+1).append(":").append(s.stackSize);}return out.length()==0?"HOTBAR EMPTY":out.toString();}
    private String memoryLine(){Runtime r=Runtime.getRuntime();return "MEM "+(r.totalMemory()-r.freeMemory())/1048576+" / "+r.maxMemory()/1048576+" MB";}
    private String facing(float yaw){String[] d={"S","SW","W","NW","N","NE","E","SE"};double y=((yaw%360)+360)%360;return d[((int)Math.floor(y/45+.5))%8];}
    private void sampleTeamView(){EntityPlayer me=mc.thePlayer;Team team=me.getTeam();if(team==null){client.telemetry.put("lunar_team_view","NO SCOREBOARD TEAM");return;}double range=mod("lunar_team_view").setting("range").number(),best=Double.MAX_VALUE;int count=0;for(Object raw:mc.theWorld.playerEntities){if(!(raw instanceof EntityPlayer)||raw==me)continue;EntityPlayer q=(EntityPlayer)raw;if(team.equals(q.getTeam())){double d=me.getDistanceToEntity(q);if(d<=range){count++;best=Math.min(best,d);}}}client.telemetry.put("lunar_team_view",count+" TEAMMATE"+(count==1?"":"S"),best==Double.MAX_VALUE?"NEAREST --":String.format(Locale.ROOT,"NEAREST %.1f m",best));}
    private void sampleTnt(){EntityTNTPrimed best=null;double dist=Double.MAX_VALUE;for(Object raw:mc.theWorld.loadedEntityList)if(raw instanceof EntityTNTPrimed){EntityTNTPrimed t=(EntityTNTPrimed)raw;double d=mc.thePlayer.getDistanceSqToEntity(t);if(d<dist){dist=d;best=t;}}client.telemetry.put("lunar_tnt",best==null?"NO PRIMED TNT":String.format(Locale.ROOT,"TNT %.2f s / %.1f m",best.fuse/20D,Math.sqrt(dist)));}
    private void sampleWaila(){MovingObjectPosition hit=mc.objectMouseOver;if(hit==null||hit.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK){client.telemetry.put("lunar_waila","NO BLOCK TARGET");return;}BlockPos pos=hit.getBlockPos();if(!mc.theWorld.isBlockLoaded(pos)){client.telemetry.put("lunar_waila","UNLOADED");return;}Block b=mc.theWorld.getBlockState(pos).getBlock();client.telemetry.put("lunar_waila",b.getLocalizedName(),"META "+b.getMetaFromState(mc.theWorld.getBlockState(pos)));}
    private String[] scoreboardLines(int max){return sidebar.rows(max);}
    private String scoreboardPlain(){return sidebar.plainJoined();}
    private boolean hypixel(){if(mc.getCurrentServerData()==null||mc.getCurrentServerData().serverIP==null)return false;String host=mc.getCurrentServerData().serverIP.trim().toLowerCase(Locale.ROOT);int slash=host.lastIndexOf('/');if(slash>=0)host=host.substring(slash+1);int colon=host.indexOf(':');if(colon>0)host=host.substring(0,colon);return host.equals("hypixel.net")||host.endsWith(".hypixel.net");}
    private void sampleHypixel(){if(!hypixel()){client.telemetry.put("lunar_hypixel","NOT ON HYPIXEL");return;}client.telemetry.put("lunar_hypixel",scoreboardLines(4));}
    private void sampleBedwars(){if(!hypixel()){client.telemetry.put("lunar_bedwars","NOT ON HYPIXEL");return;}String plain=scoreboardPlain().toLowerCase(Locale.ROOT);if(!plain.contains("bed wars")&&!plain.contains("bedwars")){client.telemetry.put("lunar_bedwars","BED WARS NOT DETECTED");return;}List<String> useful=new ArrayList<String>();for(String line:scoreboardLines(15)){String x=EnumChatFormatting.getTextWithoutFormattingCodes(line);if(x==null)continue;String l=x.toLowerCase(Locale.ROOT);if(l.contains("bed")||l.contains("diamond")||l.contains("emerald")||l.contains("kills")||l.contains("final"))useful.add(line);}if(useful.isEmpty())useful.add("BED WARS ACTIVE");client.telemetry.put("lunar_bedwars",useful.toArray(new String[useful.size()]));}
    private void sampleSkyblock(String id){if(!hypixel()){client.telemetry.put(id,"NOT ON HYPIXEL");return;}String plain=scoreboardPlain().toLowerCase(Locale.ROOT);if(!plain.contains("skyblock")){client.telemetry.put(id,"SKYBLOCK NOT DETECTED");return;}List<String> useful=new ArrayList<String>();for(String line:scoreboardLines(15)){String x=EnumChatFormatting.getTextWithoutFormattingCodes(line);if(x==null)continue;String l=x.toLowerCase(Locale.ROOT);if(l.contains("purse")||l.contains("bits")||l.contains("skyblock"))useful.add(line);}if(useful.isEmpty())useful.add("HYPIXEL SKYBLOCK");client.telemetry.put(id,useful.toArray(new String[useful.size()]));}
    private void sampleSkyblockAddons(){if(!hypixel()||!scoreboardPlain().toLowerCase(Locale.ROOT).contains("skyblock")){client.telemetry.put("lunar_sba","SKYBLOCK NOT DETECTED");return;}String action=EnumChatFormatting.getTextWithoutFormattingCodes(lastActionBar);if(action==null||action.trim().isEmpty())action="SKYBLOCK ACTIVE";List<String> rows=new ArrayList<String>();rows.add(action.trim());for(String line:sidebar.plainLines()){String l=line.toLowerCase(Locale.ROOT);if((l.contains("purse")||l.contains("bits")||l.contains("location"))&&rows.size()<4)rows.add(line);}client.telemetry.put("lunar_sba",rows.toArray(new String[rows.size()]));}
    private void sampleNeu(){ItemStack held=mc.thePlayer.getHeldItem();if(held==null){client.telemetry.put("lunar_neu","NO HELD ITEM");return;}List<String> rows=new ArrayList<String>();rows.add(held.getDisplayName());net.minecraft.nbt.NBTTagCompound tag=held.getTagCompound();if(tag!=null&&tag.hasKey("ExtraAttributes",10)){net.minecraft.nbt.NBTTagCompound extra=tag.getCompoundTag("ExtraAttributes");if(extra.hasKey("id",8))rows.add("ID "+extra.getString("id"));}List<String> tip=held.getTooltip(mc.thePlayer,false);for(int i=1;i<tip.size()&&rows.size()<5;i++){String line=tip.get(i);if(line!=null&&!line.trim().isEmpty())rows.add(line);}client.telemetry.put("lunar_neu",rows.toArray(new String[rows.size()]));}
    private void sampleTier(){MovingObjectPosition hit=mc.objectMouseOver;if(hit==null||!(hit.entityHit instanceof EntityPlayer)){client.telemetry.put("lunar_tier","NO PLAYER TARGET");return;}EntityPlayer q=(EntityPlayer)hit.entityHit;Team t=q.getTeam();if(t instanceof ScorePlayerTeam){ScorePlayerTeam s=(ScorePlayerTeam)t;client.telemetry.put("lunar_tier",s.getColorPrefix()+q.getName()+s.getColorSuffix());}else client.telemetry.put("lunar_tier",t==null?q.getName():t.getRegisteredName()+" "+q.getName());}
    private String[] buildMinimapRows(){int r=mod("lunar_minimap").setting("radius").integer();List<String> rows=new ArrayList<String>();rows.add("N");int centerY=(int)Math.floor(mc.thePlayer.posY)-1;char player=facingChar(mc.thePlayer.rotationYaw);for(int dz=-r;dz<=r;dz++){StringBuilder line=new StringBuilder();for(int dx=-r;dx<=r;dx++){if(dx==0&&dz==0){line.append(player);continue;}line.append(minimapCell(dx,dz,centerY));}rows.add(line.toString());}return rows.toArray(new String[rows.size()]);}
    private char facingChar(float yaw){double y=((yaw%360)+360)%360;int q=((int)Math.floor(y/90D+.5D))&3;return q==0?'v':q==1?'<':q==2?'^':'>'; }
    private char minimapCell(int dx,int dz,int centerY){for(int dy=4;dy>=-5;dy--){BlockPos pos=new BlockPos(mc.thePlayer.posX+dx,centerY+dy,mc.thePlayer.posZ+dz);if(!mc.theWorld.isBlockLoaded(pos))return '?';Material material=mc.theWorld.getBlockState(pos).getBlock().getMaterial();if(material==Material.air)continue;if(material==Material.water)return '~';if(material==Material.lava)return '!';return '#';}return '.';}
    private void sampleInventoryDelta(){Map<String,Integer> current=new TreeMap<String,Integer>();for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null){String key=s.getItem().getUnlocalizedName();Integer old=current.get(key);current.put(key,(old==null?0:old)+s.stackSize);}if(!previousInventory.isEmpty()){TreeSet<String> keys=new TreeSet<String>();keys.addAll(previousInventory.keySet());keys.addAll(current.keySet());List<String> changes=new ArrayList<String>();for(String key:keys){int before=previousInventory.containsKey(key)?previousInventory.get(key):0,after=current.containsKey(key)?current.get(key):0;if(after!=before)changes.add((after>before?"+":"")+(after-before)+" "+cleanItemKey(key));if(changes.size()>=3)break;}if(!changes.isEmpty())lastInventoryDelta=join(changes," / ");}previousInventory=current;}
    private String join(List<String> values,String separator){StringBuilder out=new StringBuilder();for(String value:values){if(out.length()>0)out.append(separator);out.append(value);}return out.toString();}
    private String cleanItemKey(String key){int dot=key.lastIndexOf('.');String s=dot>=0?key.substring(dot+1):key;return s.replace('_',' ').toUpperCase(Locale.ROOT);}
    private void sampleLowLight(){lowLight.clear();int r=mod("lunar_light_overlay").setting("radius").integer(),threshold=mod("lunar_light_overlay").setting("threshold").integer(),y=(int)Math.floor(mc.thePlayer.posY)-1,capped=0;for(int dz=-r;dz<=r&&capped<128;dz++)for(int dx=-r;dx<=r&&capped<128;dx++){BlockPos floor=new BlockPos(mc.thePlayer.posX+dx,y,mc.thePlayer.posZ+dz),air=floor.up();if(!mc.theWorld.isBlockLoaded(floor)||!mc.theWorld.isBlockLoaded(air))continue;if(mc.theWorld.getBlockState(floor).getBlock().getMaterial()!=Material.air&&mc.theWorld.isAirBlock(air)&&mc.theWorld.getLightFromNeighbors(air)<=threshold){lowLight.add(air);capped++;}}}

    @SubscribeEvent public void attack(AttackEntityEvent event){if(event.entityPlayer!=mc.thePlayer)return;long n=now();lastAttackNs=n;if(enabled("lunar_combo")){double timeout=mod("lunar_combo").setting("timeout").number();if(event.target==comboTarget&&n-comboAtNs<timeout*1_000_000_000L)combo++;else combo=1;comboTarget=event.target;comboAtNs=n;}else comboTarget=event.target;}
    @SubscribeEvent public void mouse(MouseEvent event){if(!event.buttonstate||!play()||mc.currentScreen!=null)return;if(event.button==1&&enabled("lunar_cooldowns")){ItemStack held=mc.thePlayer.getHeldItem();if(held!=null&&held.getItem()==Items.ender_pearl)lastPearlNs=now();}if(enabled("lunar_worldedit")&&(event.button==0||event.button==1)){ItemStack held=mc.thePlayer.getHeldItem();MovingObjectPosition hit=mc.objectMouseOver;if(held!=null&&held.getItem()==Items.wooden_axe&&hit!=null&&hit.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK){if(event.button==0)cuiOne=hit.getBlockPos();else cuiTwo=hit.getBlockPos();client.message("CUI "+(event.button==0?"pos1":"pos2")+" = "+hit.getBlockPos().getX()+", "+hit.getBlockPos().getY()+", "+hit.getBlockPos().getZ());}}}
    @SubscribeEvent public void key(InputEvent.KeyInputEvent event){if(!Keyboard.getEventKeyState()||Keyboard.isRepeatEvent()||mc.currentScreen!=null||mc.thePlayer==null)return;int key=Keyboard.getEventKey();if(enabled("lunar_quickplay")&&mod("lunar_quickplay").key()!=0&&key==mod("lunar_quickplay").key()){mc.displayGuiScreen(new ForgeQuickplayScreen(client,mod("lunar_quickplay").setting("mode").raw()));return;}if(enabled("lunar_auto_text")&&mod("lunar_auto_text").key()!=0&&key==mod("lunar_auto_text").key()){mc.thePlayer.sendChatMessage(mod("lunar_auto_text").setting("text").raw());return;}if(enabled("lunar_screenshot")&&key==Keyboard.KEY_F2)client.message("Screenshot requested - Forge keeps vanilla F2 capture intact.");}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void overlayPre(RenderGameOverlayEvent.Pre event){if(event.type==RenderGameOverlayEvent.ElementType.BOSSHEALTH&&enabled("lunar_bossbar"))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void overlayPost(RenderGameOverlayEvent.Post event){if(event.type!=RenderGameOverlayEvent.ElementType.ALL||!enabled("lunar_tab")||mc.thePlayer==null||!mc.gameSettings.keyBindPlayerList.isKeyDown())return;String text="FORGE CLIENT  |  "+client.telemetry.fps+" FPS  |  "+client.telemetry.ping+" ms";int x=(event.resolution.getScaledWidth()-mc.fontRendererObj.getStringWidth(text))/2,y=6;Gui.drawRect(x-6,y-4,x+mc.fontRendererObj.getStringWidth(text)+6,y+12,0xA0100D11);mc.fontRendererObj.drawStringWithShadow(text,x,y,0xFFFFA24A);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void chat(ClientChatReceivedEvent event){if(event.message==null)return;String plain=EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());if(event.type==2){lastActionBar=event.message.getFormattedText();if(enabled("lunar_actionbar"))event.setCanceled(true);return;}if(enabled("lunar_kill_sounds")&&plain!=null){String lower=plain.toLowerCase(Locale.ROOT),name=mc.thePlayer==null?"":mc.thePlayer.getName().toLowerCase(Locale.ROOT);if((lower.contains("killed")&&lower.contains(name))||lower.contains("you killed")||lower.contains("final kill")){if(mc.thePlayer!=null)mc.thePlayer.playSound("random.orb",.7F,1.35F);}}}
    @SubscribeEvent public void fog(EntityViewRenderEvent.FogDensity event){if(enabled("lunar_fog")){event.density=(float)mod("lunar_fog").setting("density").number();event.setCanceled(true);}}
    @SubscribeEvent public void renderTick(TickEvent.RenderTickEvent event){if(mc.thePlayer==null)return;if(event.phase==TickEvent.Phase.START&&enabled("lunar_hurt_cam")){previousHurtTime=mc.thePlayer.hurtTime;previousMaxHurtTime=mc.thePlayer.maxHurtTime;mc.thePlayer.hurtTime=0;mc.thePlayer.maxHurtTime=0;hurtThisTick=true;}else if(event.phase==TickEvent.Phase.END&&hurtThisTick){mc.thePlayer.hurtTime=previousHurtTime;mc.thePlayer.maxHurtTime=previousMaxHurtTime;hurtThisTick=false;}}
    @SubscribeEvent public void world(RenderWorldLastEvent event){if(!play())return;double px=mc.thePlayer.lastTickPosX+(mc.thePlayer.posX-mc.thePlayer.lastTickPosX)*event.partialTicks,py=mc.thePlayer.lastTickPosY+(mc.thePlayer.posY-mc.thePlayer.lastTickPosY)*event.partialTicks,pz=mc.thePlayer.lastTickPosZ+(mc.thePlayer.posZ-mc.thePlayer.lastTickPosZ)*event.partialTicks;if(enabled("lunar_chunk_borders")){int cx=((int)Math.floor(mc.thePlayer.posX))>>4,cz=((int)Math.floor(mc.thePlayer.posZ))>>4;double y=Math.floor(mc.thePlayer.posY)-16;drawBox(new AxisAlignedBB(cx*16,y,cz*16,cx*16+16,y+32,cz*16+16).offset(-px,-py,-pz),0xFFFFA24A,1.4F);}if(enabled("lunar_hitbox")&&mc.objectMouseOver!=null&&mc.objectMouseOver.entityHit!=null){AxisAlignedBB box=mc.objectMouseOver.entityHit.getEntityBoundingBox();if(box!=null)drawBox(box.expand(.03,.03,.03).offset(-px,-py,-pz),0xFFFF7A45,1.5F);}if(enabled("lunar_light_overlay"))for(BlockPos pos:lowLight)drawBox(new AxisAlignedBB(pos.getX()+.35,pos.getY()+.02,pos.getZ()+.35,pos.getX()+.65,pos.getY()+.05,pos.getZ()+.65).offset(-px,-py,-pz),0xFFFF4B32,1F);if(enabled("lunar_worldedit")&&(cuiOne!=null||cuiTwo!=null)){if(cuiOne!=null)drawBlock(cuiOne,px,py,pz,0xFFFFA24A);if(cuiTwo!=null)drawBlock(cuiTwo,px,py,pz,0xFF65D1FF);if(cuiOne!=null&&cuiTwo!=null){double minX=Math.min(cuiOne.getX(),cuiTwo.getX()),minY=Math.min(cuiOne.getY(),cuiTwo.getY()),minZ=Math.min(cuiOne.getZ(),cuiTwo.getZ()),maxX=Math.max(cuiOne.getX(),cuiTwo.getX())+1,maxY=Math.max(cuiOne.getY(),cuiTwo.getY())+1,maxZ=Math.max(cuiOne.getZ(),cuiTwo.getZ())+1;drawBox(new AxisAlignedBB(minX,minY,minZ,maxX,maxY,maxZ).offset(-px,-py,-pz),0xFFFFD27A,2F);}}if(enabled("lunar_markers")){Waypoint point=client.waypoints.nearest(client.sampler.worldKey(),mc.thePlayer.dimension,mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ);if(point!=null)drawBox(new AxisAlignedBB(point.x-.12,point.y,point.z-.12,point.x+.12,point.y+24,point.z+.12).offset(-px,-py,-pz),0xFFFFA24A,1.5F);}if((enabled("lunar_replay")&&mod("lunar_replay").setting("trail").bool())||enabled("lunar_rewind"))drawTrail(px,py,pz);}
    private void drawBlock(BlockPos pos,double px,double py,double pz,int color){drawBox(new AxisAlignedBB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1).expand(.002,.002,.002).offset(-px,-py,-pz),color,1.5F);}
    private void drawBox(AxisAlignedBB box,int color,float width){float a=((color>>>24)&255)/255F,r=((color>>>16)&255)/255F,g=((color>>>8)&255)/255F,b=(color&255)/255F;boolean tex=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);float old=GL11.glGetFloat(GL11.GL_LINE_WIDTH);GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.disableTexture2D();GlStateManager.depthMask(false);GlStateManager.color(r,g,b,a);GL11.glLineWidth(width);try{RenderGlobal.drawSelectionBoundingBox(box);}finally{GL11.glLineWidth(old);GlStateManager.depthMask(mask);if(tex)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();GlStateManager.color(1,1,1,1);}}
    private void drawTrail(double px,double py,double pz){if(trail.size()<2)return;boolean tex=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),blend=GL11.glIsEnabled(GL11.GL_BLEND),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.disableTexture2D();GlStateManager.depthMask(false);GlStateManager.color(1F,.55F,.2F,.75F);GL11.glLineWidth(1.6F);GL11.glBegin(GL11.GL_LINE_STRIP);try{for(TrailPoint point:trail)GL11.glVertex3d(point.x-px,point.y-py+.08,point.z-pz);}finally{GL11.glEnd();GL11.glLineWidth(1F);GlStateManager.depthMask(mask);if(tex)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();GlStateManager.color(1,1,1,1);}}
}
