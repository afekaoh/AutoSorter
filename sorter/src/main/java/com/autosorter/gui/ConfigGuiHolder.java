package com.autosorter.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.autosorter.model.SmartChest;
import org.jetbrains.annotations.NotNull;

public class ConfigGuiHolder implements InventoryHolder{

    private final SmartChest chest;
    private Inventory inventory; // We'll set this when creating it

    public ConfigGuiHolder(SmartChest chest){
        this.chest = chest;
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

    // We need a way to set the inventory after creation
    public void setInventory(Inventory inventory){
        this.inventory = inventory;
    }
}