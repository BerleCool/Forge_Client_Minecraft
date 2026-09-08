package dev.forgeclient.core;
public final class Waypoint {
    public final String name,world;public final int dimension;public final double x,y,z;
    public Waypoint(String name,String world,int dimension,double x,double y,double z){
        if(name==null||!name.matches("[a-zA-Z0-9][a-zA-Z0-9 _-]{0,31}")||world==null||world.isEmpty()||world.length()>128)throw new IllegalArgumentException("Invalid waypoint name/world");
        if(!valid(x)||!valid(y)||!valid(z))throw new IllegalArgumentException("Waypoint coordinate out of bounds");
        this.name=name;this.world=world;this.dimension=dimension;this.x=x;this.y=y;this.z=z;
    }
    private static boolean valid(double n){return Double.isFinite(n)&&Math.abs(n)<=30000000;}
    public boolean visible(String world,int dimension){return this.world.equals(world)&&this.dimension==dimension;}
    public double distance(double px,double py,double pz){return Math.sqrt((px-x)*(px-x)+(py-y)*(py-y)+(pz-z)*(pz-z));}
}
