package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

/**
 * Evaluates a {@link PolicyEvaluationInput} against a
 * {@link ThresholdPolicyConfig} and returns a deterministic
 * {@link PolicyDecision}.
 *
 * <p>Implementations MUST derive any timestamp used in the result
 * from the input, MUST NOT call wall-clock APIs, and MUST NOT mutate
 * executors or queues.
 */
public interface PolicyEvaluator {

    PolicyDecision evaluate(PolicyEvaluationInput input, ThresholdPolicyConfig config);
}
