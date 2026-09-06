package dev.forgeclient.minecraft;

/** Detection only. OptiFine is proprietary and is intentionally not redistributed in Forge Client. */
public final class OptiFineCompatibility {
    private static final boolean PRESENT=detect();
    private OptiFineCompatibility(){}
    public static boolean present(){return PRESENT;}
    private static boolean detect(){ClassLoader loader=OptiFineCompatibility.class.getClassLoader();for(String name:new String[]{"Config","optifine.OptiFineForgeTweaker"})try{Class.forName(name,false,loader);return true;}catch(Throwable ignored){}return false;}
}
