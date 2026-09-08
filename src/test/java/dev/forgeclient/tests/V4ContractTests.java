package dev.forgeclient.tests;

import dev.forgeclient.core.ClientModule;
import dev.forgeclient.core.ModuleCatalog;
import dev.forgeclient.core.ModuleImplementationAudit;
import dev.forgeclient.core.ModuleRegistry;
import dev.forgeclient.core.QuickplayCatalog;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class V4ContractTests {
    private V4ContractTests(){}
    public static void main(String[] args){
        ModuleRegistry registry=ModuleCatalog.create();
        if(registry.all().size()!=89)throw new AssertionError("Expected 89 live modules, got "+registry.all().size());
        ModuleImplementationAudit.validate(registry);Map<String,ModuleImplementationAudit.Entry> audit=ModuleImplementationAudit.build(registry);
        if(audit.size()!=89)throw new AssertionError("Audit coverage mismatch: "+audit.size());
        ClientModule quickplay=registry.get("lunar_quickplay");if(!quickplay.available()||!quickplay.holdBinding)throw new AssertionError("Quickplay must be a live action binding");
        List<QuickplayCatalog.Entry> entries=QuickplayCatalog.all();if(entries.size()<18)throw new AssertionError("Quickplay catalog unexpectedly small");
        Set<String> ids=new HashSet<String>(),commands=new HashSet<String>();for(QuickplayCatalog.Entry entry:entries){if(!ids.add(entry.id))throw new AssertionError("Duplicate Quickplay id "+entry.id);if(!entry.command.startsWith("/"))throw new AssertionError("Unsafe Quickplay command "+entry.command);commands.add(entry.command);}
        if(!commands.contains("/play bedwars_eight_one")||!commands.contains("/play bedwars_eight_two")||!commands.contains("/play solo_insane")||!commands.contains("/play duels_classic_duel"))throw new AssertionError("Core Hypixel queues missing");
        if(QuickplayCatalog.filter("BED WARS","double").size()!=1)throw new AssertionError("Quickplay search/category filter broken");
        System.out.println("Forge Client 0.4 contracts: 89/89 implementation audit, "+entries.size()+" Quickplay destinations, no duplicate ids.");
    }
}
