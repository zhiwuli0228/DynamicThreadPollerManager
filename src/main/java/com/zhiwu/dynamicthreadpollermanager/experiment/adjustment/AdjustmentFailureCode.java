package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

/**
 * Failure or rejection code attached to a rejected / failed
 * adjustment. The code space is intentionally small and stable so
 * downstream audit logs can aggregate by code without parsing the
 * free-form reason.
 */
public enum AdjustmentFailureCode {
    /** Readiness assessment is NOT_READY. */
    NOT_READY,
    /** READY_WITH_RISK was provided but the caller did not accept the risk profile. */
    RISK_NOT_ACCEPTED,
    /** Cooldown window since the last applied adjustment has not elapsed. */
    COOLDOWN_ACTIVE,
    /** New command direction immediately reverses the last applied adjustment. */
    OPPOSITE_DIRECTION,
    /** Per-run adjustment cap has been reached. */
    RUN_LIMIT_EXCEEDED,
    /** Command failed input validation before evaluation. */
    INVALID_COMMAND,
    /** Adapter probe reported a runtime failure during the attempted adjustment. */
    PROBE_FAILURE,
    /** Adapter does not support the requested adjustment type. */
    UNSUPPORTED,
    /** Target executor not found in the registry. */
    EXECUTOR_NOT_FOUND
}
