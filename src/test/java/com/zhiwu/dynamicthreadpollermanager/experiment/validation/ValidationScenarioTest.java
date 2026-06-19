package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.LoadScenario;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationScenarioTest {

    private static final LoadScenario workload = new LoadScenario("load-1", "test");
    private static final ManagedExecutorConfig execConfig = ManagedExecutorConfig.defaultConfig();
    private static final ThresholdPolicyConfig policy =
            new ThresholdPolicyConfig("t1", 2, 8, 4, 10, 2, 1);

    @Test
    void shouldConstructWithValidFields() {
        ValidationScenario scenario = new ValidationScenario(
                "s1", workload, execConfig, List.of(policy), policy,
                30_000, 5, 1000);

        assertEquals("s1", scenario.scenarioId());
        assertEquals(30_000, scenario.durationMs());
        assertEquals(5, scenario.minIterations());
        assertEquals(1000, scenario.warmupPeriodMs());
        assertEquals(List.of(policy), scenario.candidatePolicies());
    }

    @Test
    void shouldRejectBlankScenarioId() {
        assertThrows(IllegalArgumentException.class, () ->
                new ValidationScenario("  ", workload, execConfig, List.of(policy),
                        policy, 30_000, 5, 1000));
    }

    @Test
    void shouldRejectDurationBelow30s() {
        assertThrows(IllegalArgumentException.class, () ->
                new ValidationScenario("s1", workload, execConfig, List.of(policy),
                        policy, 29_999, 5, 1000));
    }

    @Test
    void shouldRejectMinIterationsBelow5() {
        assertThrows(IllegalArgumentException.class, () ->
                new ValidationScenario("s1", workload, execConfig, List.of(policy),
                        policy, 30_000, 4, 1000));
    }

    @Test
    void shouldRejectWarmupBelow1000() {
        assertThrows(IllegalArgumentException.class, () ->
                new ValidationScenario("s1", workload, execConfig, List.of(policy),
                        policy, 30_000, 5, 999));
    }

    @Test
    void shouldRejectEmptyCandidatePolicies() {
        assertThrows(IllegalArgumentException.class, () ->
                new ValidationScenario("s1", workload, execConfig, List.of(),
                        policy, 30_000, 5, 1000));
    }

    @Test
    void shouldDefensiveCopyCandidatePolicies() {
        List<ThresholdPolicyConfig> mutable = new java.util.ArrayList<>(List.of(policy));
        ValidationScenario scenario = new ValidationScenario(
                "s1", workload, execConfig, mutable, policy, 30_000, 5, 1000);
        mutable.add(new ThresholdPolicyConfig("t2", 1, 2, 3, 4, 1, 1));
        assertEquals(1, scenario.candidatePolicies().size());
    }
}
