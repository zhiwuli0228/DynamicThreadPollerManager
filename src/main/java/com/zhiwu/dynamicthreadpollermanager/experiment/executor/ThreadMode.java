package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

/**
 * Controls whether a {@link ManagedExecutor} uses platform threads
 * or virtual threads. PLATFORM is the default.
 */
public enum ThreadMode {
    PLATFORM,
    VIRTUAL
}
