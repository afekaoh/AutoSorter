package com.autosorter.model;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

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

    // public SmartChest(Block block) {
    // if (!(block instanceof InventoryHolder tempHolder)) {
    // throw new IllegalArgumentException("State must be a Chest or DoubleChest");
    // }
    // var holder = tempHolder.getInventory().getHolder();

    // if (holder instanceof DoubleChest doubleChest) {
    // this.doubleChest = doubleChest;
    // this.singleChest = null;
    // } else if (holder instanceof Chest chest) {
    // this.doubleChest = null;
    // this.singleChest = chest;
    // } else {
    // throw new IllegalArgumentException("Holder must be a Chest or DoubleChest");
    // }
    // }

    public static Optional<SmartChest> from(Block block) {
        if (!(block instanceof InventoryHolder tempHolder)) {
            return Optional.empty();
        }
        return from(tempHolder);
    }

    public static Optional<SmartChest> from(InventoryHolder holder) {
        var newHolder = holder.getInventory().getHolder();
        if (holder == null || !(newHolder instanceof DoubleChest || newHolder instanceof Chest)) {
            return Optional.empty();
        }
        return Optional.of(new SmartChest(holder));
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
}
