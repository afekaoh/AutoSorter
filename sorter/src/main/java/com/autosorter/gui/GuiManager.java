package com.autosorter.gui;

import com.autosorter.data.ChestDataManager;
import com.autosorter.gui.enums.*;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class GuiManager{

    private final ChestDataManager dataManager;

    private static final Set<UUID> ignoreNextClose = new HashSet<>();

    public GuiManager(ChestDataManager dataManager){
        this.dataManager = dataManager;
    }


    public void openConfigGui(Player player, SmartChest chest){
        openMenu(player, chest, GuiMenuType.MAIN);
    }

    public void openMenu(Player player, SmartChest chest, GuiMenuType type){
        ConfigGuiHolder holder = new ConfigGuiHolder(chest, type);
        Inventory gui = Bukkit.createInventory(holder,
                                               GuiMenuSize.fromType(type).size(),
                                               GuiTitle.fromType(type).component());
        holder.setInventory(gui); // Link holder and inventory
        // Populate the GUI with current chest data
        populateGui(holder);
        // Cache the current state of the chest
        dataManager.cacheGuiSnapshot(chest);
        // Open the GUI for the player
        player.openInventory(gui);
    }


    public void openMenu(Player player, SmartChest chest, GuiMenuType type, boolean confirmClearFilters){
        ConfigGuiHolder holder = new ConfigGuiHolder(chest, type, confirmClearFilters);
        Inventory gui = Bukkit.createInventory(holder,
                                               GuiMenuSize.fromType(type).size(),
                                               GuiTitle.fromType(type).component());
        holder.setInventory(gui); // Link holder and inventory
        // Populate the GUI with current chest data
        populateGui(holder);
        // Cache the current state of the chest
        dataManager.cacheGuiSnapshot(chest);
        // Open the GUI for the player
        player.openInventory(gui);
    }


    private void populateGui(InventoryHolder holder){
        if(!(holder instanceof ConfigGuiHolder configHolder)) return;
        GuiMenuType type = configHolder.getMenuType();

        switch(type){
            case MAIN -> populateMainMenu(holder);
            case FILTERS -> populateFiltersMenu(holder);
            case SYSTEM_SELECTOR -> populateSystemSelectorMenu(holder, 0); // Default to page 0
            case SETTINGS -> populateSettingsMenu(holder);
            case INFO -> populateInfoMenu(holder);
        }
    }

    private void populateMainMenu(InventoryHolder holder){
        if(!(holder instanceof ConfigGuiHolder configHolder)){
            return;
        }

        Inventory gui = configHolder.getInventory();
        SmartChest chest = configHolder.getChest();

        gui.clear();
        for(int i = 0; i < GuiMenuSize.ROW_LENGTH.size(); i++){
            for(int j = 0; j < GuiMenuSize.MAIN.size() / GuiMenuSize.ROW_LENGTH.size(); j++){
                int slot = i + j * GuiMenuSize.ROW_LENGTH.size();
                // clear a 3X3 grid in the middle of the GUI
                if(i >= 3 && i <= 5 && j >= 1 && j <= 3){
                    continue; // Skip the center grid
                }
                if(i % 2 == 0){
                    // Fill even rows with gray filler
                    gui.setItem(slot, createGuiItem(GuiIcon.FILLER_PURPLE.material(),
                                                    Component.text(" ", NamedTextColor.DARK_GRAY),
                                                    List.of()));
                }
                else {
                    // Fill odd rows with empty item
                    gui.setItem(slot, createGuiItem(GuiIcon.FILLER_PINK.material(), Component.empty(), List.of()));
                }
            }
        }
        ChestType currentType = dataManager.getChestType(chest);
        var filtersNumber = dataManager.getFilters(chest).size();

        // Use GuiSlot and GuiIcon enums here
        ItemStack typeButton = createGuiItem(
                GuiIcon.TYPE.material(),
                Component.text("Change Chest Type"),
                List.of(
                        Component.text("Current type:", NamedTextColor.GRAY)
                                 .append(Component.space())
                                 .append(Component.text(currentType.name(), NamedTextColor.YELLOW)),
                        Component.text("Click to change the type of this chest.", NamedTextColor.GRAY)
                       ));
        gui.setItem(GuiSlot.MAIN_SETTINGS.slot(), typeButton);

        ItemStack systemButton = createGuiItem(
                GuiIcon.SYSTEM.material(),
                Component.text("Change Sorting System"),
                List.of(
                        Component.text("Current system:", NamedTextColor.GRAY)
                                 .append(Component.space())
                                 .append(Component.text("Not implemented yet", NamedTextColor.YELLOW)),
                        Component.text("Click to change the sorting system for this chest.", NamedTextColor.GRAY)
                       ));
        gui.setItem(GuiSlot.MAIN_CHANGE_SYSTEM.slot(), systemButton);

        ItemStack filtersButton = createGuiItem(
                currentType == ChestType.RECEIVER ?
                GuiIcon.FILTERS_ACTIVE.material() : GuiIcon.FILTERS_INACTIVE.material(),
                Component.text("Edit Filters"),
                currentType == ChestType.RECEIVER ?
                List.of(
                        Component.text("Current number of filters:", NamedTextColor.GRAY)
                                 .append(Component.space())
                                 .append(Component.text(filtersNumber, NamedTextColor.YELLOW)),
                        Component.text("Click to edit filters for this chest.", NamedTextColor.GRAY)
                       )
                                                  : List.of(
                        Component.text("Current number of filters: " + filtersNumber, NamedTextColor.YELLOW),
                        Component.text("You cannot edit filters for this chest.", NamedTextColor.GRAY)
                                                           ));
        gui.setItem(GuiSlot.MAIN_FILTERS.slot(), filtersButton);

//        ItemStack infoButton = createGuiItem(GuiIcon.INFO.material(),
//                                             Component.text("Chest Info"), List.of());
//        gui.setItem(GuiSlot.MAIN_INFO.slot(), infoButton);

        ItemStack exitButton = createGuiItem(
                GuiIcon.EXIT.material(),
                Component.text("Close"),
                List.of(
                        Component.text("Click to close the configuration menu.", NamedTextColor.GRAY)
                       ));
        gui.setItem(GuiSlot.MAIN_EXIT.slot(), exitButton);

    }

    private void populateFiltersMenu(InventoryHolder holder){
        if(!(holder instanceof ConfigGuiHolder configHolder)) return;
        Inventory gui = configHolder.getInventory();
        SmartChest chest = configHolder.getChest();
        boolean showConfirmation = configHolder.isConfirmClearFilters();

        gui.clear();
        ChestType currentType = dataManager.getChestType(chest);
        List<ItemStack> filters = dataManager.getFilters(chest);

        if(currentType != ChestType.RECEIVER){
            ItemStack fillerItem = createGuiItem(
                    GuiIcon.FILLER.material(),
                    Component.text("This chest is not a receiver chest", NamedTextColor.RED),
                    List.of(Component.text("You cannot edit filters for this chest."))
                                                );
            IntStream.range(0, GuiMenuSize.FILTERS.size()).forEach(i -> gui.setItem(i, fillerItem));
        }
        else {

            // --- Place filter items (slots 0–44) ---
            IntStream.range(GuiSlot.FILTERS_START_SLOT.slot(),
                            GuiSlot.FILTERS_START_SLOT.slot() + Math.min(filters.size(),
                                                                         GuiSlot.FILTERS_END_SLOT.slot() - GuiSlot.FILTERS_START_SLOT.slot() + 1))
                     .forEach(i -> gui.setItem(i, filters.get(i - GuiSlot.FILTERS_START_SLOT.slot())));
        }
        // Fill empty slots with gray filler
        IntStream.range(GuiSlot.FILTERS_END_SLOT.slot() + 1, GuiMenuSize.FILTERS.size())
                 .forEach(i -> gui.setItem(i, createGuiItem(GuiIcon.FILLER.material(),
                                                            Component.text(" ", NamedTextColor.DARK_GRAY),
                                                            List.of())));

        if(showConfirmation){
            gui.setItem(GuiSlot.FILTERS_CONFIRM_CLEAR_YES.slot(), createGuiItem(
                    GuiIcon.CONFIRM.material(),
                    Component.text("Yes, Clear", NamedTextColor.GREEN),
                    List.of(Component.text("This will remove all filters."))));

            gui.setItem(GuiSlot.FILTERS_CLEAR_ALL.slot(), createGuiItem(
                    GuiIcon.FILLER.material(),
                    Component.text("Confirm Clear All", NamedTextColor.GRAY),
                    List.of(Component.text("Are you sure?"))));

            gui.setItem(GuiSlot.FILTERS_CONFIRM_CLEAR_NO.slot(), createGuiItem(
                    GuiIcon.CANCEL.material(),
                    Component.text("Cancel", NamedTextColor.RED),
                    List.of(Component.text("Go back without clearing."))));
        }
        else {
            gui.setItem(GuiSlot.FILTERS_CLEAR_ALL.slot(), createGuiItem(
                    GuiIcon.CLEAR.material(),
                    Component.text("Clear All Filters", NamedTextColor.RED),
                    List.of(Component.text("Removes all filter items."))));
        }

        // Return to main menu
        gui.setItem(GuiSlot.FILTERS_RETURN.slot(), createGuiItem(
                GuiIcon.SAVE.material(),
                Component.text("Save Changes", NamedTextColor.YELLOW),
                List.of(
                        Component.text("Click to save changes and return to the main menu.", NamedTextColor.GRAY)
                       )));
    }

    private void populateSystemSelectorMenu(InventoryHolder holder, int page){
        // this method can be extended for pagination not supported yet
        if(!(holder instanceof ConfigGuiHolder configHolder)) return;
        Inventory gui = configHolder.getInventory();

        gui.clear();

        List<String> systems = List.of(""); //dataManager.getAllSystemNames();
        int perPage = 45;
        int start = page * perPage;

        IntStream.iterate(0, i -> i < perPage && (start + i) < systems.size(), i -> i + 1)
                 .forEachOrdered(i -> {
                     String system = systems.get(start + i);
                     ItemStack paper = createGuiItem(
                             GuiIcon.SYSTEM.material(),
                             Component.text(system, NamedTextColor.WHITE),
                             List.of(Component.text("Click to select this system."))
                                                    );
                     gui.setItem(i, paper);
                 });



        /*
        Is kept for future use if pagination is needed
        // Previous page
        if (page > 0) {
            gui.setItem(GuiSlot.SYSTEM_PREV_PAGE.slot(),
                        createGuiItem(GuiIcon.RETURN.material(), Component.text("Previous Page"), List.of()));
        }

        // Next page
        if ((start + perPage) < systems.size()) {
            gui.setItem(GuiSlot.SYSTEM_NEXT_PAGE.slot(),
                        createGuiItem(GuiIcon.RETURN.material(), Component.text("Next Page"), List.of()));
        }
        */

        // Return
        gui.setItem(GuiSlot.SYSTEM_RETURN.slot(),
                    createGuiItem(GuiIcon.EXIT.material(), Component.text("Back"), List.of()));
    }

    private void populateSettingsMenu(InventoryHolder holder){
        if(!(holder instanceof ConfigGuiHolder configHolder)) return;
        Inventory gui = configHolder.getInventory();
        SmartChest chest = configHolder.getChest();

        gui.clear();

        ChestType currentType = dataManager.getChestType(chest);

        // Create buttons for each chest type
        for(ChestType type : ChestType.values()){
            int slot = switch(type){
                case INPUT -> GuiSlot.SETTINGS_INPUT_SLOT.slot();
                case RECEIVER -> GuiSlot.SETTINGS_RECEIVER_SLOT.slot();
                case OVERFLOW -> GuiSlot.SETTINGS_OVERFLOW_SLOT.slot();
                case NONE -> GuiSlot.SETTINGS_NONE_SLOT.slot();
            };

            Component name = Component.text(type.name().charAt(0) + type.name().substring(1).toLowerCase());
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to set chest to " + type.name().toLowerCase()));
            if(type == currentType){
                lore.add(Component.text("(Currently Selected)", NamedTextColor.YELLOW));
            }

            ItemStack icon = switch(type){
                case INPUT -> createGuiItem(GuiIcon.TYPE_INPUT.material(), name, lore);
                case RECEIVER -> createGuiItem(GuiIcon.TYPE_RECEIVER.material(), name, lore);
                case OVERFLOW -> createGuiItem(GuiIcon.TYPE_OVERFLOW.material(), name, lore);
                case NONE -> createGuiItem(GuiIcon.TYPE_NONE.material(), name, lore);
                default -> null; // Should never happen
            };

            gui.setItem(slot, icon);
        }

        gui.setItem(GuiSlot.SETTINGS_RETURN.slot(), createGuiItem(
                GuiIcon.SAVE.material(),
                Component.text("Save Changes", NamedTextColor.YELLOW),
                List.of(
                        Component.text("Click to save changes and return to the main menu.", NamedTextColor.GRAY)
                       )));
    }

    private void populateInfoMenu(InventoryHolder holder){
        if(!(holder instanceof ConfigGuiHolder configHolder)) return;

        Inventory gui = configHolder.getInventory();
        SmartChest chest = configHolder.getChest();

        gui.clear();

        ChestType type = dataManager.getChestType(chest);
        String system = "Not implemented yet"; //dataManager.getSystemNameForChest(chest);
        int filterCount = dataManager.getFilters(chest).size();

        List<Component> infoLines = List.of(
                Component.text("Chest Type: " + type.name(), NamedTextColor.GREEN),
                Component.text("System: " + system, NamedTextColor.AQUA),
                Component.text("Filters: " + filterCount, NamedTextColor.GRAY)
                                           );

        ItemStack info = createGuiItem(GuiIcon.INFO.material(),
                                       Component.text("Chest Info", NamedTextColor.GOLD),
                                       infoLines);
        gui.setItem(GuiSlot.INFO_BOOK.slot(), info);

        gui.setItem(GuiSlot.INFO_RETURN.slot(),
                    createGuiItem(
                            GuiIcon.SAVE.material(),
                            Component.text("Save Changes", NamedTextColor.YELLOW),
                            List.of(
                                    Component.text("Click to save changes and return to the main menu.",
                                                   NamedTextColor.GRAY)
                                   )));
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