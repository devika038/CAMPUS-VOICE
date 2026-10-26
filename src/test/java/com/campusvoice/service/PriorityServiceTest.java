package com.campusvoice.service;

import com.campusvoice.entity.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityServiceTest {

    private PriorityService priorityService;

    @BeforeEach
    void setUp() {
        priorityService = new PriorityService();
    }

    @Test
    void testCategoryBasePriorities() {
        // Harassment -> CRITICAL (3)
        assertEquals(Priority.CRITICAL, priorityService.calculatePriority("Harassment", "Normal description"));

        // Infrastructure & Lab Equipment -> HIGH (2)
        assertEquals(Priority.HIGH, priorityService.calculatePriority("Infrastructure", "Normal description"));
        assertEquals(Priority.HIGH, priorityService.calculatePriority("Lab Equipment", "Normal description"));

        // Hostel, Examination, Academics -> MEDIUM (1)
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Hostel", "Normal description"));
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Examination", "Normal description"));
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Academics", "Normal description"));

        // Library, Other -> LOW (0)
        assertEquals(Priority.LOW, priorityService.calculatePriority("Library", "Normal description"));
        assertEquals(Priority.LOW, priorityService.calculatePriority("Other", "Normal description"));
    }

    @Test
    void testUrgentKeywordsIncrementPriority() {
        // Library (0) + urgent (1) = MEDIUM (1)
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Library", "This is an urgent request"));
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Other", "Extreme danger identified"));
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Library", "There is a fire hazard"));
        assertEquals(Priority.MEDIUM, priorityService.calculatePriority("Other", "The condition is unsafe"));

        // Hostel (1) + urgent (1) = HIGH (2)
        assertEquals(Priority.HIGH, priorityService.calculatePriority("Hostel", "Urgent water problem"));

        // Infrastructure (2) + urgent (1) = CRITICAL (3)
        assertEquals(Priority.CRITICAL, priorityService.calculatePriority("Infrastructure", "Unsafe broken stairs"));

        // Harassment (3) + urgent (1) = CRITICAL (clamped to 3)
        assertEquals(Priority.CRITICAL, priorityService.calculatePriority("Harassment", "Urgent intervention needed"));
    }
}
