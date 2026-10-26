package com.campusvoice.entity;

import java.util.List;

public enum ComplaintStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED;

    public boolean canTransitionTo(ComplaintStatus target) {
        if (target == null) return false;
        return switch (this) {
            case PENDING -> target == ASSIGNED;
            case ASSIGNED -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == RESOLVED || target == REJECTED;
            case RESOLVED, REJECTED -> false;
        };
    }

    public List<ComplaintStatus> getAllowedNextStatuses() {
        return switch (this) {
            case PENDING -> List.of(ASSIGNED);
            case ASSIGNED -> List.of(IN_PROGRESS);
            case IN_PROGRESS -> List.of(RESOLVED, REJECTED);
            case RESOLVED, REJECTED -> List.of();
        };
    }

    public boolean isTerminal() {
        return this == RESOLVED || this == REJECTED;
    }

    public static ComplaintStatus fromString(String text) {
        if (text == null || text.isBlank()) return null;
        for (ComplaintStatus s : ComplaintStatus.values()) {
            if (s.name().equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + text);
    }
}
