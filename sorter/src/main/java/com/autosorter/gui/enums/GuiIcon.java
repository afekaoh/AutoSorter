package com.autosorter.gui.enums;


import org.bukkit.Material;

public enum GuiIcon{
    TYPE(Material.CHEST),
    TYPE_INPUT(Material.HOPPER),
    TYPE_RECEIVER(Material.ENDER_CHEST),
    TYPE_OVERFLOW(Material.BUCKET),
    TYPE_NONE(Material.SNOW_BLOCK),
    SYSTEM(Material.COMPASS),
    FILTERS_ACTIVE(Material.TUBE_CORAL_BLOCK),
    FILTERS_INACTIVE(Material.DEAD_TUBE_CORAL_BLOCK),
    INFO(Material.BOOK),
    PRIORITY(Material.COMPARATOR), // For future use, currently not implemented
    SAVE(Material.WRITABLE_BOOK),
    CLEAR(Material.BARRIER),
    EXIT(Material.DARK_OAK_DOOR),
    PLAYER(Material.PLAYER_HEAD), // For future use, currently not implemented
    CONFIRM(Material.LIME_CONCRETE),
    CANCEL(Material.RED_CONCRETE),
    FILLER(Material.GRAY_STAINED_GLASS_PANE),
    FILLER_PURPLE(Material.PURPLE_STAINED_GLASS_PANE),
    FILLER_PINK(Material.PINK_STAINED_GLASS_PANE);


    private final Material material;

    GuiIcon(Material material){
        this.material = material;
    }

    public Material material(){
        return material;
    }


}