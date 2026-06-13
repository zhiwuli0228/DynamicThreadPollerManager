package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.QueueResizeCommand;

/**
 * Safety gate for queue capacity resize operations. Evaluates whether
 * a resize command can be safely applied to a managed executor.
 */
public final class QueueResizeSafetyGate {

    public enum GateResult { PERMIT, DENY }

    public record EvaluationResult(GateResult result, String reason) {
        public boolean permitted() {
            return result == GateResult.PERMIT;
        }
    }

    public EvaluationResult evaluate(QueueResizeCommand command, ManagedExecutor executor) {
        if (executor.isShutdown() || executor.isTerminated()) {
            return new EvaluationResult(GateResult.DENY,
                    "executor is not in RUNNING state");
        }

        int currentQueueCapacity = executor.getQueueCapacity();
        if (command.targetQueueCapacity() == currentQueueCapacity) {
            return new EvaluationResult(GateResult.DENY,
                    "target queue capacity equals current capacity");
        }

        if (command.targetQueueCapacity() < currentQueueCapacity) {
            int currentQueueDepth = executor.getQueueSize();
            if (currentQueueDepth > command.targetQueueCapacity()) {
                return new EvaluationResult(GateResult.DENY,
                        "current queue depth (" + currentQueueDepth
                                + ") exceeds target capacity ("
                                + command.targetQueueCapacity() + ")");
            }
        }

        return new EvaluationResult(GateResult.PERMIT, "resize permitted");
    }
}
