package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A named group of ManagedExecutors sharing a resource budget and coordinated
 * by a central GroupCoordinator. Construction validates that the initial total
 * allocation does not exceed the configured budget.
 */
public final class ExecutorGroup {

    private final ExecutorGroupConfig config;
    private final Map<String, ManagedExecutor> members;
    private final ResourceBudget budget;
    private final GroupCoordinator coordinator;
    private final GroupCoordinationHistory history;

    public ExecutorGroup(
            ExecutorGroupConfig config,
            Map<String, ManagedExecutor> members,
            Map<String, ExecutorAdjustmentAdapter> adapters,
            Supplier<Instant> clock) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(members, "members must not be null");
        if (members.isEmpty()) {
            throw new IllegalArgumentException("members must not be empty");
        }
        this.members = Map.copyOf(members);
        Objects.requireNonNull(adapters, "adapters must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        int totalCorePoolSize = members.values().stream()
                .mapToInt(ManagedExecutor::getCorePoolSize).sum();
        if (totalCorePoolSize > config.maxTotalThreads()) {
            throw new IllegalArgumentException(
                    "Total core pool size " + totalCorePoolSize
                    + " exceeds budget " + config.maxTotalThreads());
        }
        if (config.maxTotalQueueCapacity() > 0) {
            int totalQueue = members.values().stream()
                    .mapToInt(ManagedExecutor::getQueueCapacity).sum();
            if (totalQueue > config.maxTotalQueueCapacity()) {
                throw new IllegalArgumentException(
                        "Total queue capacity " + totalQueue
                        + " exceeds budget " + config.maxTotalQueueCapacity());
            }
        }

        this.budget = new ResourceBudget(
                config.maxTotalThreads(), config.maxTotalQueueCapacity());
        for (Map.Entry<String, ManagedExecutor> entry : members.entrySet()) {
            this.budget.reserve(entry.getKey(), entry.getValue().getCorePoolSize());
        }

        this.history = new GroupCoordinationHistory();
        this.coordinator = new GroupCoordinator(
                config, budget, history,
                new CrossExecutorOscillationDetector(8),
                Map.copyOf(adapters), clock);
    }

    public ExecutorGroupConfig getConfig() {
        return config;
    }

    public Map<String, ManagedExecutor> getMembers() {
        return members;
    }

    public ResourceBudget getBudget() {
        return budget;
    }

    public GroupCoordinator getCoordinator() {
        return coordinator;
    }

    public GroupCoordinationHistory getHistory() {
        return history;
    }

    public int size() {
        return members.size();
    }

    public boolean contains(String executorId) {
        return members.containsKey(executorId);
    }

    /** Convenience constructor for single-executor groups (primarily testing). */
    public static ExecutorGroup singleExecutor(
            String executorName,
            ManagedExecutor executor,
            ExecutorAdjustmentAdapter adapter,
            int maxTotalThreads,
            Supplier<Instant> clock) {
        ExecutorGroupConfig config = ExecutorGroupConfig.defaults(
                "single-" + executorName, maxTotalThreads);
        Map<String, ManagedExecutor> members = Map.of(executorName, executor);
        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(executorName, adapter);
        return new ExecutorGroup(config, members, adapters, clock);
    }
}
