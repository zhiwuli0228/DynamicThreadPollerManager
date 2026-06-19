package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

/** Outcome of a group coordination decision. */
public enum CoordinationOutcome {
    /** No conflict, command applied unchanged. */
    APPROVED_AS_IS,
    /** Preempted others to accommodate the request. */
    MODIFIED,
    /** Cannot allocate resources, command blocked. */
    REJECTED,
    /** Partial allocation granted — less than requested. */
    CAPPED
}
