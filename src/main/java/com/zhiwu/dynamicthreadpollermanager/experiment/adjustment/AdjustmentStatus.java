package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

/**
 * Status of a single adjustment attempt. The five values cover the
 * full adapter contract and are also used as the
 * {@code AdjustmentEvidence.status()} field.
 */
public enum AdjustmentStatus {
    /** Target applied successfully. */
    APPLIED,
    /** Safety gate or input validation rejected the command. */
    REJECTED,
    /** Target matches the current state; no mutation occurred. */
    NO_OP,
    /** Allowed by safety gate but the adapter failed during execution. */
    FAILED,
    /** Adapter cannot perform the requested adjustment type. */
    DEFERRED
}
