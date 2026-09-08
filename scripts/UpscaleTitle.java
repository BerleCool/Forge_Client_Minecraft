import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.util.Iterator;

/** Dependency-free build helper for the 0.4.1 title asset. */
public final class UpscaleTitle {
    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, w, h, null);
        } finally { g.dispose(); }
        return dst;
    }
    private static BufferedImage sharpen(BufferedImage src) {
        float s = 0.10f;
        float[] kernel = {0,-s,0,-s,1+4*s,-s,0,-s,0};
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        new ConvolveOp(new Kernel(3,3,kernel), ConvolveOp.EDGE_NO_OP, null).filter(src, dst);
        return dst;
    }
    private static void writeJpeg(BufferedImage image, File file) throws Exception {
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpg");
        if (!it.hasNext()) throw new IllegalStateException("No JPEG writer available");
        ImageWriter writer = it.next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            ImageWriteParam p = writer.getDefaultWriteParam();
            if (p.canWriteCompressed()) {
                p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                p.setCompressionQuality(0.92f);
            }
            writer.write(null, new IIOImage(image, null, null), p);
        } finally { writer.dispose(); }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: UpscaleTitle <title.jpg>");
        File file = new File(args[0]);
        BufferedImage src = ImageIO.read(file);
        if (src == null) throw new IllegalArgumentException("Unreadable title image: " + file);
        if (src.getWidth() == 3840 && src.getHeight() == 2160) {
            System.out.println("Title artwork already 3840x2160; keeping existing bytes.");
            return;
        }
        if (src.getWidth() * 9 != src.getHeight() * 16) throw new IllegalStateException("Title artwork must remain 16:9");
        BufferedImage stage = resize(src, 1280, 720);
        BufferedImage full = sharpen(resize(stage, 3840, 2160));
        File tmp = new File(file.getParentFile(), file.getName()+".tmp");
        writeJpeg(full, tmp);
        if (!file.delete() || !tmp.renameTo(file)) throw new IllegalStateException("Could not replace title artwork");
        BufferedImage check = ImageIO.read(file);
        if (check == null || check.getWidth()!=3840 || check.getHeight()!=2160) throw new IllegalStateException("4K verification failed");
        System.out.println("Upscaled title artwork to 3840x2160 ("+file.length()+" bytes)");
    }
}
