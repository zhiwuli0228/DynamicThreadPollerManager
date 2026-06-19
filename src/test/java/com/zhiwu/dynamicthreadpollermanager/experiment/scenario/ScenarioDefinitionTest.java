package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioDefinitionTest {

    @Test
    void shouldRetainAllFieldsForValidInputs() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "steady-small",
                ScenarioProfile.STEADY,
                42L,
                5,
                10,
                "Small steady workload"
        );

        assertEquals("steady-small", definition.scenarioId());
        assertEquals(ScenarioProfile.STEADY, definition.profile());
        assertEquals(42L, definition.seed());
        assertEquals(5, definition.stepCount());
        assertEquals(10, definition.baseWorkUnits());
        assertEquals("Small steady workload", definition.description());
    }

    @Test
    void shouldTreatNullDescriptionAsEmpty() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "burst-1",
                ScenarioProfile.BURST,
                1L,
                3,
                2,
                null
        );

        assertEquals("", definition.description());
    }

    @Test
    void shouldRejectBlankScenarioId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioDefinition("", ScenarioProfile.STEADY, 0L, 1, 1, "x"));
    }

    @Test
    void shouldRejectNullScenarioId() {
        assertThrows(NullPointerException.class,
                () -> new ScenarioDefinition(null, ScenarioProfile.STEADY, 0L, 1, 1, "x"));
    }

    @Test
    void shouldRejectNullProfile() {
        assertThrows(NullPointerException.class,
                () -> new ScenarioDefinition("x", null, 0L, 1, 1, "x"));
    }

    @Test
    void shouldRejectNonPositiveStepCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioDefinition("x", ScenarioProfile.STEADY, 0L, 0, 1, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioDefinition("x", ScenarioProfile.STEADY, 0L, -1, 1, "x"));
    }

    @Test
    void shouldRejectNonPositiveBaseWorkUnits() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioDefinition("x", ScenarioProfile.STEADY, 0L, 1, 0, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioDefinition("x", ScenarioProfile.STEADY, 0L, 1, -1, "x"));
    }
}
