package dev.forgeclient.minecraft;
import dev.forgeclient.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import java.util.*;

/** Registered with ClientCommandHandler: these commands never go to the server. */
public final class ForgeCommand extends CommandBase {
    private final ForgeClient client;
    public ForgeCommand(ForgeClient client){this.client=client;}
    public String getCommandName(){return "forgeclient";}
    public String getCommandUsage(ICommandSender sender){return "/forgeclient [hud | preset <name> | waypoint <add|remove|list> [name]]";}
    public int getRequiredPermissionLevel(){return 0;}
    public void processCommand(ICommandSender sender,String[] args)throws CommandException{
        Minecraft mc=Minecraft.getMinecraft();
        if(args.length==0){client.requestOpen();return;}
        try{
            if(args[0].equalsIgnoreCase("hud")){ForgeScreen screen=new ForgeScreen(null);screen.openHud();mc.displayGuiScreen(screen);return;}
            if(args[0].equalsIgnoreCase("preset")&&args.length==2){client.modules.preset(args[1].toLowerCase(Locale.ROOT));client.snapshot();client.message("Preset applied: "+args[1]);return;}
            if(!args[0].equalsIgnoreCase("waypoint")){client.message(getCommandUsage(sender));return;}
            if(mc.thePlayer==null||mc.theWorld==null){client.message("Join a world to manage its waypoints.");return;}
            String world=client.sampler.worldKey();int dimension=mc.thePlayer.dimension;
            if(args.length<2||args[1].equalsIgnoreCase("list")){
                int count=0;for(Waypoint p:client.waypoints.all())if(p.visible(world,dimension)){client.message(p.name+" / "+Math.round(p.distance(mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ))+" m");count++;}
                if(count==0)client.message("No waypoints here. /forgeclient waypoint add Home");return;
            }
            if(args.length<3){client.message("Provide a waypoint name.");return;}
            String name=String.join(" ",Arrays.copyOfRange(args,2,args.length));
            Properties before=client.waypoints.encode();boolean mutated=false;
            try{
                if(args[1].equalsIgnoreCase("add")){client.waypoints.add(new Waypoint(name,world,dimension,mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ));mutated=true;}
                else if(args[1].equalsIgnoreCase("remove")){mutated=client.waypoints.remove(name,world,dimension);if(!mutated){client.message("Waypoint not found in this world.");return;}}
                else {client.message(getCommandUsage(sender));return;}
                client.saveWaypoints();client.message("Waypoint "+(args[1].equalsIgnoreCase("add")?"saved: ":"removed: ")+name);
            }catch(java.io.IOException error){if(mutated)client.waypoints.decode(before);throw error;}
        }catch(java.io.IOException|IllegalArgumentException|IllegalStateException error){client.message("Could not apply command: "+error.getMessage());}
    }
    public List<String> addTabCompletionOptions(ICommandSender sender,String[] args,BlockPos pos){
        if(args.length==1)return getListOfStringsMatchingLastWord(args,"hud","preset","waypoint");
        if(args.length==2&&args[0].equalsIgnoreCase("preset"))return getListOfStringsMatchingLastWord(args,"default","minimal","pvp","explorer");
        if(args.length==2&&args[0].equalsIgnoreCase("waypoint"))return getListOfStringsMatchingLastWord(args,"add","remove","list");
        return Collections.emptyList();
    }
}
