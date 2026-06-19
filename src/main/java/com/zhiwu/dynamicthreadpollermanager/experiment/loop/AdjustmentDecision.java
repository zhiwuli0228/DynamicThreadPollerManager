package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PolicyScore;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immutable decision produced by one loop iteration, carrying the full
 * diagnosis→decision chain. {@code selectedScore} and {@code selectedPolicy}
 * are null only for NO_OP decisions.
 */
public record AdjustmentDecision(
        PressureClassification classification,
        PolicyScore selectedScore,
        ThresholdPolicyConfig selectedPolicy,
        PolicyDecision policyDecision,
        String rationale,
        Instant decidedAt
) {
    public AdjustmentDecision {
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(policyDecision, "policyDecision must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
        if (rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        Objects.requireNonNull(decidedAt, "decidedAt must not be null");
    }

    public boolean isNoOp() {
        return policyDecision.action() == PolicyAction.HOLD;
    }

    public ScaleAdjustmentCommand toCommand(
            ManagedExecutor executor, String runId, Supplier<Instant> clock) {
        if (isNoOp()) {
            return ScaleAdjustmentCommand.noOp(
                    runId,
                    policyDecision.timestamp(),
                    policyDecision.currentPoolSize(),
                    "loop no-op: " + rationale,
                    "loop-decision:" + (selectedScore != null ? selectedScore.policyId() : "no-op"),
                    clock);
        }
        return ScaleAdjustmentCommand.create(
                runId,
                policyDecision.timestamp(),
                policyDecision.currentPoolSize(),
                policyDecision.proposedPoolSize(),
                rationale,
                "loop-decision:" + selectedScore.policyId(),
                clock);
    }
}
