package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaySummaryBuilderTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ReplaySummaryBuilder builder = new ReplaySummaryBuilder();

    @Test
    void shouldEnforceCountConservation() {
        // 5 evidence, 3 decisions, 2 skipped
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                decision(2, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of("missing", "missing"));

        assertEquals(5, summary.evidenceCount());
        assertEquals(3, summary.decisionCount());
        assertEquals(2, summary.skippedCount());
    }

    @Test
    void shouldCountActionsAndGateStatuses() {
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.SCALE_DOWN, GateStatus.CAPPED, 8, 6),
                decision(2, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                decision(3, PolicyAction.SCALE_UP, GateStatus.HOLD, 8, 8),
                decision(4, PolicyAction.HOLD, GateStatus.REJECTED, 8, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(2, summary.scaleUpCount());
        assertEquals(1, summary.scaleDownCount());
        assertEquals(2, summary.holdCount());
        assertEquals(1, summary.acceptedCount());
        assertEquals(1, summary.cappedCount());
        assertEquals(2, summary.gateHoldCount());
        assertEquals(1, summary.rejectedCount());
    }

    @Test
    void shouldComputeRatios() {
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                decision(1, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                decision(2, PolicyAction.SCALE_UP, GateStatus.CAPPED, 8, 10),
                decision(3, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                decision(4, PolicyAction.SCALE_UP, GateStatus.CAPPED, 8, 10)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(5, summary.evidenceCount());
        assertEquals(0.6, summary.holdRatio(), 1e-9);
        assertEquals(0.4, summary.cappedRatio(), 1e-9);
    }

    @Test
    void shouldHandleZeroEvidence() {
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(), List.of());

        assertEquals(0, summary.evidenceCount());
        assertEquals(0.0, summary.holdRatio());
        assertEquals(0.0, summary.cappedRatio());
        assertEquals(0, summary.directionFlipCount());
        assertEquals(0, summary.alternatingStreakMax());
    }

    @Test
    void shouldCountDirectionFlipsOnlyOnNonHoldSequence() {
        // non-HOLD sequence: UP, DOWN, DOWN, UP → flips: 1, 0, 1 = 2
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8),
                decision(2, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 8, 6),
                decision(3, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 6, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(2, summary.directionFlipCount());
    }

    @Test
    void shouldIgnoreHoldsWhenCountingDirectionFlips() {
        // non-HOLD sequence: UP, DOWN → 1 flip
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                decision(2, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                decision(3, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(1, summary.directionFlipCount());
    }

    @Test
    void shouldComputeAlternatingStreakMaxFromUpDownSequence() {
        // non-HOLD sequence: UP, DOWN, UP, DOWN → alternating streak = 4
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8),
                decision(2, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(3, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(4, summary.alternatingStreakMax());
    }

    @Test
    void shouldIgnoreHoldsWhenCountingAlternatingStreak() {
        // non-HOLD sequence: UP, DOWN, UP, DOWN → streak 4 even with holds interleaved
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                decision(2, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8),
                decision(3, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                decision(4, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(5, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(4, summary.alternatingStreakMax());
    }

    @Test
    void shouldResetAlternatingStreakOnRepeatedDirection() {
        // non-HOLD: UP, DOWN, DOWN, UP → longest alternating run is 2 (UP, DOWN)
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8),
                decision(2, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 8, 6),
                decision(3, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 6, 8)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of());

        assertEquals(2, summary.alternatingStreakMax());
    }

    @Test
    void shouldRejectNullRunId() {
        assertThrows(IllegalArgumentException.class, () -> builder.build(
                "", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(), List.of()));
    }

    @Test
    void shouldExposeDecisionEvidenceAndSkippedReasons() {
        List<ReplayDecisionEvidence> evidence = List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10)
        );
        ReplayRunSummary summary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                evidence, List.of("pressure field missing", "timestamp disorder"));

        assertEquals(evidence, summary.decisionEvidence());
        assertEquals(List.of("pressure field missing", "timestamp disorder"), summary.skippedReasons());
    }

    @Test
    void shouldBuildScenarioSummaryFromRunSummaries() {
        ReplayRunSummary runA = builder.build(
                "run-A", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8)
                ),
                List.of());
        ReplayRunSummary runB = builder.build(
                "run-B", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 10, 12)
                ),
                List.of());

        ReplayScenarioSummary scenario = ReplaySummaryBuilder.summarizeScenario(
                ScenarioProfile.STEADY, "default", List.of(runA, runB));

        assertEquals(ScenarioProfile.STEADY, scenario.scenarioProfile());
        assertEquals("default", scenario.policyConfigLabel());
        assertEquals(List.of("run-A", "run-B"), scenario.runIds());
        assertEquals(4, scenario.totalEvidenceCount());
        assertEquals(4, scenario.totalDecisionCount());
        assertEquals(0, scenario.totalSkippedCount());
        // flips in runA = 1 (UP,DOWN), runB = 0 → aggregate = 1
        assertEquals(1, scenario.aggregateDirectionFlipCount());
    }

    @Test
    void shouldRejectMismatchedScenarioProfile() {
        ReplayRunSummary runA = builder.build(
                "run-A", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> ReplaySummaryBuilder.summarizeScenario(
                ScenarioProfile.RAMP, "default", List.of(runA)));
    }

    @Test
    void shouldRejectMismatchedConfigLabel() {
        ReplayRunSummary runA = builder.build(
                "run-A", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> ReplaySummaryBuilder.summarizeScenario(
                ScenarioProfile.STEADY, "conservative", List.of(runA)));
    }

    @Test
    void shouldExposeSkippedReasonList() {
        assertTrue(builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(), List.of("a", "b")).skippedReasons().contains("a"));
    }

    private static ReplayDecisionEvidence decision(int index,
                                                   PolicyAction action,
                                                   GateStatus status,
                                                   int current,
                                                   int proposed) {
        return new ReplayDecisionEvidence(
                "run-1", "scenario-1", ScenarioProfile.STEADY,
                "default", "default-adaptive", index,
                T0.plusSeconds(index), T0.plusSeconds(index),
                action, status, current, proposed, "test reason");
    }
}
