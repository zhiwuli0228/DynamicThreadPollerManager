package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ManualPressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.PressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioExperimentRunnerTest {

    private final ExperimentCoordinator coordinator = new ExperimentCoordinator();
    private final EvidenceRecorder recorder = new InMemoryEvidenceRecorder();
    private final PressureSampler sampler = new ManualPressureSampler();
    private final BaselineExecutorPreset preset = BaselineExecutorPreset.fixedSmall();

    @Test
    void shouldReturnFinalizedOutcomeWithExpectedFields() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        ScenarioExperimentRunner runner = newRunner(executor, increasingClock(0, 1));

        ScenarioDefinition definition = new ScenarioDefinition(
                "steady", ScenarioProfile.STEADY, 0L, 3, 2, "small steady");
        ScenarioRunOutcome outcome = runner.run(definition, preset);

        assertNotNull(outcome.runId());
        assertEquals("steady", outcome.scenarioId());
        assertEquals(preset.policyId(), outcome.policyId());
        assertEquals(3, outcome.completedStepCount());
        assertEquals(6L, outcome.totalWorkUnits());
        assertEquals(3, outcome.evidenceCount());
        assertEquals(RunState.FINALIZED, outcome.finalState());
        assertEquals(RunState.FINALIZED, runner.stateOf(outcome.runId()));
    }

    @Test
    void shouldRecordAtLeastOneSnapshotAssociatedWithTheRun() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        ScenarioExperimentRunner runner = newRunner(executor, increasingClock(0, 1));

        ScenarioDefinition definition = new ScenarioDefinition(
                "ramp", ScenarioProfile.RAMP, 0L, 4, 2, "small ramp");
        ScenarioRunOutcome outcome = runner.run(definition, preset);

        List<ObservedSnapshot> recorded = recorder.snapshots(outcome.runId());
        assertTrue(recorded.size() >= 1,
                "evidence recorder must hold at least one snapshot for the run id");
        for (ObservedSnapshot snapshot : recorded) {
            assertEquals(outcome.runId(), snapshot.runId());
        }
        assertEquals(outcome.evidenceCount(), recorded.size());
    }

    @Test
    void shouldComputeCorrectTotalsForBurstProfile() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        ScenarioExperimentRunner runner = newRunner(executor, increasingClock(0, 1));

        ScenarioDefinition definition = new ScenarioDefinition(
                "burst", ScenarioProfile.BURST, 0L, 6, 2, "burst 6 steps");
        ScenarioRunOutcome outcome = runner.run(definition, preset);

        // BURST for 6 steps at base 2: indexes 0, 3 spike to 6; others are 2.
        // Total = 6 + 2 + 2 + 6 + 2 + 2 = 20
        assertEquals(6, outcome.completedStepCount());
        assertEquals(20L, outcome.totalWorkUnits());
    }

    @Test
    void shouldUseBaselinePolicyIdInOutcome() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        ScenarioExperimentRunner runner = newRunner(executor, increasingClock(0, 1));

        ScenarioDefinition definition = new ScenarioDefinition(
                "steady", ScenarioProfile.STEADY, 0L, 1, 1, "");
        ScenarioRunOutcome outcome = runner.run(definition, preset);

        assertEquals(preset.policyId(), outcome.policyId());
    }

    @Test
    void shouldDriveLifecycleToFinalizedState() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        ScenarioExperimentRunner runner = newRunner(executor, increasingClock(0, 1));

        ScenarioDefinition definition = new ScenarioDefinition(
                "steady", ScenarioProfile.STEADY, 0L, 2, 1, "");
        ScenarioRunOutcome outcome = runner.run(definition, preset);

        assertEquals(RunState.FINALIZED, coordinator.getRun(outcome.runId()).state());
    }

    private static Supplier<Instant> increasingClock(long startEpochMillis, long stepMillis) {
        AtomicInteger ticks = new AtomicInteger();
        return () -> Instant.ofEpochMilli(startEpochMillis + (long) ticks.getAndIncrement() * stepMillis);
    }

    private ScenarioExperimentRunner newRunner(BaselineWorkloadExecutor executor,
                                               Supplier<Instant> clock) {
        return new ScenarioExperimentRunner(
                coordinator,
                new DeterministicScenarioPlanner(),
                executor,
                sampler,
                recorder,
                clock
        );
    }
}
