package dev.forgeclient.minecraft;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/** Resolution-independent Dawn-inspired title screen. */
public final class ForgeMainMenu extends GuiScreen {
    private static final ResourceLocation ART=new ResourceLocation("forgeclient","textures/gui/title.jpg");
    private static final ResourceLocation LOGO=new ResourceLocation("forgeclient","textures/gui/logo.jpg");
    private static final int BTN_H=28,GAP=8;
    private int buttonX,buttonW,firstY,clientX,clientY,clientW,clientH;

    @Override public void initGui(){layout();}
    private void layout(){buttonW=Math.max(180,Math.min(300,width/3));buttonX=(width-buttonW)/2;firstY=Math.max(150,height/2-28);clientW=92;clientH=24;clientX=14;clientY=height-42;}

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        layout();GlStateManager.disableLighting();GlStateManager.disableFog();GlStateManager.color(1,1,1,1);
        drawCover(ART,0,0,width,height,16F/9F);
        // The generated raster is atmosphere only. Controls/text below are native-resolution.
        drawRect(0,0,width,height,0x68100D12);drawRect(0,0,width,2,0xFFFF8A2A);
        int logoSize=Math.max(42,Math.min(66,height/9));drawTextureLinear(LOGO,18,16,logoSize,logoSize);
        GlStateManager.pushMatrix();float titleScale=Math.max(2F,Math.min(3.5F,width/520F));GlStateManager.scale(titleScale,titleScale,1);
        String title="FORGE";int tx=(int)((width/2F)/titleScale-fontRendererObj.getStringWidth(title)/2F),ty=(int)(58/titleScale);fontRendererObj.drawStringWithShadow(title,tx,ty,0xFFFFA24A);GlStateManager.popMatrix();
        String subtitle="MINECRAFT 1.8.9  //  CLIENT ALPHA";fontRendererObj.drawStringWithShadow(subtitle,(width-fontRendererObj.getStringWidth(subtitle))/2,92,0xFFD9CFCA);
        int pad=14,top=firstY-14,bottom=firstY+4*(BTN_H+GAP)-GAP+14;drawRect(buttonX-pad,top,buttonX+buttonW+pad,bottom,0xD0141117);drawRect(buttonX-pad,top,buttonX-pad+2,bottom,0xFFFF8A2A);
        button(mouseX,mouseY,0,"SINGLEPLAYER");button(mouseX,mouseY,1,"MULTIPLAYER");button(mouseX,mouseY,2,"OPTIONS");button(mouseX,mouseY,3,"QUIT");
        boolean hover=inside(mouseX,mouseY,clientX,clientY,clientW,clientH);drawRect(clientX,clientY,clientX+clientW,clientY+clientH,hover?0xE633241C:0xD019151A);drawRect(clientX,clientY,clientX+3,clientY+clientH,0xFFFF8A2A);String c="FORGE CLIENT";fontRendererObj.drawStringWithShadow(c,clientX+9,clientY+8,hover?0xFFFFC47A:0xFFE9DFD9);
        String version="v"+ForgeClient.VERSION+"  //  RIGHT SHIFT IN-GAME";fontRendererObj.drawStringWithShadow(version,width-fontRendererObj.getStringWidth(version)-10,height-16,0xFFAFA4A0);super.drawScreen(mouseX,mouseY,partialTicks);
    }
    private void button(int mx,int my,int index,String label){int y=firstY+index*(BTN_H+GAP);boolean hover=inside(mx,my,buttonX,y,buttonW,BTN_H);drawRect(buttonX,y,buttonX+buttonW,y+BTN_H,hover?0xED38271F:0xE51B171C);drawRect(buttonX,y,buttonX+3,y+BTN_H,hover?0xFFFFB05A:0xFFFF7B22);drawRect(buttonX+4,y+BTN_H-1,buttonX+buttonW,y+BTN_H,0x553E3436);int color=hover?0xFFFFC98F:0xFFE7DDD8;fontRendererObj.drawStringWithShadow(label,buttonX+(buttonW-fontRendererObj.getStringWidth(label))/2,y+10,color);}
    private boolean inside(int mx,int my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{if(button!=0){super.mouseClicked(x,y,button);return;}if(inside(x,y,clientX,clientY,clientW,clientH)){mc.displayGuiScreen(new ForgeScreen(this));return;}for(int i=0;i<4;i++){int by=firstY+i*(BTN_H+GAP);if(!inside(x,y,buttonX,by,buttonW,BTN_H))continue;if(i==0)mc.displayGuiScreen(new GuiSelectWorld(this));else if(i==1)mc.displayGuiScreen(new GuiMultiplayer(this));else if(i==2)mc.displayGuiScreen(new GuiOptions(this,mc.gameSettings));else mc.shutdown();return;}super.mouseClicked(x,y,button);}
    private void drawCover(ResourceLocation texture,int x,int y,int w,int h,float aspect){float screen=w/(float)Math.max(1,h),u0=0,v0=0,u1=1,v1=1;if(screen>aspect){float visible=aspect/screen;v0=(1-visible)/2;v1=1-v0;}else if(screen<aspect){float visible=screen/aspect;u0=(1-visible)/2;u1=1-u0;}drawTextureLinear(texture,x,y,w,h,u0,v0,u1,v1);}
    private void drawTextureLinear(ResourceLocation texture,int x,int y,int w,int h){drawTextureLinear(texture,x,y,w,h,0,0,1,1);}
    private void drawTextureLinear(ResourceLocation texture,int x,int y,int w,int h,float u0,float v0,float u1,float v1){mc.getTextureManager().bindTexture(texture);int oldMin=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER),oldMag=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);Tessellator t=Tessellator.getInstance();WorldRenderer r=t.getWorldRenderer();r.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX);r.pos(x,y+h,0).tex(u0,v1).endVertex();r.pos(x+w,y+h,0).tex(u1,v1).endVertex();r.pos(x+w,y,0).tex(u1,v0).endVertex();r.pos(x,y,0).tex(u0,v0).endVertex();t.draw();GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,oldMin);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,oldMag);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
