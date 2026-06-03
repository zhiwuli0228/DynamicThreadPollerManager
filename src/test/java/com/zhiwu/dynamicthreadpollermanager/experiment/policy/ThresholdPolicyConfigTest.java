package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThresholdPolicyConfigTest {

    @Test
    void shouldExposeAllConfigurationFields() {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig(
                "policy-1", 2, 16, 12, 8, 4, 3);

        assertEquals("policy-1", config.policyId());
        assertEquals(2, config.minPoolSize());
        assertEquals(16, config.maxPoolSize());
        assertEquals(12, config.scaleUpActiveThreadsThreshold());
        assertEquals(8, config.scaleUpQueueSizeThreshold());
        assertEquals(4, config.scaleDownActiveThreadsThreshold());
        assertEquals(3, config.scaleStep());
    }

    @Test
    void shouldRejectBlankPolicyId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("", 1, 8, 4, 2, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("   ", 1, 8, 4, 2, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig(null, 1, 8, 4, 2, 1, 1));
    }

    @Test
    void shouldRejectNonPositiveMinPoolSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 0, 8, 4, 2, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", -1, 8, 4, 2, 1, 1));
    }

    @Test
    void shouldRejectMaxLessThanMin() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 8, 4, 4, 2, 1, 1));
    }

    @Test
    void shouldAcceptMaxEqualToMin() {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig("policy-1", 4, 4, 0, 0, 0, 1);
        assertEquals(4, config.minPoolSize());
        assertEquals(4, config.maxPoolSize());
    }

    @Test
    void shouldRejectNegativeThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 1, 8, -1, 2, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 1, 8, 4, -1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 1, 8, 4, 2, -1, 1));
    }

    @Test
    void shouldAcceptZeroThresholds() {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig("policy-1", 1, 8, 0, 0, 0, 1);
        assertEquals(0, config.scaleUpActiveThreadsThreshold());
        assertEquals(0, config.scaleUpQueueSizeThreshold());
        assertEquals(0, config.scaleDownActiveThreadsThreshold());
    }

    @Test
    void shouldRejectNonPositiveScaleStep() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 1, 8, 4, 2, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyConfig("policy-1", 1, 8, 4, 2, 1, -2));
    }

    @Test
    void defaultAdaptiveShouldExposeSpecificValues() {
        ThresholdPolicyConfig config = ThresholdPolicyConfig.defaultAdaptive();
        assertEquals("default-adaptive", config.policyId());
        assertEquals(1, config.minPoolSize());
        assertEquals(32, config.maxPoolSize());
        assertEquals(24, config.scaleUpActiveThreadsThreshold());
        assertEquals(16, config.scaleUpQueueSizeThreshold());
        assertEquals(4, config.scaleDownActiveThreadsThreshold());
        assertEquals(2, config.scaleStep());
    }
}
