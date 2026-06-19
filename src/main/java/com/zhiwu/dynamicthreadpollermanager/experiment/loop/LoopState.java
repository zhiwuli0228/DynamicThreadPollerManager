package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

/**
 * Lifecycle states for the adaptive adjustment loop.
 */
public enum LoopState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
    EMERGENCY_STOPPED
}
