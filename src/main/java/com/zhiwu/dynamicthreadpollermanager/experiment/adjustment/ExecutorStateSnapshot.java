package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.time.Instant;
import java.util.Objects;

/**
 * Snapshot of the controlled executor state observed by an
 * {@link ExecutorAdjustmentAdapter}. The snapshot records the pool
 * state required for an audit trail. Queue state is read-only and
 * MUST NOT be used as an authorization to mutate queue capacity.
 */
public final class ExecutorStateSnapshot {

    private final Instant observedAt;
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final Integer activeCount;
    private final Integer queueSize;
    private final Integer queueCapacity;

    private ExecutorStateSnapshot(Builder builder) {
        this.observedAt = builder.observedAt;
        this.corePoolSize = builder.corePoolSize;
        this.maximumPoolSize = builder.maximumPoolSize;
        this.activeCount = builder.activeCount;
        this.queueSize = builder.queueSize;
        this.queueCapacity = builder.queueCapacity;
    }

    public static Builder builder(Instant observedAt) {
        return new Builder(observedAt);
    }

    public Instant observedAt() {
        return observedAt;
    }

    public int corePoolSize() {
        return corePoolSize;
    }

    public int maximumPoolSize() {
        return maximumPoolSize;
    }

    public Integer activeCount() {
        return activeCount;
    }

    public Integer queueSize() {
        return queueSize;
    }

    public Integer queueCapacity() {
        return queueCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ExecutorStateSnapshot that
                && corePoolSize == that.corePoolSize
                && maximumPoolSize == that.maximumPoolSize
                && observedAt.equals(that.observedAt)
                && Objects.equals(activeCount, that.activeCount)
                && Objects.equals(queueSize, that.queueSize)
                && Objects.equals(queueCapacity, that.queueCapacity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(observedAt, corePoolSize, maximumPoolSize,
                activeCount, queueSize, queueCapacity);
    }

    @Override
    public String toString() {
        return "ExecutorStateSnapshot{observedAt=%s, core=%d, max=%d, active=%s, queue=%s, capacity=%s}"
                .formatted(observedAt, corePoolSize, maximumPoolSize,
                        activeCount, queueSize, queueCapacity);
    }

    public static final class Builder {
        private final Instant observedAt;
        private int corePoolSize;
        private int maximumPoolSize;
        private Integer activeCount;
        private Integer queueSize;
        private Integer queueCapacity;

        private Builder(Instant observedAt) {
            this.observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        }

        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder activeCount(int activeCount) {
            this.activeCount = activeCount;
            return this;
        }

        public Builder queueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public ExecutorStateSnapshot build() {
            if (corePoolSize <= 0) {
                throw new IllegalArgumentException("corePoolSize must be positive, was " + corePoolSize);
            }
            if (maximumPoolSize < corePoolSize) {
                throw new IllegalArgumentException(
                        "maximumPoolSize must be >= corePoolSize, was " + maximumPoolSize);
            }
            if (queueSize != null && queueSize < 0) {
                throw new IllegalArgumentException("queueSize must be >= 0, was " + queueSize);
            }
            if (queueCapacity != null && queueCapacity < 0) {
                throw new IllegalArgumentException("queueCapacity must be >= 0, was " + queueCapacity);
            }
            return new ExecutorStateSnapshot(this);
        }
    }
}
