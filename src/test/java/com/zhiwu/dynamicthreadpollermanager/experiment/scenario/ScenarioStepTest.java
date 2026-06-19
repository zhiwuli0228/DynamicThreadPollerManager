package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioStepTest {

    @Test
    void shouldRetainAllFields() {
        ScenarioStep step = new ScenarioStep(3, 12, 250L);

        assertEquals(3, step.index());
        assertEquals(12, step.workUnits());
        assertEquals(250L, step.plannedDelayMillis());
    }

    @Test
    void shouldAllowZeroValuesForWorkAndDelay() {
        ScenarioStep step = new ScenarioStep(0, 0, 0L);

        assertEquals(0, step.index());
        assertEquals(0, step.workUnits());
        assertEquals(0L, step.plannedDelayMillis());
    }

    @Test
    void shouldRejectNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioStep(-1, 1, 0L));
    }

    @Test
    void shouldRejectNegativeWorkUnits() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioStep(0, -1, 0L));
    }

    @Test
    void shouldRejectNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioStep(0, 1, -1L));
    }
}
