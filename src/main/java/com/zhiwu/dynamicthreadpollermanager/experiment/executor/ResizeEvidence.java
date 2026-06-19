package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

public record ResizeEvidence(
        boolean success,
        ExecutorStateSnapshot beforeState,
        ExecutorStateSnapshot afterState,
        long rebuildDurationMs,
        int drainedTaskCount,
        int rejectedTaskCount,
        String direction,
        int oldQueueCapacity,
        int newQueueCapacity,
        String errorMessage) {

    public static ResizeEvidence from(RebuildResult result) {
        return new ResizeEvidence(
                result.success(),
                result.beforeState(),
                result.afterState(),
                result.rebuildDurationMs(),
                result.drainedTaskCount(),
                result.rejectedTaskCount(),
                result.direction().name(),
                result.oldQueueCapacity(),
                result.newQueueCapacity(),
                result.errorMessage());
    }
}
