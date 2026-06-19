package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.RejectionPolicyCommand;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.function.Predicate;

public final class RejectionPolicySafetyGate {

    public enum GateResult { PERMIT, DENY }

    public record EvaluationResult(GateResult result, String reason) {
        public boolean permitted() {
            return result == GateResult.PERMIT;
        }
    }

    private final Predicate<String> isResizeInProgress;

    public RejectionPolicySafetyGate(Predicate<String> isResizeInProgress) {
        this.isResizeInProgress = Objects.requireNonNull(
                isResizeInProgress, "isResizeInProgress must not be null");
    }

    public EvaluationResult evaluate(RejectionPolicyCommand command,
                                      ManagedExecutor executor,
                                      String executorId) {
        if (executor.isShutdown() || executor.isTerminated()) {
            return new EvaluationResult(GateResult.DENY,
                    "executor is not in RUNNING state");
        }

        if (command.targetPolicy() == null) {
            return new EvaluationResult(GateResult.DENY,
                    "target policy must not be null");
        }

        RejectedExecutionHandler currentPolicy = executor.getRejectionPolicy();
        if (command.targetPolicy().getClass() == currentPolicy.getClass()) {
            return new EvaluationResult(GateResult.DENY,
                    "target policy is same type as current policy (no-op)");
        }

        if (isResizeInProgress.test(executorId)) {
            return new EvaluationResult(GateResult.DENY,
                    "executor is currently undergoing queue resize");
        }

        return new EvaluationResult(GateResult.PERMIT, "policy replacement permitted");
    }
}
