package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.ThresholdPolicyScorer;

/**
 * Stub feedback calibrator for Change 1. Always returns the same scorer.
 * Full implementation provided in Change 2.
 */
public final class FeedbackCalibrator {

    private final double maxAdjustmentPerCycle;
    private final double minWeight;
    private final double maxWeight;

    public FeedbackCalibrator(double maxAdjustmentPerCycle, double minWeight, double maxWeight) {
        if (maxAdjustmentPerCycle <= 0 || maxAdjustmentPerCycle > 0.2) {
            throw new IllegalArgumentException(
                    "maxAdjustmentPerCycle must be in (0, 0.2], was " + maxAdjustmentPerCycle);
        }
        if (minWeight < 0.05 || minWeight > 0.2) {
            throw new IllegalArgumentException(
                    "minWeight must be in [0.05, 0.20], was " + minWeight);
        }
        if (maxWeight < 0.3 || maxWeight > 0.6) {
            throw new IllegalArgumentException(
                    "maxWeight must be in [0.30, 0.60], was " + maxWeight);
        }
        this.maxAdjustmentPerCycle = maxAdjustmentPerCycle;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    public FeedbackCalibrator() {
        this(0.05, 0.10, 0.50);
    }

    /** Stub: always returns the same scorer unchanged. */
    public ThresholdPolicyScorer calibrate(
            AdjustmentHistory history,
            ThresholdPolicyScorer currentScorer,
            int windowSize) {
        return currentScorer;
    }
}
