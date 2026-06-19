package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservedSnapshotSerializationTest {

    @Test
    void shouldRoundTripObservedSnapshot() {
        PressureSnapshot ps = new PressureSnapshot(
                Instant.parse("2026-06-13T10:00:00Z"), 3, 4, 5, 100L, 0.5);
        RuntimeObservation obs = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:00Z"),
                MetricValue.present(3),
                MetricValue.present(4),
                MetricValue.present(5),
                MetricValue.present(100L),
                MetricValue.present(0.5),
                MetricValue.present(60L),
                MetricValue.present(4),
                MetricValue.present(1000L));
        ObservedSnapshot original = new ObservedSnapshot("run-001", ps, obs);

        Map<String, Object> map = original.toMap();
        ObservedSnapshot restored = ObservedSnapshot.fromMap(map);

        assertEquals(original, restored);
    }

    @Test
    void shouldPreserveRunId() {
        PressureSnapshot ps = new PressureSnapshot(
                Instant.parse("2026-06-13T10:00:00Z"), 1, 2, 3, 10L, 0.0);
        RuntimeObservation obs = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:00Z"),
                MetricValue.present(1),
                MetricValue.absent(),
                MetricValue.present(3),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent());
        ObservedSnapshot original = new ObservedSnapshot("test-run-id-123", ps, obs);

        Map<String, Object> map = original.toMap();
        ObservedSnapshot restored = ObservedSnapshot.fromMap(map);

        assertEquals("test-run-id-123", restored.runId());
        assertEquals(original, restored);
    }
}
