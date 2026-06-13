package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

public record RebuildResult(
        boolean success,
        ExecutorStateSnapshot beforeState,
        ExecutorStateSnapshot afterState,
        long rebuildDurationMs,
        int drainedTaskCount,
        int rejectedTaskCount,
        QueueResizeCommand.Direction direction,
        int oldQueueCapacity,
        int newQueueCapacity,
        String errorMessage) {
}
