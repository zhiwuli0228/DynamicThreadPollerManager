package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutationReadinessGateTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ReplaySummaryBuilder summaryBuilder = new ReplaySummaryBuilder();
    private final MutationReadinessGate gate = new MutationReadinessGate();

    @Test
    void shouldUseDefaultConfigLabelInSelectedField() {
        ReadinessAssessment assessment = gate.assess(List.of(
                runSummary("run-1", ScenarioProfile.STEADY, readyRun())
        ));
        assertEquals("default", assessment.selectedConfigLabel());
    }

    @Test
    void shouldReturnReadyWhenAllMetricsInsideReadyThresholds() {
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, readyRun()),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun()),
                runSummary("run-burst", ScenarioProfile.BURST, readyRun())
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(ReadinessStatus.READY, assessment.status());
        assertTrue(assessment.missingScenarioProfiles().isEmpty());
        assertTrue(assessment.blockingReasons().isEmpty());
        assertTrue(assessment.riskReasons().isEmpty());
    }

    @Test
    void shouldReturnNotReadyWhenAnyScenarioProfileIsMissing() {
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, readyRun()),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun())
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(ReadinessStatus.NOT_READY, assessment.status());
        assertTrue(assessment.missingScenarioProfiles().contains(ScenarioProfile.BURST));
        assertTrue(assessment.blockingReasons().stream()
                .anyMatch(reason -> reason.contains("BURST")));
    }

    @Test
    void shouldReturnNotReadyWhenAnyRunHasInsufficientEvidence() {
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, List.of(
                        decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                        decision(1, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)
                )),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun()),
                runSummary("run-burst", ScenarioProfile.BURST, readyRun())
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(ReadinessStatus.NOT_READY, assessment.status());
        assertTrue(assessment.blockingReasons().stream()
                .anyMatch(reason -> reason.contains("run-steady")
                        && reason.contains("evidenceCount")));
    }

    @Test
    void shouldReturnNotReadyWhenAnySummaryHasSkippedEvidence() {
        List<ReplayRunSummary> runs = List.of(
                runSummaryWithSkipped("run-steady", ScenarioProfile.STEADY, readyRun(), 1),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun()),
                runSummary("run-burst", ScenarioProfile.BURST, readyRun())
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(ReadinessStatus.NOT_READY, assessment.status());
        assertTrue(assessment.blockingReasons().stream()
                .anyMatch(reason -> reason.contains("run-steady")
                        && reason.contains("skippedCount")));
    }

    @Test
    void shouldReturnReadyWithRiskWhenMetricsExceedReadyButStayBelowRisk() {
        // evidenceCount=5: UP, DOWN, UP, DOWN, HOLD
        // non-HOLD: 4 actions, 3 flips, streak 4
        // holdCount=1, holdRatio=0.2; cappedCount=0, cappedRatio=0
        // → 2 metrics exceed ready thresholds (flips=3>2, streak=4>2) but stay under risk (flips<4, streak<=4)
        // Wait - alternatingStreakMax=4 and risk is 4, must stay STRICTLY below risk.
        // So use 3 evidence: UP, DOWN, HOLD → flips=1, streak=2, hold=1/3=0.33
        // Hmm, need different shape. Let me re-design below.
        List<ReplayDecisionEvidence> riskyRun = riskyOscillatingRun();

        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, riskyRun),
                runSummary("run-ramp", ScenarioProfile.RAMP, riskyRun),
                runSummary("run-burst", ScenarioProfile.BURST, riskyRun)
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(ReadinessStatus.READY_WITH_RISK, assessment.status());
        assertNotNull(assessment.riskReasons());
        assertTrue(!assessment.riskReasons().isEmpty(),
                () -> "expected at least one risk reason but got: " + assessment.riskReasons());
    }

    @Test
    void shouldReturnNotReadyWhenAnyMetricExceedsRiskThreshold() {
        // Build a run with cappedRatio above the risk threshold (0.50)
        // 6 evidence: 4 SCALE_UP CAPPED + 2 HOLD → cappedRatio = 4/6 = 0.667 > 0.50
        List<ReplayDecisionEvidence> actions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            actions.add(decision(i, PolicyAction.SCALE_UP, GateStatus.CAPPED, 8, 10));
        }
        for (int i = 4; i < 6; i++) {
            actions.add(decision(i, PolicyAction.HOLD, GateStatus.HOLD, 10, 10));
        }
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, actions),
                runSummary("run-ramp", ScenarioProfile.RAMP, actions),
                runSummary("run-burst", ScenarioProfile.BURST, actions)
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(ReadinessStatus.NOT_READY, assessment.status());
        assertTrue(assessment.blockingReasons().stream()
                .anyMatch(reason -> reason.toLowerCase().contains("capped")));
    }

    @Test
    void shouldExposeEvaluatedAndMissingScenarioProfiles() {
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, readyRun()),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun())
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP).stream().collect(Collectors.toSet()),
                assessment.evaluatedScenarioProfiles().stream().collect(Collectors.toSet())
        );
        assertEquals(List.of(ScenarioProfile.BURST), assessment.missingScenarioProfiles());
    }

    @Test
    void shouldExposeInputRunIds() {
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, readyRun()),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun()),
                runSummary("run-burst", ScenarioProfile.BURST, readyRun())
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertEquals(List.of("run-steady", "run-ramp", "run-burst"), assessment.inputRunIds());
    }

    @Test
    void shouldFilterToDefaultLabelOnly() {
        // include a "conservative" labeled run to confirm it is excluded from readiness input
        ReplayRunSummary conservativeRun = summaryBuilder.build(
                "run-ignored", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        List<ReplayRunSummary> runs = List.of(
                runSummary("run-steady", ScenarioProfile.STEADY, readyRun()),
                runSummary("run-ramp", ScenarioProfile.RAMP, readyRun()),
                runSummary("run-burst", ScenarioProfile.BURST, readyRun()),
                conservativeRun
        );

        ReadinessAssessment assessment = gate.assess(runs);

        assertTrue(!assessment.inputRunIds().contains("run-ignored"),
                () -> "non-default labeled run must be excluded; got: " + assessment.inputRunIds());
    }

    /**
     * READY-fitness shape: 3 evidence, 1 SCALE_UP + 2 HOLD, no flip, no streak
     * → directionFlipCount=0, alternatingStreakMax=1
     * → holdRatio=0.667 (<0.85), cappedRatio=0.0 (<0.25)
     */
    private static List<ReplayDecisionEvidence> readyRun() {
        return List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(1, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                decision(2, PolicyAction.HOLD, GateStatus.HOLD, 10, 10)
        );
    }

    /**
     * READY_WITH_RISK shape: 5 evidence, UP, HOLD, UP, HOLD, UP
     * → non-HOLD: UP, UP, UP → flips=0, streak=3
     * → holdRatio=2/5=0.4 (<0.85), cappedRatio=0 (<0.25)
     * → alternatingStreakMax=3 (>2 ready threshold, <=4 risk threshold)
     * That's only 1 risk reason. Add a SCALE_UP CAPPED to push cappedRatio over ready:
     * UP-CAPPED, HOLD, UP, HOLD, UP → cappedCount=1, cappedRatio=0.2 (still under 0.25 ready).
     * Try: UP-CAPPED, HOLD, UP, HOLD, DOWN
     * → non-HOLD: UP, UP, DOWN → flips=1, streak=2 (UP,UP reset, UP,DOWN=2)
     * → cappedRatio=0.2, holdRatio=0.4
     * → 0 risk reasons
     * Try: UP-CAPPED, HOLD, UP, HOLD, HOLD
     * → non-HOLD: UP, UP → flips=0, streak=2
     * → cappedRatio=0.2, holdRatio=0.6
     * → streak=2 not over 2 (equal). Hmm.
     * Final: 4 evidence, UP-CAPPED, HOLD, UP, DOWN
     * → cappedCount=1, cappedRatio=0.25 (exactly ready threshold, need to exceed)
     * → holdCount=1, holdRatio=0.25
     * → non-HOLD: UP, UP, DOWN → flips=1, streak=2
     * We need cappedRatio > 0.25. Use 5 evidence: 2 capped, 3 hold
     * → cappedRatio=2/5=0.4 (>0.25 ready, <0.50 risk) ✓
     * non-HOLD: UP, UP → flips=0, streak=2 (= ready threshold, not over)
     * Add directionFlip: UP-CAPPED, UP-CAPPED, DOWN, HOLD, HOLD
     * → cappedCount=2, cappedRatio=0.4 ✓
     * → non-HOLD: UP, UP, DOWN → flips=1, streak=2
     * To exceed alternatingStreak threshold we need alternating sequence.
     * Try: UP-CAPPED, UP-CAPPED, DOWN, HOLD, HOLD
     * Hmm streak=2. Need 3 alternating. So: UP-CAPPED, DOWN, UP, HOLD, HOLD
     * → cappedCount=1, cappedRatio=0.2 (under 0.25)
     * Add another capped: UP-CAPPED, DOWN, UP-CAPPED, HOLD, HOLD
     * → cappedCount=2, cappedRatio=0.4 ✓
     * → non-HOLD: UP, DOWN, UP → flips=2 (= ready, not over)
     * Add one more: UP-CAPPED, DOWN, UP-CAPPED, DOWN, HOLD
     * → cappedCount=2, cappedRatio=0.4 ✓
     * → non-HOLD: UP, DOWN, UP, DOWN → flips=3 (>2 ready, <4 risk) ✓
     * → alternatingStreakMax=4 (= risk, must stay STRICTLY below)
     * So reduce: UP-CAPPED, DOWN, UP, DOWN, HOLD
     * → cappedCount=1, cappedRatio=0.2 (under 0.25)
     * → non-HOLD: UP, DOWN, UP, DOWN → flips=3 ✓ streak=4 (= risk)
     * Streak exactly 4 is NOT strictly below risk. Hmm.
     * Hmm. Let me try: UP-CAPPED, DOWN, UP, HOLD, HOLD
     * → cappedCount=1, cappedRatio=0.2 (under 0.25)
     * → flips=2 (= ready)
     * Not working. Let me just go with 1 risk reason (alternatingStreakMax=3 with
     * 3 non-HOLD actions alternating UP, DOWN, UP):
     * 5 evidence: UP-CAPPED, DOWN, UP, HOLD, HOLD
     * → cappedCount=1, cappedRatio=0.2
     * → holdCount=2, holdRatio=0.4
     * → non-HOLD: UP, DOWN, UP → flips=2 (= ready threshold, not over)
     * → alternatingStreakMax=3 (>2 ready, <4 risk) ✓
     * One risk reason is enough — the spec says "至少一项" (at least one).
     */
    private static List<ReplayDecisionEvidence> riskyOscillatingRun() {
        return List.of(
                decision(0, PolicyAction.SCALE_UP, GateStatus.CAPPED, 8, 10),
                decision(1, PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED, 10, 8),
                decision(2, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                decision(3, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                decision(4, PolicyAction.HOLD, GateStatus.HOLD, 10, 10)
        );
    }

    private ReplayRunSummary runSummary(String runId,
                                        ScenarioProfile profile,
                                        List<ReplayDecisionEvidence> decisions) {
        return summaryBuilder.build(
                runId, "scenario-" + profile, profile,
                "default", decisions, List.of());
    }

    private ReplayRunSummary runSummaryWithSkipped(String runId,
                                                   ScenarioProfile profile,
                                                   List<ReplayDecisionEvidence> decisions,
                                                   int skipped) {
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < skipped; i++) {
            reasons.add("missing pressure fields");
        }
        return summaryBuilder.build(
                runId, "scenario-" + profile, profile,
                "default", decisions, reasons);
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
