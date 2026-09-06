package dev.forgeclient.core;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Validated, deterministic configuration value. Mutations notify the owning module. */
public final class Setting {
    public enum Kind { BOOLEAN, NUMBER, CHOICE }
    public final String id,name,description;
    public final Kind kind;
    public final double min,max,step;
    public final List<String> choices;
    private final String initial;
    private String value;
    private Runnable changed=()->{};
    private Setting(String id,String name,String description,Kind kind,String initial,double min,double max,double step,String... options) {
        if(id==null||!id.matches("[a-z0-9_]+"))throw new IllegalArgumentException("Invalid setting id");
        this.id=id;this.name=name;this.description=description;this.kind=kind;
        this.min=min;this.max=max;this.step=step;
        this.choices=Collections.unmodifiableList(Arrays.asList(options.clone()));
        if(!valid(initial))throw new IllegalArgumentException("Invalid default for "+id);
        this.initial=kind==Kind.NUMBER?normalized(Double.parseDouble(initial)):initial;
        value=this.initial;
    }
    public static Setting bool(String id,String name,String description,boolean initial) {
        return new Setting(id,name,description,Kind.BOOLEAN,Boolean.toString(initial),0,1,1);
    }
    public static Setting number(String id,String name,String description,double initial,double min,double max,double step) {
        if(!Double.isFinite(min)||!Double.isFinite(max)||!Double.isFinite(step)||min>max||step<=0||!Double.isFinite(initial))
            throw new IllegalArgumentException("Invalid numeric bounds");
        return new Setting(id,name,description,Kind.NUMBER,Double.toString(initial),min,max,step);
    }
    public static Setting choice(String id,String name,String description,String initial,String... options) {
        if(options==null||options.length==0)throw new IllegalArgumentException("No choices");
        return new Setting(id,name,description,Kind.CHOICE,initial,0,options.length-1,1,options);
    }
    void onChange(Runnable listener){changed=Objects.requireNonNull(listener);}
    private boolean valid(String candidate) {
        if(candidate==null)return false;
        if(kind==Kind.BOOLEAN)return candidate.equals("true")||candidate.equals("false");
        if(kind==Kind.CHOICE)return choices.contains(candidate);
        try{return !candidate.trim().isEmpty()&&Double.isFinite(Double.parseDouble(candidate));}
        catch(NumberFormatException e){return false;}
    }
    private String normalized(double number) {
        double clamped=Math.max(min,Math.min(max,number));
        double quantized=min+Math.round((clamped-min)/step)*step;
        BigDecimal v=BigDecimal.valueOf(Math.max(min,Math.min(max,quantized)));
        // Eliminate binary rounding tails while retaining sub-pixel settings.
        return v.setScale(8,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
    public boolean set(String candidate) {
        if(!valid(candidate))return false;
        String next=kind==Kind.NUMBER?normalized(Double.parseDouble(candidate)):candidate;
        if(!next.equals(value)){value=next;changed.run();}
        return true;
    }
    public String raw(){return value;}
    public String display(){return kind==Kind.BOOLEAN?(bool()?"On":"Off"):value;}
    public boolean bool(){return Boolean.parseBoolean(value);}
    public double number(){return Double.parseDouble(value);}
    public int integer(){return (int)Math.round(number());}
    public void toggle(){if(kind==Kind.BOOLEAN)set(Boolean.toString(!bool()));}
    public void cycle(int direction){if(kind==Kind.CHOICE)set(choices.get(Math.floorMod(choices.indexOf(value)+direction,choices.size())));}
    public void fraction(double fraction){if(kind==Kind.NUMBER&&Double.isFinite(fraction))set(Double.toString(min+Math.max(0,Math.min(1,fraction))*(max-min)));}
    public void reset(){set(initial);}
}
