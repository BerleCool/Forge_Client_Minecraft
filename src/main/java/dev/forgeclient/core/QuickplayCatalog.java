package dev.forgeclient.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Small, deterministic Hypixel queue catalog for Forge Client's local selector.
 * The UI and control flow are independently implemented. Queue identifiers are
 * public server command identifiers, not copied Quickplay source code.
 */
public final class QuickplayCatalog {
    public static final class Entry {
        public final String id, category, name, description, command;
        private Entry(String id, String category, String name, String description, String command) {
            this.id=id;this.category=category;this.name=name;this.description=description;this.command=command;
        }
        public boolean matches(String query) {
            String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
            return q.isEmpty() || (id+" "+category+" "+name+" "+description+" "+command).toLowerCase(Locale.ROOT).contains(q);
        }
    }

    private static final List<Entry> ALL;
    private static final List<String> CATEGORIES;
    static {
        List<Entry> e=new ArrayList<Entry>();
        add(e,"bw_solo","BED WARS","Solo","8 teams / 1 player","/play bedwars_eight_one");
        add(e,"bw_doubles","BED WARS","Doubles","8 teams / 2 players","/play bedwars_eight_two");
        add(e,"bw_3v3v3v3","BED WARS","3v3v3v3","4 teams / 3 players","/play bedwars_four_three");
        add(e,"bw_4v4v4v4","BED WARS","4v4v4v4","4 teams / 4 players","/play bedwars_four_four");
        add(e,"sw_solo_normal","SKYWARS","Solo Normal","Solo SkyWars / Normal kits","/play solo_normal");
        add(e,"sw_solo_insane","SKYWARS","Solo Insane","Solo SkyWars / Insane kits","/play solo_insane");
        add(e,"sw_teams_normal","SKYWARS","Teams Normal","Teams SkyWars / Normal kits","/play teams_normal");
        add(e,"sw_teams_insane","SKYWARS","Teams Insane","Teams SkyWars / Insane kits","/play teams_insane");
        add(e,"duel_classic","DUELS","Classic Duel","Classic 1v1","/play duels_classic_duel");
        add(e,"duel_uhc","DUELS","UHC Duel","UHC 1v1","/play duels_uhc_duel");
        add(e,"duel_sumo","DUELS","Sumo Duel","Sumo 1v1","/play duels_sumo_duel");
        add(e,"duel_op","DUELS","OP Duel","OP 1v1","/play duels_op_duel");
        add(e,"duel_bridge","DUELS","Bridge Duel","Bridge 1v1","/play duels_bridge_duel");
        add(e,"duel_bow","DUELS","Bow Duel","Bow 1v1","/play duels_bow_duel");
        add(e,"lobby_main","LOBBIES","Main Lobby","Return to the Hypixel lobby","/lobby");
        add(e,"lobby_bedwars","LOBBIES","Bed Wars Lobby","Open the Bed Wars lobby","/lobby bedwars");
        add(e,"lobby_skywars","LOBBIES","SkyWars Lobby","Open the SkyWars lobby","/lobby skywars");
        add(e,"lobby_duels","LOBBIES","Duels Lobby","Open the Duels lobby","/lobby duels");
        add(e,"housing_home","OTHER","Housing Home","Go to your Housing home","/home");
        ALL=Collections.unmodifiableList(e);
        Set<String> cats=new LinkedHashSet<String>();
        cats.add("ALL");for(Entry entry:e)cats.add(entry.category);
        CATEGORIES=Collections.unmodifiableList(new ArrayList<String>(cats));
    }
    private QuickplayCatalog(){}
    private static void add(List<Entry> out,String id,String category,String name,String description,String command){out.add(new Entry(id,category,name,description,command));}
    public static List<Entry> all(){return ALL;}
    public static List<String> categories(){return CATEGORIES;}
    public static List<Entry> filter(String category,String query){
        List<Entry> out=new ArrayList<Entry>();String c=category==null?"ALL":category;
        for(Entry entry:ALL)if(("ALL".equals(c)||entry.category.equals(c))&&entry.matches(query))out.add(entry);
        return out;
    }
    public static int defaultIndex(List<Entry> entries,String legacyMode){
        if(entries==null||entries.isEmpty())return -1;
        String wanted="Bed Wars Solo".equals(legacyMode)?"bw_solo":"Bed Wars Doubles".equals(legacyMode)?"bw_doubles":"SkyWars Solo".equals(legacyMode)?"sw_solo_normal":"Duels".equals(legacyMode)?"duel_classic":"Lobby".equals(legacyMode)?"lobby_main":"bw_solo";
        for(int i=0;i<entries.size();i++)if(entries.get(i).id.equals(wanted))return i;
        return 0;
    }
}
