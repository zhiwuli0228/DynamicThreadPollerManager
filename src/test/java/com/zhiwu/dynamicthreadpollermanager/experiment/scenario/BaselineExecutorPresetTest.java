package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaselineExecutorPresetTest {

    @Test
    void shouldRetainAllFields() {
        BaselineExecutorPreset preset = new BaselineExecutorPreset(
                "baseline-fixed", 4, 8, 16);

        assertEquals("baseline-fixed", preset.policyId());
        assertEquals(4, preset.corePoolSize());
        assertEquals(8, preset.maximumPoolSize());
        assertEquals(16, preset.queueCapacity());
    }

    @Test
    void shouldExposeFixedSmallFactory() {
        BaselineExecutorPreset preset = BaselineExecutorPreset.fixedSmall();

        assertEquals("baseline-fixed", preset.policyId());
        assertEquals(2, preset.corePoolSize());
        assertEquals(2, preset.maximumPoolSize());
        assertEquals(10, preset.queueCapacity());
    }

    @Test
    void shouldRejectBlankPolicyId() {
        assertThrows(IllegalArgumentException.class,
                () -> new BaselineExecutorPreset("", 1, 1, 0));
    }

    @Test
    void shouldRejectNullPolicyId() {
        assertThrows(NullPointerException.class,
                () -> new BaselineExecutorPreset(null, 1, 1, 0));
    }

    @Test
    void shouldRejectNonPositiveCoreSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new BaselineExecutorPreset("baseline-fixed", 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BaselineExecutorPreset("baseline-fixed", -1, 1, 0));
    }

    @Test
    void shouldRejectMaximumSmallerThanCore() {
        assertThrows(IllegalArgumentException.class,
                () -> new BaselineExecutorPreset("baseline-fixed", 4, 3, 0));
    }

    @Test
    void shouldAllowMaximumEqualToCore() {
        BaselineExecutorPreset preset = new BaselineExecutorPreset("baseline-fixed", 4, 4, 0);
        assertEquals(4, preset.maximumPoolSize());
    }

    @Test
    void shouldRejectNegativeQueueCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new BaselineExecutorPreset("baseline-fixed", 1, 1, -1));
    }

    @Test
    void shouldAllowZeroQueueCapacity() {
        BaselineExecutorPreset preset = new BaselineExecutorPreset("baseline-fixed", 1, 1, 0);
        assertEquals(0, preset.queueCapacity());
    }
}
