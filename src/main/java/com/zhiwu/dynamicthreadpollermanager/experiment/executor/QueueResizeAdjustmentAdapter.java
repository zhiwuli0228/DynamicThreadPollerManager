package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate.EvaluationResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate.GateResult;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter that receives a {@link QueueResizeCommand}, evaluates it
 * through the safety gate, and executes the resize via executor
 * rebuild. Includes an idempotency guard to prevent concurrent
 * resize on the same executor.
 */
public final class QueueResizeAdjustmentAdapter {

    private final ExecutorRegistry registry;
    private final QueueResizeSafetyGate safetyGate;
    private final ExecutorRebuildStrategy rebuildStrategy;
    private final ConcurrentHashMap<String, Boolean> resizeInProgress = new ConcurrentHashMap<>();

    public QueueResizeAdjustmentAdapter(
            ExecutorRegistry registry,
            QueueResizeSafetyGate safetyGate,
            ExecutorRebuildStrategy rebuildStrategy) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.safetyGate = Objects.requireNonNull(safetyGate, "safetyGate must not be null");
        this.rebuildStrategy = Objects.requireNonNull(rebuildStrategy, "rebuildStrategy must not be null");
    }

    public boolean isResizeInProgress(String executorId) {
        return resizeInProgress.containsKey(executorId);
    }

    public QueueResizeResult apply(String executorId, QueueResizeCommand command) {
        Objects.requireNonNull(executorId, "executorId must not be null");
        Objects.requireNonNull(command, "command must not be null");

        // Idempotency guard
        Boolean alreadyInProgress = resizeInProgress.putIfAbsent(executorId, Boolean.TRUE);
        if (alreadyInProgress != null) {
            return QueueResizeResult.failed("RESIZE_IN_PROGRESS",
                    "resize already in progress for executor " + executorId);
        }

        try {
            return doApply(executorId, command);
        } finally {
            resizeInProgress.remove(executorId);
        }
    }

    private QueueResizeResult doApply(String executorId, QueueResizeCommand command) {
        Optional<ManagedExecutor> found = registry.get(executorId);
        if (found.isEmpty()) {
            return QueueResizeResult.failed("EXECUTOR_NOT_FOUND",
                    "no executor with id " + executorId);
        }

        ManagedExecutor executor = found.get();
        ExecutorStateSnapshot beforeState = executor.toSnapshot();

        EvaluationResult gateResult = safetyGate.evaluate(command, executor);
        if (gateResult.result() == GateResult.DENY) {
            ResizeEvidence evidence = new ResizeEvidence(
                    false, beforeState, null, 0, 0, 0,
                    command.direction(executor.getQueueCapacity()).name(),
                    executor.getQueueCapacity(), command.targetQueueCapacity(),
                    gateResult.reason());
            return QueueResizeResult.failed("SAFETY_GATE_DENIED",
                    gateResult.reason(), evidence);
        }

        RebuildResult rebuildResult = rebuildStrategy.rebuild(executorId, executor, command);
        ResizeEvidence evidence = ResizeEvidence.from(rebuildResult);

        if (rebuildResult.success()) {
            return QueueResizeResult.success(evidence);
        }
        return QueueResizeResult.failed("REBUILD_FAILED",
                rebuildResult.errorMessage(), evidence);
    }
}
