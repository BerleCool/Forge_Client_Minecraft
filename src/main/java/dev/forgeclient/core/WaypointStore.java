package dev.forgeclient.core;
import java.io.IOException;
import java.util.*;
public final class WaypointStore {
    private final List<Waypoint> points=new ArrayList<>();
    public List<Waypoint> all(){return Collections.unmodifiableList(points);}
    public void add(Waypoint point){
        for(Waypoint p:points)if(p.visible(point.world,point.dimension)&&p.name.equalsIgnoreCase(point.name))throw new IllegalArgumentException("Waypoint already exists");
        if(points.size()>=64)throw new IllegalStateException("Maximum 64 waypoints");points.add(point);
    }
    public boolean remove(String name,String world,int dimension){return points.removeIf(p->p.visible(world,dimension)&&p.name.equalsIgnoreCase(name));}
    public Waypoint nearest(String world,int dimension,double x,double y,double z){Waypoint nearest=null;double best=Double.POSITIVE_INFINITY;for(Waypoint p:points)if(p.visible(world,dimension)){double d=p.distance(x,y,z);if(d<best){best=d;nearest=p;}}return nearest;}
    public Properties encode(){
        Properties p=new Properties();p.setProperty("schema","1");p.setProperty("count",Integer.toString(points.size()));
        for(int i=0;i<points.size();i++){Waypoint v=points.get(i);String k="point."+i+".";p.setProperty(k+"name",v.name);p.setProperty(k+"world",v.world);p.setProperty(k+"dimension",Integer.toString(v.dimension));p.setProperty(k+"x",Double.toString(v.x));p.setProperty(k+"y",Double.toString(v.y));p.setProperty(k+"z",Double.toString(v.z));}return p;
    }
    public void decode(Properties p)throws IOException{
        try{
            if(!"1".equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported waypoint schema");
            int count=Integer.parseInt(p.getProperty("count","0"));if(count<0||count>64)throw new IllegalArgumentException("Invalid waypoint count");
            WaypointStore staged=new WaypointStore();
            for(int i=0;i<count;i++){String k="point."+i+".";staged.add(new Waypoint(p.getProperty(k+"name"),p.getProperty(k+"world"),Integer.parseInt(p.getProperty(k+"dimension")),Double.parseDouble(p.getProperty(k+"x")),Double.parseDouble(p.getProperty(k+"y")),Double.parseDouble(p.getProperty(k+"z"))));}
            points.clear();points.addAll(staged.points);
        }catch(RuntimeException e){throw new IOException("Invalid waypoint file",e);}
    }
}
