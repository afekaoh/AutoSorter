package com.autosorter.model;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Objects;

public class SmartChest {
    private final Chest singleChest;
    private final DoubleChest doubleChest;

    public SmartChest(InventoryHolder holder) {
        var newHolder = holder.getInventory().getHolder();
        if (newHolder instanceof DoubleChest doubleChest) {
            this.doubleChest = doubleChest;
            this.singleChest = null;
        } else if (newHolder instanceof Chest chest) {
            this.doubleChest = null;
            this.singleChest = chest;
        } else {
            throw new IllegalArgumentException("Holder must be a Chest or DoubleChest");
        }
    }

    public SmartChest(BlockState state) {
        if (!(state instanceof InventoryHolder tempHolder)) {
            throw new IllegalArgumentException("State must be a Chest or DoubleChest");
        }
        var holder = tempHolder.getInventory().getHolder();

        if (holder instanceof DoubleChest doubleChest) {
            this.doubleChest = doubleChest;
            this.singleChest = null;
        } else if (holder instanceof Chest chest) {
            this.doubleChest = null;
            this.singleChest = chest;
        } else {
            throw new IllegalArgumentException("Holder must be a Chest or DoubleChest");
        }
    }

    public Inventory getInventory() {
        return this.doubleChest != null ? this.doubleChest.getInventory() : this.singleChest.getInventory();
    }

    public Location getLocation() {
        if (this.doubleChest != null) {
            Chest right = (Chest) doubleChest.getRightSide();
            // Always use the right side location for consistency
            return right.getLocation();
        }
        return this.singleChest.getLocation();
    }

    public HashMap<Integer, ItemStack> addItem(ItemStack item) {
        return this.getInventory().addItem(item);
    }

    public boolean isDouble() {
        return this.doubleChest != null;
    }

    // // Saves data into game block of type container (in our case, chest)
    // public PersistentDataContainer getPersistentDataContainer() {
    // if (this.singleChest != null) { // is single chest
    // return this.singleChest.getPersistentDataContainer();
    // } else if (this.doubleChest != null) { // is double chest
    // ((Chest) doubleChest.getRightSide()).getBlock().getState().update(true,
    // false); // DEBUG
    // return ((Chest)
    // this.doubleChest.getRightSide()).getPersistentDataContainer();
    // }
    // throw new IllegalStateException("No chest available to get
    // PersistentDataContainer");
    // }

    // public void update() {
    // if (this.singleChest != null) {
    // this.singleChest.update(true);
    // } else if (this.doubleChest != null) {
    // ((Chest) this.doubleChest.getRightSide()).update(true);
    // }
    // }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SmartChest that))
            return false;
        return this.getLocation().equals(that.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getLocation());
    }

    public Chest getRightHalf() {
        if (this.doubleChest != null) {
            return (Chest) this.doubleChest.getRightSide();
        }
        return null;
    }

    public Chest getLeftHalf() {
        if (this.doubleChest != null) {
            return (Chest) this.doubleChest.getLeftSide();
        }
        return null;
    }

    // public String getFromPersistentDataContainerString(NamespacedKey key,
    // PersistentDataType<String, String> type) {
    // List<PersistentDataContainer> containers = this.getPersistentDataContainer();
    // for (PersistentDataContainer container : containers) {
    // String typeName = container.get(key, type);
    // if (typeName != null) {
    // return typeName;
    // }
    // }
    // return null; // Return null if the key is not found in any container
    // }

    // public void setToPersistentDataContainerString(NamespacedKey key,
    // PersistentDataType<String, String> type,
    // String value) {
    // List<PersistentDataContainer> containers = this.getPersistentDataContainer();
    // for (PersistentDataContainer container : containers) {
    // container.set(key, type, value);
    // }
    // this.update(); // Ensure the changes are saved to the block state
    // }

    // For future use, if needed
    // public int getFromPersistentDataContainerInteger(NamespacedKey key,
    // PersistentDataType<Integer, Integer> type) {
    // PersistentDataContainer container = chest.getPersistentDataContainer();
    // String typeName = container.get(key, type);
    // return typeName;
    // }
}
