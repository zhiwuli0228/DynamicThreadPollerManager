package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedPressureMetricsTest {

    private static ObservedSnapshot snapshot(
            int activeThreads, int poolSize, int queueSize, long completed) {
        Instant now = Instant.now();
        PressureSnapshot ps = new PressureSnapshot(
                now, activeThreads, poolSize, queueSize, completed, 0.0);
        RuntimeObservation obs = new RuntimeObservation(
                now,
                MetricValue.present(activeThreads),
                MetricValue.present(queueSize),
                MetricValue.absent());
        return new ObservedSnapshot("run-1", ps, obs);
    }

    @Test
    void shouldComputeAllMetricsFromNonEmptyList() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 5, 10L),
                snapshot(3, 4, 8, 20L),
                snapshot(4, 4, 10, 30L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 3000L, 4, 3);

        assertEquals(30L, metrics.completedTaskCount());
        assertEquals(0L, metrics.rejectedTaskCount());
        assertTrue(metrics.avgQueueDepth() > 0);
        assertEquals(10, metrics.maxQueueDepth());
        assertEquals(3000L, metrics.totalDurationMs());
        assertTrue(metrics.throughputPerSecond() > 0);
        assertTrue(metrics.avgActiveThreads() > 0);
        assertEquals(4, metrics.maxPoolSize());
        assertEquals(3, metrics.snapshotCount());
    }

    @Test
    void shouldComputePositiveGrowthRateForIncreasingQueue() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 2, 10L),
                snapshot(2, 4, 4, 20L),
                snapshot(2, 4, 6, 30L),
                snapshot(2, 4, 8, 40L),
                snapshot(2, 4, 10, 50L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 5000L, 4, 5);

        assertTrue(metrics.queueGrowthRate() > 0,
                "growth rate should be positive for increasing queue, was "
                        + metrics.queueGrowthRate());
    }

    @Test
    void shouldComputeNegativeGrowthRateForDecreasingQueue() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 10, 10L),
                snapshot(2, 4, 8, 20L),
                snapshot(2, 4, 6, 30L),
                snapshot(2, 4, 4, 40L),
                snapshot(2, 4, 2, 50L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 5000L, 4, 5);

        assertTrue(metrics.queueGrowthRate() < 0,
                "growth rate should be negative for decreasing queue, was "
                        + metrics.queueGrowthRate());
    }

    @Test
    void shouldComputeNearZeroGrowthRateForStableQueue() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 3, 10L),
                snapshot(2, 4, 3, 20L),
                snapshot(2, 4, 3, 30L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 3000L, 4, 3);

        assertEquals(0.0, metrics.queueGrowthRate(), 0.01);
    }

    @Test
    void shouldComputeThreadUtilizationRatio() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(3, 8, 2, 10L),
                snapshot(3, 8, 2, 20L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 2000L, 8, 2);

        assertEquals(3.0 / 8.0, metrics.threadUtilizationRatio(), 0.01);
    }

    @Test
    void shouldReturnZeroUtilizationWhenMaxPoolIsZero() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(0, 0, 0, 0L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 1000L, 0, 1);

        assertEquals(0.0, metrics.threadUtilizationRatio());
    }

    @Test
    void shouldHandleEmptySnapshotList() {
        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(List.of(), 1000L, 4, 5);

        assertEquals(0L, metrics.completedTaskCount());
        assertEquals(0L, metrics.rejectedTaskCount());
        assertEquals(0.0, metrics.avgQueueDepth());
        assertEquals(0, metrics.maxQueueDepth());
        assertEquals(1000L, metrics.totalDurationMs());
        assertEquals(0.0, metrics.throughputPerSecond());
        assertEquals(0.0, metrics.avgActiveThreads());
        assertEquals(4, metrics.maxPoolSize());
        assertEquals(0, metrics.snapshotCount());
        assertEquals(0.0, metrics.queueGrowthRate());
        assertEquals(0.0, metrics.threadUtilizationRatio());
    }

    @Test
    void shouldReturnZeroThroughputForZeroDuration() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 2, 10L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 0L, 4, 1);

        assertEquals(0.0, metrics.throughputPerSecond());
    }

    @Test
    void shouldPreserveOtherFieldsWhenUpdatingRejectedCount() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 2, 10L));

        NormalizedPressureMetrics original =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 1000L, 4, 1);
        NormalizedPressureMetrics updated = original.withRejectedTaskCount(5L);

        assertEquals(5L, updated.rejectedTaskCount());
        assertEquals(original.completedTaskCount(), updated.completedTaskCount());
        assertEquals(original.avgQueueDepth(), updated.avgQueueDepth());
        assertEquals(original.maxQueueDepth(), updated.maxQueueDepth());
        assertEquals(original.throughputPerSecond(), updated.throughputPerSecond());
        assertEquals(original.queueGrowthRate(), updated.queueGrowthRate());
        assertEquals(original.threadUtilizationRatio(),
                updated.threadUtilizationRatio());
    }

    @Test
    void shouldReturnElevenEntriesInToMap() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(2, 4, 2, 10L));

        NormalizedPressureMetrics metrics =
                NormalizedPressureMetrics.fromSnapshots(snapshots, 1000L, 4, 1);

        assertEquals(11, metrics.toMap().size());
        assertTrue(metrics.toMap().containsKey("queueGrowthRate"));
        assertTrue(metrics.toMap().containsKey("threadUtilizationRatio"));
    }
}
