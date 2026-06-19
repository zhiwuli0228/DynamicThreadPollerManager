package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

/**
 * Default {@link ControlGate} implementation that enforces explicit
 * hold, accept, and cap semantics without mutating executor state.
 *
 * <p>Gate precedence:
 * <ol>
 *   <li>Explicit {@link PolicyAction#HOLD} → {@link GateStatus#HOLD} at current size.</li>
 *   <li>Proposed size equal to current size → {@link GateStatus#HOLD} at current size.</li>
 *   <li>Proposed size above {@code maxPoolSize} → cap to max; {@link GateStatus#CAPPED}
 *       unless the cap equals current, in which case {@link GateStatus#HOLD}.</li>
 *   <li>Proposed size below {@code minPoolSize} → cap to min; {@link GateStatus#CAPPED}
 *       unless the cap equals current, in which case {@link GateStatus#HOLD}.</li>
 *   <li>Otherwise → {@link GateStatus#ACCEPTED} preserving the proposed size.</li>
 * </ol>
 */
public final class DefaultControlGate implements ControlGate {

    @Override
    public PolicyDecision apply(PolicyEvaluationInput input,
                                ThresholdPolicyConfig config,
                                PolicyAction action,
                                int proposedPoolSize,
                                String reason) {
        PressureSnapshot snapshot = input.snapshot();
        int currentPoolSize = snapshot.poolSize();

        if (action == PolicyAction.HOLD) {
            return new PolicyDecision(
                    input.runId(),
                    config.policyId(),
                    input.evaluatedAt(),
                    PolicyAction.HOLD,
                    GateStatus.HOLD,
                    currentPoolSize,
                    currentPoolSize,
                    reason
            );
        }

        if (proposedPoolSize == currentPoolSize) {
            return new PolicyDecision(
                    input.runId(),
                    config.policyId(),
                    input.evaluatedAt(),
                    action,
                    GateStatus.HOLD,
                    currentPoolSize,
                    currentPoolSize,
                    reason
            );
        }

        int boundedSize = proposedPoolSize;
        GateStatus status = GateStatus.ACCEPTED;

        if (proposedPoolSize > config.maxPoolSize()) {
            boundedSize = config.maxPoolSize();
            status = GateStatus.CAPPED;
        } else if (proposedPoolSize < config.minPoolSize()) {
            boundedSize = config.minPoolSize();
            status = GateStatus.CAPPED;
        }

        if (boundedSize == currentPoolSize) {
            return new PolicyDecision(
                    input.runId(),
                    config.policyId(),
                    input.evaluatedAt(),
                    action,
                    GateStatus.HOLD,
                    currentPoolSize,
                    currentPoolSize,
                    reason
            );
        }

        return new PolicyDecision(
                input.runId(),
                config.policyId(),
                input.evaluatedAt(),
                action,
                status,
                currentPoolSize,
                boundedSize,
                reason
        );
    }
}
