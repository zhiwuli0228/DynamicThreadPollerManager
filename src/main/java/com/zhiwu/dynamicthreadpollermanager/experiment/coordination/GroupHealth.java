package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopState;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated health status of all loops in an ExecutorGroup.
 * Includes loop state counts, per-executor loop states, budget snapshot,
 * and human-readable warnings.
 */
public record GroupHealth(
        String groupId,
        int totalExecutors,
        int runningLoops,
        int pausedLoops,
        int stoppedLoops,
        int emergencyStoppedLoops,
        Map<String, LoopState> loopStates,
        Map<String, Integer> budgetSnapshot,
        int budgetAvailable,
        List<String> activeWarnings) {

    public GroupHealth {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(loopStates, "loopStates must not be null");
        Objects.requireNonNull(budgetSnapshot, "budgetSnapshot must not be null");
        Objects.requireNonNull(activeWarnings, "activeWarnings must not be null");
        loopStates = Map.copyOf(loopStates);
        budgetSnapshot = Map.copyOf(budgetSnapshot);
        activeWarnings = List.copyOf(activeWarnings);
    }
}
