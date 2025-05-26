// package com.autosorter.listeners;

// import java.util.List;

// import org.bukkit.Bukkit;
// import org.bukkit.Material;
// import org.bukkit.World;
// import org.bukkit.block.Block;
// import org.bukkit.block.BlockFace;
// import org.bukkit.event.EventHandler;
// import org.bukkit.event.Listener;
// import org.bukkit.event.block.BlockPlaceEvent;

// import com.autosorter.AutoSorter;
// import com.autosorter.data.ChestDataManager;

// public class ChestPlacementListener implements Listener {

// private final AutoSorter plugin;
// private final ChestDataManager dataManager;

// public ChestPlacementListener(AutoSorter plugin, ChestDataManager
// dataManager) {
// this.plugin = plugin;
// this.dataManager = dataManager;
// }

// @EventHandler
// public void onChestPlace(BlockPlaceEvent event) {
// if (event.getBlock().getType() != Material.CHEST)
// return;

// Block placed = event.getBlock();
// World world = placed.getWorld();

// // Check 4 cardinal neighbors for a chest
// for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.EAST,
// BlockFace.SOUTH, BlockFace.WEST)) {
// Block neighbor = placed.getRelative(face);
// if (neighbor.getType() != Material.CHEST)
// continue;

// // Give the server a tick to convert them to a double chest
// Bukkit.getScheduler().runTaskLater(plugin, () ->
// dataManager.syncDoubleChest(placed, neighbor), 2L);
// break;
// }
// }

// }
