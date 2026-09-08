package dev.forgeclient.minecraft;
import dev.forgeclient.core.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Reversible vanilla options. No file writes, mixins or game-logic/network modifications. */
public final class VisualOverrides {
    private final Minecraft mc;private final ModuleRegistry modules;
    private final SettingLease<Float> gamma;
    private final SettingLease<Boolean> fancy,shadows,bobbing,smooth;
    private final SettingLease<Integer> particles,clouds;
    public VisualOverrides(Minecraft mc,ModuleRegistry modules){
        this.mc=mc;this.modules=modules;
        gamma=new SettingLease<>(()->mc.gameSettings.gammaSetting,v->mc.gameSettings.gammaSetting=v);
        fancy=new SettingLease<>(()->mc.gameSettings.fancyGraphics,v->{mc.gameSettings.fancyGraphics=v;if(mc.renderGlobal!=null&&mc.theWorld!=null)mc.renderGlobal.loadRenderers();});
        shadows=new SettingLease<>(()->mc.gameSettings.entityShadows,v->mc.gameSettings.entityShadows=v);
        bobbing=new SettingLease<>(()->mc.gameSettings.viewBobbing,v->mc.gameSettings.viewBobbing=v);
        smooth=new SettingLease<>(()->mc.gameSettings.smoothCamera,v->mc.gameSettings.smoothCamera=v);
        particles=new SettingLease<>(()->mc.gameSettings.particleSetting,v->mc.gameSettings.particleSetting=v);
        clouds=new SettingLease<>(()->mc.gameSettings.clouds,v->mc.gameSettings.clouds=v);
    }
    public boolean zooming(){int key=modules.get("zoom").key();return mc.theWorld!=null&&mc.thePlayer!=null&&mc.currentScreen==null&&Display.isActive()&&modules.enabled("zoom")&&key>0&&key<256&&Keyboard.isKeyDown(key);}
    public float zoomFactor(){return (float)modules.get("zoom").setting("factor").number();}
    public void tick(){
        if(mc.theWorld==null||mc.thePlayer==null||mc.currentScreen!=null||!Display.isActive()){releaseAll();return;}
        if(modules.enabled("fullbright"))gamma.apply((float)modules.get("fullbright").setting("gamma").number());else gamma.release();
        if(modules.enabled("fast_graphics"))fancy.apply(false);else fancy.release();
        if(modules.enabled("entity_shadows"))shadows.apply(false);else shadows.release();
        if(modules.enabled("steady_camera"))bobbing.apply(false);else bobbing.release();
        if(modules.enabled("clouds"))clouds.apply(0);else clouds.release();
        if(modules.enabled("particle_budget"))particles.apply(modules.get("particle_budget").setting("density").raw().equals("Minimal")?2:1);else particles.release();
        if(zooming()&&modules.get("zoom").setting("smooth").bool())smooth.apply(true);else smooth.release();
    }
    public void releaseAll(){gamma.release();fancy.release();shadows.release();bobbing.release();smooth.release();particles.release();clouds.release();}
}
