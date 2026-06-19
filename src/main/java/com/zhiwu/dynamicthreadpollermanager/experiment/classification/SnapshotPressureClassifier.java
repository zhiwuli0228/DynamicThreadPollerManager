package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stateless, trend-aware implementation of {@link PressureClassifier}.
 * Each {@code classify()} call is fully self-contained — no cross-call state.
 */
public final class SnapshotPressureClassifier implements PressureClassifier {

    @Override
    public PressureClassification classify(
            List<ObservedSnapshot> snapshots,
            ClassifierConfig config,
            long rejectedTaskCount,
            long totalDurationMs) {

        Objects.requireNonNull(snapshots, "snapshots must not be null");
        Objects.requireNonNull(config, "config must not be null");

        Instant now = Instant.now();

        if (snapshots.isEmpty()) {
            NormalizedPressureMetrics emptyMetrics =
                    NormalizedPressureMetrics.fromSnapshots(snapshots, totalDurationMs, 0, 0);
            return new PressureClassification(
                    PressureState.NORMAL, 0.0,
                    List.of("No snapshots available — defaulting to NORMAL"),
                    emptyMetrics, now);
        }

        int fallbackPoolSize = computeFallbackPoolSize(snapshots);
        NormalizedPressureMetrics metrics = NormalizedPressureMetrics
                .fromSnapshots(snapshots, totalDurationMs, fallbackPoolSize,
                        config.trendWindowSize())
                .withRejectedTaskCount(rejectedTaskCount);

        int snapshotCount = snapshots.size();

        // 1. REJECTION_ACTIVE
        if (metrics.rejectedTaskCount() > 0) {
            return build(metrics, PressureState.REJECTION_ACTIVE, 0.95,
                    "rejectedTaskCount=%d > 0".formatted(metrics.rejectedTaskCount()),
                    now, snapshotCount, config.trendWindowSize());
        }

        double utilization = metrics.threadUtilizationRatio();
        double growth = metrics.queueGrowthRate();
        int maxQueue = metrics.maxQueueDepth();

        // 2. OVERLOAD
        boolean queueOverThreshold;
        if (config.queueCapacity() == Integer.MAX_VALUE) {
            queueOverThreshold = maxQueue > 0;
        } else if (config.queueCapacity() == 0) {
            queueOverThreshold = true;
        } else {
            queueOverThreshold = maxQueue >= config.queueCapacity() * 0.5;
        }

        if (utilization >= 0.8 && queueOverThreshold) {
            double conf = utilization >= 0.95 ? 0.95 : 0.85;
            return build(metrics, PressureState.OVERLOAD, conf,
                    "threadUtilization=%.2f, maxQueueDepth=%d, queueCapacity=%d"
                            .formatted(utilization, maxQueue, config.queueCapacity()),
                    now, snapshotCount, config.trendWindowSize());
        }

        // 3. QUEUE_BUILDUP
        if (growth > config.queueGrowthThreshold() && utilization < 0.8) {
            double conf = Math.min(0.9, 0.6 + growth * 0.3);
            return build(metrics, PressureState.QUEUE_BUILDUP, conf,
                    "queueGrowthRate=%.2f > threshold=%.2f, utilization=%.2f"
                            .formatted(growth, config.queueGrowthThreshold(), utilization),
                    now, snapshotCount, config.trendWindowSize());
        }

        // 4. RECOVERY (pure trend — no cross-call state)
        if (growth < -config.queueGrowthThreshold() && utilization < 0.5 && maxQueue > 0) {
            double conf = Math.min(0.9, 0.6 + Math.abs(growth) * 0.3);
            return build(metrics, PressureState.RECOVERY, conf,
                    "queueGrowthRate=%.2f < -threshold=%.2f, utilization=%.2f, maxQueueDepth=%d"
                            .formatted(growth, config.queueGrowthThreshold(), utilization, maxQueue),
                    now, snapshotCount, config.trendWindowSize());
        }

        // 5. UNDER_UTILIZED
        if (utilization < 0.3 && maxQueue == 0 && metrics.rejectedTaskCount() == 0) {
            double conf = utilization < 0.1 ? 0.95 : 0.80;
            return build(metrics, PressureState.UNDER_UTILIZED, conf,
                    "threadUtilization=%.2f < 0.3, queue empty, no rejections"
                            .formatted(utilization),
                    now, snapshotCount, config.trendWindowSize());
        }

        // 6. NORMAL
        double conf = 0.6 + (1.0 - Math.abs(utilization - 0.5)) * 0.4;
        return build(metrics, PressureState.NORMAL, conf,
                "Balanced: utilization=%.2f, queueGrowthRate=%.2f, maxQueueDepth=%d"
                        .formatted(utilization, growth, maxQueue),
                now, snapshotCount, config.trendWindowSize());
    }

    private PressureClassification build(
            NormalizedPressureMetrics metrics, PressureState state,
            double confidence, String evidence, Instant now,
            int snapshotCount, int trendWindowSize) {
        double factor = shortSequenceConfidenceFactor(snapshotCount, trendWindowSize);
        double adjustedConfidence = clamp(confidence * factor);
        return new PressureClassification(
                state, adjustedConfidence,
                List.of(evidence), metrics, now);
    }

    private static double shortSequenceConfidenceFactor(
            int snapshotCount, int trendWindowSize) {
        if (snapshotCount >= trendWindowSize) return 1.0;
        return (double) snapshotCount / trendWindowSize;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static int computeFallbackPoolSize(List<ObservedSnapshot> snapshots) {
        return snapshots.stream()
                .mapToInt(s -> s.snapshot().poolSize())
                .filter(ps -> ps > 0)
                .findFirst()
                .orElse(1);
    }
}
