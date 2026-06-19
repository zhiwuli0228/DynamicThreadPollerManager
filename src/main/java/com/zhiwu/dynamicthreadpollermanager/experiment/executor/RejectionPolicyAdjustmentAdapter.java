package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.RejectionPolicySafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.RejectionPolicySafetyGate.EvaluationResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.RejectionPolicySafetyGate.GateResult;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionHandler;

public final class RejectionPolicyAdjustmentAdapter {

    private final ExecutorRegistry registry;
    private final RejectionPolicySafetyGate safetyGate;

    public RejectionPolicyAdjustmentAdapter(
            ExecutorRegistry registry,
            RejectionPolicySafetyGate safetyGate) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.safetyGate = Objects.requireNonNull(safetyGate, "safetyGate must not be null");
    }

    public PolicyReplacementResult apply(String executorId,
                                          RejectionPolicyCommand command) {
        Objects.requireNonNull(executorId, "executorId must not be null");
        Objects.requireNonNull(command, "command must not be null");

        Optional<ManagedExecutor> found = registry.get(executorId);
        if (found.isEmpty()) {
            return PolicyReplacementResult.failed(
                    "EXECUTOR_NOT_FOUND",
                    "no executor with id " + executorId);
        }

        ManagedExecutor executor = found.get();
        RejectedExecutionHandler beforePolicy = executor.getRejectionPolicy();
        Instant startTime = Instant.now();

        EvaluationResult gateResult = safetyGate.evaluate(command, executor, executorId);

        if (gateResult.result() == GateResult.DENY) {
            PolicyReplacementEvidence evidence = new PolicyReplacementEvidence(
                    beforePolicy.getClass().getCanonicalName(),
                    beforePolicy.getClass().getCanonicalName(),
                    executor.toSnapshot(),
                    startTime,
                    false,
                    gateResult.reason());
            return PolicyReplacementResult.denied(
                    "SAFETY_GATE_DENIED",
                    gateResult.reason(),
                    evidence);
        }

        try {
            executor.setRejectionPolicy(command.targetPolicy());
        } catch (RuntimeException ex) {
            PolicyReplacementEvidence evidence = new PolicyReplacementEvidence(
                    beforePolicy.getClass().getCanonicalName(),
                    beforePolicy.getClass().getCanonicalName(),
                    executor.toSnapshot(),
                    startTime,
                    false,
                    "setRejectionPolicy failed: " + ex.getMessage());
            return PolicyReplacementResult.failed(
                    "POLICY_SET_FAILED",
                    ex.getMessage(),
                    evidence);
        }

        PolicyReplacementEvidence evidence = new PolicyReplacementEvidence(
                beforePolicy.getClass().getCanonicalName(),
                command.targetPolicy().getClass().getCanonicalName(),
                executor.toSnapshot(),
                startTime,
                true,
                command.reason());

        return PolicyReplacementResult.success(evidence);
    }
}
