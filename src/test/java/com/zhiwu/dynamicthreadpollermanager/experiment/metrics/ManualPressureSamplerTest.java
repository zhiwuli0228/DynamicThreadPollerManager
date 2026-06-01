package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ManualPressureSamplerTest {

    @Test
    void shouldSampleWithSuppliedTimestampAndObservation() {
        PressureSampler sampler = new ManualPressureSampler();
        Instant at = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                Instant.parse("2026-06-02T09:59:00Z"),
                MetricValue.present(8),
                MetricValue.present(40),
                MetricValue.present(0.6)
        );

        ObservedSnapshot sampled = sampler.sample("run-1", observation, at);

        assertEquals("run-1", sampled.runId());
        PressureSnapshot snapshot = sampled.snapshot();
        assertEquals(at, snapshot.timestamp(), "sampler should override the observation timestamp");
        assertEquals(8, snapshot.activeThreads());
        assertEquals(40, snapshot.queueSize());
        assertEquals(0.6, snapshot.cpuUtilization());
    }

    @Test
    void shouldSupportCustomAssembler() {
        SnapshotAssembler assembler = (runId, observation) -> new ObservedSnapshot(
                runId,
                new PressureSnapshot(observation.timestamp(), -1, -1, -1.0),
                observation
        );
        PressureSampler sampler = new ManualPressureSampler(assembler);
        Instant at = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                at,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        );

        ObservedSnapshot sampled = sampler.sample("run-1", observation, at);

        assertEquals(-1, sampled.snapshot().activeThreads());
        assertEquals(-1, sampled.snapshot().queueSize());
        assertEquals(-1.0, sampled.snapshot().cpuUtilization());
    }

    @Test
    void shouldRejectNullAssembler() {
        assertThrows(NullPointerException.class, () -> new ManualPressureSampler(null));
    }

    @Test
    void shouldRejectNullArguments() {
        PressureSampler sampler = new ManualPressureSampler();
        Instant at = Instant.parse("2026-06-02T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                at,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        );

        assertThrows(NullPointerException.class, () -> sampler.sample(null, observation, at));
        assertThrows(NullPointerException.class, () -> sampler.sample("run-1", null, at));
        assertThrows(NullPointerException.class, () -> sampler.sample("run-1", observation, null));
    }
}
