package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

/**
 * Discrete policy actions that the adaptive policy layer may propose
 * after evaluating a {@link PolicyEvaluationInput}.
 *
 * <p>This enum is intentionally narrow: it describes what the policy
 * wants, not how an executor should mutate itself. Mutation is owned
 * by a separate downstream component and is out of scope for this
 * package.
 */
public enum PolicyAction {
    SCALE_UP,
    SCALE_DOWN,
    HOLD
}
