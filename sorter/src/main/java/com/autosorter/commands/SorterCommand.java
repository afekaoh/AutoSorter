package com.autosorter.commands;

import com.autosorter.AutoSorter;
import com.autosorter.data.ChestDataManager;
import com.autosorter.gui.GuiManager;
import com.autosorter.model.SmartChest;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
            var rm = plugin.getChestDataManager().getRoutMap();
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
        }
        Block targetBlock = player.getTargetBlockExact(5); // 5 blocks range
        if (targetBlock == null) {
            player.sendMessage("§cYou must be looking directly at a chest to configure it.");
            return true;
        }
        try {
            // Check if the target block is a chest
            SmartChest chest = new SmartChest(targetBlock.getState());
            guiManager.openConfigGui(player, chest);
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cYou must be looking directly at a chest to configure it.");
            return true;
        }
    }
}