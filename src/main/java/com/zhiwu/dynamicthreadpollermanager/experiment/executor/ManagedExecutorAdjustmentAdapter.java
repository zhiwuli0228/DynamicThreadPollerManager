package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentFailureCode;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.RuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.SafetyGateDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;

import java.util.Objects;

public class ManagedExecutorAdjustmentAdapter implements ExecutorAdjustmentAdapter {

    private final ExecutorRegistry registry;
    private final RuntimeAdjustmentSafetyGate safetyGate;
    private final String executorName;
    private final ReadinessAssessment readiness;

    public ManagedExecutorAdjustmentAdapter(
            ExecutorRegistry registry,
            RuntimeAdjustmentSafetyGate safetyGate,
            String executorName,
            ReadinessAssessment readiness) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.safetyGate = Objects.requireNonNull(safetyGate, "safetyGate must not be null");
        this.executorName = Objects.requireNonNull(executorName, "executorName must not be null");
        if (executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }
        this.readiness = Objects.requireNonNull(readiness, "readiness must not be null");
    }

    @Override
    public ExecutorStateSnapshot currentState() {
        ManagedExecutor executor = registry.get(executorName)
                .orElseThrow(() -> new IllegalStateException(
                        "executor not found in registry: " + executorName));
        return executor.toSnapshot();
    }

    @Override
    public AdjustmentResult apply(ScaleAdjustmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        ManagedExecutor executor = registry.get(executorName).orElse(null);
        if (executor == null) {
            ExecutorStateSnapshot emptySnapshot = ExecutorStateSnapshot.builder(command.decisionTimestamp())
                    .corePoolSize(1)
                    .maximumPoolSize(1)
                    .build();
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.FAILED,
                    emptySnapshot,
                    command.targetPoolSize(),
                    null,
                    emptySnapshot,
                    command.reason(),
                    AdjustmentFailureCode.EXECUTOR_NOT_FOUND,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        ExecutorStateSnapshot beforeState = executor.toSnapshot();

        SafetyGateDecision decision = safetyGate.evaluate(command, beforeState, readiness);

        if (decision.outcome() == SafetyGateDecision.Outcome.REJECTED) {
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.REJECTED,
                    beforeState,
                    command.targetPoolSize(),
                    beforeState.corePoolSize(),
                    beforeState,
                    decision.reason() != null ? decision.reason() : command.reason(),
                    decision.failureCode(),
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        if (decision.outcome() == SafetyGateDecision.Outcome.NO_OP) {
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.NO_OP,
                    beforeState,
                    command.targetPoolSize(),
                    beforeState.corePoolSize(),
                    beforeState,
                    decision.reason() != null ? decision.reason() : command.reason(),
                    null,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        try {
            int target = command.targetPoolSize();
            if (target > executor.getMaximumPoolSize()) {
                executor.setMaximumPoolSize(target);
            }
            executor.setCorePoolSize(target);
        } catch (RuntimeException ex) {
            ExecutorStateSnapshot failureSnapshot = executor.toSnapshot();
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.FAILED,
                    beforeState,
                    command.targetPoolSize(),
                    beforeState.corePoolSize(),
                    failureSnapshot,
                    command.reason(),
                    AdjustmentFailureCode.INVALID_COMMAND,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        safetyGate.recordApplied(decision);

        ExecutorStateSnapshot afterState = executor.toSnapshot();
        return new AdjustmentResult(
                command,
                AdjustmentStatus.APPLIED,
                beforeState,
                command.targetPoolSize(),
                afterState.corePoolSize(),
                afterState,
                command.reason(),
                null,
                command.sourceDecisionRef(),
                command.decisionTimestamp());
    }
}
