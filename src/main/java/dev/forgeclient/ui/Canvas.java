package dev.forgeclient.ui;
import dev.forgeclient.core.Telemetry;
public interface Canvas {
    void rect(int x,int y,int width,int height,int color);
    void text(String text,int x,int y,int color,boolean shadow);
    int textWidth(String text);
    void push(double x,double y,double scale);
    void pop();
    void clip(Rect bounds);
    void unclip();
    void item(Telemetry.Item item,int x,int y);
    default void grain(int x,int y,int width,int height,int opacity) {}
}
