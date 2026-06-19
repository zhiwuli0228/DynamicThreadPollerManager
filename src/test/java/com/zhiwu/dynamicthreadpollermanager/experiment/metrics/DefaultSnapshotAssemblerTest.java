package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSnapshotAssemblerTest {

    private final SnapshotAssembler assembler = new DefaultSnapshotAssembler();

    @Test
    void shouldAssembleSnapshotFromAllPresentMetrics() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                now,
                MetricValue.present(10),
                MetricValue.present(12),
                MetricValue.present(50),
                MetricValue.present(300L),
                MetricValue.present(0.75)
        );

        ObservedSnapshot observed = assembler.assemble("run-1", observation);

        assertEquals("run-1", observed.runId());
        PressureSnapshot snapshot = observed.snapshot();
        assertEquals(now, snapshot.timestamp());
        assertEquals(10, snapshot.activeThreads());
        assertEquals(12, snapshot.poolSize());
        assertEquals(50, snapshot.queueSize());
        assertEquals(300L, snapshot.completedTaskCount());
        assertEquals(0.75, snapshot.cpuUtilization());
    }

    @Test
    void shouldSubstituteZeroForAbsentMetrics() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                now,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        );

        ObservedSnapshot observed = assembler.assemble("run-1", observation);

        PressureSnapshot snapshot = observed.snapshot();
        assertEquals(0, snapshot.activeThreads());
        assertEquals(0, snapshot.poolSize());
        assertEquals(0, snapshot.queueSize());
        assertEquals(0L, snapshot.completedTaskCount());
        assertEquals(0.0, snapshot.cpuUtilization());
    }

    @Test
    void shouldPreserveAbsentMetricsOnObservation() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                now,
                MetricValue.present(7),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.present(0.4)
        );

        ObservedSnapshot observed = assembler.assemble("run-1", observation);

        assertTrue(observed.observation().activeThreads().isPresent());
        assertTrue(observed.observation().poolSize().isAbsent());
        assertTrue(observed.observation().queueSize().isAbsent());
        assertTrue(observed.observation().completedTaskCount().isAbsent());
        assertTrue(observed.observation().cpuUtilization().isPresent());
        assertEquals(7, observed.observation().activeThreads().asOptional().orElseThrow());
        assertEquals(0.4, observed.observation().cpuUtilization().asOptional().orElseThrow());
    }

    @Test
    void shouldMixPresentAndAbsentMetrics() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                now,
                MetricValue.present(5),
                MetricValue.present(6),
                MetricValue.absent(),
                MetricValue.present(50L),
                MetricValue.absent()
        );

        ObservedSnapshot observed = assembler.assemble("run-1", observation);

        PressureSnapshot snapshot = observed.snapshot();
        assertEquals(5, snapshot.activeThreads());
        assertEquals(6, snapshot.poolSize());
        assertEquals(0, snapshot.queueSize());
        assertEquals(50L, snapshot.completedTaskCount());
        assertEquals(0.0, snapshot.cpuUtilization());
    }

    @Test
    void shouldRejectNullRunId() {
        Instant now = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                now,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        );
        assertThrows(NullPointerException.class, () -> assembler.assemble(null, observation));
    }

    @Test
    void shouldRejectNullObservation() {
        assertThrows(NullPointerException.class, () -> assembler.assemble("run-1", null));
    }
}
