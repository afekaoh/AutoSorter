package com.autosorter.gui.enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum GuiTitle{
    MAIN(Component.text("Sorter Chest Config", NamedTextColor.DARK_PURPLE)),
    FILTERS(Component.text("Edit Filters", NamedTextColor.DARK_AQUA)),
    SETTINGS(Component.text("Chest Settings", NamedTextColor.DARK_GREEN)),
    SYSTEM_SELECTOR(Component.text("Select System", NamedTextColor.DARK_RED));

    private final Component title;

    GuiTitle(Component title){
        this.title = title;
    }

    public Component component(){
        return title;
    }

    public static GuiTitle fromType(GuiMenuType type){
        return switch(type){
            case MAIN -> MAIN;
            case FILTERS -> FILTERS;
            case SETTINGS -> SETTINGS;
            case SYSTEM_SELECTOR -> SYSTEM_SELECTOR;
            default -> throw new IllegalArgumentException("Unknown GuiMenuType: " + type);
        };
    }
}