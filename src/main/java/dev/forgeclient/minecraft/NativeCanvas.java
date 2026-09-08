package dev.forgeclient.minecraft;

import dev.forgeclient.core.Telemetry;
import dev.forgeclient.ui.Canvas;
import dev.forgeclient.ui.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/** LWJGL 2 canvas. Uses Minecraft's font and explicitly restores cached GL state. */
public final class NativeCanvas implements Canvas {
    private static final ResourceLocation STONE=new ResourceLocation("forgeclient","textures/gui/stone.png");
    private final Minecraft mc;
    private final double[] stackX=new double[128],stackY=new double[128],stackScale=new double[128];private int transformDepth;
    private final Deque<Rect> clips=new ArrayDeque<>();
    private final FloatBuffer color=BufferUtils.createFloatBuffer(16);
    private final IntBuffer integers=BufferUtils.createIntBuffer(16);
    private double offsetX,offsetY,localScale,physicalScale;
    private boolean began,blend,depth,alpha,texture,lighting,cull,rescale,depthMask,light0,light1;
    private int matrixMode,textureId,blendSrc,blendDst,blendSrcAlpha,blendDstAlpha,alphaFunc,depthFunc;
    private float alphaRef;
    private Rect originalScissor;
    private boolean scissorEnabled;
    public NativeCanvas(Minecraft mc){this.mc=mc;}
    public void begin(double scale){
        if(began)throw new IllegalStateException("Canvas already active");
        began=true;transformDepth=0;clips.clear();offsetX=offsetY=0;localScale=1;
        physicalScale=new ScaledResolution(mc).getScaleFactor()*scale;
        blend=GL11.glIsEnabled(GL11.GL_BLEND);depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);lighting=GL11.glIsEnabled(GL11.GL_LIGHTING);cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        rescale=GL11.glIsEnabled(32826);light0=GL11.glIsEnabled(GL11.GL_LIGHT0);light1=GL11.glIsEnabled(GL11.GL_LIGHT1);
        depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);textureId=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        blendSrc=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);blendDst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);blendDstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        alphaFunc=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);alphaRef=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        color.clear();GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);
        scissorEnabled=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);integers.clear();GL11.glGetInteger(GL11.GL_SCISSOR_BOX,integers);
        originalScissor=new Rect(integers.get(0),integers.get(1),integers.get(2),integers.get(3));
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.scale(scale,scale,1);
        GlStateManager.disableDepth();GlStateManager.depthMask(false);GlStateManager.disableLighting();GlStateManager.disableCull();
        GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
        GlStateManager.enableAlpha();GlStateManager.alphaFunc(GL11.GL_GREATER,.001F);GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);
    }
    public void end(){
        if(!began)return;
        try{
            while(transformDepth>0)pop();
            clips.clear();GL11.glScissor(originalScissor.x,originalScissor.y,originalScissor.width,originalScissor.height);
            if(scissorEnabled)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.popMatrix();GlStateManager.matrixMode(matrixMode);
            GlStateManager.bindTexture(textureId);
            if(texture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
            GlStateManager.tryBlendFuncSeparate(blendSrc,blendDst,blendSrcAlpha,blendDstAlpha);
            if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();GlStateManager.depthMask(depthMask);GlStateManager.depthFunc(depthFunc);
            if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();GlStateManager.alphaFunc(alphaFunc,alphaRef);
            if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();
            if(light0)GlStateManager.enableLight(0);else GlStateManager.disableLight(0);
            if(light1)GlStateManager.enableLight(1);else GlStateManager.disableLight(1);
            if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
            if(rescale)GlStateManager.enableRescaleNormal();else GlStateManager.disableRescaleNormal();
            GlStateManager.color(color.get(0),color.get(1),color.get(2),color.get(3));
        }finally{began=false;}
    }
    public void rect(int x,int y,int width,int height,int color){if(width>0&&height>0)Gui.drawRect(x,y,x+width,y+height,color);}
    public void text(String text,int x,int y,int color,boolean shadow){if(text!=null&&!text.isEmpty())mc.fontRendererObj.drawString(text,x,y,color,shadow);}
    public int textWidth(String text){return text==null?0:mc.fontRendererObj.getStringWidth(text);}
    public void push(double x,double y,double scale){
        if(transformDepth>=stackX.length)throw new IllegalStateException("Canvas transform stack overflow");stackX[transformDepth]=offsetX;stackY[transformDepth]=offsetY;stackScale[transformDepth]=localScale;transformDepth++;
        offsetX+=x*localScale;offsetY+=y*localScale;localScale*=scale;GlStateManager.pushMatrix();GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
    }
    public void pop(){if(transformDepth<=0)throw new IllegalStateException("Unbalanced canvas transform");transformDepth--;offsetX=stackX[transformDepth];offsetY=stackY[transformDepth];localScale=stackScale[transformDepth];GlStateManager.popMatrix();}
    public void clip(Rect bounds){
        int x=(int)Math.floor((offsetX+bounds.x*localScale)*physicalScale);
        int right=(int)Math.ceil((offsetX+bounds.right()*localScale)*physicalScale);
        int top=(int)Math.floor((offsetY+bounds.y*localScale)*physicalScale);
        int bottom=(int)Math.ceil((offsetY+bounds.bottom()*localScale)*physicalScale);
        Rect r=new Rect(x,mc.displayHeight-bottom,right-x,bottom-top).intersect(new Rect(0,0,mc.displayWidth,mc.displayHeight));
        if(!clips.isEmpty())r=r.intersect(clips.peek());else if(scissorEnabled)r=r.intersect(originalScissor);
        clips.push(r);applyClip(r);
    }
    private void applyClip(Rect r){GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(r.x,r.y,r.width,r.height);}
    public void unclip(){
        if(clips.isEmpty())throw new IllegalStateException("Unbalanced canvas clip");clips.pop();
        if(!clips.isEmpty())applyClip(clips.peek());else if(scissorEnabled)applyClip(originalScissor);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    public void item(Telemetry.Item item,int x,int y){
        if(!(item.stack instanceof ItemStack)){rect(x+3,y+3,10,10,0xFF6A9BB0);return;}
        GlStateManager.pushMatrix();
        try{
            GlStateManager.enableDepth();GlStateManager.depthMask(true);RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI((ItemStack)item.stack,x,y);mc.getRenderItem().renderItemOverlays(mc.fontRendererObj,(ItemStack)item.stack,x,y);
        }finally{
            RenderHelper.disableStandardItemLighting();GlStateManager.disableDepth();GlStateManager.depthMask(false);
            GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
            GlStateManager.enableAlpha();GlStateManager.alphaFunc(GL11.GL_GREATER,.001F);GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
        }
    }
    public void grain(int x,int y,int width,int height,int opacity){
        if(width<=0||height<=0)return;
        mc.getTextureManager().bindTexture(STONE);GlStateManager.enableTexture2D();GlStateManager.enableBlend();GlStateManager.color(1,1,1,Math.max(0,Math.min(255,opacity))/255F);
        Tessellator tess=Tessellator.getInstance();WorldRenderer w=tess.getWorldRenderer();
        w.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX);
        w.pos(x,y+height,0).tex(0,height/32.0).endVertex();w.pos(x+width,y+height,0).tex(width/32.0,height/32.0).endVertex();
        w.pos(x+width,y,0).tex(width/32.0,0).endVertex();w.pos(x,y,0).tex(0,0).endVertex();tess.draw();GlStateManager.color(1,1,1,1);
    }
}
