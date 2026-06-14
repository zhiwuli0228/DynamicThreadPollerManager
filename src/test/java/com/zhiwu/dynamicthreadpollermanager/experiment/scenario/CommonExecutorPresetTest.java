package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommonExecutorPresetTest {

    @Test
    void shouldCreateValidPreset() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "fixed-4", "FIXED_THREAD_POOL", 4, 4, -1, "desc");
        assertEquals("fixed-4", preset.presetId());
        assertEquals("FIXED_THREAD_POOL", preset.executorType());
        assertEquals(4, preset.corePoolSize());
        assertEquals(4, preset.maxPoolSize());
        assertEquals(-1, preset.queueCapacity());
        assertEquals("desc", preset.description());
    }

    @Test
    void shouldAllowNullDescription() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "test", "CACHED_THREAD_POOL", 0, Integer.MAX_VALUE, 0, null);
        assertNull(preset.description());
    }

    @Test
    void shouldRejectBlankPresetId() {
        assertThrows(IllegalArgumentException.class, () ->
                new CommonExecutorPreset("", "FIXED_THREAD_POOL", 1, 1, -1, null));
    }

    @Test
    void shouldRejectNullPresetId() {
        assertThrows(NullPointerException.class, () ->
                new CommonExecutorPreset(null, "FIXED_THREAD_POOL", 1, 1, -1, null));
    }

    @Test
    void shouldRejectInvalidExecutorType() {
        assertThrows(IllegalArgumentException.class, () ->
                new CommonExecutorPreset("test", "INVALID", 1, 1, -1, null));
    }

    @Test
    void shouldRejectMaxLessThanCore() {
        assertThrows(IllegalArgumentException.class, () ->
                new CommonExecutorPreset("test", "FIXED_THREAD_POOL", 4, 2, -1, null));
    }

    @Test
    void shouldRejectQueueCapacityLessThanMinusOne() {
        assertThrows(IllegalArgumentException.class, () ->
                new CommonExecutorPreset("test", "FIXED_THREAD_POOL", 1, 2, -2, null));
    }

    @Test
    void shouldAcceptAllValidTypes() {
        assertDoesNotThrow(() -> new CommonExecutorPreset("a", "FIXED_THREAD_POOL", 1, 1, -1, null));
        assertDoesNotThrow(() -> new CommonExecutorPreset("b", "CACHED_THREAD_POOL", 0, 100, 0, null));
        assertDoesNotThrow(() -> new CommonExecutorPreset("c", "SINGLE_THREAD_EXECUTOR", 1, 1, -1, null));
    }

    @Test
    void toBaselinePresetShouldMapUnboundedQueue() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "test", "FIXED_THREAD_POOL", 2, 4, -1, null);
        BaselineExecutorPreset bp = preset.toBaselinePreset();
        assertEquals("test", bp.policyId());
        assertEquals(2, bp.corePoolSize());
        assertEquals(4, bp.maximumPoolSize());
        assertEquals(Integer.MAX_VALUE, bp.queueCapacity());
    }

    @Test
    void toBaselinePresetShouldMapSynchronousQueue() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "test", "CACHED_THREAD_POOL", 0, 100, 0, null);
        BaselineExecutorPreset bp = preset.toBaselinePreset();
        assertEquals(0, bp.queueCapacity());
    }

    @Test
    void toBaselinePresetShouldPassBoundedQueueDirectly() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "test", "FIXED_THREAD_POOL", 2, 2, 10, null);
        BaselineExecutorPreset bp = preset.toBaselinePreset();
        assertEquals(10, bp.queueCapacity());
    }

    @Test
    void toMapAndFromMapRoundTripShouldPreserveValues() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "fixed-4", "FIXED_THREAD_POOL", 4, 4, -1, "desc");
        CommonExecutorPreset restored = CommonExecutorPreset.fromMap(preset.toMap());
        assertEquals(preset.presetId(), restored.presetId());
        assertEquals(preset.executorType(), restored.executorType());
        assertEquals(preset.corePoolSize(), restored.corePoolSize());
        assertEquals(preset.maxPoolSize(), restored.maxPoolSize());
        assertEquals(preset.queueCapacity(), restored.queueCapacity());
        assertEquals(preset.description(), restored.description());
    }

    @Test
    void toMapShouldIncludeDescriptionOnlyWhenNonNull() {
        CommonExecutorPreset withDesc = new CommonExecutorPreset(
                "a", "FIXED_THREAD_POOL", 1, 1, -1, "hello");
        assertTrue(withDesc.toMap().containsKey("description"));

        CommonExecutorPreset withoutDesc = new CommonExecutorPreset(
                "b", "FIXED_THREAD_POOL", 1, 1, -1, null);
        assertFalse(withoutDesc.toMap().containsKey("description"));
    }
}
