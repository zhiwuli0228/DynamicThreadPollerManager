package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricDeltaTest {

    @Test
    void computeShouldReturnImprovedWhenHigherIsBetter() {
        MetricDelta delta = MetricDelta.compute("throughputPerSecond", 1000.0, 1200.0, true);
        assertEquals("IMPROVED", delta.direction());
        assertEquals(200.0, delta.absoluteDelta());
        assertEquals(20.0, delta.relativeDelta());
    }

    @Test
    void computeShouldReturnRegressedWhenHigherIsBetter() {
        MetricDelta delta = MetricDelta.compute("throughputPerSecond", 1200.0, 1000.0, true);
        assertEquals("REGRESSED", delta.direction());
        assertEquals(-200.0, delta.absoluteDelta());
    }

    @Test
    void computeShouldReturnImprovedWhenLowerIsBetter() {
        MetricDelta delta = MetricDelta.compute("avgQueueDepth", 10.0, 3.0, false);
        assertEquals("IMPROVED", delta.direction());
        assertEquals(-7.0, delta.absoluteDelta());
    }

    @Test
    void computeShouldReturnRegressedWhenLowerIsBetter() {
        MetricDelta delta = MetricDelta.compute("avgQueueDepth", 3.0, 10.0, false);
        assertEquals("REGRESSED", delta.direction());
    }

    @Test
    void computeShouldReturnNeutralWhenBelowThreshold() {
        MetricDelta delta = MetricDelta.compute("throughputPerSecond", 1000.0, 1005.0, true);
        assertEquals("NEUTRAL", delta.direction());
        assertEquals(0.5, delta.relativeDelta());
    }

    @Test
    void computeShouldHandleZeroBaselineValue() {
        MetricDelta delta = MetricDelta.compute("rejectedTaskCount", 0.0, 5.0, false);
        assertEquals(0.0, delta.relativeDelta());
    }

    @Test
    void toMapAndFromMapRoundTripShouldPreserveValues() {
        MetricDelta original = MetricDelta.compute("test", 100.0, 150.0, true);
        MetricDelta restored = MetricDelta.fromMap(original.toMap());

        assertEquals(original.metricName(), restored.metricName());
        assertEquals(original.baselineValue(), restored.baselineValue());
        assertEquals(original.managedValue(), restored.managedValue());
        assertEquals(original.absoluteDelta(), restored.absoluteDelta());
        assertEquals(original.relativeDelta(), restored.relativeDelta());
        assertEquals(original.direction(), restored.direction());
    }
}
