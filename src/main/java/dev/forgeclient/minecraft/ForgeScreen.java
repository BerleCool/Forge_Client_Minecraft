package dev.forgeclient.minecraft;

import dev.forgeclient.ui.*;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.IOException;
import java.util.List;

/** Thin Minecraft adapter around the same OverlayView exercised by the headless tests. */
public final class ForgeScreen extends GuiScreen implements UiHost {
    private final GuiScreen parent;
    private final ForgeClient client;
    private final OverlayView view;
    private NativeCanvas canvas;
    private double scale=1;
    public ForgeScreen(GuiScreen parent){this.parent=parent;client=ForgeClient.instance();view=new OverlayView(client.modules,client.telemetry,this);}
    @Override public void initGui(){canvas=new NativeCanvas(mc);Keyboard.enableRepeatEvents(true);}
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        scale=UiLayout.scaleFor(width,height);canvas.begin(scale);
        try{view.draw(canvas,(int)Math.ceil(width/scale),(int)Math.ceil(height/scale),logical(mouseX),logical(mouseY));}
        finally{canvas.end();}
    }
    private int logical(int value){return (int)Math.floor(value/scale);}
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{view.mouseDown(logical(x),logical(y),button);}
    @Override protected void mouseReleased(int x,int y,int state){view.mouseUp();}
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed){if(button==0)view.drag(logical(x),logical(y),isShiftKeyDown());}
    @Override public void handleMouseInput()throws IOException{
        super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel==0)return;
        int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;
        view.wheel(Integer.signum(wheel),logical(x),logical(y));
    }
    @Override protected void keyTyped(char character,int code)throws IOException{
        boolean capture=view.capturingBinding();
        if(!capture&&code==client.openKey.getKeyCode()){closeScreen();return;}
        if(view.key(character,code,isCtrlKeyDown(),isShiftKeyDown()))return;
        if(code==Keyboard.KEY_ESCAPE||code==Keyboard.KEY_RSHIFT)closeScreen();
    }
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);view.mouseUp();client.snapshot();}
    public void openHud(){view.setTab(OverlayView.Tab.HUD);}
    public String keyName(int code){return client.keyName(code);}
    public boolean keyAvailable(int code){return code==0||code!=client.openKey.getKeyCode();}
    public List<String> profiles(){return client.profileNames();}
    public String activeProfile(){return client.activeProfile();}
    public void saveProfile(String name){client.saveProfile(name);}
    public void loadProfile(String name){client.loadProfile(name);}
    public void closeScreen(){mc.displayGuiScreen(parent);}
    public void message(String message){view.showMessage(message);}
}
