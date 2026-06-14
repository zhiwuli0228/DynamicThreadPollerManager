package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassifierConfigTest {

    @Test
    void shouldCreateWithValidValues() {
        ClassifierConfig config = new ClassifierConfig(5, 0.1, 10, 100);
        assertEquals(5, config.trendWindowSize());
        assertEquals(0.1, config.queueGrowthThreshold());
        assertEquals(10, config.rejectionWindowSize());
        assertEquals(100, config.queueCapacity());
    }

    @Test
    void shouldReturnDefaults() {
        ClassifierConfig config = ClassifierConfig.defaults();
        assertEquals(5, config.trendWindowSize());
        assertEquals(0.1, config.queueGrowthThreshold());
        assertEquals(10, config.rejectionWindowSize());
        assertEquals(Integer.MAX_VALUE, config.queueCapacity());
    }

    @Test
    void shouldRejectTrendWindowSizeBelowTwo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClassifierConfig(1, 0.1, 10, 100));
    }

    @Test
    void shouldRejectNonPositiveGrowthThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClassifierConfig(5, 0.0, 10, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ClassifierConfig(5, -0.5, 10, 100));
    }

    @Test
    void shouldRejectRejectionWindowSizeBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClassifierConfig(5, 0.1, 0, 100));
    }

    @Test
    void shouldRejectNegativeQueueCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClassifierConfig(5, 0.1, 10, -1));
    }

    @Test
    void shouldAcceptMaxValueAsUnboundedQueue() {
        ClassifierConfig config = new ClassifierConfig(5, 0.1, 10,
                Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.queueCapacity());
    }

    @Test
    void shouldAcceptZeroQueueCapacityForSyncQueue() {
        ClassifierConfig config = new ClassifierConfig(5, 0.1, 10, 0);
        assertEquals(0, config.queueCapacity());
    }
}
