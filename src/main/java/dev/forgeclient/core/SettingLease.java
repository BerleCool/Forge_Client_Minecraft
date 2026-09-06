package dev.forgeclient.core;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
/** Temporarily override a vanilla setting without overwriting a newer user/mod change. */
public final class SettingLease<T> {
    private final Supplier<T> read;private final Consumer<T> write;
    private T before,last;private boolean owned;
    public SettingLease(Supplier<T> read,Consumer<T> write){this.read=read;this.write=write;}
    public void apply(T next){T current=read.get();if(!owned||!Objects.equals(current,last))before=current;if(!Objects.equals(current,next))write.accept(next);last=next;owned=true;}
    public void release(){if(!owned)return;if(Objects.equals(read.get(),last)&&!Objects.equals(last,before))write.accept(before);owned=false;before=last=null;}
    public boolean owned(){return owned;}
}
