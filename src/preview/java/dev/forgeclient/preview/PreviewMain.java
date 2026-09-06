package dev.forgeclient.preview;

import dev.forgeclient.core.*;
import dev.forgeclient.ui.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Interactive shared-UI harness with synthetic data; this executable is NOT a Minecraft mod. */
public final class PreviewMain {
    private static final class Host implements UiHost {
        final ModuleRegistry registry;final Map<String,Properties> profiles=new LinkedHashMap<>();String active="default";Runnable close=()->{};OverlayView view;
        Host(ModuleRegistry registry){this.registry=registry;profiles.put(active,ConfigCodec.encode(registry));}
        public String keyName(int code){return code==0?"NONE":Integer.toString(code);}
        public List<String> profiles(){return new ArrayList<>(profiles.keySet());}
        public String activeProfile(){return active;}
        public void saveProfile(String name){profiles.put(name,ConfigCodec.encode(registry));active=name;message("Saved preview profile.");}
        public void loadProfile(String name){ConfigCodec.decode(profiles.get(name),registry);active=name;}
        public void closeScreen(){close.run();}
        public void message(String text){if(view!=null)view.showMessage(text);}
    }
    private static final class Panel extends JPanel {
        final ModuleRegistry registry=ModuleCatalog.create();final Host host=new Host(registry);final OverlayView view=new OverlayView(registry,PreviewData.create(),host);
        int mx=-1,my=-1;double scale=1;
        Panel(){
            host.view=view;setPreferredSize(new Dimension(1280,720));setFocusable(true);
            MouseAdapter mouse=new MouseAdapter(){
                public void mousePressed(MouseEvent e){requestFocusInWindow();view.mouseDown(logical(e.getX()),logical(e.getY()),e.getButton()==MouseEvent.BUTTON3?1:0);repaint();}
                public void mouseReleased(MouseEvent e){view.mouseUp();repaint();}
                public void mouseMoved(MouseEvent e){mx=logical(e.getX());my=logical(e.getY());repaint();}
                public void mouseDragged(MouseEvent e){mouseMoved(e);view.drag(mx,my,e.isShiftDown());repaint();}
                public void mouseWheelMoved(MouseWheelEvent e){view.wheel(-e.getWheelRotation(),logical(e.getX()),logical(e.getY()));repaint();}
            };
            addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
            addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){boolean handled=view.key(e.getKeyChar(),key(e),e.isControlDown(),e.isShiftDown());if(!handled&&e.getKeyCode()==KeyEvent.VK_ESCAPE)host.closeScreen();repaint();}});
        }
        int logical(int value){return (int)Math.floor(value/scale);}
        protected void paintComponent(Graphics original){
            super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();
            try{
                // Synthetic block landscape only; no claim that this is an in-game screenshot.
                g.setColor(new Color(45,48,64));g.fillRect(0,0,getWidth(),getHeight());
                for(int i=0;i<getWidth();i+=64){int h=90+(int)(Math.sin(i*.008)*40);g.setColor(new Color(45,57,48));g.fillRect(i,getHeight()/2-h,64,getHeight());}
                scale=UiLayout.scaleFor(getWidth(),getHeight());g.scale(scale,scale);
                view.draw(new AwtCanvas(g),(int)Math.ceil(getWidth()/scale),(int)Math.ceil(getHeight()/scale),mx,my);
            }finally{g.dispose();}
        }
    }
    private static int key(KeyEvent e){
        switch(e.getKeyCode()){
            case KeyEvent.VK_ESCAPE:return 1;case KeyEvent.VK_BACK_SPACE:return 14;case KeyEvent.VK_TAB:return 15;
            case KeyEvent.VK_ENTER:return 28;case KeyEvent.VK_A:return 30;case KeyEvent.VK_F:return 33;
            case KeyEvent.VK_C:return 46;case KeyEvent.VK_SLASH:return 53;case KeyEvent.VK_SPACE:return 57;
            case KeyEvent.VK_SHIFT:return e.getKeyLocation()==KeyEvent.KEY_LOCATION_RIGHT?54:42;
            case KeyEvent.VK_LEFT:return 203;case KeyEvent.VK_RIGHT:return 205;case KeyEvent.VK_UP:return 200;
            case KeyEvent.VK_DOWN:return 208;case KeyEvent.VK_DELETE:return 211;default:return 0;
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].equals("--render")){
            Path output=Paths.get(args.length>1?args[1]:"build/reports/ui");Files.createDirectories(output);
            Panel panel=new Panel();panel.setSize(1280,720);
            for(OverlayView.Tab tab:OverlayView.Tab.values()){
                panel.view.setTab(tab);BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
                ImageIO.write(image,"png",output.resolve("forge-"+tab.name().toLowerCase(Locale.ROOT)+".png").toFile());
            }
            System.out.println("Rendered shared UI with synthetic telemetry to "+output);return;
        }
        SwingUtilities.invokeLater(()->{JFrame frame=new JFrame("Forge Client - shared UI preview (not Minecraft)");Panel panel=new Panel();panel.host.close=frame::dispose;frame.setContentPane(panel);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.pack();frame.setLocationRelativeTo(null);frame.setVisible(true);panel.requestFocusInWindow();});
    }
}
