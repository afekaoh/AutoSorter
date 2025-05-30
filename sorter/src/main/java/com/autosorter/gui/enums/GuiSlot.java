package com.autosorter.gui.enums;

public enum GuiSlot{
    // --- Main Menu ---
    MAIN_SETTINGS(12),
    MAIN_FILTERS(13),
    MAIN_CHANGE_SYSTEM(14),
    MAIN_EXIT(32),
    // --- Filters Menu ---
    FILTERS_START_SLOT(0),
    FILTERS_END_SLOT(44),
    FILTERS_CLEAR_ALL(45),
    FILTERS_CONFIRM_CLEAR_YES(46),
    FILTERS_CONFIRM_CLEAR_NO(52),
    FILTERS_RETURN(53),
    // --- System Selector ---
    SYSTEM_PREV_PAGE(45), // For future use, currently not implemented
    SYSTEM_NEXT_PAGE(53), // For future use, currently not implemented
    SYSTEM_RETURN(49),
    // --- Settings Menu ---
    SETTINGS_INPUT_SLOT(1),
    SETTINGS_RECEIVER_SLOT(3),
    SETTINGS_OVERFLOW_SLOT(5),
    SETTINGS_NONE_SLOT(7),
    SETTINGS_RETURN(24),
    // --- Info Menu ---
    INFO_BOOK(13),
    INFO_RETURN(24);

    private final int slot;

    GuiSlot(int slot){
        this.slot = slot;
    }


    public int slot(){
        return slot;
    }
}
