package com.autosorter.gui.enums;

public enum GuiMenuSize{
    ROW_LENGTH(9),
    MAIN(5 * ROW_LENGTH.size),
    FILTERS(6 * ROW_LENGTH.size),
    SETTINGS(3 * ROW_LENGTH.size),
    SYSTEM_SELECTOR(6 * ROW_LENGTH.size),
    INFO(3 * ROW_LENGTH.size);

    private final int size;

    GuiMenuSize(int size){
        this.size = size;
    }

    public int size(){
        return size;
    }

    public static GuiMenuSize fromType(GuiMenuType type){
        return switch(type){
            case MAIN -> MAIN;
            case FILTERS -> FILTERS;
            case SETTINGS -> SETTINGS;
            case SYSTEM_SELECTOR -> SYSTEM_SELECTOR;
            case INFO -> INFO;
            default -> throw new IllegalArgumentException("Unknown GuiMenuType: " + type);
        };
    }
}
