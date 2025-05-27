package com.autosorter.commands;

import com.autosorter.AutoSorter;
import com.autosorter.gui.GuiManager;
import com.autosorter.model.SmartChest;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                sender.sendMessage(ChatColor.GREEN + "AutoSorter backup saved to chest_data.yml.");
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Backup failed: " + e.getMessage());
                e.printStackTrace();
            }
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