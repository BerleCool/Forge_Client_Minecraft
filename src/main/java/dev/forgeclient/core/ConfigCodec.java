package dev.forgeclient.core;

import java.util.Properties;

public final class ConfigCodec {
    private ConfigCodec(){}
    public static Properties encode(ModuleRegistry registry){
        Properties p=new Properties();p.setProperty("schema","1");
        for(ClientModule m:registry.all()){
            String k="module."+m.id+".";
            p.setProperty(k+"enabled",Boolean.toString(m.enabled()));p.setProperty(k+"favorite",Boolean.toString(m.favorite()));
            p.setProperty(k+"key",Integer.toString(m.key()));p.setProperty(k+"x",Double.toString(m.placement.x()));p.setProperty(k+"y",Double.toString(m.placement.y()));
            for(Setting s:m.settings())p.setProperty(k+"setting."+s.id,s.raw());
        }
        return p;
    }
    public static void decode(Properties p,ModuleRegistry registry){
        if(p==null||!"1".equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported configuration schema");
        for(ClientModule m:registry.all()){
            m.reset();String k="module."+m.id+".";
            String e=p.getProperty(k+"enabled"),f=p.getProperty(k+"favorite");
            if("true".equals(e)||"false".equals(e))m.enabled(Boolean.parseBoolean(e));
            if("true".equals(f)||"false".equals(f))m.favorite(Boolean.parseBoolean(f));
            try{int key=Integer.parseInt(p.getProperty(k+"key","0"));if(!registry.isKeyUsed(key,m))m.bind(key);}catch(NumberFormatException ignored){}
            double x=m.placement.x(),y=m.placement.y();
            try{x=Double.parseDouble(p.getProperty(k+"x",Double.toString(x)));}catch(NumberFormatException ignored){}
            try{y=Double.parseDouble(p.getProperty(k+"y",Double.toString(y)));}catch(NumberFormatException ignored){}
            m.placement.set(x,y);
            for(Setting s:m.settings())if(p.containsKey(k+"setting."+s.id))s.set(p.getProperty(k+"setting."+s.id));
        }
    }
}
