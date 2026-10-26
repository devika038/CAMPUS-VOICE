package com.campusvoice.entity;

public enum Role {
    STUDENT("Student"),
    FACULTY("Faculty"),
    DEPARTMENT_ADMIN("Department Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String text) {
        if (text == null) return null;
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(text) || r.displayName.equalsIgnoreCase(text)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + text);
    }
}
