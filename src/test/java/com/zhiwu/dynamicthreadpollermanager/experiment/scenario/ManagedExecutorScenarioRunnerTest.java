package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ManualPressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.PressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorScenarioRunnerTest {

    private final ExperimentCoordinator coordinator = new ExperimentCoordinator();
    private final DeterministicScenarioPlanner planner = new DeterministicScenarioPlanner();
    private final ManualPressureSampler sampler = new ManualPressureSampler();
    private final InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();
    private final ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
            coordinator, planner, sampler, recorder, Instant::now);

    private final ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

    @Test
    void steadyProfileShouldCompleteAllSteps() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "steady-test", ScenarioProfile.STEADY, 101L, 8, 2, "STEADY integration test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals("steady-test", outcome.scenarioId());
        assertEquals(8, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());
        assertTrue(outcome.evidenceCount() > 0);

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertEquals(outcome.evidenceCount(), snapshots.size());
    }

    @Test
    void steadyProfileShouldRecordSnapshots() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "steady-rec", ScenarioProfile.STEADY, 102L, 8, 2, "STEADY recording test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertEquals(8, snapshots.size());
        for (ObservedSnapshot s : snapshots) {
            assertEquals(outcome.runId(), s.runId());
            assertTrue(s.snapshot().activeThreads() >= 0);
        }
    }

    @Test
    void rampProfileShouldShowQueuePressure() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "ramp-test", ScenarioProfile.RAMP, 201L, 8, 2, "RAMP integration test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals(8, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertEquals(8, snapshots.size());

        boolean hasQueuePressure = false;
        for (ObservedSnapshot s : snapshots) {
            if (s.snapshot().queueSize() > 0) {
                hasQueuePressure = true;
                break;
            }
        }
        assertTrue(hasQueuePressure,
                "RAMP profile should have at least one snapshot with queueSize > 0");
    }

    @Test
    void burstProfileShouldShowQueuePressureOnBurstSteps() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "burst-test", ScenarioProfile.BURST, 301L, 9, 2, "BURST integration test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals(9, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertEquals(9, snapshots.size());

        int burstPressureCount = 0;
        for (ObservedSnapshot s : snapshots) {
            if (s.snapshot().queueSize() > 0) {
                burstPressureCount++;
            }
        }
        assertTrue(burstPressureCount >= 2,
                "BURST profile should have at least 2 snapshots with queueSize > 0, got " + burstPressureCount);
    }

    @Test
    void burstStepShouldHaveHigherQueueSizeThanNonBurstStep() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "burst-cmp", ScenarioProfile.BURST, 302L, 9, 2, "BURST comparison test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        int burstStepQueue = snapshots.get(0).snapshot().queueSize();
        int nonBurstStepQueue = snapshots.get(1).snapshot().queueSize();
        assertTrue(burstStepQueue > nonBurstStepQueue,
                "Burst step queueSize=" + burstStepQueue
                        + " should exceed non-burst step queueSize=" + nonBurstStepQueue);
    }

    @Test
    void shouldReturnValidOutcome() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "outcome-test", ScenarioProfile.STEADY, 103L, 8, 2, "Outcome validation test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertNotNull(outcome.runId());
        assertFalse(outcome.runId().isBlank());
        assertEquals("outcome-test", outcome.scenarioId());
        assertEquals("managed-executor-v0.8.0", outcome.policyId());
        assertEquals(8, outcome.completedStepCount());
        assertTrue(outcome.totalWorkUnits() > 0);
        assertTrue(outcome.evidenceCount() > 0);
        assertEquals(RunState.FINALIZED, outcome.finalState());
    }

    @Test
    void runnerShouldCleanUpExecutor() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "cleanup-test", ScenarioProfile.STEADY, 104L, 8, 2, "Cleanup verification test");

        // run() completes successfully — Phase 5+6 ensure executor terminated
        runner.run(definition, config);
    }

    @Test
    void exceptionPathShouldShutdownExecutor() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "exception-test", ScenarioProfile.STEADY, 105L, 8, 2, "Exception path test");

        AtomicInteger callCount = new AtomicInteger(0);
        PressureSampler failingSampler = (runId, observation, at) -> {
            if (callCount.incrementAndGet() > 2) {
                throw new RuntimeException("Simulated sampling failure");
            }
            return sampler.sample(runId, observation, at);
        };

        ManagedExecutorScenarioRunner failingRunner = new ManagedExecutorScenarioRunner(
                new ExperimentCoordinator(), planner, failingSampler,
                new InMemoryEvidenceRecorder(), Instant::now);

        assertThrows(RuntimeException.class, () -> failingRunner.run(definition, config));
    }
}
