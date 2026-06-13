package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import java.time.Instant;

public record PolicyReplacementEvidence(
        String beforePolicyClass,
        String afterPolicyClass,
        ExecutorStateSnapshot executorState,
        Instant replacedAt,
        boolean success,
        String reason) {
}
