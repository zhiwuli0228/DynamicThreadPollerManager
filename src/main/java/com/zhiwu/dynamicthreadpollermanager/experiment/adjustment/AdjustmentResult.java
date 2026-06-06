package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.time.Instant;
import java.util.Objects;

/**
 * Structured result returned by an {@link ExecutorAdjustmentAdapter}
 * after attempting a single adjustment. The result is the
 * single-source-of-truth for what the adapter observed and did; it
 * is the input to {@link AdjustmentEvidence} construction.
 *
 * <p>Status / failure code consistency:
 * <ul>
 *   <li>{@link AdjustmentStatus#REJECTED} and
 *       {@link AdjustmentStatus#FAILED} MUST carry a non-null
 *       {@link AdjustmentFailureCode}.</li>
 *   <li>{@link AdjustmentStatus#APPLIED} and
 *       {@link AdjustmentStatus#NO_OP} MUST NOT carry a failure
 *       code.</li>
 * </ul>
 */
public final class AdjustmentResult {

    private final ScaleAdjustmentCommand command;
    private final AdjustmentStatus status;
    private final ExecutorStateSnapshot beforeState;
    private final int requestedPoolSize;
    private final Integer appliedPoolSize;
    private final ExecutorStateSnapshot afterState;
    private final String reason;
    private final AdjustmentFailureCode failureCode;
    private final String sourceDecisionRef;
    private final Instant decisionTimestamp;

    public AdjustmentResult(ScaleAdjustmentCommand command,
                            AdjustmentStatus status,
                            ExecutorStateSnapshot beforeState,
                            int requestedPoolSize,
                            Integer appliedPoolSize,
                            ExecutorStateSnapshot afterState,
                            String reason,
                            AdjustmentFailureCode failureCode,
                            String sourceDecisionRef,
                            Instant decisionTimestamp) {
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.beforeState = Objects.requireNonNull(beforeState, "beforeState must not be null");
        this.afterState = Objects.requireNonNull(afterState, "afterState must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (requestedPoolSize < 0) {
            throw new IllegalArgumentException("requestedPoolSize must be >= 0, was " + requestedPoolSize);
        }
        if (appliedPoolSize != null && appliedPoolSize < 0) {
            throw new IllegalArgumentException(
                    "appliedPoolSize must be >= 0 when present, was " + appliedPoolSize);
        }
        if (sourceDecisionRef == null || sourceDecisionRef.isBlank()) {
            throw new IllegalArgumentException("sourceDecisionRef must not be blank");
        }
        this.decisionTimestamp = Objects.requireNonNull(decisionTimestamp,
                "decisionTimestamp must not be null");
        if (status == AdjustmentStatus.REJECTED
                || status == AdjustmentStatus.FAILED
                || status == AdjustmentStatus.DEFERRED) {
            if (failureCode == null) {
                throw new IllegalArgumentException(
                        "status=" + status + " requires a non-null failureCode");
            }
        } else {
            if (failureCode != null) {
                throw new IllegalArgumentException(
                        "status=" + status + " must not carry a failureCode");
            }
        }
        this.requestedPoolSize = requestedPoolSize;
        this.appliedPoolSize = appliedPoolSize;
        this.reason = reason;
        this.failureCode = failureCode;
        this.sourceDecisionRef = sourceDecisionRef;
    }

    public ScaleAdjustmentCommand command() {
        return command;
    }

    public AdjustmentStatus status() {
        return status;
    }

    public ExecutorStateSnapshot beforeState() {
        return beforeState;
    }

    public int requestedPoolSize() {
        return requestedPoolSize;
    }

    public Integer appliedPoolSize() {
        return appliedPoolSize;
    }

    public ExecutorStateSnapshot afterState() {
        return afterState;
    }

    public String reason() {
        return reason;
    }

    public AdjustmentFailureCode failureCode() {
        return failureCode;
    }

    public String sourceDecisionRef() {
        return sourceDecisionRef;
    }

    public Instant decisionTimestamp() {
        return decisionTimestamp;
    }
}
