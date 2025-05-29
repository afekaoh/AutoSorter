package com.autosorter;

import com.autosorter.commands.SorterCommand;
import com.autosorter.data.ChestDataManager;
import com.autosorter.gui.GuiManager;
import com.autosorter.listeners.ChestDestructionListener;
import com.autosorter.listeners.GuiListener;
import com.autosorter.listeners.RoutingListener;
import com.autosorter.listeners.WandListener;
import com.autosorter.model.RouterManager;
import com.autosorter.utils.ChestPersistenceManager;
import com.autosorter.utils.ChestPersistenceManager.RoutingDataRecord;

import java.io.IOException;
import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;

public class AutoSorter extends JavaPlugin{

    private ChestDataManager chestDataManager;
    private ChestPersistenceManager persistenceManager;

    @Override
    public void onEnable(){
        getLogger().info("AutoSorter is enabling!");
        // Load configuration if needed
        if(!getDataFolder().exists())
            getDataFolder().mkdirs();
        // int intervalMinutes = getConfig().getInt("backup-interval-minutes", 10);
        this.persistenceManager = new ChestPersistenceManager(this);
        // Load existing chest data
        RoutingDataRecord routingDataRecord;
        try {
            routingDataRecord = persistenceManager.loadChestsAndRouting();
        } catch(IOException e){
            getLogger().severe("Failed to load chest data: " + e.getMessage());
            return;
        }

        // Initialize Managers
        this.chestDataManager = new ChestDataManager(
                routingDataRecord.inputChests(),
                routingDataRecord.receiverChestsFilterMap(),
                routingDataRecord.overflowChests());
        GuiManager guiManager = new GuiManager(this.chestDataManager);
        RouterManager routerManager = new RouterManager(this, this.chestDataManager);

        // Register Commands

        // Register Sorter Command might be replaced with a costume item instead of a command.
        Objects.requireNonNull(getCommand("sortchest")).setExecutor(new SorterCommand(this, guiManager));

        // Register Listeners

        // Register GUI Listener
        getServer()
                .getPluginManager()
                .registerEvents(new GuiListener(this, this.chestDataManager, guiManager), this);

        // Register Chest Breaking Listener
        getServer()
                .getPluginManager()
                .registerEvents(new ChestDestructionListener(this.chestDataManager), this);

        // Register Routing Listener
        getServer()
                .getPluginManager()
                .registerEvents(new RoutingListener(this, chestDataManager, routerManager), this);

        // Register Wand Listener
        getServer()
                .getPluginManager()
                .registerEvents(new WandListener(guiManager), this);
    }

    @Override
    public void onDisable(){
        try {
            backupChests();
        } catch(IOException e){
            getLogger().severe("Failed to save chest data: " + e.getMessage());
        }
        getLogger().info("AutoSorter is disabling.");
    }

    // Add getters if other classes need them
    public ChestDataManager getChestDataManager(){
        return chestDataManager;
    }

    public void backupChests() throws IOException{
        // Save any data if needed
        var inputChests = this.chestDataManager.getInputChests();
        var receiverChests = this.chestDataManager.getReceiverChestsFilterMap();
        var overflowChests = this.chestDataManager.getOverflowChests();
        this.persistenceManager.saveChestsAndRouting(inputChests, receiverChests, overflowChests);
        getLogger().info("AutoSorter backup saved to chest_data.yml.");
    }

}