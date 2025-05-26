package com.autosorter.model;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SmartChest {
    private final Chest singleChest;
    private final DoubleChest doubleChest;

    public SmartChest(InventoryHolder holder) {

        if (holder instanceof DoubleChest dc) {
            this.doubleChest = dc;
            this.singleChest = null;
        } else if (holder instanceof Chest chest) {
            this.singleChest = chest;
            this.doubleChest = null;
        } else {
            throw new IllegalArgumentException("Holder must be a Chest or DoubleChest");
        }
    }

    public SmartChest(BlockState state) {

        if (state instanceof DoubleChest dc) {
            this.doubleChest = dc;
            this.singleChest = null;
        } else if (state instanceof Chest chest) {
            this.singleChest = chest;
            this.doubleChest = null;
        } else {
            throw new IllegalArgumentException("Holder must be a Chest or DoubleChest");
        }
    }

    public Inventory getInventory() {
        return doubleChest != null ? doubleChest.getInventory() : singleChest.getInventory();
    }

    public Location getLocation() {
        if (doubleChest != null) {
            Chest right = (Chest) doubleChest.getRightSide();
            // Always use the right side location for consistency
            return right.getLocation();
        }
        return singleChest.getLocation();
    }

    public HashMap<Integer, ItemStack> addItem(ItemStack item) {
        return getInventory().addItem(item);
    }

    public boolean isDouble() {
        return doubleChest != null;
    }

    public Chest getSingleChest() {
        // must check if singleChest is null to avoid NPE
        return singleChest;
    }

    public DoubleChest getDoubleChest() {
        // must check if doubleChest is null to avoid NPE
        return doubleChest;
    }

    private List<PersistentDataContainer> getPersistentDataContainer() {
        if (singleChest != null) {
            return List.of(singleChest.getPersistentDataContainer());
        } else if (doubleChest != null) {
            return List.of(
                    ((Chest) doubleChest.getLeftSide()).getPersistentDataContainer(),
                    ((Chest) doubleChest.getRightSide()).getPersistentDataContainer());
        }
        throw new IllegalStateException("No chest available to get PersistentDataContainer");
    }

    public void update() {
        if (singleChest != null) {
            singleChest.update();
        } else if (doubleChest != null) {
            ((Chest) doubleChest.getLeftSide()).update();
            ((Chest) doubleChest.getRightSide()).update();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SmartChest that))
            return false;
        return getLocation().equals(that.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation());
    }

    public String getFromPersistentDataContainerString(NamespacedKey key, PersistentDataType<String, String> type) {
        List<PersistentDataContainer> containers = this.getPersistentDataContainer();
        for (PersistentDataContainer container : containers) {
            String typeName = container.get(key, type);
            if (typeName != null) {
                return typeName;
            }
        }
        return null; // Return null if the key is not found in any container
    }

    public void setToPersistentDataContainerString(NamespacedKey key, PersistentDataType<String, String> type,
            String value) {
        List<PersistentDataContainer> containers = this.getPersistentDataContainer();
        for (PersistentDataContainer container : containers) {
            container.set(key, type, value);
        }
        this.update(); // Ensure the changes are saved to the block state
    }

    // For future use, if needed
    // public int getFromPersistentDataContainerInteger(NamespacedKey key,
    // PersistentDataType<Integer, Integer> type) {
    // PersistentDataContainer container = chest.getPersistentDataContainer();
    // String typeName = container.get(key, type);
    // return typeName;
    // }
}
