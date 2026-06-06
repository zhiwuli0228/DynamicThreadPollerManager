package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * In-memory adjustable executor probe. The probe owns a small
 * mutable state representing the controlled executor (core/maximum
 * pool size and a fixed queue capacity) and applies scale
 * adjustments to that state. It exists so the first bounded change
 * can prove command, gate, result and evidence semantics without
 * touching a real production thread pool.
 *
 * <p>State invariants:
 * <ul>
 *   <li>{@code 1 <= corePoolSize <= maximumPoolSize}.</li>
 *   <li>Queue capacity is read-only after construction; mutation
 *       APIs are intentionally absent.</li>
 *   <li>Adjustment count is incremented on every {@code APPLIED}
 *       result and never decremented.</li>
 * </ul>
 */
public class InMemoryAdjustableExecutorProbe implements ExecutorAdjustmentAdapter {

    private final int maximumPoolSize;
    private final int queueCapacity;
    private final Supplier<Instant> clock;
    private int corePoolSize;
    private int appliedAdjustmentCount;

    public InMemoryAdjustableExecutorProbe(int initialCorePoolSize,
                                           int maximumPoolSize,
                                           int queueCapacity,
                                           Supplier<Instant> clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (initialCorePoolSize <= 0) {
            throw new IllegalArgumentException(
                    "initialCorePoolSize must be positive, was " + initialCorePoolSize);
        }
        if (maximumPoolSize < initialCorePoolSize) {
            throw new IllegalArgumentException(
                    "maximumPoolSize must be >= initialCorePoolSize, was "
                            + maximumPoolSize + " vs " + initialCorePoolSize);
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must be >= 0, was " + queueCapacity);
        }
        this.corePoolSize = initialCorePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueCapacity = queueCapacity;
        this.clock = clock;
        this.appliedAdjustmentCount = 0;
    }

    @Override
    public ExecutorStateSnapshot currentState() {
        Instant now = clock.get();
        return ExecutorStateSnapshot.builder(now)
                .corePoolSize(corePoolSize)
                .maximumPoolSize(maximumPoolSize)
                .activeCount(0)
                .queueSize(0)
                .queueCapacity(queueCapacity)
                .build();
    }

    @Override
    public AdjustmentResult apply(ScaleAdjustmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ExecutorStateSnapshot before = currentState();

        if (command.targetPoolSize() < 1) {
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.REJECTED,
                    before,
                    command.targetPoolSize(),
                    before.corePoolSize(),
                    before,
                    command.reason(),
                    AdjustmentFailureCode.INVALID_COMMAND,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }
        if (command.targetPoolSize() > maximumPoolSize) {
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.REJECTED,
                    before,
                    command.targetPoolSize(),
                    before.corePoolSize(),
                    before,
                    command.reason(),
                    AdjustmentFailureCode.INVALID_COMMAND,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }
        if (command.isNoOp() || command.targetPoolSize() == corePoolSize) {
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.NO_OP,
                    before,
                    command.targetPoolSize(),
                    before.corePoolSize(),
                    before,
                    command.reason(),
                    null,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        try {
            doSetCorePoolSize(command.targetPoolSize());
        } catch (RuntimeException ex) {
            ExecutorStateSnapshot failureSnapshot = currentState();
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.FAILED,
                    before,
                    command.targetPoolSize(),
                    before.corePoolSize(),
                    failureSnapshot,
                    command.reason(),
                    AdjustmentFailureCode.PROBE_FAILURE,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        appliedAdjustmentCount += 1;
        ExecutorStateSnapshot after = currentState();
        return new AdjustmentResult(
                command,
                AdjustmentStatus.APPLIED,
                before,
                command.targetPoolSize(),
                after.corePoolSize(),
                after,
                command.reason(),
                null,
                command.sourceDecisionRef(),
                command.decisionTimestamp());
    }

    public int appliedAdjustmentCount() {
        return appliedAdjustmentCount;
    }

    /**
     * Hook for tests to inject failures. Default implementation
     * just assigns the new value.
     */
    protected void doSetCorePoolSize(int newCorePoolSize) {
        this.corePoolSize = newCorePoolSize;
    }
}
