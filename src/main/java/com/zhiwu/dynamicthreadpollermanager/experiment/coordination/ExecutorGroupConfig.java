package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration for an ExecutorGroup: shared budget limits,
 * default priority, per-member priority overrides, coordination timeout,
 * and fail-open behavior.
 */
public record ExecutorGroupConfig(
        String groupId,
        int maxTotalThreads,
        int maxTotalQueueCapacity,
        AdjustmentPriority defaultPriority,
        Map<String, AdjustmentPriority> memberPriorities,
        long coordinationTimeoutMs,
        boolean failOpen) {

    public ExecutorGroupConfig {
        Objects.requireNonNull(groupId, "groupId must not be null");
        if (groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must not be blank");
        }
        if (maxTotalThreads < 1) {
            throw new IllegalArgumentException(
                    "maxTotalThreads must be >= 1, was " + maxTotalThreads);
        }
        if (maxTotalQueueCapacity < 0) {
            throw new IllegalArgumentException(
                    "maxTotalQueueCapacity must be >= 0, was " + maxTotalQueueCapacity);
        }
        if (coordinationTimeoutMs < 100) {
            throw new IllegalArgumentException(
                    "coordinationTimeoutMs must be >= 100, was " + coordinationTimeoutMs);
        }
        Objects.requireNonNull(defaultPriority, "defaultPriority must not be null");
        Objects.requireNonNull(memberPriorities, "memberPriorities must not be null");
        memberPriorities = Map.copyOf(memberPriorities);
    }

    public static ExecutorGroupConfig defaults(String groupId, int maxTotalThreads) {
        return new ExecutorGroupConfig(
                groupId, maxTotalThreads, 0, AdjustmentPriority.NORMAL,
                Map.of(), 5000, false);
    }
}
