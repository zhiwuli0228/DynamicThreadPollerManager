package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

/**
 * Stable, machine-readable failure codes produced by
 * {@link ReplayEvidenceValidator}. Each code identifies a single
 * structural problem in the candidate {@link ReplayRunInput}.
 */
public enum ReplayFailureCode {
    MISSING_RUN_ID,
    MISSING_SCENARIO_ID,
    MISSING_SCENARIO_PROFILE,
    EMPTY_SNAPSHOTS,
    INSUFFICIENT_SNAPSHOTS,
    RUN_ID_MISMATCH,
    UNORDERED_TIMESTAMP,
    MISSING_PRESSURE_FIELDS
}
