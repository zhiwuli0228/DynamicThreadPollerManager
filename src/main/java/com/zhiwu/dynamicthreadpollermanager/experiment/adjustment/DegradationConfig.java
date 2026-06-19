package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

/**
 * Immutable configuration for degradation detection thresholds.
 * Used by {@link RollbackAwareAdjustmentAdapter} to determine
 * whether a post-adjustment state has degraded beyond acceptable
 * limits and should trigger a rollback.
 *
 * @param queueDepthThreshold       maximum acceptable increase in
 *                                  queue depth (post - pre)
 * @param throughputDropThreshold    maximum acceptable fractional
 *                                  drop in throughput (0.0–1.0)
 * @param latencyIncreaseThreshold  maximum acceptable fractional
 *                                  increase in latency (0.0–∞)
 */
public record DegradationConfig(
        int queueDepthThreshold,
        double throughputDropThreshold,
        double latencyIncreaseThreshold
) {
    public DegradationConfig {
        if (queueDepthThreshold < 0) {
            throw new IllegalArgumentException(
                    "queueDepthThreshold must be >= 0, was " + queueDepthThreshold);
        }
        if (throughputDropThreshold < 0.0 || throughputDropThreshold > 1.0) {
            throw new IllegalArgumentException(
                    "throughputDropThreshold must be in [0.0, 1.0], was " + throughputDropThreshold);
        }
        if (latencyIncreaseThreshold < 0.0) {
            throw new IllegalArgumentException(
                    "latencyIncreaseThreshold must be >= 0.0, was " + latencyIncreaseThreshold);
        }
    }

    /**
     * Default configuration with conservative thresholds:
     * queue depth increase of 50, 20% throughput drop, 50% latency increase.
     */
    public static DegradationConfig defaults() {
        return new DegradationConfig(50, 0.20, 0.50);
    }
}
