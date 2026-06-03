package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import java.util.Objects;

/**
 * Immutable threshold policy configuration used by
 * {@link ThresholdPolicyEvaluator}.
 *
 * <p>All values are validated at construction time so downstream
 * evaluation never has to defend against malformed configuration.
 */
public final class ThresholdPolicyConfig {

    private final String policyId;
    private final int minPoolSize;
    private final int maxPoolSize;
    private final int scaleUpActiveThreadsThreshold;
    private final int scaleUpQueueSizeThreshold;
    private final int scaleDownActiveThreadsThreshold;
    private final int scaleStep;

    public ThresholdPolicyConfig(String policyId,
                                 int minPoolSize,
                                 int maxPoolSize,
                                 int scaleUpActiveThreadsThreshold,
                                 int scaleUpQueueSizeThreshold,
                                 int scaleDownActiveThreadsThreshold,
                                 int scaleStep) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId must not be blank");
        }
        if (minPoolSize <= 0) {
            throw new IllegalArgumentException("minPoolSize must be > 0");
        }
        if (maxPoolSize < minPoolSize) {
            throw new IllegalArgumentException("maxPoolSize must be >= minPoolSize");
        }
        if (scaleUpActiveThreadsThreshold < 0) {
            throw new IllegalArgumentException("scaleUpActiveThreadsThreshold must be >= 0");
        }
        if (scaleUpQueueSizeThreshold < 0) {
            throw new IllegalArgumentException("scaleUpQueueSizeThreshold must be >= 0");
        }
        if (scaleDownActiveThreadsThreshold < 0) {
            throw new IllegalArgumentException("scaleDownActiveThreadsThreshold must be >= 0");
        }
        if (scaleStep <= 0) {
            throw new IllegalArgumentException("scaleStep must be > 0");
        }
        this.policyId = policyId;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.scaleUpActiveThreadsThreshold = scaleUpActiveThreadsThreshold;
        this.scaleUpQueueSizeThreshold = scaleUpQueueSizeThreshold;
        this.scaleDownActiveThreadsThreshold = scaleDownActiveThreadsThreshold;
        this.scaleStep = scaleStep;
    }

    public static ThresholdPolicyConfig defaultAdaptive() {
        return new ThresholdPolicyConfig(
                "default-adaptive",
                1,
                32,
                24,
                16,
                4,
                2
        );
    }

    public String policyId() {
        return policyId;
    }

    public int minPoolSize() {
        return minPoolSize;
    }

    public int maxPoolSize() {
        return maxPoolSize;
    }

    public int scaleUpActiveThreadsThreshold() {
        return scaleUpActiveThreadsThreshold;
    }

    public int scaleUpQueueSizeThreshold() {
        return scaleUpQueueSizeThreshold;
    }

    public int scaleDownActiveThreadsThreshold() {
        return scaleDownActiveThreadsThreshold;
    }

    public int scaleStep() {
        return scaleStep;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ThresholdPolicyConfig that
                && policyId.equals(that.policyId)
                && minPoolSize == that.minPoolSize
                && maxPoolSize == that.maxPoolSize
                && scaleUpActiveThreadsThreshold == that.scaleUpActiveThreadsThreshold
                && scaleUpQueueSizeThreshold == that.scaleUpQueueSizeThreshold
                && scaleDownActiveThreadsThreshold == that.scaleDownActiveThreadsThreshold
                && scaleStep == that.scaleStep;
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyId, minPoolSize, maxPoolSize,
                scaleUpActiveThreadsThreshold, scaleUpQueueSizeThreshold,
                scaleDownActiveThreadsThreshold, scaleStep);
    }

    @Override
    public String toString() {
        return "ThresholdPolicyConfig{policyId='%s', min=%d, max=%d, scaleStep=%d}"
                .formatted(policyId, minPoolSize, maxPoolSize, scaleStep);
    }
}
