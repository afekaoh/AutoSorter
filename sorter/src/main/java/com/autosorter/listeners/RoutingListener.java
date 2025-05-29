package com.autosorter.listeners;

import com.autosorter.AutoSorter;
import com.autosorter.data.ChestDataManager;
import com.autosorter.model.RouterManager;
import com.autosorter.model.SmartChest;
import com.autosorter.model.ChestType;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class RoutingListener implements Listener{

    private final AutoSorter plugin;
    private final ChestDataManager dataManager;
    private final RouterManager routerManager;

    public RoutingListener(AutoSorter plugin, ChestDataManager dataManager, RouterManager routerManager){
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.routerManager = routerManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){

        if(!(event.getWhoClicked() instanceof org.bukkit.entity.Player))
            return;
        Inventory topInventory = event.getView().getTopInventory();

        var chest = SmartChest.from(topInventory.getHolder());
        if(chest.isEmpty())
            return; // Not a valid chest
        // If the chest is not an input chest, we ignore this event
        if(dataManager.getChestType(chest.get()) != ChestType.INPUT)
            return;

        Bukkit.getScheduler().runTask(plugin, () -> routerManager.startRoutingTaskIfNeeded(chest.get()));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event){

        if(!(event.getWhoClicked() instanceof org.bukkit.entity.Player))
            return;
        Inventory dragged = event.getInventory();

        var chest = SmartChest.from(dragged.getHolder());
        if(chest.isEmpty())
            return; // Not a valid chest
        // If the chest is not an input chest, we ignore this event
        if(dataManager.getChestType(chest.get()) != ChestType.INPUT)
            return;

        Bukkit.getScheduler().runTask(plugin, () -> routerManager.startRoutingTaskIfNeeded(chest.get()));
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event){
        Inventory dest = event.getDestination();
        InventoryHolder holder = dest.getHolder();

        var chest = SmartChest.from(holder);
        if(chest.isEmpty())
            return; // Not a valid chest
        // If the chest is not an input chest, we ignore this event
        if(dataManager.getChestType(chest.get()) != ChestType.INPUT)
            return;

        Bukkit.getScheduler().runTask(plugin, () -> routerManager.startRoutingTaskIfNeeded(chest.get()));
    }
}
