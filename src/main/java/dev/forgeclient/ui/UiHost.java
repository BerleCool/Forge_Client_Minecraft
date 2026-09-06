package dev.forgeclient.ui;
import java.util.List;
public interface UiHost {
    String keyName(int code);
    List<String> profiles();
    String activeProfile();
    void saveProfile(String name);
    void loadProfile(String name);
    void closeScreen();
    void message(String text);
    default boolean keyAvailable(int code){return code!=1&&code!=54;}
}
