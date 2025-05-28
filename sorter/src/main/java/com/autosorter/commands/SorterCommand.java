package com.autosorter.commands;

import com.autosorter.AutoSorter;
import com.autosorter.gui.GuiManager;
import com.autosorter.model.SmartChest;
import com.autosorter.model.SortingWand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SorterCommand implements CommandExecutor {

    private final AutoSorter plugin;
    private final GuiManager guiManager;

    public SorterCommand(AutoSorter plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            var rm = plugin.getChestDataManager().getRouteMap();
            if (rm.isEmpty()) {
                player.sendMessage("§cNo chests configured yet.");
                return true;
            }
            player.sendMessage("§aConfigured chests:");
            rm.forEach((material, chests) -> {
                String chestInfo = "§e" + material.name() + " §7-> " + chests.size() + " chests";
                player.sendMessage(chestInfo);
            });
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("backup")) {
            try {
                plugin.backupChests();
                sender.sendMessage(Component.text("AutoSorter backup saved to chest_data.yml.")
                        .colorIfAbsent(NamedTextColor.GREEN));
            } catch (IOException e) {
                var message = Component.text("Backup failed: ")
                        .append(Component.text(e.getMessage()))
                        .colorIfAbsent(NamedTextColor.RED);
                sender.sendMessage(message);
                e.printStackTrace();
            }
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("wand")) {
            var inventory = player.getInventory();
            ItemStack wand = SortingWand.createWand();
            // Check if the player already has a Sorting Wand
            if (inventory.containsAtLeast(wand, 1)) {
                player.sendMessage(
                        Component.text("Your inventory is full! Please make space to receive the Sorting Wand.")
                                .colorIfAbsent(NamedTextColor.RED));
                return true;
            }
            if (inventory.firstEmpty() == -1) {
                player.sendMessage(
                        Component.text("Your inventory is full! Please make space to receive the Sorting Wand.")
                                .colorIfAbsent(NamedTextColor.RED));
                return true;
            }
            player.getInventory().addItem(SortingWand.createWand());
            player.updateInventory(); // Ensure the inventory is updated
            player.sendMessage(Component.text("You received a Sorting Wand!").colorIfAbsent(NamedTextColor.GREEN));
            return true;
        }
        Block targetBlock = player.getTargetBlockExact(5); // 5 blocks range
        if (targetBlock == null) {
            player.sendMessage("§cYou must be looking directly at a chest to configure it.");
            return true;
        }

        var chest = SmartChest.from(targetBlock);
        if (chest.isEmpty()) {
            player.sendMessage("§cYou must be looking directly at a chest to configure it.");
            return true;
        }
        guiManager.openConfigGui(player, chest.get());
        return true;

    }
}