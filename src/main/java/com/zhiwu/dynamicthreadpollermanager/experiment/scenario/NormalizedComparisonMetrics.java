package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NormalizedComparisonMetrics(
        long completedTaskCount,
        long rejectedTaskCount,
        double avgQueueDepth,
        int maxQueueDepth,
        long totalDurationMs,
        double throughputPerSecond,
        double avgActiveThreads,
        int maxPoolSize,
        int snapshotCount
) {
    public static NormalizedComparisonMetrics fromSnapshots(
            List<ObservedSnapshot> snapshots,
            long totalDurationMs,
            int fallbackPoolSize) {

        Objects.requireNonNull(snapshots, "snapshots must not be null");

        if (snapshots.isEmpty()) {
            return new NormalizedComparisonMetrics(
                    0L, 0L, 0.0, 0, totalDurationMs, 0.0, 0.0, fallbackPoolSize, 0);
        }

        int count = snapshots.size();

        PressureSnapshot last = snapshots.get(count - 1).snapshot();
        long completed = last.completedTaskCount();

        double avgQueue = 0.0;
        int maxQueue = 0;
        for (ObservedSnapshot s : snapshots) {
            int qs = s.snapshot().queueSize();
            avgQueue += qs;
            if (qs > maxQueue) {
                maxQueue = qs;
            }
        }
        avgQueue /= count;

        double avgActive = 0.0;
        for (ObservedSnapshot s : snapshots) {
            avgActive += s.snapshot().activeThreads();
        }
        avgActive /= count;

        int maxPool = fallbackPoolSize;
        for (ObservedSnapshot s : snapshots) {
            int ps = s.snapshot().poolSize();
            if (ps > maxPool) {
                maxPool = ps;
            }
        }

        double throughput = totalDurationMs > 0
                ? (double) completed / (totalDurationMs / 1000.0)
                : 0.0;

        return new NormalizedComparisonMetrics(
                completed, 0L, avgQueue, maxQueue,
                totalDurationMs, throughput, avgActive, maxPool, count);
    }

    public NormalizedComparisonMetrics withRejectedTaskCount(long rejected) {
        return new NormalizedComparisonMetrics(
                completedTaskCount, rejected, avgQueueDepth, maxQueueDepth,
                totalDurationMs, throughputPerSecond, avgActiveThreads,
                maxPoolSize, snapshotCount);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("completedTaskCount", completedTaskCount);
        map.put("rejectedTaskCount", rejectedTaskCount);
        map.put("avgQueueDepth", avgQueueDepth);
        map.put("maxQueueDepth", maxQueueDepth);
        map.put("totalDurationMs", totalDurationMs);
        map.put("throughputPerSecond", throughputPerSecond);
        map.put("avgActiveThreads", avgActiveThreads);
        map.put("maxPoolSize", maxPoolSize);
        map.put("snapshotCount", snapshotCount);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static NormalizedComparisonMetrics fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        return new NormalizedComparisonMetrics(
                ((Number) map.get("completedTaskCount")).longValue(),
                ((Number) map.get("rejectedTaskCount")).longValue(),
                ((Number) map.get("avgQueueDepth")).doubleValue(),
                ((Number) map.get("maxQueueDepth")).intValue(),
                ((Number) map.get("totalDurationMs")).longValue(),
                ((Number) map.get("throughputPerSecond")).doubleValue(),
                ((Number) map.get("avgActiveThreads")).doubleValue(),
                ((Number) map.get("maxPoolSize")).intValue(),
                ((Number) map.get("snapshotCount")).intValue());
    }
}
