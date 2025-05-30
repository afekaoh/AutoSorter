package com.autosorter.listeners;

import com.autosorter.AutoSorter;
import com.autosorter.data.ChestDataManager;
import com.autosorter.gui.ConfigGuiHolder;
import com.autosorter.gui.GuiManager;
import com.autosorter.gui.enums.GuiMenuType;
import com.autosorter.gui.enums.GuiSlot;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuiListener implements Listener{

    private final AutoSorter plugin;
    private final ChestDataManager dataManager;
    private final GuiManager guiManager; // Need this to refresh GUI

    public GuiListener(AutoSorter plugin, ChestDataManager dataManager, GuiManager guiManager){
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder)) return;

        if(!(event.getWhoClicked() instanceof Player)) return;
        if(event.getClickedInventory() == null) return; // Ignore clicks outside any inventory

        SmartChest chest = holder.getChest();
        GuiMenuType menu = holder.getMenuType();

        switch(menu){
            case MAIN -> handleMainMenuClick(event);
            case FILTERS -> handleFiltersMenuClick(event);
            case SETTINGS -> handleSettingsMenuClick(event);
            case SYSTEM_SELECTOR -> handleSystemSelectorClick(event);
            case INFO -> handleInfoMenuClick(event);
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder))
            return; // It should be as it was checked in onInventoryClick but just in case

        SmartChest chest = holder.getChest();
        GuiMenuType type;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ChestType chestType = dataManager.getChestType(chest);
        event.setCancelled(true);
        if(slot == GuiSlot.MAIN_SETTINGS.slot()) type = GuiMenuType.SETTINGS;
        else if(slot == GuiSlot.MAIN_CHANGE_SYSTEM.slot()) type = GuiMenuType.SYSTEM_SELECTOR;
        else if(slot == GuiSlot.MAIN_FILTERS.slot() && chestType == ChestType.RECEIVER) type = GuiMenuType.FILTERS;
        else {
            if(slot == GuiSlot.MAIN_EXIT.slot()) player.closeInventory();
            return; // Exit doesn't require opening a new menu
        }
        GuiManager.markIgnoreClose(player.getUniqueId());
        Bukkit.getScheduler().runTask((plugin), () -> guiManager.openMenu(player, chest, type));
    }

    private void handleFiltersMenuClick(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder))
            return; // It should be as it was checked in onInventoryClick but just in case

        SmartChest chest = holder.getChest();
        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        Inventory guiInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        boolean isReceiver = dataManager.getChestType(chest) == ChestType.RECEIVER;
        Component alreadyInFilterMessage =
                Component.text("That item is already in the filter.").colorIfAbsent(NamedTextColor.RED);
        if(rawSlot == GuiSlot.FILTERS_RETURN.slot()){
            event.setCancelled(true); // Prevent item movement in non-receiver chests

            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.MAIN));
            return;
        }
        if(!isReceiver){
            if(guiInventory.equals(clickedInventory) || event.isShiftClick())
                event.setCancelled(true); // Prevent item movement in non-receiver chests
            return; // Not a receiver chest, ignore filter interactions
        }
        if(rawSlot == GuiSlot.FILTERS_CLEAR_ALL.slot()){
            event.setCancelled(true); // Prevent item movement

            if(!holder.isConfirmClearFilters()){
                syncFiltersFromGui(guiInventory, chest, false); // Sync filters before confirmation

                GuiManager.markIgnoreClose(player.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.FILTERS,
                                                                                true)); // Open confirmation menu
            }
            return;
        }
        if(rawSlot == GuiSlot.FILTERS_CONFIRM_CLEAR_YES.slot() && holder.isConfirmClearFilters()){ // Confirm clear
            // filters
            event.setCancelled(true); // Prevent item movement

            Set<ItemStack> filters = dataManager.clearFilters(chest);
            var playerInventory = player.getInventory();
            for(ItemStack item : filters){
                if(item != null && !item.getType().isAir()){
                    if(canReceiveItem(item, playerInventory)){
                        playerInventory.addItem(item.clone()); // Add cleared filters back to player inventory
                    }
                    else {
                        player.getWorld().dropItemNaturally(player.getLocation(), item.clone()); // Drop if no space
                    }
                }
            }
            player.sendMessage(Component.text("All filters cleared!", NamedTextColor.GREEN));
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.FILTERS));
            return;
        }
        if(rawSlot == GuiSlot.FILTERS_CONFIRM_CLEAR_NO.slot() && holder.isConfirmClearFilters()){ // Cancel clear
            // filters
            event.setCancelled(true); // Prevent item movement

            GuiManager.markIgnoreClose(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.FILTERS));
            return;
        }

