package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.time.Instant;
import java.util.Objects;

/**
 * Runtime adjustment evidence. Distinct from offline replay
 * evidence: {@link #evidenceType()} is always
 * {@code "runtime_adjustment"} and the record carries the command
 * that drove the adjustment plus the before/requested/applied/after
 * state.
 */
public final class AdjustmentEvidence {

    public static final String EVIDENCE_TYPE = "runtime_adjustment";

    private final ScaleAdjustmentCommand command;
    private final ExecutorStateSnapshot beforeState;
    private final int requestedPoolSize;
    private final Integer appliedPoolSize;
    private final ExecutorStateSnapshot afterState;
    private final AdjustmentStatus status;
    private final String reason;
    private final AdjustmentFailureCode failureCode;
    private final String sourceDecisionRef;
    private final Instant decisionTimestamp;
    private final Instant recordedTimestamp;

    public AdjustmentEvidence(ScaleAdjustmentCommand command,
                              ExecutorStateSnapshot beforeState,
                              int requestedPoolSize,
                              Integer appliedPoolSize,
                              ExecutorStateSnapshot afterState,
                              AdjustmentStatus status,
                              String reason,
                              AdjustmentFailureCode failureCode,
                              String sourceDecisionRef,
                              Instant decisionTimestamp,
                              Instant recordedTimestamp) {
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.beforeState = Objects.requireNonNull(beforeState, "beforeState must not be null");
        this.afterState = Objects.requireNonNull(afterState, "afterState must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (requestedPoolSize < 0) {
            throw new IllegalArgumentException(
                    "requestedPoolSize must be >= 0, was " + requestedPoolSize);
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
        this.recordedTimestamp = Objects.requireNonNull(recordedTimestamp,
                "recordedTimestamp must not be null");
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

    public String evidenceType() {
        return EVIDENCE_TYPE;
    }

    public String commandId() {
        return command.commandId();
    }

    public String runId() {
        return command.runId();
    }

    public ScaleAdjustmentCommand command() {
        return command;
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

    public AdjustmentStatus status() {
        return status;
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

    public Instant recordedTimestamp() {
        return recordedTimestamp;
    }
}
