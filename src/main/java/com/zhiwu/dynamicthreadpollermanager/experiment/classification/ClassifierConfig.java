package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

/**
 * Immutable classifier configuration with capacity-aware thresholds.
 */
public record ClassifierConfig(
        int trendWindowSize,
        double queueGrowthThreshold,
        int rejectionWindowSize,
        int queueCapacity
) {
    public ClassifierConfig {
        if (trendWindowSize < 2) {
            throw new IllegalArgumentException(
                    "trendWindowSize must be >= 2, was " + trendWindowSize);
        }
        if (queueGrowthThreshold <= 0) {
            throw new IllegalArgumentException(
                    "queueGrowthThreshold must be > 0, was " + queueGrowthThreshold);
        }
        if (rejectionWindowSize < 1) {
            throw new IllegalArgumentException(
                    "rejectionWindowSize must be >= 1, was " + rejectionWindowSize);
        }
        if (queueCapacity < 0 && queueCapacity != Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "queueCapacity must be >= 0 or Integer.MAX_VALUE, was " + queueCapacity);
        }
    }

    public static ClassifierConfig defaults() {
        return new ClassifierConfig(5, 0.1, 10, Integer.MAX_VALUE);
    }
}
