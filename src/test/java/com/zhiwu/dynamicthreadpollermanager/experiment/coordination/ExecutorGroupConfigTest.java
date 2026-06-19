package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorGroupConfigTest {

    @Test
    void shouldConstructWithValidValues() {
        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "test-group", 20, 50, AdjustmentPriority.NORMAL,
                Map.of(), 5000, false);
        assertEquals("test-group", config.groupId());
        assertEquals(20, config.maxTotalThreads());
        assertEquals(50, config.maxTotalQueueCapacity());
        assertEquals(AdjustmentPriority.NORMAL, config.defaultPriority());
        assertEquals(5000, config.coordinationTimeoutMs());
        assertFalse(config.failOpen());
    }

    @Test
    void defaultsShouldReturnSensibleDefaults() {
        ExecutorGroupConfig config = ExecutorGroupConfig.defaults("my-group", 30);
        assertEquals("my-group", config.groupId());
        assertEquals(30, config.maxTotalThreads());
        assertEquals(0, config.maxTotalQueueCapacity());
        assertEquals(AdjustmentPriority.NORMAL, config.defaultPriority());
        assertEquals(5000, config.coordinationTimeoutMs());
        assertFalse(config.failOpen());
        assertTrue(config.memberPriorities().isEmpty());
    }

    @Test
    void shouldRejectBlankGroupId() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroupConfig("  ", 10, 0, AdjustmentPriority.NORMAL,
                        Map.of(), 5000, false));
    }

    @Test
    void shouldRejectZeroMaxThreads() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroupConfig("g", 0, 0, AdjustmentPriority.NORMAL,
                        Map.of(), 5000, false));
    }

    @Test
    void shouldRejectNegativeMaxThreads() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroupConfig("g", -1, 0, AdjustmentPriority.NORMAL,
                        Map.of(), 5000, false));
    }

    @Test
    void shouldRejectNegativeQueueCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroupConfig("g", 10, -1, AdjustmentPriority.NORMAL,
                        Map.of(), 5000, false));
    }

    @Test
    void shouldRejectShortTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroupConfig("g", 10, 0, AdjustmentPriority.NORMAL,
                        Map.of(), 50, false));
    }

    @Test
    void memberPrioritiesShouldBeImmutable() {
        Map<String, AdjustmentPriority> priorities = new java.util.HashMap<>();
        priorities.put("exec-A", AdjustmentPriority.HIGH);
        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "g", 10, 0, AdjustmentPriority.NORMAL, priorities, 5000, false);
        assertThrows(UnsupportedOperationException.class, () ->
                config.memberPriorities().put("exec-B", AdjustmentPriority.LOW));
    }
}
