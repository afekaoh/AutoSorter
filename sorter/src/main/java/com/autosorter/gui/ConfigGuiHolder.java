package com.autosorter.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.autosorter.model.SmartChest;

public class ConfigGuiHolder implements InventoryHolder {

    private final SmartChest chest;
    private Inventory inventory; // We'll set this when creating it

    public ConfigGuiHolder(SmartChest chest) {
        this.chest = chest;
    }

    public SmartChest getChest() {
        return chest;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // We need a way to set the inventory after creation
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}