package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.ScaleDecision;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable result of evaluating a {@link PolicyEvaluationInput}
 * against a {@link ThresholdPolicyConfig} and applying a
 * {@link ControlGate}.
 *
 * <p>{@link #toScaleDecision()} produces a downstream
 * {@link ScaleDecision} only when the decision is executor-applicable
 * (accepted or capped with a non-hold action). It throws for
 * {@link GateStatus#HOLD}, {@link GateStatus#REJECTED}, and for any
 * {@link PolicyAction#HOLD} action because no scale target exists.
 */
public final class PolicyDecision {

    private final String runId;
    private final String policyId;
    private final Instant timestamp;
    private final PolicyAction action;
    private final GateStatus gateStatus;
    private final int currentPoolSize;
    private final int proposedPoolSize;
    private final String reason;

    public PolicyDecision(String runId,
                          String policyId,
                          Instant timestamp,
                          PolicyAction action,
                          GateStatus gateStatus,
                          int currentPoolSize,
                          int proposedPoolSize,
                          String reason) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId must not be blank");
        }
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.gateStatus = Objects.requireNonNull(gateStatus, "gateStatus must not be null");
        if (currentPoolSize < 0) {
            throw new IllegalArgumentException("currentPoolSize must be >= 0");
        }
        if (proposedPoolSize < 0) {
            throw new IllegalArgumentException("proposedPoolSize must be >= 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        this.runId = runId;
        this.policyId = policyId;
        this.currentPoolSize = currentPoolSize;
        this.proposedPoolSize = proposedPoolSize;
        this.reason = reason;
    }

    public String runId() {
        return runId;
    }

    public String policyId() {
        return policyId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public PolicyAction action() {
        return action;
    }

    public GateStatus gateStatus() {
        return gateStatus;
    }

    public int currentPoolSize() {
        return currentPoolSize;
    }

    public int proposedPoolSize() {
        return proposedPoolSize;
    }

    public String reason() {
        return reason;
    }

    /**
     * Convert this decision to a {@link ScaleDecision} only when it is
     * executor-applicable: the gate accepted or capped the proposal,
     * and the action is not {@link PolicyAction#HOLD}.
     *
     * @throws IllegalStateException when conversion is not allowed.
     */
    public ScaleDecision toScaleDecision() {
        if (gateStatus != GateStatus.ACCEPTED && gateStatus != GateStatus.CAPPED) {
            throw new IllegalStateException(
                    "Cannot convert decision with gateStatus=" + gateStatus + " to ScaleDecision");
        }
        if (action == PolicyAction.HOLD) {
            throw new IllegalStateException(
                    "Cannot convert decision with action=HOLD to ScaleDecision");
        }
        return new ScaleDecision(timestamp, runId, currentPoolSize, proposedPoolSize, reason);
    }
}
