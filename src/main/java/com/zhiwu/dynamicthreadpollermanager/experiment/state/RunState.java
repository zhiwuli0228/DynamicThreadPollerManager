package com.zhiwu.dynamicthreadpollermanager.experiment.state;

/**
 * Lifecycle state for an experiment run.
 */
public enum RunState {
    CREATED,
    RUNNING,
    STOPPED,
    FINALIZED
}
