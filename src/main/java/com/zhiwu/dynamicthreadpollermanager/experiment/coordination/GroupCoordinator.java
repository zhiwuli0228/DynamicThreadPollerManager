package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Centralized coordination interceptor. Evaluates scale-up commands
 * against the shared resource budget, resolves conflicts by priority,
 * and applies preemption directly through adapters when necessary.
 *
 * <p>Thread-safe: {@link #coordinate(ScaleAdjustmentCommand, String)}
 * is synchronized on an internal lock to serialize all coordination
 * decisions across the group.
 */
public final class GroupCoordinator {

    private final ExecutorGroupConfig config;
    private final ResourceBudget budget;
    private final GroupCoordinationHistory history;
    private final CrossExecutorOscillationDetector crossDetector;
    private final Map<String, ExecutorAdjustmentAdapter> adapters;
    private final Supplier<Instant> clock;
    private final Object lock = new Object();

    public GroupCoordinator(
            ExecutorGroupConfig config,
            ResourceBudget budget,
            GroupCoordinationHistory history,
            CrossExecutorOscillationDetector crossDetector,
            Map<String, ExecutorAdjustmentAdapter> adapters,
            Supplier<Instant> clock) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.budget = Objects.requireNonNull(budget, "budget must not be null");
        this.history = Objects.requireNonNull(history, "history must not be null");
        this.crossDetector = Objects.requireNonNull(crossDetector, "crossDetector must not be null");
        this.adapters = Map.copyOf(Objects.requireNonNull(adapters, "adapters must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public GroupCoordinationResult coordinate(
            ScaleAdjustmentCommand command, String executorName) {

        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(executorName, "executorName must not be null");

        AdjustmentPriority priority = config.memberPriorities()
                .getOrDefault(executorName, config.defaultPriority());

        synchronized (lock) {
            if (command.isNoOp()) {
                return recordAndReturn(executorName, command, command,
                        CoordinationOutcome.APPROVED_AS_IS,
                        "no-op command, no resource impact",
                        List.of(), false);
            }

            int currentPoolSize = command.currentPoolSize();
            int targetPoolSize = command.targetPoolSize();
            int delta = targetPoolSize - currentPoolSize;

            if (delta <= 0) {
                if (delta < 0) {
                    budget.release(executorName, -delta);
                }
                boolean crossOsc = crossDetector.wouldCrossOscillate(
                        command, executorName, history);
                return recordAndReturn(executorName, command, command,
                        CoordinationOutcome.APPROVED_AS_IS,
                        "scale-down: released " + (-delta) + " threads",
                        List.of(), crossOsc);
            }

            int available = budget.availableThreads();
            if (available >= delta) {
                budget.reserve(executorName, delta);
                boolean crossOsc = crossDetector.wouldCrossOscillate(
                        command, executorName, history);
                return recordAndReturn(executorName, command, command,
                        CoordinationOutcome.APPROVED_AS_IS,
                        "scale-up: reserved " + delta + " threads",
                        List.of(), crossOsc);
            }

            int shortfall = delta - available;
            List<String> preempted = new ArrayList<>();
            int collected = 0;

            List<String> candidates = budget.getThreadAllocations().keySet().stream()
                    .filter(id -> !id.equals(executorName))
                    .filter(id -> priority.canPreempt(
                            config.memberPriorities().getOrDefault(id, config.defaultPriority())))
                    .sorted((a, b) -> {
                        AdjustmentPriority pa = config.memberPriorities()
                                .getOrDefault(a, config.defaultPriority());
                        AdjustmentPriority pb = config.memberPriorities()
                                .getOrDefault(b, config.defaultPriority());
                        return Integer.compare(pa.getLevel(), pb.getLevel());
                    })
                    .collect(Collectors.toList());

            for (String candidateId : candidates) {
                if (collected >= shortfall) break;
                int allocated = budget.allocatedThreads(candidateId);
                int preemptible = Math.max(0, allocated - 1);
                int take = Math.min(preemptible, shortfall - collected);
                if (take > 0) {
                    applyPreemption(candidateId, allocated - take,
                            "preempted by " + executorName
                            + " (priority: " + priority + ")");
                    budget.release(candidateId, take);
                    collected += take;
                    preempted.add(candidateId + ":" + take);
                }
            }

            int totalAvailable = available + collected;

            if (collected >= shortfall) {
                budget.reserve(executorName, delta);
                boolean crossOsc = crossDetector.wouldCrossOscillate(
                        command, executorName, history);
                return recordAndReturn(executorName, command, command,
                        CoordinationOutcome.MODIFIED,
                        "preempted " + preempted.size() + " executors: " + preempted,
                        preempted, crossOsc);
            }

            if (collected > 0) {
                int cappedTarget = currentPoolSize + collected;
                budget.reserve(executorName, collected);
                ScaleAdjustmentCommand cappedCommand = ScaleAdjustmentCommand.create(
                        command.runId(),
                        command.decisionTimestamp(),
                        currentPoolSize,
                        cappedTarget,
                        "capped from " + targetPoolSize + " to " + cappedTarget
                                + " (original: " + command.reason() + ")",
                        command.sourceDecisionRef(),
                        clock);
                boolean crossOsc = crossDetector.wouldCrossOscillate(
                        cappedCommand, executorName, history);
                return recordAndReturn(executorName, command, cappedCommand,
                        CoordinationOutcome.CAPPED,
                        "capped: requested " + delta + ", granted " + collected
                                + ", preempted: " + preempted,
                        preempted, crossOsc);
            }

            boolean crossOsc = crossDetector.wouldCrossOscillate(
                    command, executorName, history);
            return recordAndReturn(executorName, command, command,
                    CoordinationOutcome.REJECTED,
                    "budget exhausted: requested " + delta
                            + ", available " + available
                            + ", no preemptible lower-priority executors",
                    List.of(), crossOsc);
        }
    }

    private void applyPreemption(
            String executorId, int newTargetPoolSize, String reason) {
        ExecutorAdjustmentAdapter adapter = adapters.get(executorId);
        if (adapter == null) return;
        ScaleAdjustmentCommand preemptCmd = ScaleAdjustmentCommand.create(
                "preempt-" + executorId,
                clock.get(),
                adapter.currentState().corePoolSize(),
                newTargetPoolSize,
                reason,
                "group-coordinator:preempt:" + executorId,
                clock);
        adapter.apply(preemptCmd);
    }

    private GroupCoordinationResult recordAndReturn(
            String executorName,
            ScaleAdjustmentCommand command,
            ScaleAdjustmentCommand approvedCommand,
            CoordinationOutcome outcome,
            String rationale,
            List<String> conflicts,
            boolean crossOscillationDetected) {

        Instant now = clock.get();
        ResourceBudget before = budget.snapshot();
        GroupCoordinationResult result = new GroupCoordinationResult(
                command, approvedCommand, outcome, rationale,
                conflicts, crossOscillationDetected, now);
        history.record(new GroupCoordinationEntry(
                executorName, command, result, before, budget.snapshot(), now));
        return result;
    }

    public ResourceBudget getBudget() {
        return budget;
    }

    public GroupCoordinationHistory getHistory() {
        return history;
    }
}
