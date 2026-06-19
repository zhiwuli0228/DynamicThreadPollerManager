package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;

/**
 * Runtime safety gate. The gate is the only place that decides
 * whether a {@link ScaleAdjustmentCommand} may be applied. The gate
 * never performs the mutation; it only returns a
 * {@link SafetyGateDecision}.
 */
public interface RuntimeAdjustmentSafetyGate {

    /**
     * Evaluate whether {@code command} may be applied given the
     * current executor state and the latest readiness assessment.
     */
    SafetyGateDecision evaluate(ScaleAdjustmentCommand command,
                                ExecutorStateSnapshot currentState,
                                ReadinessAssessment readiness);

    /**
     * Record an allowed adjustment as applied. The gate uses this
     * to update cooldown, per-run applied counter, and the
     * "last applied direction" state consumed by the
     * opposite-direction rule.
     *
     * <p>Contract:
     * <ul>
     *   <li>Callers MUST call this exactly once per
     *       {@link SafetyGateDecision} whose outcome is
     *       {@link SafetyGateDecision.Outcome#ALLOW}, and
     *       only after the adapter has actually applied the
     *       command. Skipping the call leaves cooldown at zero
     *       and the next evaluation can fire immediately, which
     *       defeats the cooldown rule.</li>
     *   <li>Calling this with a decision whose outcome is
     *       {@link SafetyGateDecision.Outcome#REJECTED} or
     *       {@link SafetyGateDecision.Outcome#NO_OP} is a no-op:
     *       the gate's per-run counter, cooldown, and last
     *       direction are not updated.</li>
     *   <li>Calling this more than once for the same
     *       {@code ALLOW} decision double-counts the adjustment
     *       and shortens the effective cooldown, so callers MUST
     *       NOT do so.</li>
     * </ul>
     */
    void recordApplied(SafetyGateDecision decision);
}