//         ----- Shift-click from player inventory -----
        if(event.isShiftClick() && player.getInventory().equals(clickedInventory)){

            ItemStack sourceStack = event.getCurrentItem();
            if(sourceStack == null || sourceStack.getType().isAir())
                return; // No item to shift-click
            event.setCancelled(true); // Prevent item movement

            int availableSlot = getFirstEmptyFilterSlot(guiInventory);
            if(availableSlot == -1){
                player.sendMessage("§cNo empty filter slots available.");
                return; // No empty slot found
            }

            boolean isItemAlreadyPresentInFilter = isItemInFilter(guiInventory, sourceStack.getType());

            if(isItemAlreadyPresentInFilter){
                player.sendMessage(alreadyInFilterMessage);
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
        if(rawSlot >= GuiSlot.FILTERS_START_SLOT.slot() && rawSlot <= GuiSlot.FILTERS_END_SLOT.slot()){
            event.setCancelled(true); // Prevent item movement
            ItemStack cursor = player.getItemOnCursor();
            ItemStack clickedItem = guiInventory.getItem(rawSlot);

            // Case 0 - Shift-click in filter slot
            if(event.isShiftClick()){
                if(clickedItem == null || clickedItem.getType().isAir()){
                    return; // No item in clicked slot
                }

                // If the clicked item is not air, try to add it to the player's inventory
                if(canReceiveItem(clickedItem, player.getInventory())){
                    // If the player has space, add the item to their inventory
                    player.getInventory().addItem(clickedItem.clone());
                    guiInventory.setItem(rawSlot, null);
                    event.setCurrentItem(null);
                }
                return;
            }

            // Case 1 - cursor is empty, filter slot has item
            if(cursor.getType().isAir()){
                if(clickedItem != null && !clickedItem.getType().isAir()){
                    player.setItemOnCursor(clickedItem);
                    guiInventory.setItem(rawSlot, null);
                }
                return;
            }

            // Case 2 - Cursor isn't empty
            if(clickedItem != null && !clickedItem.getType().isAir()){
                if(isItemInFilter(guiInventory, cursor.getType())){
                    player.sendMessage("§cThat item is already in the filter.");
                    return; // Item already exists in the filter
                }
                ItemStack single = cursor.clone();
                single.setAmount(1);
                if(cursor.getAmount() == 1){
                    player.setItemOnCursor(clickedItem);
                }
                else {
                    cursor.setAmount(cursor.getAmount() - 1);
                    int playerInventorySlot = player.getInventory().firstEmpty();
                    if(playerInventorySlot == -1) // no empty slot available
                        player.getWorld().dropItemNaturally(player.getLocation(), cursor.clone());
                    else
                        player.getInventory().setItem(playerInventorySlot, cursor.clone());
                    player.setItemOnCursor(clickedItem);
                }
                guiInventory.setItem(rawSlot, single);
                return;
            }

//            Case 3 - Cursor has items, filter slot is empty
            if(isItemInFilter(guiInventory, cursor.getType())){
                player.sendMessage(alreadyInFilterMessage);
                return; // Item already exists in the filter
            }

            ItemStack single = cursor.clone();
            single.setAmount(1);
            guiInventory.setItem(rawSlot, single);

            if(cursor.getAmount() == 1){
                player.setItemOnCursor(null);
            }
            else {
                cursor.setAmount(cursor.getAmount() - 1);
                player.setItemOnCursor(cursor);
            }
            return;
        }
        // Block all other GUI clicks
        if(guiInventory.equals(clickedInventory)){
            event.setCancelled(true);
        }
    }

    private void handleSettingsMenuClick(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder))
            return; // It should be as it was checked in onInventoryClick but just in case

        SmartChest chest = holder.getChest();
        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        event.setCancelled(true); // Prevent item movement

        ChestType selectedType;
        if(rawSlot == GuiSlot.SETTINGS_INPUT_SLOT.slot()) selectedType = ChestType.INPUT;
        else if(rawSlot == GuiSlot.SETTINGS_RECEIVER_SLOT.slot()) selectedType = ChestType.RECEIVER;
        else if(rawSlot == GuiSlot.SETTINGS_OVERFLOW_SLOT.slot()) selectedType = ChestType.OVERFLOW;
        else if(rawSlot == GuiSlot.SETTINGS_NONE_SLOT.slot()) selectedType = ChestType.NONE;
        else selectedType = null;

        if(selectedType != null){
            dataManager.setChestType(chest, selectedType);
            Component typeMessage = Component.text("Chest type set to: ")
                                             .colorIfAbsent(NamedTextColor.YELLOW)
                                             .append(Component.text(selectedType.name()).colorIfAbsent(NamedTextColor.GOLD));
            player.sendMessage(typeMessage);
            GuiManager.markIgnoreClose(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.SETTINGS));
            return; // Refresh the GUI to reflect the new type
        }

        if(rawSlot == GuiSlot.SETTINGS_RETURN.slot()){
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.MAIN));
        }
    }

    private void handleSystemSelectorClick(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder))
            return; // It should be as it was checked in onInventoryClick but just in case

        SmartChest chest = holder.getChest();
        // not supported yet keep for future use
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if(slot == GuiSlot.SYSTEM_RETURN.slot()){
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.MAIN));
            // Return to main menu
        }

        //        int slot = event.getRawSlot();
