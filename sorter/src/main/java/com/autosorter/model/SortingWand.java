package com.autosorter.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class SortingWand {
    public static final String WAND_NAME = "Sorting Wand";
    private static final String WAND_LORE = "Right-click a chest to configure sorting.";
    private static final Material WAND_MATERIAL = Material.LIGHTNING_ROD;

    public static ItemStack createWand() {
        ItemStack wand = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            Component displayName = Component.text(WAND_NAME).colorIfAbsent(NamedTextColor.GOLD);
            meta.customName(displayName);
            meta.lore(List.of(
                    Component.text(WAND_LORE).colorIfAbsent(NamedTextColor.LIGHT_PURPLE)));
            wand.setItemMeta(meta);
        }
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        // meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return wand;
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() != WAND_MATERIAL)
            return false;

        if (!item.hasItemMeta())
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomName())
            return false;

        TextComponent customName = (TextComponent) meta.customName();
        if (!customName.content().equals(WAND_NAME))
            return false;

        if (!meta.hasLore() || meta.lore().isEmpty())
            return false;
        List<Component> lore = meta.lore();
        if (!((TextComponent) lore.get(0)).content().equals(WAND_LORE))
            return false;

        return true;
    }
}