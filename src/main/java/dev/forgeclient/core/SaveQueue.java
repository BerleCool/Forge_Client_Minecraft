package dev.forgeclient.core;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Single-writer debounce. Snapshots contain strings only, never mutable game objects. */
public final class SaveQueue implements AutoCloseable {
    private final ProfileStore store;
    private final Consumer<IOException> onError;
    private final ScheduledExecutorService worker;
    private final Map<String,Properties> pending=new LinkedHashMap<>();
    private ScheduledFuture<?> scheduled;
    private IOException failure;
    private boolean closed;
    public SaveQueue(ProfileStore store,Consumer<IOException> onError){
        this.store=store;this.onError=onError;
        worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Forge-Config-Writer");t.setDaemon(true);return t;});
    }
    public synchronized void submit(String name,Properties source){
        if(closed)return;
        Properties copy=new Properties();copy.putAll(source);pending.put(name,copy);
        if(scheduled!=null)scheduled.cancel(false);
        scheduled=worker.schedule(this::drain,300,TimeUnit.MILLISECONDS);
    }
    private void drain(){
        Map<String,Properties> batch;
        synchronized(this){batch=new LinkedHashMap<>(pending);pending.clear();}
        for(Map.Entry<String,Properties> entry:batch.entrySet()){
            try{store.save(entry.getKey(),entry.getValue());}
            catch(IOException e){
                synchronized(this){failure=e;}
                try{onError.accept(e);}catch(RuntimeException ignored){/* Error reporters must not kill the writer. */}
            }
        }
    }
    public void flush()throws IOException{
        Future<?> barrier;
        synchronized(this){
            if(closed){if(failure!=null)throw failure;return;}
            if(scheduled!=null)scheduled.cancel(false);
            barrier=worker.submit(this::drain);
        }
        try{barrier.get();}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Interrupted while saving",e);}
        catch(ExecutionException e){throw new IOException("Configuration writer failed",e.getCause());}
        synchronized(this){if(failure!=null){IOException error=failure;failure=null;throw error;}}
    }
    @Override public void close(){
        synchronized(this){if(closed)return;}
        try{flush();}catch(IOException ignored){/* Already surfaced through onError. */}
        synchronized(this){closed=true;worker.shutdown();}
        try{if(!worker.awaitTermination(5,TimeUnit.SECONDS))worker.shutdownNow();}
        catch(InterruptedException e){Thread.currentThread().interrupt();worker.shutdownNow();}
    }
}