//        ItemStack clicked = gui.getItem(slot);
//        if(clicked == null) return;
//
//        if(slot == GuiSlot.SYSTEM_RETURN.slot()){
//            guiManager.openMenu(player, chest, GuiMenuType.MAIN);
//            return;
//        }
//
//        String systemName = clicked.getItemMeta().getDisplayName();
//        if(!systemName.isBlank()){
//            dataManager.assignChestToSystem(chest, systemName);
//            player.sendMessage("§aChest assigned to system: §f" + systemName);
//            guiManager.openMenu(player, chest, GuiMenuType.MAIN);
//        }
    }

    private void handleInfoMenuClick(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder))
            return; // It should be as it was checked in onInventoryClick but just in case

        SmartChest chest = holder.getChest();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if(slot == GuiSlot.INFO_RETURN.slot()){
            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenu(player, chest, GuiMenuType.MAIN));
        }
    }


    private boolean isItemInFilter(Inventory guiInventory, Material itemType){

        // Get item from the GUI slot
        // Item already exists in the filter

        return IntStream.
                rangeClosed(GuiSlot.FILTERS_START_SLOT.slot(), GuiSlot.FILTERS_END_SLOT.slot())
                .mapToObj(guiInventory::getItem)
                .anyMatch(slotItem -> slotItem != null && slotItem.getType() == itemType); // Item not found in any
        // filter slot
    }

    private int getFirstEmptyFilterSlot(Inventory guiInventory){
        for(int i = GuiSlot.FILTERS_START_SLOT.slot(); i <= GuiSlot.FILTERS_END_SLOT.slot(); i++){
            ItemStack slotItem = guiInventory.getItem(i); // Get item from the GUI slot
            if(slotItem == null || slotItem.getType().isAir()){
                return i; // Return the first empty slot found
            }
        }
        return -1; // No empty slot found
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder))
            return;
        if(!(event.getWhoClicked() instanceof Player))
            return;
        if(event.getView().getTopInventory() != event.getInventory())
            return; // Ignore drag events outside the top inventory
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        if(!(event.getInventory().getHolder() instanceof ConfigGuiHolder holder)){
            return; // Not our GUI
        }
        if(!(event.getPlayer() instanceof Player player))
            return;

        if(GuiManager.shouldIgnoreClose(player.getUniqueId())){
            return; // This close is from a GUI refresh — skip saving
        }
        if(holder.getMenuType() == GuiMenuType.FILTERS){
            SmartChest chest = holder.getChest();
            Inventory gui = event.getInventory();

            // Only save filters if it's a Receiver chest
            if(dataManager.getChestType(chest) == ChestType.RECEIVER){
                syncFiltersFromGui(gui, chest, true); // Sync filters from GUI to data manager
                event.getPlayer().sendMessage(Component.text("Filters saved successfully!", NamedTextColor.GREEN));
            }
        }
    }

    public boolean canReceiveItem(ItemStack itemToTransfer, Inventory inventory){
        int firstEmpty = inventory.firstEmpty();
        if(firstEmpty >= 0){
            return true; // Chest has space
        }
        var contents = inventory.getContents();
        for(ItemStack itemInChest : contents){

            if(itemInChest != null && itemInChest.isSimilar(itemToTransfer)
               && itemInChest.getAmount() < itemInChest.getMaxStackSize()){
                return true; // Chest has space for this material
            }
        }
        return false; // No space for this material
    }

    private void syncFiltersFromGui(Inventory gui, SmartChest chest, boolean shouldRoute){
        List<ItemStack> filters = IntStream.rangeClosed(GuiSlot.FILTERS_START_SLOT.slot(),
                                                        GuiSlot.FILTERS_END_SLOT.slot())
                                           .mapToObj(gui::getItem)
                                           .filter(item -> item != null && !item.getType().isAir())
                                           .map(ItemStack::clone)
                                           .collect(Collectors.toList());

        dataManager.setFilters(chest, filters);
        if(shouldRoute){
            dataManager.updateRoutingIfChanged(chest); // Update routing if needed
        }
    }
}