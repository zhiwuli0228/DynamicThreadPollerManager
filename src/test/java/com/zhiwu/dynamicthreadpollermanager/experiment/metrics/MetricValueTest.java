package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricValueTest {

    @Test
    void shouldExposePresentValue() {
        MetricValue<Integer> value = MetricValue.present(42);

        assertTrue(value.isPresent());
        assertFalse(value.isAbsent());
        assertEquals(42, value.asOptional().orElseThrow());
    }

    @Test
    void shouldExposeAbsentValue() {
        MetricValue<Integer> value = MetricValue.absent();

        assertFalse(value.isPresent());
        assertTrue(value.isAbsent());
        assertTrue(value.asOptional().isEmpty());
    }

    @Test
    void shouldRejectNullPresentValue() {
        assertThrows(NullPointerException.class, () -> MetricValue.present(null));
    }
}
