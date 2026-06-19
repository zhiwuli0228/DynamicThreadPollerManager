package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.LoadScenario;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.util.List;
import java.util.Objects;

public record ValidationScenario(
        String scenarioId,
        LoadScenario workload,
        ManagedExecutorConfig executorConfig,
        List<ThresholdPolicyConfig> candidatePolicies,
        ThresholdPolicyConfig bestStaticPolicy,
        long durationMs,
        int minIterations,
        long warmupPeriodMs
) {
    public ValidationScenario {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        if (scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        Objects.requireNonNull(workload, "workload must not be null");
        Objects.requireNonNull(executorConfig, "executorConfig must not be null");
        Objects.requireNonNull(candidatePolicies, "candidatePolicies must not be null");
        if (candidatePolicies.isEmpty()) {
            throw new IllegalArgumentException("candidatePolicies must not be empty");
        }
        Objects.requireNonNull(bestStaticPolicy, "bestStaticPolicy must not be null");
        if (durationMs < 30_000) {
            throw new IllegalArgumentException("durationMs must be >= 30000, was " + durationMs);
        }
        if (minIterations < 5) {
            throw new IllegalArgumentException("minIterations must be >= 5, was " + minIterations);
        }
        if (warmupPeriodMs < 1000) {
            throw new IllegalArgumentException("warmupPeriodMs must be >= 1000, was " + warmupPeriodMs);
        }
        candidatePolicies = List.copyOf(candidatePolicies);
    }
}
