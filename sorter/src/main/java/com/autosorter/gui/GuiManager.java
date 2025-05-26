package com.autosorter.gui;

import com.autosorter.AutoSorter;
import com.autosorter.data.ChestDataManager;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GuiManager {

    private final AutoSorter plugin;
    private final ChestDataManager dataManager;
    public static final int GUI_SIZE = 54; // 6 rows
    public static final int FILTER_START_SLOT = 9; // Second row
    public static final int FILTER_END_SLOT = 44; // End of fifth row (36 slots)

    // Slot numbers for buttons
    public static final int INPUT_BUTTON_SLOT = 1;
    public static final int RECEIVER_BUTTON_SLOT = 3;
    public static final int OVERFLOW_BUTTON_SLOT = 5;
    public static final int NONE_BUTTON_SLOT = 7;
    public static final int SAVE_BUTTON_SLOT = 49; // Middle of bottom row

    private static final Set<UUID> ignoreNextClose = new HashSet<>();

    public GuiManager(AutoSorter plugin, ChestDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void openConfigGui(Player player, SmartChest chest) {
        ConfigGuiHolder holder = new ConfigGuiHolder(chest);
        Component title = Component.text("Sorter Chest Config");
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        holder.setInventory(gui); // Link holder and inventory

        // Populate the GUI with current chest data
        populateGui(gui, chest);

        // Cache the current state of the chest
        dataManager.cacheGuiSnapshot(chest);

        // Open the GUI for the player
        player.openInventory(gui);

    }

    private void populateGui(Inventory gui, SmartChest chest) {
        gui.clear(); // Start fresh

        ChestType currentType = dataManager.getChestType(chest);

        // --- Create Buttons ---
        ItemStack inputButton = createGuiItem(Material.HOPPER, "§aInput Chest",
                Arrays.asList("§7Click to set this chest", "§7as an §aInput Chest§7.",
                        (currentType == ChestType.INPUT ? "§e(Currently Selected)" : "")));

        ItemStack receiverButton = createGuiItem(Material.CHEST, "§bReceiver Chest",
                Arrays.asList("§7Click to set this chest", "§7as a §bReceiver Chest§7.",
                        "§7(Allows setting item filters).",
                        (currentType == ChestType.RECEIVER ? "§e(Currently Selected)" : "")));

        ItemStack overflowButton = createGuiItem(Material.BARRIER, "§cOverflow Chest",
                Arrays.asList("§7Click to set this chest", "§7as an §cOverflow Chest§7.",
                        "§7(Catches un-sortable items).",
                        (currentType == ChestType.OVERFLOW ? "§e(Currently Selected)" : "")));

        ItemStack noneButton = createGuiItem(Material.GLASS_PANE, "§8None / Remove",
                Arrays.asList("§7Click to remove this chest", "§7from the sorting system.",
                        (currentType == ChestType.NONE ? "§e(Currently Selected)" : "")));

        ItemStack saveButton = createGuiItem(Material.WRITABLE_BOOK, "§6Save & Close",
                List.of("§7Saves the current filters", "§7and closes this window."));

        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>());

        // --- Place Buttons & Filler ---
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, filler); // Fill all with glass first
        }
        gui.setItem(INPUT_BUTTON_SLOT, inputButton);
        gui.setItem(RECEIVER_BUTTON_SLOT, receiverButton);
        gui.setItem(OVERFLOW_BUTTON_SLOT, overflowButton);
        gui.setItem(NONE_BUTTON_SLOT, noneButton);
        gui.setItem(SAVE_BUTTON_SLOT, saveButton);

        List<ItemStack> filters = dataManager.getFilters(chest);

        // --- Place Filters (if Receiver) ---
        if (currentType == ChestType.RECEIVER) {
            // Only place filters — don't overwrite with glass panes if none exist
            for (int i = 0; i <= GuiManager.FILTER_END_SLOT - GuiManager.FILTER_START_SLOT; i++) {
                int slot = GuiManager.FILTER_START_SLOT + i;
                if (i < filters.size()) {
                    gui.setItem(slot, filters.get(i));
                } else {
                    gui.clear(slot); // Make sure empty slots are empty
                }
            }
        }
    }

    // Helper method to create GUI items
    public static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Helper method to create GUI items
    public static ItemStack createGuiItem(Material material, String name, List<String> lore, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void clearInventory(Inventory inv) {
        inv.clear(); // Clear the inventory
    }

    public static void markIgnoreClose(UUID playerId) {
        ignoreNextClose.add(playerId);
    }

    public static boolean shouldIgnoreClose(UUID playerId) {
        // should ignore close if the playerId is in the set and remove it to prevent
        // multiple ignores
        return ignoreNextClose.remove(playerId);
    }
}