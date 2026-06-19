package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScenarioProfileTest {

    @Test
    void shouldContainOriginalValues() {
        assertNotNull(ScenarioProfile.STEADY);
        assertNotNull(ScenarioProfile.RAMP);
        assertNotNull(ScenarioProfile.BURST);
    }

    @Test
    void shouldContainLongTailValue() {
        assertNotNull(ScenarioProfile.LONG_TAIL);
    }

    @Test
    void shouldContainMixedCpuIoValue() {
        assertNotNull(ScenarioProfile.MIXED_CPU_IO);
    }

    @Test
    void shouldContainDownstreamBlockedValue() {
        assertNotNull(ScenarioProfile.DOWNSTREAM_BLOCKED);
    }

    @Test
    void shouldHaveSixDistinctValues() {
        assertEquals(6, ScenarioProfile.values().length);
    }

    @Test
    void shouldHaveDistinctNewValuesFromExisting() {
        assertDistinct(ScenarioProfile.LONG_TAIL, ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST);
        assertDistinct(ScenarioProfile.MIXED_CPU_IO, ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST);
        assertDistinct(ScenarioProfile.DOWNSTREAM_BLOCKED, ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST);
    }

    @Test
    void newValuesShouldBeDistinctFromEachOther() {
        assertDistinct(ScenarioProfile.LONG_TAIL, ScenarioProfile.MIXED_CPU_IO, ScenarioProfile.DOWNSTREAM_BLOCKED);
    }

    private void assertDistinct(ScenarioProfile candidate, ScenarioProfile... others) {
        for (ScenarioProfile other : others) {
            org.junit.jupiter.api.Assertions.assertNotEquals(other, candidate,
                    candidate + " should not equal " + other);
        }
    }
}
