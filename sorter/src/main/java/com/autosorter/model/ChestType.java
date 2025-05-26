package com.autosorter.model;

public enum ChestType {
    NONE("Not a Sorter Chest"),
    INPUT("Input Chest"),
    RECEIVER("Receiver Chest"),
    OVERFLOW("Overflow Chest");

    private final String displayName;

    ChestType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ChestType fromString(String text) {
        for (ChestType b : ChestType.values()) {
            if (b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        return NONE; // Default or fallback
    }
}