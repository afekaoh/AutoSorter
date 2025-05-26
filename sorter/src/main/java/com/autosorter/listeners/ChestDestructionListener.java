package com.autosorter.listeners;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import com.autosorter.data.ChestDataManager;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

public class ChestDestructionListener implements Listener {

    private final ChestDataManager chestDataManager;

    public ChestDestructionListener(ChestDataManager chestDataManager) {
        this.chestDataManager = chestDataManager;
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        if (block.getType() != Material.CHEST)
            return;

        SmartChest chest = new SmartChest(block.getState());

        if (!chestDataManager.isSorterChest(chest))
            return; // Not managed by the plugin

        // Cancel default item drop
        event.setDropItems(true);
        event.setCancelled(true);

        // Drop filter items if RECEIVER
        if (chestDataManager.getChestType(chest) == ChestType.RECEIVER) {
            for (ItemStack item : chestDataManager.getFilters(chest)) {
                block.getWorld().dropItemNaturally(block.getLocation(), item.clone());
            }
        }

        // Clean up plugin data
        chestDataManager.removeChest(chest);

        var contents = chest.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        // If double chest, manually break the other half
        if (chest.isDouble()) {

            Chest leftChest = chest.getLeftHalf();
            Chest rightChest = chest.getRightHalf();

            leftChest.getBlock().setType(Material.AIR);
            leftChest.getWorld().dropItemNaturally(leftChest.getLocation(), new ItemStack(Material.CHEST));
            rightChest.getBlock().setType(Material.AIR);
            rightChest.getWorld().dropItemNaturally(rightChest.getLocation(), new ItemStack(Material.CHEST));
        } else {
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHEST));
        }

    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleChestExplosion(event.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        handleChestExplosion(event.blockList());
    }

    private void handleChestExplosion(List<Block> blocks) {
        Set<Location> alreadyProcessed = new HashSet<>();

        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();

            if (block.getType() != Material.CHEST)
                continue;

            SmartChest chest = new SmartChest(block.getState());
            Location normalized = chest.getLocation();

            if (alreadyProcessed.contains(normalized))
                continue;

            if (!chestDataManager.isSorterChest(chest))
                continue;

            alreadyProcessed.add(normalized);

            // Remove both halves from the explosion to suppress default drops
            iterator.remove(); // remove current block

            if (chest.isDouble()) {
                Chest left = chest.getLeftHalf();
                Chest right = chest.getRightHalf();

                if (!left.getLocation().equals(block.getLocation()))
                    blocks.remove(left.getBlock());
                if (!right.getLocation().equals(block.getLocation()))
                    blocks.remove(right.getBlock());
            }

            // Drop filter items
            if (chestDataManager.getChestType(chest) == ChestType.RECEIVER) {
                for (ItemStack item : chestDataManager.getFilters(chest)) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item.clone());
                }
            }

            // Drop inventory contents
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }

            // Remove from plugin
            chestDataManager.removeChest(chest);

            // Break and drop both sides
            if (chest.isDouble()) {
                Chest left = chest.getLeftHalf();
                Chest right = chest.getRightHalf();

                left.getBlock().setType(Material.AIR);
                left.getWorld().dropItemNaturally(left.getLocation(), new ItemStack(Material.CHEST));
                right.getBlock().setType(Material.AIR);
                right.getWorld().dropItemNaturally(right.getLocation(), new ItemStack(Material.CHEST));
            } else {
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHEST));
            }
        }
    }

}
