package dev.forgeclient.core;
import java.util.Objects;
public final class ChatDeduplicator {
    private String accepted;private long at;private boolean present;
    public boolean suppress(String text,long now,long window){if(present&&Objects.equals(text,accepted)&&now-at>=0&&now-at<window)return true;accepted=text;at=now;present=true;return false;}
    public void clear(){accepted=null;at=0;present=false;}
}
