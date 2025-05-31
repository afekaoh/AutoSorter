package com.autosorter.gui;

import com.autosorter.gui.enums.GuiMenuType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.autosorter.model.SmartChest;
import org.jetbrains.annotations.NotNull;

public class ConfigGuiHolder implements InventoryHolder{

    private final SmartChest chest;
    private Inventory inventory; // We'll set this when creating it
    private final GuiMenuType menuType;
    private final boolean confirmClearFilters;

    public ConfigGuiHolder(SmartChest chest, GuiMenuType type){
        this.chest = chest;
        this.menuType = type;
        this.confirmClearFilters = false; // Default to false if not specified
    }

    public ConfigGuiHolder(SmartChest chest, GuiMenuType type, boolean confirmClearFilters){
        this.chest = chest;
        this.menuType = type;
        this.confirmClearFilters = confirmClearFilters;
    }

    public SmartChest getChest(){
        return chest;
    }

    @Override
    public @NotNull Inventory getInventory(){
        if(inventory == null){
            // Create a new inventory if it hasn't been set yet
            throw new IllegalStateException("Inventory not set for ConfigGuiHolder");
        }
        return inventory;
    }

    public GuiMenuType getMenuType(){
        return menuType;
    }

    // We need a way to set the inventory after creation
    public void setInventory(Inventory inventory){
        this.inventory = inventory;
    }

    public boolean isConfirmClearFilters(){
        return confirmClearFilters;
    }

}