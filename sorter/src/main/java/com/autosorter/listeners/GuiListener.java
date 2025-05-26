package com.autosorter.listeners;

import com.autosorter.AutoSorter;
import com.autosorter.data.ChestDataManager;
import com.autosorter.gui.ConfigGuiHolder;
import com.autosorter.gui.GuiManager;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuiListener implements Listener {

    private final AutoSorter plugin;
    private final ChestDataManager dataManager;
    private final GuiManager guiManager; // Need this to refresh GUI

    public GuiListener(AutoSorter plugin, ChestDataManager dataManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return; // Ignore clicks outside of any inventory
        if (!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder))
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        SmartChest chest = holder.getChest();
        boolean isReceiver = dataManager.getChestType(chest) == ChestType.RECEIVER;

        Inventory clickedInventory = event.getClickedInventory();
        Inventory guiInventory = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();

        boolean isFilterSlot = rawSlot >= GuiManager.FILTER_START_SLOT && rawSlot <= GuiManager.FILTER_END_SLOT;

        // --- Button Clicks ---
        ChestType oldType = dataManager.getChestType(chest);
        ChestType newType = null;
        if (rawSlot == GuiManager.INPUT_BUTTON_SLOT ||
                rawSlot == GuiManager.RECEIVER_BUTTON_SLOT ||
                rawSlot == GuiManager.OVERFLOW_BUTTON_SLOT ||
                rawSlot == GuiManager.NONE_BUTTON_SLOT ||
                rawSlot == GuiManager.SAVE_BUTTON_SLOT) {
            event.setCancelled(true); // prevent item movement
        }
        if (rawSlot == GuiManager.INPUT_BUTTON_SLOT) {
            newType = ChestType.INPUT;
        } else if (rawSlot == GuiManager.RECEIVER_BUTTON_SLOT) {
            newType = ChestType.RECEIVER;
        } else if (rawSlot == GuiManager.OVERFLOW_BUTTON_SLOT) {
            newType = ChestType.OVERFLOW;
        } else if (rawSlot == GuiManager.NONE_BUTTON_SLOT) {
            newType = ChestType.NONE;
        } else if (rawSlot == GuiManager.SAVE_BUTTON_SLOT) {
            player.closeInventory(); // Will trigger InventoryCloseEvent for saving
            return;
        }

        if (newType != null && newType != oldType) {
            dataManager.setChestType(chest, newType);
            GuiManager.markIgnoreClose(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openConfigGui(player, chest));
            return;
        }

        // ----- Shift-click from player inventory -----
        if (event.isShiftClick() && clickedInventory.equals(player.getInventory()) && isReceiver) {
            ItemStack sourceStack = event.getCurrentItem();
            if (sourceStack == null || sourceStack.getType().isAir())
                return;

            event.setCancelled(true);

            int availableSlot = getFirstEmptyFilterSlot(guiInventory);
            if (availableSlot == -1) {
                player.sendMessage("§cNo empty filter slots available.");
                return; // No empty slot found
            }

            boolean isItemAlreadyPresentInFilter = isItemInFilter(guiInventory, sourceStack.getType());

            if (isItemAlreadyPresentInFilter) {
                player.sendMessage("§cThat item is already in the filter.");
                return;
            }

            ItemStack singleItemFromSourceStack = sourceStack.clone();
            singleItemFromSourceStack.setAmount(1);
            guiInventory.setItem(availableSlot, singleItemFromSourceStack);

            sourceStack.setAmount(sourceStack.getAmount() - 1); // Sets new amount in player inventory to be 1 less
            player.getInventory().setItem(event.getSlot(), sourceStack.getAmount() > 0 ? sourceStack : null);

            return;
        }

        // ----- Filter slot interaction -----
        if (isReceiver && isFilterSlot) {
            event.setCancelled(true);

            ItemStack cursor = player.getItemOnCursor();
            ItemStack clickedSlot = guiInventory.getItem(rawSlot);

            // Case 0 - Shift-click in filter slot

            if (event.isShiftClick()) {
                if (clickedSlot == null || clickedSlot.getType().isAir()) {
                    return; // No item in clicked slot
                }
                int playerInventorySlot = player.getInventory().firstEmpty();

                if (playerInventorySlot == -1) // no empty slot available
                    return;

                player.getInventory().setItem(playerInventorySlot, clickedSlot.clone());
                guiInventory.setItem(rawSlot, null);
                event.setCurrentItem(null);
                return;
            }

            // Case 1 - cursor is empty, filter slot has item

            // Remove item from slot and set on cursor
            if (cursor == null || cursor.getType().isAir()) {
                if (clickedSlot != null && !clickedSlot.getType().isAir()) {
                    player.setItemOnCursor(clickedSlot);
                    guiInventory.setItem(rawSlot, null);
                }

                return;
            }

            // Case 2 - Cursor isn't empty

            // Swapping contents if no dupes:
            if (clickedSlot != null && !clickedSlot.getType().isAir()) {
                if (isItemInFilter(guiInventory, cursor.getType())) {
                    player.sendMessage("§cThat item is already in the filter.");
                    return; // Item already exists in the filter
                }

                ItemStack single = cursor.clone();
                single.setAmount(1);

                if (cursor.getAmount() == 1) {
                    player.setItemOnCursor(clickedSlot);
                } else {
                    cursor.setAmount(cursor.getAmount() - 1);

                    // cursor has more than one item in the stack, moving leftovers to inventory or
                    // dropping if there's no room

                    int playerInventorySlot = player.getInventory().firstEmpty();

                    if (playerInventorySlot == -1) // no empty slot available
                        player.getWorld().dropItemNaturally(player.getLocation(), cursor.clone());
                    else
                        player.getInventory().setItem(playerInventorySlot, cursor.clone());

                    player.setItemOnCursor(clickedSlot);
                }

                guiInventory.setItem(rawSlot, single);

                return;
            }

            // Case 3 - Cursor has items, filter slot is empty

            if (isItemInFilter(guiInventory, cursor.getType())) {
                player.sendMessage("§cThat item is already in the filter.");
                return; // Item already exists in the filter
            }

            ItemStack single = cursor.clone();
            single.setAmount(1);
            guiInventory.setItem(rawSlot, single);

            if (cursor.getAmount() == 1) {
                player.setItemOnCursor(null);
            } else {
                cursor.setAmount(cursor.getAmount() - 1);
                player.setItemOnCursor(cursor);
            }
            return;
        }
        // Block all other GUI clicks
        if (clickedInventory.equals(guiInventory)) {
            event.setCancelled(true);
        }
    }

    private boolean isItemInFilter(Inventory guiInventory, Material itemType) {

        for (int i = GuiManager.FILTER_START_SLOT; i <= GuiManager.FILTER_END_SLOT; i++) {
            ItemStack slotItem = guiInventory.getItem(i); // Get item from the GUI slot

            if (slotItem != null && slotItem.getType() == itemType)
                return true; // Item already exists in the filter
        }

        return false; // Item not found in any filter slot
    }

    private int getFirstEmptyFilterSlot(Inventory guiInventory) {
        for (int i = GuiManager.FILTER_START_SLOT; i <= GuiManager.FILTER_END_SLOT; i++) {
            ItemStack slotItem = guiInventory.getItem(i); // Get item from the GUI slot
            if (slotItem == null || slotItem.getType().isAir()) {
                return i; // Return the first empty slot found
            }
        }
        return -1; // No empty slot found
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof ConfigGuiHolder))
            return;

        for (int slot : event.getRawSlots()) {
            if (slot >= GuiManager.FILTER_START_SLOT && slot <= GuiManager.FILTER_END_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder)) {
            return; // Not our GUI
        }
        if (!(event.getPlayer() instanceof Player player))
            return;

        if (GuiManager.shouldIgnoreClose(player.getUniqueId())) {
            return; // This close is from a GUI refresh — skip saving
        }

        SmartChest chest = holder.getChest();
        Inventory gui = event.getInventory();

        // Only save filters if it's a Receiver chest
        if (dataManager.getChestType(chest) == ChestType.RECEIVER) {
            List<ItemStack> filters = new ArrayList<>();
            for (int i = GuiManager.FILTER_START_SLOT; i <= GuiManager.FILTER_END_SLOT; i++) {
                ItemStack item = gui.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    // Make a copy to avoid issues, ensure it's a single item (or desired stack)
                    ItemStack filterItem = item.clone();
                    // filterItem.setAmount(1); // Optional: Store only 1 as a representation
                    filters.add(filterItem);
                }
            }
            dataManager.setFilters(chest, filters);

            dataManager.updateRoutingIfChanged(chest);

            ((Player) event.getPlayer()).sendMessage("§aReceiver chest filters saved!");
        }
    }
}