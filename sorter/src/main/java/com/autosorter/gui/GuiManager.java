package com.autosorter.gui;

import com.autosorter.data.ChestDataManager;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
import java.util.stream.IntStream;

public class GuiManager{

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

    public GuiManager(ChestDataManager dataManager){
        this.dataManager = dataManager;
    }

    public void openConfigGui(Player player, SmartChest chest){
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

    private void populateGui(Inventory gui, SmartChest chest){
        gui.clear(); // Start fresh

        ChestType currentType = dataManager.getChestType(chest);

        // --- Create Buttons ---

        // Input Chest Button
        Component inputChestName = Component.text("Input Chest").colorIfAbsent(NamedTextColor.GREEN);
        ItemStack inputButton = createGuiItem(
                Material.HOPPER,
                inputChestName,
                List.of(
                        Component.text("Click to set this chest").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("as an ")
                                 .append(inputChestName)
                                 .append(Component.text("."))
                                 .colorIfAbsent(NamedTextColor.GRAY),
                        currentType == ChestType.INPUT
                        ? Component.text("(Currently Selected)").colorIfAbsent(NamedTextColor.YELLOW)
                        : Component.empty()));
        // Receiver Chest Button
        Component receiverChestName = Component.text("Receiver Chest").colorIfAbsent(NamedTextColor.AQUA);
        ItemStack receiverButton = createGuiItem(
                Material.CHEST,
                receiverChestName,
                Arrays.asList(
                        Component.text("Click to set this chest").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("as a ")
                                 .append(receiverChestName)
                                 .append(Component.text("."))
                                 .colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("Allows setting item filters").colorIfAbsent(NamedTextColor.GRAY),
                        (currentType == ChestType.RECEIVER
                         ? Component.text("(Currently Selected)").colorIfAbsent(NamedTextColor.YELLOW)
                         : Component.empty())));
        // Overflow Chest Button

        Component overflowChestName = Component.text("Overflow Chest").colorIfAbsent(NamedTextColor.RED);

        ItemStack overflowButton = createGuiItem(
                Material.BARRIER,
                overflowChestName,
                Arrays.asList(
                        Component.text("Click to set this chest").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("as an ")
                                 .append(overflowChestName)
                                 .append(Component.text("."))
                                 .colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("Catches un-sortable items").colorIfAbsent(NamedTextColor.GRAY),
                        (currentType == ChestType.OVERFLOW
                         ? Component.text("(Currently Selected)").colorIfAbsent(NamedTextColor.YELLOW)
                         : Component.empty())));

        // None / Remove Chest Buttons
        boolean isNone = currentType == ChestType.NONE;
        Component removeButtonName = Component.text("Remove").colorIfAbsent(NamedTextColor.DARK_GRAY);

        ItemStack removeButton = createGuiItem(
                Material.GLASS_PANE,
                removeButtonName,
                Arrays.asList(
                        Component.text("Click to remove this chest").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("from the sorting system.").colorIfAbsent(NamedTextColor.GRAY)));

        Component noneButtonName = Component.text("None").colorIfAbsent(NamedTextColor.DARK_GRAY);

        ItemStack noneButton = createGuiItem(
                Material.GLASS_PANE,
                noneButtonName,
                Arrays.asList(
                        Component.text("This chest is not part").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("of the sorting system.").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("(Currently Selected)").colorIfAbsent(NamedTextColor.YELLOW)));

        // Save & Close Button
        Component saveButtonName = Component.text("Save & Close").colorIfAbsent(NamedTextColor.GOLD);
        ItemStack saveButton = createGuiItem(
                Material.WRITABLE_BOOK,
                saveButtonName,
                Arrays.asList(
                        Component.text("Saves the current type and filters").colorIfAbsent(NamedTextColor.GRAY),
                        Component.text("and closes this window.").colorIfAbsent(NamedTextColor.GRAY)));

        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), new ArrayList<>());

        // --- Place Buttons & Filler ---
        // Fill all with glass first
        IntStream.range(0, GUI_SIZE).forEach(i -> gui.setItem(i, filler));

        gui.setItem(INPUT_BUTTON_SLOT, inputButton);
        gui.setItem(RECEIVER_BUTTON_SLOT, receiverButton);
        gui.setItem(OVERFLOW_BUTTON_SLOT, overflowButton);

        if(isNone)
            gui.setItem(NONE_BUTTON_SLOT, noneButton);
        else
            gui.setItem(NONE_BUTTON_SLOT, removeButton);

        gui.setItem(SAVE_BUTTON_SLOT, saveButton);

        List<ItemStack> filters = dataManager.getFilters(chest);

        // --- Place Filters (if Receiver) ---
        if(currentType == ChestType.RECEIVER){
            // Only place filters — don't overwrite with glass panes if none exist
            for(int i = 0; i <= GuiManager.FILTER_END_SLOT - GuiManager.FILTER_START_SLOT; i++){
                int slot = GuiManager.FILTER_START_SLOT + i;
                if(i < filters.size()){
                    gui.setItem(slot, filters.get(i));
                }
                else {
                    gui.clear(slot); // Make sure empty slots are empty
                }
            }
        }
    }

    // Helper method to create GUI items
    public static ItemStack createGuiItem(Material material, Component name, List<Component> lore){
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if(meta != null){
            // Set the display name and lore
            meta.displayName(name);
            var filteredLore = lore.stream().filter(Component.IS_NOT_EMPTY).toList(); // Filter out empty components
            meta.lore(filteredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void clearInventory(Inventory inv){
        inv.clear(); // Clear the inventory
    }

    public static void markIgnoreClose(UUID playerId){
        ignoreNextClose.add(playerId);
    }

    public static boolean shouldIgnoreClose(UUID playerId){
        // should ignore close if the playerId is in the set and remove it to prevent
        // multiple ignores
        return ignoreNextClose.remove(playerId);
    }
}