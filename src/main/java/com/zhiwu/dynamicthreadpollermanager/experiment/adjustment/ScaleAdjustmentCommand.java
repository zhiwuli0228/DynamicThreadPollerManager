package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Deterministic scale adjustment command. The command is the only
 * mutation input consumed by {@link ExecutorAdjustmentAdapter}. A
 * command may be derived from a {@code ScaleDecision} or any
 * equivalent source, but the {@code sourceDecisionRef} is preserved
 * so the resulting evidence remains traceable.
 *
 * <p>The default {@code commandId} format is
 * {@code <runId>:<decisionTimestamp>:<currentPoolSize>-><targetPoolSize>}.
 * Callers that need a different id may pass a deterministic string
 * via the constructor, but in the first change all commands are
 * produced through {@link #create} so the id strategy is uniform.
 */
public final class ScaleAdjustmentCommand {

    private final String commandId;
    private final String runId;
    private final Instant decisionTimestamp;
    private final int currentPoolSize;
    private final int targetPoolSize;
    private final String reason;
    private final String sourceDecisionRef;
    private final Instant createdAt;

    /**
     * Package-private constructor. The {@link #create} factory is
     * the public path. This raw constructor is exposed within the
     * package so that adapters and tests can build commands with
     * intentionally invalid sizes to exercise their own validation
     * layer.
     */
    ScaleAdjustmentCommand(String commandId,
                           String runId,
                           Instant decisionTimestamp,
                           int currentPoolSize,
                           int targetPoolSize,
                           String reason,
                           String sourceDecisionRef,
                           Instant createdAt) {
        this.commandId = commandId;
        this.runId = runId;
        this.decisionTimestamp = decisionTimestamp;
        this.currentPoolSize = currentPoolSize;
        this.targetPoolSize = targetPoolSize;
        this.reason = reason;
        this.sourceDecisionRef = sourceDecisionRef;
        this.createdAt = createdAt;
    }

    /**
     * Build an executable command with a deterministic
     * {@code commandId}. The current and target pool sizes must
     * differ; no-op targets are rejected and must be expressed via
     * {@link #noOp}.
     */
    public static ScaleAdjustmentCommand create(String runId,
                                                Instant decisionTimestamp,
                                                int currentPoolSize,
                                                int targetPoolSize,
                                                String reason,
                                                String sourceDecisionRef,
                                                Supplier<Instant> clock) {
        Objects.requireNonNull(runId, "runId must not be null");
        if (runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Objects.requireNonNull(decisionTimestamp, "decisionTimestamp must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireNonBlank(reason, "reason");
        requireNonBlank(sourceDecisionRef, "sourceDecisionRef");
        if (currentPoolSize < 0) {
            throw new IllegalArgumentException("currentPoolSize must be >= 0, was " + currentPoolSize);
        }
        if (targetPoolSize < 0) {
            throw new IllegalArgumentException("targetPoolSize must be >= 0, was " + targetPoolSize);
        }
        if (currentPoolSize == targetPoolSize) {
            throw new IllegalArgumentException(
                    "currentPoolSize equals targetPoolSize; use noOp() for no-op commands");
        }
        String id = "%s:%s:%d->%d".formatted(runId, decisionTimestamp, currentPoolSize, targetPoolSize);
        return new ScaleAdjustmentCommand(id, runId, decisionTimestamp,
                currentPoolSize, targetPoolSize, reason, sourceDecisionRef, clock.get());
    }

    /**
     * Build a no-op command for the case where the source decision
     * target matches the current pool size. The command id is still
     * deterministic and follows the same naming convention.
     */
    public static ScaleAdjustmentCommand noOp(String runId,
                                              Instant decisionTimestamp,
                                              int currentPoolSize,
                                              String reason,
                                              String sourceDecisionRef,
                                              Supplier<Instant> clock) {
        return noOp(runId, decisionTimestamp, currentPoolSize, reason, sourceDecisionRef, clock, currentPoolSize);
    }

    /**
     * Internal no-op factory that also lets tests pin the target
     * pool size independently of the current size. The current and
     * target must be equal.
     */
    static ScaleAdjustmentCommand noOp(String runId,
                                       Instant decisionTimestamp,
                                       int currentPoolSize,
                                       String reason,
                                       String sourceDecisionRef,
                                       Supplier<Instant> clock,
                                       int targetPoolSize) {
        Objects.requireNonNull(runId, "runId must not be null");
        if (runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Objects.requireNonNull(decisionTimestamp, "decisionTimestamp must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireNonBlank(reason, "reason");
        requireNonBlank(sourceDecisionRef, "sourceDecisionRef");
        if (currentPoolSize != targetPoolSize) {
            throw new IllegalArgumentException(
                    "noOp requires currentPoolSize == targetPoolSize");
        }
        String id = "%s:%s:%d->%d".formatted(runId, decisionTimestamp, currentPoolSize, targetPoolSize);
        return new ScaleAdjustmentCommand(id, runId, decisionTimestamp,
                currentPoolSize, targetPoolSize, reason, sourceDecisionRef, clock.get());
    }

    public String commandId() {
        return commandId;
    }

    public String runId() {
        return runId;
    }

    public Instant decisionTimestamp() {
        return decisionTimestamp;
    }

    public int currentPoolSize() {
        return currentPoolSize;
    }

    public int targetPoolSize() {
        return targetPoolSize;
    }

    public String reason() {
        return reason;
    }

    public String sourceDecisionRef() {
        return sourceDecisionRef;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isNoOp() {
        return currentPoolSize == targetPoolSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ScaleAdjustmentCommand that
                && commandId.equals(that.commandId);
    }

    @Override
    public int hashCode() {
        return commandId.hashCode();
    }

    @Override
    public String toString() {
        return "ScaleAdjustmentCommand{commandId='%s', runId='%s', current=%d, target=%d}"
                .formatted(commandId, runId, currentPoolSize, targetPoolSize);
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
