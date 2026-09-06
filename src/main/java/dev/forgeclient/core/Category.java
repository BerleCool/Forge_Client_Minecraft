package dev.forgeclient.core;
public enum Category {
    HUD("HUD"), VISUAL("Visuals"), PERFORMANCE("Performance"), UTILITY("Utilities");
    public final String title;
    Category(String title) { this.title=title; }
}
