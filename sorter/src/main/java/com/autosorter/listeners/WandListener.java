package com.autosorter.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.autosorter.AutoSorter;
import com.autosorter.gui.GuiManager;
import com.autosorter.model.SmartChest;
import com.autosorter.model.SortingWand;

import java.util.Optional;

public class WandListener implements Listener {
    private final GuiManager guiManager;

    public WandListener(AutoSorter plugin, GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onPlayerUseWand(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!SortingWand.isWand(item))
            return;

        event.setCancelled(true);
        event.setUseItemInHand(Result.DENY);
        event.setUseInteractedBlock(Result.DENY);
        Block clicked = event.getClickedBlock();
        if (clicked == null)
            return;

        Optional<SmartChest> optional = SmartChest.from(clicked);
        if (optional.isEmpty())
            return;
        guiManager.openConfigGui(player, optional.get());
    }
}
