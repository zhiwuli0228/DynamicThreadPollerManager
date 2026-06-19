package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedComparisonMetricsTest {

    private ObservedSnapshot createSnapshot(Instant time, int active, int pool, int queue, long completed) {
        PressureSnapshot ps = new PressureSnapshot(time, active, pool, queue, completed, 0.0);
        RuntimeObservation obs = new RuntimeObservation(time,
                MetricValue.present(active),
                MetricValue.present(pool),
                MetricValue.present(queue),
                MetricValue.present(completed),
                MetricValue.absent());
        return new ObservedSnapshot("run-1", ps, obs);
    }

    @Test
    void fromSnapshotsShouldComputeAllFields() {
        Instant now = Instant.now();
        List<ObservedSnapshot> snapshots = new ArrayList<>();
        snapshots.add(createSnapshot(now, 2, 4, 5, 100L));
        snapshots.add(createSnapshot(now.plusSeconds(1), 3, 4, 8, 200L));
        snapshots.add(createSnapshot(now.plusSeconds(2), 1, 4, 3, 300L));

        NormalizedComparisonMetrics metrics = NormalizedComparisonMetrics.fromSnapshots(
                snapshots, 3000L, 4);

        assertEquals(300L, metrics.completedTaskCount());
        assertEquals(0L, metrics.rejectedTaskCount());
        assertEquals((5.0 + 8.0 + 3.0) / 3.0, metrics.avgQueueDepth(), 0.01);
        assertEquals(8, metrics.maxQueueDepth());
        assertEquals(3000L, metrics.totalDurationMs());
        assertTrue(metrics.throughputPerSecond() > 0);
        assertEquals((2.0 + 3.0 + 1.0) / 3.0, metrics.avgActiveThreads(), 0.01);
        assertEquals(4, metrics.maxPoolSize());
        assertEquals(3, metrics.snapshotCount());
    }

    @Test
    void fromSnapshotsWithEmptyListShouldReturnZeroMetrics() {
        NormalizedComparisonMetrics metrics = NormalizedComparisonMetrics.fromSnapshots(
                Collections.emptyList(), 1000L, 4);

        assertEquals(0L, metrics.completedTaskCount());
        assertEquals(0L, metrics.rejectedTaskCount());
        assertEquals(0.0, metrics.avgQueueDepth());
        assertEquals(0, metrics.maxQueueDepth());
        assertEquals(1000L, metrics.totalDurationMs());
        assertEquals(0.0, metrics.throughputPerSecond());
        assertEquals(0.0, metrics.avgActiveThreads());
        assertEquals(4, metrics.maxPoolSize());
        assertEquals(0, metrics.snapshotCount());
    }

    @Test
    void fromSnapshotsWithZeroDurationShouldReturnZeroThroughput() {
        Instant now = Instant.now();
        List<ObservedSnapshot> snapshots = List.of(
                createSnapshot(now, 1, 2, 0, 50L));

        NormalizedComparisonMetrics metrics = NormalizedComparisonMetrics.fromSnapshots(
                snapshots, 0L, 2);

        assertEquals(0.0, metrics.throughputPerSecond());
        assertEquals(50L, metrics.completedTaskCount());
    }

    @Test
    void withRejectedTaskCountShouldOverrideDefaultZero() {
        Instant now = Instant.now();
        List<ObservedSnapshot> snapshots = List.of(
                createSnapshot(now, 1, 2, 0, 100L));
        NormalizedComparisonMetrics original = NormalizedComparisonMetrics.fromSnapshots(
                snapshots, 1000L, 2);

        NormalizedComparisonMetrics updated = original.withRejectedTaskCount(5L);

        assertEquals(5L, updated.rejectedTaskCount());
        assertEquals(original.completedTaskCount(), updated.completedTaskCount());
        assertEquals(original.avgQueueDepth(), updated.avgQueueDepth());
    }

    @Test
    void toMapAndFromMapRoundTripShouldPreserveValues() {
        Instant now = Instant.now();
        List<ObservedSnapshot> snapshots = List.of(
                createSnapshot(now, 2, 4, 3, 200L));
        NormalizedComparisonMetrics original = NormalizedComparisonMetrics.fromSnapshots(
                snapshots, 1000L, 4);

        NormalizedComparisonMetrics restored = NormalizedComparisonMetrics.fromMap(original.toMap());

        assertEquals(original.completedTaskCount(), restored.completedTaskCount());
        assertEquals(original.rejectedTaskCount(), restored.rejectedTaskCount());
        assertEquals(original.throughputPerSecond(), restored.throughputPerSecond());
        assertEquals(original.maxPoolSize(), restored.maxPoolSize());
        assertEquals(original.snapshotCount(), restored.snapshotCount());
    }
}
