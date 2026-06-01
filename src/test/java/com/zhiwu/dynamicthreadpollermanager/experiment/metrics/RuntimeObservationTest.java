package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeObservationTest {

    @Test
    void shouldExposeAllMetrics() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                now,
                MetricValue.present(10),
                MetricValue.present(50),
                MetricValue.present(0.75)
        );

        assertEquals(now, observation.timestamp());
        assertEquals(10, observation.activeThreads().asOptional().orElseThrow());
        assertEquals(50, observation.queueSize().asOptional().orElseThrow());
        assertEquals(0.75, observation.cpuUtilization().asOptional().orElseThrow());
    }

    @Test
    void shouldRejectNullTimestamp() {
        assertThrows(NullPointerException.class, () -> new RuntimeObservation(
                null,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        ));
    }

    @Test
    void shouldRejectNullMetricValue() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        assertThrows(NullPointerException.class, () -> new RuntimeObservation(
                now,
                null,
                MetricValue.absent(),
                MetricValue.absent()
        ));
    }

    @Test
    void shouldCreateCopyWithNewTimestamp() {
        Instant t1 = Instant.parse("2026-06-02T10:00:00Z");
        Instant t2 = Instant.parse("2026-06-02T10:00:05Z");
        RuntimeObservation original = new RuntimeObservation(
                t1,
                MetricValue.present(10),
                MetricValue.absent(),
                MetricValue.present(0.5)
        );

        RuntimeObservation updated = original.withTimestamp(t2);

        assertEquals(t2, updated.timestamp());
        assertEquals(10, updated.activeThreads().asOptional().orElseThrow());
        assertTrue(updated.queueSize().isAbsent());
        assertEquals(0.5, updated.cpuUtilization().asOptional().orElseThrow());
        assertEquals(t1, original.timestamp(), "original observation must be unchanged");
    }
}
