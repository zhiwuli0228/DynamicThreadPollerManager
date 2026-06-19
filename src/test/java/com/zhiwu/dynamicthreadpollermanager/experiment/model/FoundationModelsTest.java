package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoundationModelsTest {

    @Test
    void loadScenarioShouldBeImmutable() {
        LoadScenario scenario = new LoadScenario("scenario-1", "Test load");

        assertEquals("scenario-1", scenario.scenarioId());
        assertEquals("Test load", scenario.description());
    }

    @Test
    void pressureSnapshotShouldBeImmutable() {
        Instant now = Instant.now();
        PressureSnapshot snapshot = new PressureSnapshot(now, 10, 50, 0.75);

        assertEquals(now, snapshot.timestamp());
        assertEquals(10, snapshot.activeThreads());
        assertEquals(0, snapshot.poolSize());
        assertEquals(50, snapshot.queueSize());
        assertEquals(0L, snapshot.completedTaskCount());
        assertEquals(0.75, snapshot.cpuUtilization());

        PressureSnapshot fullSnapshot = new PressureSnapshot(now, 10, 12, 50, 100L, 0.75);
        assertEquals(12, fullSnapshot.poolSize());
        assertEquals(100L, fullSnapshot.completedTaskCount());
    }

    @Test
    void controlPolicyShouldBeImmutable() {
        ControlPolicy policy = new ControlPolicy("policy-1", "type-a", "Description");

        assertEquals("policy-1", policy.policyId());
        assertEquals("type-a", policy.policyType());
        assertEquals("Description", policy.description());
    }

    @Test
    void scaleDecisionShouldBeImmutable() {
        Instant now = Instant.now();
        ScaleDecision decision = new ScaleDecision(now, "run-1", 10, 15, "Scaling up");

        assertEquals(now, decision.timestamp());
        assertEquals("run-1", decision.runId());
        assertEquals(10, decision.currentPoolSize());
        assertEquals(15, decision.proposedPoolSize());
        assertEquals("Scaling up", decision.reasoning());
    }

    @Test
    void adjustmentEventShouldBeImmutable() {
        Instant now = Instant.now();
        AdjustmentEvent event = new AdjustmentEvent(now, "run-1", 10, 15, "Scale up");

        assertEquals(now, event.timestamp());
        assertEquals("run-1", event.runId());
        assertEquals(10, event.previousPoolSize());
        assertEquals(15, event.newPoolSize());
        assertEquals("Scale up", event.adjustmentReason());
    }

    @Test
    void resultSeriesShouldBeImmutable() {
        Instant now = Instant.now();
        PressureSnapshot snapshot = new PressureSnapshot(now, 10, 50, 0.75);
        ResultSeries series = new ResultSeries("run-1", List.of(snapshot));

        assertEquals("run-1", series.runId());
        assertEquals(1, series.size());
        assertEquals(snapshot, series.snapshots().get(0));
    }

    @Test
    void analysisSummaryShouldComputeDuration() {
        Instant start = Instant.parse("2024-01-01T10:00:00Z");
        Instant end = Instant.parse("2024-01-01T10:01:00Z");
        AnalysisSummary summary = new AnalysisSummary(
                "run-1", "scenario-1::policy-1", "scenario-1", "policy-1", start, end, "COMPLETED", 10
        );

        assertEquals(60, summary.duration().getSeconds());
        assertEquals("run-1", summary.runId());
        assertEquals("scenario-1::policy-1", summary.experimentKey());
        assertEquals("scenario-1", summary.scenarioId());
        assertEquals("policy-1", summary.policyId());
        assertEquals("COMPLETED", summary.outcome());
    }

    @Test
    void foundationObjectsShouldNotDependOnSamplingOrMutation() {
        assertDoesNotThrow(() -> {
            new LoadScenario("s1", "desc");
            new ControlPolicy("p1", "type", "desc");
            new PressureSnapshot(Instant.now(), 1, 1, 0.1);
            new ScaleDecision(Instant.now(), "r1", 1, 2, "reason");
            new AdjustmentEvent(Instant.now(), "r1", 1, 2, "reason");
            new ResultSeries("r1", List.of());
            new AnalysisSummary("r1", "s1::p1", "s1", "p1", Instant.now(), Instant.now(), "ok", 0);
        });
    }
}
