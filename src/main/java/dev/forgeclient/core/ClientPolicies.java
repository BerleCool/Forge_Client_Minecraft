package dev.forgeclient.core;
public final class ClientPolicies {
    private ClientPolicies(){}
    public static int frameCap(boolean focused,boolean menu,int background,int menuCap,int vanilla){
        if(focused&&!menu)return 0;int requested=focused?menuCap:background;
        return Math.max(1,vanilla>0?Math.min(requested,vanilla):requested);
    }
    public static boolean maySprint(boolean focused,boolean gui,boolean world,float forward,boolean sneak,boolean usingItem,boolean collision,boolean blind,int food,boolean flyingAllowed){
        return focused&&!gui&&world&&forward>=.8F&&!sneak&&!usingItem&&!collision&&!blind&&(food>6||flyingAllowed);
    }
}
