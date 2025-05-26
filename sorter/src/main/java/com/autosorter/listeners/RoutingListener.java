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

public class RoutingListener implements Listener {

    private final AutoSorter plugin;
    private final ChestDataManager dataManager;
    private final RouterManager routerManager;

    public RoutingListener(AutoSorter plugin, ChestDataManager dataManager, RouterManager routerManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.routerManager = routerManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player))
            return;
        Inventory topInventory = event.getView().getTopInventory();

        if (topInventory == null)
            return;

        try {
            SmartChest chest = new SmartChest(topInventory.getHolder());

            if (dataManager.getChestType(chest) != ChestType.INPUT)
                return;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                routerManager.startRoutingTaskIfNeeded(chest);
            }, 1L);
        } catch (IllegalArgumentException e) {
            // If top inventory is not a chest
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player))
            return;
        Inventory dragged = event.getInventory();
        try {
            if (dragged == null)
                return;
            SmartChest chest = new SmartChest(dragged.getHolder());
            if (dataManager.getChestType(chest) != ChestType.INPUT)
                return;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                routerManager.startRoutingTaskIfNeeded(chest);
            }, 1L);
        } catch (IllegalArgumentException e) {
            // If the holder is not a Chest, we ignore this event
            return;
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory dest = event.getDestination();
        InventoryHolder holder = dest.getHolder();
        try {
            if (holder == null)
                return;
            SmartChest chest = new SmartChest(holder);
            if (dataManager.getChestType(chest) != ChestType.INPUT)
                return;
            // Valid move into an input chest — route the item
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                routerManager.startRoutingTaskIfNeeded(chest);
            }, 1L);
        } catch (IllegalArgumentException e) {
            // If the holder is not a Chest, we ignore this event
            return;
        }
    }
}
