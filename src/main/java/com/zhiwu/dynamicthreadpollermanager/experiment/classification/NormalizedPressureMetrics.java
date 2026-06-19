package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized pressure metrics computed from a time series of snapshots.
 * 9 base fields (same calculation logic as NormalizedComparisonMetrics)
 * plus 2 derived signals for classification.
 */
public record NormalizedPressureMetrics(
        long completedTaskCount,
        long rejectedTaskCount,
        double avgQueueDepth,
        int maxQueueDepth,
        long totalDurationMs,
        double throughputPerSecond,
        double avgActiveThreads,
        int maxPoolSize,
        int snapshotCount,
        double queueGrowthRate,
        double threadUtilizationRatio
) {

    public static NormalizedPressureMetrics fromSnapshots(
            List<ObservedSnapshot> snapshots,
            long totalDurationMs,
            int fallbackPoolSize,
            int trendWindowSize) {

        Objects.requireNonNull(snapshots, "snapshots must not be null");

        if (snapshots.isEmpty()) {
            return new NormalizedPressureMetrics(
                    0L, 0L, 0.0, 0, totalDurationMs, 0.0, 0.0,
                    fallbackPoolSize, 0, 0.0, 0.0);
        }

        int count = snapshots.size();

        // --- 9 base metrics ---

        PressureSnapshot last = snapshots.get(count - 1).snapshot();
        long completed = last.completedTaskCount();

        double avgQueue = 0.0;
        int maxQueue = 0;
        for (ObservedSnapshot s : snapshots) {
            int qs = s.snapshot().queueSize();
            avgQueue += qs;
            if (qs > maxQueue) maxQueue = qs;
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
            if (ps > maxPool) maxPool = ps;
        }

        double throughput = totalDurationMs > 0
                ? (double) completed / (totalDurationMs / 1000.0)
                : 0.0;

        // --- 2 derived signals ---

        double growthRate = 0.0;
        if (trendWindowSize > 0 && count >= 2) {
            int window = Math.min(trendWindowSize, count);
            growthRate = computeQueueGrowthRate(snapshots, count - window, window);
        }

        double utilizationRatio = maxPool > 0 ? avgActive / maxPool : 0.0;

        return new NormalizedPressureMetrics(
                completed, 0L, avgQueue, maxQueue,
                totalDurationMs, throughput, avgActive, maxPool, count,
                growthRate, utilizationRatio);
    }

    public NormalizedPressureMetrics withRejectedTaskCount(long rejected) {
        return new NormalizedPressureMetrics(
                completedTaskCount, rejected, avgQueueDepth, maxQueueDepth,
                totalDurationMs, throughputPerSecond, avgActiveThreads,
                maxPoolSize, snapshotCount, queueGrowthRate, threadUtilizationRatio);
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
        map.put("queueGrowthRate", queueGrowthRate);
        map.put("threadUtilizationRatio", threadUtilizationRatio);
        return map;
    }

    private static double computeQueueGrowthRate(
            List<ObservedSnapshot> snapshots, int offset, int window) {
        int n = window;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = snapshots.get(offset + i).snapshot().queueSize();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return 0.0;
        return (n * sumXY - sumX * sumY) / denominator;
    }
}
