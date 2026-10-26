package com.campusvoice.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintStatusTest {

    @Test
    void testAllowedTransitions() {
        // PENDING -> ASSIGNED
        assertTrue(ComplaintStatus.PENDING.canTransitionTo(ComplaintStatus.ASSIGNED));
        assertFalse(ComplaintStatus.PENDING.canTransitionTo(ComplaintStatus.IN_PROGRESS));
        assertFalse(ComplaintStatus.PENDING.canTransitionTo(ComplaintStatus.RESOLVED));

        // ASSIGNED -> IN_PROGRESS
        assertTrue(ComplaintStatus.ASSIGNED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
        assertFalse(ComplaintStatus.ASSIGNED.canTransitionTo(ComplaintStatus.RESOLVED));
        assertFalse(ComplaintStatus.ASSIGNED.canTransitionTo(ComplaintStatus.PENDING));

        // IN_PROGRESS -> RESOLVED or REJECTED
        assertTrue(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.RESOLVED));
        assertTrue(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.REJECTED));
        assertFalse(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.ASSIGNED));

        // RESOLVED is terminal
        assertTrue(ComplaintStatus.RESOLVED.isTerminal());
        assertFalse(ComplaintStatus.RESOLVED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
        assertFalse(ComplaintStatus.RESOLVED.canTransitionTo(ComplaintStatus.REJECTED));

        // REJECTED is terminal
        assertTrue(ComplaintStatus.REJECTED.isTerminal());
        assertFalse(ComplaintStatus.REJECTED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
        assertFalse(ComplaintStatus.REJECTED.canTransitionTo(ComplaintStatus.RESOLVED));
    }
}
