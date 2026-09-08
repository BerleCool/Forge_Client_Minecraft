package dev.forgeclient.minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import java.util.Collections;
import java.util.Set;
public final class ForgeGuiFactory implements IModGuiFactory {
    public void initialize(Minecraft minecraft){}
    public Class<? extends GuiScreen> mainConfigGuiClass(){return ForgeScreen.class;}
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories(){return Collections.emptySet();}
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element){return null;}
}
