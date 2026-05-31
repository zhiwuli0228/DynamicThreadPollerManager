package com.zhiwu.dynamicthreadpollermanager.experiment.coordinator;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.AnalysisSummary;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.ExperimentRun;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentCoordinatorTest {

    private final ExperimentCoordinator coordinator = new ExperimentCoordinator();

    @Test
    void shouldCreateRunWithScenarioAndPolicyIdentity() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");

        assertNotNull(run.runId());
        assertEquals("scenario-1", run.scenarioId());
        assertEquals("policy-1", run.policyId());
        assertEquals(RunState.CREATED, run.state());
    }

    @Test
    void shouldStartCreatedRun() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");

        ExperimentRun started = coordinator.startRun(run.runId());

        assertEquals(RunState.RUNNING, started.state());
        assertEquals(run.runId(), started.runId());
    }

    @Test
    void shouldStopRunningRun() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");
        coordinator.startRun(run.runId());

        ExperimentRun stopped = coordinator.stopRun(run.runId());

        assertEquals(RunState.STOPPED, stopped.state());
    }

    @Test
    void shouldFinalizeStoppedRun() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");
        coordinator.startRun(run.runId());
        coordinator.stopRun(run.runId());

        ExperimentRun finalized = coordinator.finalizeRun(run.runId());

        assertEquals(RunState.FINALIZED, finalized.state());
    }

    @Test
    void shouldGenerateSummaryAfterFinalization() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");
        coordinator.startRun(run.runId());
        coordinator.stopRun(run.runId());
        coordinator.finalizeRun(run.runId());

        AnalysisSummary summary = coordinator.generateSummary(run.runId());

        assertEquals(run.runId(), summary.runId());
        assertEquals(run.experimentKey(), summary.experimentKey());
        assertEquals("scenario-1", summary.scenarioId());
        assertEquals("policy-1", summary.policyId());
        assertEquals("COMPLETED", summary.outcome());
    }

    @Test
    void shouldPreventInvalidStateTransitions() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");

        assertThrows(IllegalStateException.class, () -> coordinator.stopRun(run.runId()));
    }

    @Test
    void shouldPreventStartingAlreadyRunningRun() {
        ExperimentRun run = coordinator.createRun("scenario-1", "policy-1");
        coordinator.startRun(run.runId());

        assertThrows(IllegalStateException.class, () -> coordinator.startRun(run.runId()));
    }

    @Test
    void shouldThrowForNonExistentRun() {
        assertThrows(IllegalArgumentException.class, () -> coordinator.getRun("non-existent"));
    }

    @Test
    void shouldProduceDeterministicExperimentKeyForSameScenarioAndPolicy() {
        ExperimentRun run1 = coordinator.createRun("scenario-1", "policy-1");
        ExperimentRun run2 = coordinator.createRun("scenario-1", "policy-1");

        assertEquals(run1.experimentKey(), run2.experimentKey());
        assertNotEquals(run1.runId(), run2.runId());
    }
}
