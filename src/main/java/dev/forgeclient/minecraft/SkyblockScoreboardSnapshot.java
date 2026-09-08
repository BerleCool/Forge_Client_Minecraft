package dev.forgeclient.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Immutable sidebar snapshot. The filter/cap/format pipeline is adapted from
 * BiscuitDevelopment/SkyblockAddons' MIT-licensed ScoreboardManager, rewritten
 * without its globals, Guava, Lombok, streams, or background data services.
 */
public final class SkyblockScoreboardSnapshot {
    private static final SkyblockScoreboardSnapshot EMPTY=new SkyblockScoreboardSnapshot("","",Collections.<String>emptyList(),Collections.<String>emptyList());
    public final String formattedTitle,plainTitle;
    private final List<String> formatted,plain;
    private SkyblockScoreboardSnapshot(String formattedTitle,String plainTitle,List<String> formatted,List<String> plain){this.formattedTitle=formattedTitle;this.plainTitle=plainTitle;this.formatted=formatted;this.plain=plain;}
    public static SkyblockScoreboardSnapshot empty(){return EMPTY;}
    public static SkyblockScoreboardSnapshot capture(Minecraft mc){
        if(mc==null||mc.theWorld==null)return EMPTY;
        Scoreboard board=mc.theWorld.getScoreboard();if(board==null)return EMPTY;
        ScoreObjective objective=board.getObjectiveInDisplaySlot(1);if(objective==null)return EMPTY;
        List<Score> filtered=new ArrayList<Score>();Collection<Score> scores=board.getSortedScores(objective);
        for(Score score:scores){String name=score.getPlayerName();if(name!=null&&!name.startsWith("#"))filtered.add(score);}
        if(filtered.size()>15)filtered=new ArrayList<Score>(filtered.subList(filtered.size()-15,filtered.size()));
        // Vanilla lays ascending scores from bottom to top. Reverse for a top-down HUD list.
        Collections.reverse(filtered);
        List<String> formatted=new ArrayList<String>(filtered.size()),plain=new ArrayList<String>(filtered.size());
        for(Score score:filtered){
            String name=score.getPlayerName();String line=ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name),name).trim();
            formatted.add(line);plain.add(strip(line));
        }
        return new SkyblockScoreboardSnapshot(objective.getDisplayName(),strip(objective.getDisplayName()),Collections.unmodifiableList(formatted),Collections.unmodifiableList(plain));
    }
    private static String strip(String text){String out=EnumChatFormatting.getTextWithoutFormattingCodes(text==null?"":text);if(out==null)return "";StringBuilder clean=new StringBuilder(out.length());for(int i=0;i<out.length();i++){char c=out.charAt(i);if(c>=32&&c!=127)clean.append(c);}return clean.toString().trim();}
    public boolean present(){return !formattedTitle.isEmpty()||!formatted.isEmpty();}
    public List<String> formattedLines(){return formatted;}
    public List<String> plainLines(){return plain;}
    public String[] rows(int maxLines){
        if(!present())return new String[]{"NO SCOREBOARD"};int max=Math.max(1,maxLines),count=Math.min(max,formatted.size());List<String> out=new ArrayList<String>(count+1);if(!formattedTitle.isEmpty())out.add(formattedTitle);for(int i=0;i<count;i++)out.add(formatted.get(i));return out.toArray(new String[out.size()]);
    }
    public String plainJoined(){StringBuilder out=new StringBuilder(plainTitle);for(String line:plain)out.append('\n').append(line);return out.toString();}
    public boolean containsIgnoreCase(String needle){return plainJoined().toLowerCase(java.util.Locale.ROOT).contains(needle.toLowerCase(java.util.Locale.ROOT));}
}
