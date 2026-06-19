package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PressureStateTest {

    @Test
    void shouldHaveSixValues() {
        assertEquals(6, PressureState.values().length);
    }

    @Test
    void shouldHaveCorrectPriorityOrder() {
        PressureState[] values = PressureState.values();
        assertEquals(PressureState.REJECTION_ACTIVE, values[0]);
        assertEquals(PressureState.OVERLOAD, values[1]);
        assertEquals(PressureState.QUEUE_BUILDUP, values[2]);
        assertEquals(PressureState.RECOVERY, values[3]);
        assertEquals(PressureState.UNDER_UTILIZED, values[4]);
        assertEquals(PressureState.NORMAL, values[5]);
    }

    @Test
    void shouldHaveHigherPriorityForRejectionThanOverload() {
        assertTrue(PressureState.REJECTION_ACTIVE.ordinal()
                < PressureState.OVERLOAD.ordinal());
    }

    @Test
    void shouldHaveNonEmptyDescription() {
        for (PressureState state : PressureState.values()) {
            assertNotNull(state.description());
            assertFalse(state.description().isBlank());
        }
    }
}
