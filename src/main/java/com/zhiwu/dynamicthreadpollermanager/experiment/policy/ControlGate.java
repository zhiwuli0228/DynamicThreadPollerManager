package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

/**
 * Applies explicit safety gates to a proposed pool-size change before
 * it leaves the policy layer. Gates are responsible for converting
 * out-of-bound proposals into capped proposals and for collapsing
 * no-op proposals into a hold.
 */
public interface ControlGate {

    PolicyDecision apply(PolicyEvaluationInput input,
                         ThresholdPolicyConfig config,
                         PolicyAction action,
                         int proposedPoolSize,
                         String reason);
}
