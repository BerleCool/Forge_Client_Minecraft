#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def path(rel):
    return ROOT / rel


def replace_version(rel, old, new):
    p = path(rel)
    text = p.read_text()
    if new in text:
        return
    if old not in text:
        raise SystemExit('Missing version anchor in %s' % rel)
    p.write_text(text.replace(old, new))


# 0.4.1 is intentionally a focused title-screen restoration release.
replace_version('build.gradle.kts', 'version = "0.4.0-alpha"', 'version = "0.4.1-alpha"')
replace_version('src/main/java/dev/forgeclient/minecraft/ForgeClient.java', 'VERSION="0.4.0-alpha"', 'VERSION="0.4.1-alpha"')
replace_version('src/main/java/dev/forgeclient/ui/OverlayView.java', 'FORGE  /  0.4.0-ALPHA', 'FORGE  /  0.4.1-ALPHA')
replace_version('scripts/verify_jar.py', "('forgeclient', '1.8.9', '0.4.0-alpha')", "('forgeclient', '1.8.9', '0.4.1-alpha')")

menu = r'''package dev.forgeclient.minecraft;

import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.io.IOException;

/**
 * Restored original Forge title-screen composition from the first Dawn-style pass.
 * The artwork itself is now generated at 3840x2160 and sampled linearly.
 */
public final class ForgeMainMenu extends GuiScreen {
    private static final ResourceLocation ART=new ResourceLocation("forgeclient","textures/gui/title.jpg");
    // Keep the original 800x450 design coordinate system so every baked control,
    // hitbox and hover region lands exactly where it did in the first version.
    private static final int ART_W=800,ART_H=450;
    private float artScale;private int artX,artY;
    @Override public void initGui(){layout();}
    private void layout(){artScale=Math.max(width/(float)ART_W,height/(float)ART_H);artX=Math.round((width-ART_W*artScale)/2F);artY=Math.round((height-ART_H*artScale)/2F);}
    private int sx(int x){return artX+Math.round(x*artScale);}private int sy(int y){return artY+Math.round(y*artScale);}
    private boolean hit(int mx,int my,int x1,int y1,int x2,int y2){return mx>=sx(x1)&&mx<=sx(x2)&&my>=sy(y1)&&my<=sy(y2);}
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        layout();GlStateManager.disableLighting();GlStateManager.disableFog();GlStateManager.color(1,1,1,1);
        mc.getTextureManager().bindTexture(ART);
        int oldMin=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER),oldMag=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        try{drawModalRectWithCustomSizedTexture(artX,artY,0,0,Math.round(ART_W*artScale),Math.round(ART_H*artScale),Math.round(ART_W*artScale),Math.round(ART_H*artScale));}
        finally{GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,oldMin);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,oldMag);}
        drawRect(sx(11),sy(392),sx(88),sy(428),0xC9121114);fontRendererObj.drawString("v"+ForgeClient.VERSION,sx(17),sy(399),0xFFFFA24A,true);fontRendererObj.drawString("Forge Client",sx(17),sy(407),0xFFE6D8D0,false);
        hover(mouseX,mouseY,294,248,508,282);hover(mouseX,mouseY,294,286,508,318);hover(mouseX,mouseY,294,323,508,355);hover(mouseX,mouseY,294,361,508,393);hover(mouseX,mouseY,6,94,90,127);hover(mouseX,mouseY,6,127,90,160);
        super.drawScreen(mouseX,mouseY,partialTicks);
    }
    private void hover(int mx,int my,int x1,int y1,int x2,int y2){if(hit(mx,my,x1,y1,x2,y2))drawRect(sx(x1),sy(y1),sx(x2),sy(y2),0x24FF8A28);}
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{
        if(button!=0)return;
        if(hit(x,y,294,248,508,282)){mc.displayGuiScreen(new GuiSelectWorld(this));return;}
        if(hit(x,y,294,286,508,318)){mc.displayGuiScreen(new GuiMultiplayer(this));return;}
        if(hit(x,y,294,323,508,355)||hit(x,y,6,127,90,160)){mc.displayGuiScreen(new GuiOptions(this,mc.gameSettings));return;}
        if(hit(x,y,294,361,508,393)){mc.shutdown();return;}
        if(hit(x,y,6,94,90,127)){mc.displayGuiScreen(new ForgeScreen(this));return;}
        super.mouseClicked(x,y,button);
    }
    @Override public boolean doesGuiPauseGame(){return false;}
}
'''
path('src/main/java/dev/forgeclient/minecraft/ForgeMainMenu.java').write_text(menu)

readme = path('README.md')
text = readme.read_text()
text = text.replace('The current 0.4 alpha combines', 'The current 0.4.1 alpha combines')
text = text.replace('`dist/Forge-Client-1.8.9-0.4.0-alpha.jar`', '`dist/Forge-Client-1.8.9-0.4.1-alpha.jar`')
section = '''## 0.4.1 title-screen restoration\n\n- Restored the exact first Dawn-style menu composition and baked-control layout instead of the later native-control redesign.\n- Rebuilt the title artwork resource at **3840x2160** using a high-quality Lanczos + restrained sharpening pass, preserving the original composition rather than changing the design.\n- Added explicit linear texture sampling for clean downscaling at 1080p/1440p/4K while keeping the original 800x450 logical hitbox map.\n- No module/runtime behavior was changed from 0.4.\n\n'''
if '## 0.4.1 title-screen restoration' not in text:
    text = text.replace('## 0.4 changes\n\n', section + '## 0.4 changes\n\n')
readme.write_text(text)
