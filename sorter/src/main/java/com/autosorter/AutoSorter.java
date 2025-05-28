package com.autosorter;

import com.autosorter.commands.SorterCommand;
import com.autosorter.data.ChestDataManager;
import com.autosorter.gui.GuiManager;
import com.autosorter.listeners.ChestDestructionListener;
import com.autosorter.listeners.GuiListener;
import com.autosorter.listeners.RoutingListener;
import com.autosorter.model.RouterManager;
import com.autosorter.utils.ChestPersistenceManager;
import com.autosorter.utils.ChestPersistenceManager.SetTransfer;

import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

public class AutoSorter extends JavaPlugin {

    private ChestDataManager chestDataManager;
    private GuiManager guiManager;
    private RouterManager routerManager;
    private ChestPersistenceManager persistenceManager;

    @Override
    public void onEnable() {
        getLogger().info("AutoSorter is enabling!");
        // Load configuration if needed
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        // int intervalMinutes = getConfig().getInt("backup-interval-minutes", 10);
        this.persistenceManager = new ChestPersistenceManager(this);
        // Load existing chest data
        SetTransfer chestSets = null;
        try {
            chestSets = persistenceManager.loadChestsAndRouting();
        } catch (IOException e) {
            getLogger().severe("Failed to load chest data: " + e.getMessage());
        }
        // Initialize Managers
        this.chestDataManager = new ChestDataManager(
                this,
                chestSets.getInputChests(),
                chestSets.getReceiverChestsFilterMap(),
                chestSets.getOverflowChests());
        this.guiManager = new GuiManager(this, this.chestDataManager);
        this.routerManager = new RouterManager(this, this.chestDataManager);

        // Register Commands

        // Register Sorter Command might be replaced with a costume item instad of a
        // command.
        getCommand("sortchest")
                .setExecutor(new SorterCommand(this, this.guiManager));

        // Register Listeners

        // Register GUI Listener
        getServer()
                .getPluginManager()
                .registerEvents(new GuiListener(this, this.chestDataManager, this.guiManager), this);

        // Register Chest Breaking Listener
        getServer()
                .getPluginManager()
                .registerEvents(new ChestDestructionListener(this.chestDataManager), this);

        // Register Routing Listener
        getServer()
                .getPluginManager()
                .registerEvents(new RoutingListener(this, chestDataManager, routerManager), this);
    }

    @Override
    public void onDisable() {
        try {
            backupChests();
        } catch (IOException e) {
            getLogger().severe("Failed to save chest data: " + e.getMessage());
        }
        getLogger().info("AutoSorter is disabling.");
    }

    // Add getters if other classes need them
    public ChestDataManager getChestDataManager() {
        return chestDataManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public void backupChests() throws IOException {
        // Save any data if needed
        var inputChests = this.chestDataManager.getInputChests();
        var receiverChests = this.chestDataManager.getReceiverChestsFilterMap();
        var overflowChests = this.chestDataManager.getOverflowChests();
        this.persistenceManager.saveChestsAndRouting(inputChests, receiverChests, overflowChests);
        getLogger().info("AutoSorter backup saved to chest_data.yml.");
    }

}