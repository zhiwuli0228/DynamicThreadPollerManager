package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

/**
 * Outcome of a {@link ControlGate} application for a proposed
 * pool-size change.
 */
public enum GateStatus {
    ACCEPTED,
    CAPPED,
    HOLD,
    REJECTED
}
