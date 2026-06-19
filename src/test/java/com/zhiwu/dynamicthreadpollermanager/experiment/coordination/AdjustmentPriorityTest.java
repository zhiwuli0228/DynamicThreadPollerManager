package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdjustmentPriorityTest {

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, AdjustmentPriority.values().length);
        assertNotNull(AdjustmentPriority.valueOf("CRITICAL"));
        assertNotNull(AdjustmentPriority.valueOf("HIGH"));
        assertNotNull(AdjustmentPriority.valueOf("NORMAL"));
        assertNotNull(AdjustmentPriority.valueOf("LOW"));
    }

    @Test
    void criticalShouldPreemptAllOthers() {
        assertTrue(AdjustmentPriority.CRITICAL.canPreempt(AdjustmentPriority.HIGH));
        assertTrue(AdjustmentPriority.CRITICAL.canPreempt(AdjustmentPriority.NORMAL));
        assertTrue(AdjustmentPriority.CRITICAL.canPreempt(AdjustmentPriority.LOW));
    }

    @Test
    void highShouldPreemptNormalAndLow() {
        assertFalse(AdjustmentPriority.HIGH.canPreempt(AdjustmentPriority.CRITICAL));
        assertTrue(AdjustmentPriority.HIGH.canPreempt(AdjustmentPriority.NORMAL));
        assertTrue(AdjustmentPriority.HIGH.canPreempt(AdjustmentPriority.LOW));
    }

    @Test
    void normalShouldOnlyPreemptLow() {
        assertFalse(AdjustmentPriority.NORMAL.canPreempt(AdjustmentPriority.CRITICAL));
        assertFalse(AdjustmentPriority.NORMAL.canPreempt(AdjustmentPriority.HIGH));
        assertTrue(AdjustmentPriority.NORMAL.canPreempt(AdjustmentPriority.LOW));
    }

    @Test
    void lowCannotPreemptAnyone() {
        assertFalse(AdjustmentPriority.LOW.canPreempt(AdjustmentPriority.CRITICAL));
        assertFalse(AdjustmentPriority.LOW.canPreempt(AdjustmentPriority.HIGH));
        assertFalse(AdjustmentPriority.LOW.canPreempt(AdjustmentPriority.NORMAL));
    }

    @Test
    void getLevelShouldReturnCorrectValues() {
        assertEquals(4, AdjustmentPriority.CRITICAL.getLevel());
        assertEquals(3, AdjustmentPriority.HIGH.getLevel());
        assertEquals(2, AdjustmentPriority.NORMAL.getLevel());
        assertEquals(1, AdjustmentPriority.LOW.getLevel());
    }
}
