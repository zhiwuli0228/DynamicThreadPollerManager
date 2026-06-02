package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.Objects;

/**
 * Immutable fixed executor sizing used by the baseline workload
 * executor. The preset is intentionally static — the runner never
 * resizes based on observations.
 */
public final class BaselineExecutorPreset {

    private final String policyId;
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final int queueCapacity;

    public BaselineExecutorPreset(String policyId,
                                  int corePoolSize,
                                  int maximumPoolSize,
                                  int queueCapacity) {
        this.policyId = requireNonBlank(policyId, "policyId");
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize must be positive, was " + corePoolSize);
        }
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException(
                    "maximumPoolSize must be >= corePoolSize, was " + maximumPoolSize);
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must be non-negative, was " + queueCapacity);
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueCapacity = queueCapacity;
    }

    public static BaselineExecutorPreset fixedSmall() {
        return new BaselineExecutorPreset("baseline-fixed", 2, 2, 10);
    }

    public String policyId() {
        return policyId;
    }

    public int corePoolSize() {
        return corePoolSize;
    }

    public int maximumPoolSize() {
        return maximumPoolSize;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof BaselineExecutorPreset that
                && corePoolSize == that.corePoolSize
                && maximumPoolSize == that.maximumPoolSize
                && queueCapacity == that.queueCapacity
                && policyId.equals(that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyId, corePoolSize, maximumPoolSize, queueCapacity);
    }

    @Override
    public String toString() {
        return "BaselineExecutorPreset{policyId='%s', corePoolSize=%d, maximumPoolSize=%d, queueCapacity=%d}"
                .formatted(policyId, corePoolSize, maximumPoolSize, queueCapacity);
    }
}
