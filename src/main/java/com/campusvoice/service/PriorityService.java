package com.campusvoice.service;

import com.campusvoice.entity.Priority;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PriorityService {

    private static final Pattern URGENT_PATTERN = Pattern.compile(
            "\\b(urgent|danger|fire|unsafe)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Calculates priority preserving the exact frontend logic from nextPriority:
     * Base weights:
     * Harassment: 3 (CRITICAL)
     * Infrastructure, Lab Equipment: 2 (HIGH)
     * Hostel, Examination, Academics: 1 (MEDIUM)
     * Library, Other: 0 (LOW)
     * Keywords: "urgent", "danger", "fire", "unsafe" increment priority rank by 1 (clamped to CRITICAL).
     */
    public Priority calculatePriority(String categoryName, String description) {
        int base = 0;
        if (categoryName != null) {
            switch (categoryName.trim()) {
                case "Harassment" -> base = 3;
                case "Infrastructure", "Lab Equipment" -> base = 2;
                case "Hostel", "Examination", "Academics" -> base = 1;
                case "Library", "Other" -> base = 0;
                default -> base = 0;
            }
        }

        boolean hasUrgentKeyword = description != null && URGENT_PATTERN.matcher(description).find();
        int score = Math.min(3, base + (hasUrgentKeyword ? 1 : 0));

        return switch (score) {
            case 3 -> Priority.CRITICAL;
            case 2 -> Priority.HIGH;
            case 1 -> Priority.MEDIUM;
            default -> Priority.LOW;
        };
    }
}
