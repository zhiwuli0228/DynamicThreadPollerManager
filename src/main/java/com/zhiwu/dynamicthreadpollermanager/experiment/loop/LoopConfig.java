package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration for an adjustment loop.
 * Cooldown is delegated to {@code RuntimeAdjustmentSafetyGate}.
 */
public record LoopConfig(
        long samplingIntervalMs,
        int maxIterations,
        int snapshotWindowSize,
        int oscillationWindowSize,
        int oscillationPatternThreshold,
        int feedbackCalibrationWindow,
        int emergencyStopThreshold,
        List<ThresholdPolicyConfig> candidatePolicies
) {
    public LoopConfig {
        if (samplingIntervalMs < 100) {
            throw new IllegalArgumentException(
                    "samplingIntervalMs must be >= 100, was " + samplingIntervalMs);
        }
        if (maxIterations < 0) {
            throw new IllegalArgumentException(
                    "maxIterations must be >= 0, was " + maxIterations);
        }
        if (snapshotWindowSize < 2) {
            throw new IllegalArgumentException(
                    "snapshotWindowSize must be >= 2, was " + snapshotWindowSize);
        }
        if (oscillationWindowSize < 4) {
            throw new IllegalArgumentException(
                    "oscillationWindowSize must be >= 4, was " + oscillationWindowSize);
        }
        if (oscillationPatternThreshold < 1) {
            throw new IllegalArgumentException(
                    "oscillationPatternThreshold must be >= 1, was " + oscillationPatternThreshold);
        }
        if (feedbackCalibrationWindow < 5) {
            throw new IllegalArgumentException(
                    "feedbackCalibrationWindow must be >= 5, was " + feedbackCalibrationWindow);
        }
        if (emergencyStopThreshold < 1) {
            throw new IllegalArgumentException(
                    "emergencyStopThreshold must be >= 1, was " + emergencyStopThreshold);
        }
        Objects.requireNonNull(candidatePolicies, "candidatePolicies must not be null");
        if (candidatePolicies.isEmpty()) {
            throw new IllegalArgumentException("candidatePolicies must not be empty");
        }
        candidatePolicies = List.copyOf(candidatePolicies);
    }

    public static LoopConfig defaults(List<ThresholdPolicyConfig> candidates) {
        return new LoopConfig(2000, 100, 20, 6, 2, 10, 2, candidates);
    }

    /** Convenience: defaults with a single candidate policy. */
    public static LoopConfig defaults(ThresholdPolicyConfig candidate) {
        return defaults(List.of(candidate));
    }
}
