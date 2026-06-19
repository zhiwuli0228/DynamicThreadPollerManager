package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.LivePressureSamplerConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ManualPressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorScenarioRunnerLiveSamplerTest {

    private final ExperimentCoordinator coordinator = new ExperimentCoordinator();
    private final DeterministicScenarioPlanner planner = new DeterministicScenarioPlanner();
    private final ManualPressureSampler sampler = new ManualPressureSampler();
    private final InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();
    private final ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
    private final LivePressureSamplerConfig samplerConfig =
            new LivePressureSamplerConfig(100, false, "integration-session");

    @Test
    void runnerWithLiveSamplerConfigShouldCompleteAllSteps() {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now, samplerConfig);

        ScenarioDefinition definition = new ScenarioDefinition(
                "live-steady", ScenarioProfile.STEADY, 601L, 8, 2, "Live sampler STEADY test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals("live-steady", outcome.scenarioId());
        assertEquals(8, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());
    }

    @Test
    void runnerWithLiveSamplerConfigShouldHaveEvidenceFromAutonomousSampling() {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now, samplerConfig);

        ScenarioDefinition definition = new ScenarioDefinition(
                "live-evidence", ScenarioProfile.STEADY, 602L, 8, 2, "Live sampler evidence test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertTrue(snapshots.size() > 0,
                "live sampler should produce at least one autonomous sample");

        for (ObservedSnapshot s : snapshots) {
            assertEquals(outcome.runId(), s.runId(),
                    "all snapshots should belong to the correct runId");
        }
    }

    @Test
    void runnerWithLiveSamplerConfigShouldNotProduceNewSamplesAfterRun() throws Exception {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now, samplerConfig);

        ScenarioDefinition definition = new ScenarioDefinition(
                "live-stop", ScenarioProfile.STEADY, 603L, 4, 2, "Live sampler stop test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        int snapshotCountAtStop = recorder.snapshots(outcome.runId()).size();
        assertTrue(snapshotCountAtStop > 0);

        // Wait to confirm sampler has stopped and no more samples arrive
        Thread.sleep(300);

        int snapshotCountAfterWait = recorder.snapshots(outcome.runId()).size();
        assertEquals(snapshotCountAtStop, snapshotCountAfterWait,
                "no new samples should appear after run completes and sampler is stopped");
    }

    @Test
    void runnerWithoutLiveSamplerConfigShouldStillWork() {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now);

        ScenarioDefinition definition = new ScenarioDefinition(
                "no-live-sampler", ScenarioProfile.STEADY, 604L, 4, 2, "No live sampler test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals("no-live-sampler", outcome.scenarioId());
        assertEquals(4, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());

        // Manual sampling produces exactly one sample per step
        assertEquals(4, recorder.snapshots(outcome.runId()).size());
    }

    @Test
    void runnerWithLiveSamplerConfigShouldHandleRampProfile() {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now, samplerConfig);

        ScenarioDefinition definition = new ScenarioDefinition(
                "live-ramp", ScenarioProfile.RAMP, 605L, 6, 2, "Live sampler RAMP test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals(6, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertTrue(snapshots.size() > 0);
    }

    @Test
    void runnerWithLiveSamplerConfigShouldHandleBurstProfile() {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now, samplerConfig);

        ScenarioDefinition definition = new ScenarioDefinition(
                "live-burst", ScenarioProfile.BURST, 606L, 9, 2, "Live sampler BURST test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals(9, outcome.completedStepCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());
        assertTrue(outcome.evidenceCount() > 0);
    }

    @Test
    void liveSamplerInjectionIsBackwardCompatible() {
        // 5-arg constructor should work without live sampler config
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now);

        assertNotNull(runner);
    }

    @Test
    void runnerWithNullSamplerConfigShouldUseManualSampling() {
        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now, null);

        ScenarioDefinition definition = new ScenarioDefinition(
                "null-config", ScenarioProfile.STEADY, 607L, 4, 2, "Null config test");

        ScenarioRunOutcome outcome = runner.run(definition, config);

        assertEquals(4, outcome.completedStepCount());
        // With null config, manual sampling should be used (1 per step)
        assertEquals(4, recorder.snapshots(outcome.runId()).size());
    }
}
