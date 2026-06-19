package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParameterBoundsTest {

    @Test
    void intBoundsWithinShouldReturnTrue() {
        IntParameterBounds bounds = IntParameterBounds.of(1, 10);
        assertTrue(bounds.within(1));
        assertTrue(bounds.within(5));
        assertTrue(bounds.within(10));
    }

    @Test
    void intBoundsWithinShouldReturnFalse() {
        IntParameterBounds bounds = IntParameterBounds.of(1, 10);
        assertFalse(bounds.within(0));
        assertFalse(bounds.within(11));
    }

    @Test
    void intBoundsInvalidConstructionShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> IntParameterBounds.of(10, 1));
    }

    @Test
    void longBoundsWithinShouldReturnTrue() {
        LongParameterBounds bounds = LongParameterBounds.of(0, Long.MAX_VALUE);
        assertTrue(bounds.within(0));
        assertTrue(bounds.within(60_000L));
        assertTrue(bounds.within(Long.MAX_VALUE));
    }

    @Test
    void longBoundsWithinShouldReturnFalse() {
        LongParameterBounds bounds = LongParameterBounds.of(0, 100);
        assertFalse(bounds.within(-1));
        assertFalse(bounds.within(101));
    }

    @Test
    void longBoundsInvalidConstructionShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> LongParameterBounds.of(100, 0));
    }

    @Test
    void intBoundsEquality() {
        IntParameterBounds a = IntParameterBounds.of(1, 10);
        IntParameterBounds b = IntParameterBounds.of(1, 10);
        IntParameterBounds c = IntParameterBounds.of(1, 20);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void runtimeSettingConstants() {
        assertEquals(1, RuntimeSetting.CORE_POOL_SIZE_BOUNDS.minValue());
        assertEquals(Integer.MAX_VALUE, RuntimeSetting.CORE_POOL_SIZE_BOUNDS.maxValue());
        assertEquals(1, RuntimeSetting.MAX_POOL_SIZE_BOUNDS.minValue());
        assertEquals(0, RuntimeSetting.KEEP_ALIVE_TIME_BOUNDS.minValue());
        assertEquals(Long.MAX_VALUE, RuntimeSetting.KEEP_ALIVE_TIME_BOUNDS.maxValue());
    }

    @Test
    void adjustableParameterEnum() {
        assertEquals(3, AdjustableParameter.values().length);
        assertNotNull(AdjustableParameter.valueOf("CORE_POOL_SIZE"));
        assertNotNull(AdjustableParameter.valueOf("MAX_POOL_SIZE"));
        assertNotNull(AdjustableParameter.valueOf("KEEP_ALIVE_TIME"));
    }

    @Test
    void nonAdjustableParameterEnum() {
        assertEquals(2, NonAdjustableParameter.values().length);
        assertNotNull(NonAdjustableParameter.valueOf("QUEUE_CAPACITY"));
        assertNotNull(NonAdjustableParameter.valueOf("REJECTION_POLICY"));
    }
}
