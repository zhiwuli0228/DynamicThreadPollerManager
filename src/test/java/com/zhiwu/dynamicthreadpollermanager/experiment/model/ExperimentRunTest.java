package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentRunTest {

    @Test
    void shouldCreateRunWithGeneratedId() {
        ExperimentRun run = new ExperimentRun("scenario-1", "policy-1");

        assertNotNull(run.runId());
        assertEquals("scenario-1", run.scenarioId());
        assertEquals("policy-1", run.policyId());
        assertEquals(RunState.CREATED, run.state());
        assertNotNull(run.createdAt());
    }

    @Test
    void shouldCreateRunWithSpecificId() {
        Instant now = Instant.now();
        ExperimentRun run = new ExperimentRun("run-123", "scenario-1", "policy-1", now, RunState.RUNNING);

        assertEquals("run-123", run.runId());
        assertEquals("scenario-1", run.scenarioId());
        assertEquals("policy-1", run.policyId());
        assertEquals(now, run.createdAt());
        assertEquals(RunState.RUNNING, run.state());
    }

    @Test
    void shouldProduceDeterministicExperimentKeyForSameScenarioAndPolicy() {
        ExperimentRun run1 = new ExperimentRun("scenario-1", "policy-1");
        ExperimentRun run2 = new ExperimentRun("scenario-1", "policy-1");

        assertEquals(run1.experimentKey(), run2.experimentKey());
        assertNotEquals(run1.runId(), run2.runId());
    }

    @Test
    void shouldTransitionStateCorrectly() {
        ExperimentRun run = new ExperimentRun("scenario-1", "policy-1");

        ExperimentRun running = run.withState(RunState.RUNNING);
        assertEquals(RunState.RUNNING, running.state());
        assertEquals(run.runId(), running.runId());

        ExperimentRun stopped = running.withState(RunState.STOPPED);
        assertEquals(RunState.STOPPED, stopped.state());
    }

    @Test
    void shouldBeEqualByRunId() {
        ExperimentRun run1 = new ExperimentRun("scenario-1", "policy-1");
        ExperimentRun run2 = new ExperimentRun(run1.runId(), "other-scenario", "other-policy",
                run1.createdAt(), RunState.RUNNING);

        assertEquals(run1, run2);
        assertEquals(run1.hashCode(), run2.hashCode());
    }
}
