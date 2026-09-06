package dev.forgeclient.minecraft;

import dev.forgeclient.core.ChatDeduplicator;
import dev.forgeclient.core.ClientPolicies;
import dev.forgeclient.core.ClientModule;
import dev.forgeclient.ui.HudRenderer;
import dev.forgeclient.ui.UiLayout;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** All event handlers are client-thread handlers. No packet injection, background world access or coremod. */
public final class ClientEvents {
    private final ForgeClient client;
    private final Minecraft mc=Minecraft.getMinecraft();
    private final VisualOverrides overrides;
    private final NativeCanvas canvas;
    private final HudRenderer hud=new HudRenderer();
    private final ChatDeduplicator chat=new ChatDeduplicator();
    private final FloatBuffer outlineColor=BufferUtils.createFloatBuffer(16);
    private final DateTimeFormatter minutes=DateTimeFormatter.ofPattern("HH:mm"),seconds=DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean sprintIntent,ownsSprint;
    public ClientEvents(ForgeClient client){this.client=client;overrides=new VisualOverrides(mc,client.modules);canvas=new NativeCanvas(mc);}
    private boolean enabled(String id){return client.modules.enabled(id);}
    private boolean play(){return mc.theWorld!=null&&mc.thePlayer!=null&&mc.currentScreen==null;}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        if(client.sampler.tick(System.nanoTime())){sprintIntent=ownsSprint=false;chat.clear();overrides.releaseAll();}
        overrides.tick();sprint();client.snapshotIfChanged();
        if(!enabled("chat_filter"))chat.clear();
    }
    @SubscribeEvent public void key(InputEvent.KeyInputEvent event) {
        if(!Keyboard.getEventKeyState()||Keyboard.isRepeatEvent())return;
        if(mc.currentScreen==null&&client.openKey.isPressed()){mc.displayGuiScreen(new ForgeScreen(null));return;}
        if(!play()||!Display.isActive())return;
        int key=Keyboard.getEventKey();
        if(key!=client.openKey.getKeyCode())client.modules.onKey(key,false);
        if(enabled("toggle_sprint")&&key==mc.gameSettings.keyBindSprint.getKeyCode())sprintIntent=!sprintIntent;
    }
    @SubscribeEvent public void mouse(MouseEvent event) {
        if(!play()||!Display.isActive()||!event.buttonstate)return;
        long now=System.nanoTime();
        if(event.button==0)client.telemetry.left.click(now);
        if(event.button==1)client.telemetry.right.click(now);
        if(enabled("toggle_sprint")&&event.button>=0&&event.button-100==mc.gameSettings.keyBindSprint.getKeyCode())sprintIntent=!sprintIntent;
    }
    private void sprint() {
        if(!play()||!Display.isActive()||!enabled("toggle_sprint"))sprintIntent=false;
        if(mc.thePlayer==null){ownsSprint=false;return;}
        boolean allowed=ClientPolicies.maySprint(Display.isActive(),mc.currentScreen!=null,mc.theWorld!=null,
            mc.thePlayer.movementInput.moveForward,mc.thePlayer.isSneaking(),mc.thePlayer.isUsingItem(),
            mc.thePlayer.isCollidedHorizontally,mc.thePlayer.isPotionActive(Potion.blindness),
            mc.thePlayer.getFoodStats().getFoodLevel(),mc.thePlayer.capabilities.allowFlying);
        if(sprintIntent&&allowed){if(!mc.thePlayer.isSprinting()){mc.thePlayer.setSprinting(true);ownsSprint=true;}}
        else if(ownsSprint){if(!mc.gameSettings.keyBindSprint.isKeyDown())mc.thePlayer.setSprinting(false);ownsSprint=false;}
    }
    @SubscribeEvent public void fov(FOVUpdateEvent event) {
        if(event.entity!=mc.thePlayer)return;
        if(enabled("fov_stabilizer"))event.newfov=1;
        if(overrides.zooming())event.newfov/=overrides.zoomFactor();
    }
    @SubscribeEvent public void gui(GuiOpenEvent event) {
        // Release before a vanilla/mod settings screen has a chance to save GameSettings.
        if(event.gui!=null&&!(event.gui instanceof ForgeScreen))overrides.releaseAll();
    }
    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        if(enabled("smart_fps")) {
            ClientModule m=client.modules.get("smart_fps");
            int cap=ClientPolicies.frameCap(Display.isActive(),mc.currentScreen!=null||mc.theWorld==null,
                    m.setting("background").integer(),m.setting("menu").integer(),mc.gameSettings.limitFramerate);
            if(cap>0)Display.sync(cap);
        }
        if(enabled("frame_graph")&&mc.theWorld!=null)client.telemetry.frames.frame(System.nanoTime());
        else if(client.telemetry.frames.size()>0)client.telemetry.frames.clear();
    }
    private boolean hudVisible(){return mc.theWorld!=null&&mc.thePlayer!=null&&!mc.gameSettings.hideGUI&&!mc.gameSettings.showDebugInfo&&!(mc.currentScreen instanceof ForgeScreen);}
    @SubscribeEvent(priority=EventPriority.LOW) public void overlayPre(RenderGameOverlayEvent.Pre event) {
        if(mc.thePlayer==null)return;
        if(event.type==RenderGameOverlayEvent.ElementType.HELMET&&enabled("no_pumpkin")) {
            ItemStack helmet=mc.thePlayer.inventory.armorItemInSlot(3);
            if(helmet!=null&&helmet.getItem()==Item.getItemFromBlock(Blocks.pumpkin))event.setCanceled(true);
        }
        if(event.type==RenderGameOverlayEvent.ElementType.PORTAL&&enabled("no_portal"))event.setCanceled(true);
        if(event.type==RenderGameOverlayEvent.ElementType.CROSSHAIRS&&enabled("crosshair")&&hudVisible()&&mc.gameSettings.thirdPersonView==0&&!mc.thePlayer.isSpectator()) {
            event.setCanceled(true);customCrosshair(event.resolution);
        }
    }
    private void customCrosshair(ScaledResolution resolution) {
        ClientModule m=client.modules.get("crosshair");int gap=m.setting("gap").integer(),length=m.setting("length").integer(),thick=m.setting("thickness").integer();
        int x=resolution.getScaledWidth()/2,y=resolution.getScaledHeight()/2,offset=thick/2;
        canvas.begin(1);
        try {
            int color=0xFFFFAA55;
            canvas.rect(x-gap-length,y-offset,length,thick,color);canvas.rect(x+gap+1,y-offset,length,thick,color);
            canvas.rect(x-offset,y-gap-length,thick,length,color);canvas.rect(x-offset,y+gap+1,thick,length,color);
            if(m.setting("dot").bool())canvas.rect(x-offset,y-offset,thick,thick,color);
        } finally{canvas.end();}
    }
    @SubscribeEvent public void overlayPost(RenderGameOverlayEvent.Post event) {
        if(event.type!=RenderGameOverlayEvent.ElementType.ALL||!hudVisible())return;
        int width=event.resolution.getScaledWidth(),height=event.resolution.getScaledHeight();
        double scale=UiLayout.scaleFor(width,height);canvas.begin(scale);
        try{hud.render(canvas,client.modules,client.telemetry,(int)Math.ceil(width/scale),(int)Math.ceil(height/scale));}
        finally{canvas.end();}
    }
    @SubscribeEvent public void blockOverlay(RenderBlockOverlayEvent event) {
        if(event.overlayType==RenderBlockOverlayEvent.OverlayType.FIRE&&enabled("no_fire"))event.setCanceled(true);
        if(event.overlayType==RenderBlockOverlayEvent.OverlayType.WATER&&enabled("no_water"))event.setCanceled(true);
    }
    @SubscribeEvent public void cull(RenderLivingEvent.Pre<?> event) {
        if(!enabled("distance_culling")||mc.thePlayer==null||event.entity instanceof EntityPlayer||event.entity instanceof IBossDisplayData||event.entity.hasCustomName())return;
        double distance=client.modules.get("distance_culling").setting("distance").number();
        if(event.entity.getDistanceSqToEntity(mc.thePlayer)>distance*distance)event.setCanceled(true);
    }
    @SubscribeEvent public void attack(AttackEntityEvent event){if(event.entityPlayer==mc.thePlayer)client.sampler.attack(event.target);}
    @SubscribeEvent(priority=EventPriority.LOW) public void chat(ClientChatReceivedEvent event) {
        if(event.type==2||event.message==null)return; // Action bar must remain untouched.
        String original=event.message.getUnformattedText();
        if(enabled("chat_filter")&&chat.suppress(original,System.nanoTime(),(long)(client.modules.get("chat_filter").setting("window").number()*1_000_000_000L))) {
            event.setCanceled(true);return;
        }
        if(enabled("chat_timestamps")) {
            String time=LocalTime.now().format(client.modules.get("chat_timestamps").setting("seconds").bool()?seconds:minutes);
            // Preserve the original component tree, styles, click events and hover events.
            event.message=new ChatComponentText("\u00a77["+time+"] \u00a7r").appendSibling(event.message);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOW) public void outline(DrawBlockHighlightEvent event) {
        if(!enabled("block_outline")||event.target==null||event.target.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK||mc.theWorld==null)return;
        Block block=mc.theWorld.getBlockState(event.target.getBlockPos()).getBlock();if(block==Blocks.air)return;
        block.setBlockBoundsBasedOnState(mc.theWorld,event.target.getBlockPos());
        AxisAlignedBB box=block.getSelectedBoundingBox(mc.theWorld,event.target.getBlockPos());if(box==null)return;
        double x=event.player.lastTickPosX+(event.player.posX-event.player.lastTickPosX)*event.partialTicks;
        double y=event.player.lastTickPosY+(event.player.posY-event.player.lastTickPosY)*event.partialTicks;
        double z=event.player.lastTickPosZ+(event.player.posZ-event.player.lastTickPosZ)*event.partialTicks;
        box=box.expand(.002,.002,.002).offset(-x,-y,-z);
        boolean oldTexture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),oldBlend=GL11.glIsEnabled(GL11.GL_BLEND),oldMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        float oldWidth=GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        int src=GL11.glGetInteger(32969),dst=GL11.glGetInteger(32968),srcAlpha=GL11.glGetInteger(32971),dstAlpha=GL11.glGetInteger(32970);
        outlineColor.clear();GL11.glGetFloat(GL11.GL_CURRENT_COLOR,outlineColor);
        GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.disableTexture2D();GlStateManager.depthMask(false);
        // Keep the existing depth test: never an x-ray outline through other blocks.
        GlStateManager.color(1,.59f,.25f,.95f);GL11.glLineWidth((float)client.modules.get("block_outline").setting("width").number());
        try{RenderGlobal.drawSelectionBoundingBox(box);event.setCanceled(true);}
        finally{
            GL11.glLineWidth(oldWidth);GlStateManager.depthMask(oldMask);
            GlStateManager.tryBlendFuncSeparate(src,dst,srcAlpha,dstAlpha);
            if(oldTexture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
            if(oldBlend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
            GlStateManager.color(outlineColor.get(0),outlineColor.get(1),outlineColor.get(2),outlineColor.get(3));
        }
    }
}
