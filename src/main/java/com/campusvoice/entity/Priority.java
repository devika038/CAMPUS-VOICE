package com.campusvoice.entity;

public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static Priority fromString(String text) {
        if (text == null || text.isBlank()) return null;
        for (Priority p : Priority.values()) {
            if (p.name().equalsIgnoreCase(text.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown priority: " + text);
    }
}
