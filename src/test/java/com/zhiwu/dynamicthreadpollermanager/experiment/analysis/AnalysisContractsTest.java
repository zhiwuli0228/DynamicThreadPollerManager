package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceSummary;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisContractsTest {

    @Test
    void replayValidationStatusShouldExposeTwoValues() {
        assertNotNull(ReplayValidationStatus.valueOf("VALID"));
        assertNotNull(ReplayValidationStatus.valueOf("INVALID"));
    }

    @Test
    void readinessStatusShouldExposeThreeValues() {
        assertNotNull(ReadinessStatus.valueOf("READY"));
        assertNotNull(ReadinessStatus.valueOf("READY_WITH_RISK"));
        assertNotNull(ReadinessStatus.valueOf("NOT_READY"));
    }

    @Test
    void replayFailureCodeShouldExposeRequiredCodes() {
        // ensure all design-required codes exist
        ReplayFailureCode.valueOf("MISSING_RUN_ID");
        ReplayFailureCode.valueOf("MISSING_SCENARIO_ID");
        ReplayFailureCode.valueOf("MISSING_SCENARIO_PROFILE");
        ReplayFailureCode.valueOf("EMPTY_SNAPSHOTS");
        ReplayFailureCode.valueOf("INSUFFICIENT_SNAPSHOTS");
        ReplayFailureCode.valueOf("RUN_ID_MISMATCH");
        ReplayFailureCode.valueOf("UNORDERED_TIMESTAMP");
        ReplayFailureCode.valueOf("MISSING_PRESSURE_FIELDS");
    }

    @Test
    void replayEvidenceValidationResultShouldExposeFields() {
        ReplayEvidenceValidationResult result = new ReplayEvidenceValidationResult(
                ReplayValidationStatus.VALID,
                List.of(),
                List.of(),
                3,
                0);

        assertEquals(ReplayValidationStatus.VALID, result.status());
        assertTrue(result.failureCodes().isEmpty());
        assertTrue(result.failureReasons().isEmpty());
        assertEquals(3, result.acceptedSnapshotCount());
        assertEquals(0, result.rejectedSnapshotCount());
        assertTrue(result.isValid());
    }

    @Test
    void replayEvidenceValidationResultShouldExposeInvalidFields() {
        ReplayEvidenceValidationResult result = new ReplayEvidenceValidationResult(
                ReplayValidationStatus.INVALID,
                List.of(ReplayFailureCode.MISSING_RUN_ID),
                List.of("runId is blank"),
                0, 0);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertEquals(1, result.failureCodes().size());
        assertEquals("runId is blank", result.failureReasons().get(0));
        assertTrue(!result.isValid());
    }

    @Test
    void replayRunInputShouldExposeAllFields() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        ObservedSnapshot observed = observed("run-1", now, 5, 8, 1);
        EvidenceSummary summary = new EvidenceSummary("run-1", 1, Optional.of(now), Optional.of(now));
        ReplayRunInput input = new ReplayRunInput(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "policy-1",
                List.of(observed), summary, 1, 10);

        assertEquals("run-1", input.runId());
        assertEquals("scenario-1", input.scenarioId());
        assertSame(ScenarioProfile.STEADY, input.scenarioProfile());
        assertEquals("policy-1", input.baselinePolicyId());
        assertEquals(1, input.snapshots().size());
        assertSame(summary, input.evidenceSummary());
        assertEquals(1, input.completedStepCount());
        assertEquals(10, input.totalWorkUnits());
    }

    @Test
    void replayRunInputShouldRejectBlankRunId() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        ObservedSnapshot observed = observed("run-1", now, 5, 8, 1);
        EvidenceSummary summary = new EvidenceSummary("run-1", 1, Optional.of(now), Optional.of(now));
        assertThrows(IllegalArgumentException.class, () -> new ReplayRunInput(
                "", "scenario-1", ScenarioProfile.STEADY, "policy-1",
                List.of(observed), summary, 1, 10));
    }

    @Test
    void replayDecisionEvidenceShouldEnforceTimestampEquality() {
        Instant snapshotTs = Instant.parse("2026-01-01T00:00:00Z");
        Instant later = Instant.parse("2026-01-01T00:01:00Z");
        assertThrows(IllegalArgumentException.class, () -> new ReplayDecisionEvidence(
                "run-1", "scenario-1", ScenarioProfile.STEADY,
                "default", "default-adaptive", 0,
                snapshotTs, later,
                PolicyAction.HOLD, GateStatus.HOLD, 8, 8, "reason"));
    }

    @Test
    void replayDecisionEvidenceShouldExposeFieldsAndReplayMode() {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");
        ReplayDecisionEvidence ev = new ReplayDecisionEvidence(
                "run-1", "scenario-1", ScenarioProfile.STEADY,
                "default", "default-adaptive", 3,
                ts, ts,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10, "scale up");

        assertEquals("run-1", ev.runId());
        assertEquals("scenario-1", ev.scenarioId());
        assertEquals(ScenarioProfile.STEADY, ev.scenarioProfile());
        assertEquals("default", ev.policyConfigLabel());
        assertEquals("default-adaptive", ev.policyId());
        assertEquals(3, ev.snapshotIndex());
        assertEquals(ts, ev.snapshotTimestamp());
        assertEquals(ts, ev.decisionTimestamp());
        assertEquals(PolicyAction.SCALE_UP, ev.action());
        assertEquals(GateStatus.ACCEPTED, ev.gateStatus());
        assertEquals(8, ev.currentPoolSize());
        assertEquals(10, ev.proposedPoolSize());
        assertEquals("scale up", ev.reason());
        assertEquals("offline_replay", ev.replayMode());
    }

    @Test
    void replayRunSummaryShouldEnforceCountConservation() {
        assertThrows(IllegalArgumentException.class, () -> new ReplayRunSummary(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                5, 3, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0.5, 0.0,
                List.of(), List.of()));
    }

    private static ObservedSnapshot observed(String runId, Instant ts, int active, int pool, int queue) {
        RuntimeObservation observation = new RuntimeObservation(
                ts,
                MetricValue.present(active),
                MetricValue.present(pool),
                MetricValue.present(queue),
                MetricValue.absent(),
                MetricValue.present(0.5)
        );
        PressureSnapshot snapshot = new PressureSnapshot(ts, active, pool, queue, 0L, 0.5);
        return new ObservedSnapshot(runId, snapshot, observation);
    }
}
