package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThresholdSensitivityAnalyzerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ReplaySummaryBuilder builder = new ReplaySummaryBuilder();
    private final ThresholdSensitivityAnalyzer analyzer = new ThresholdSensitivityAnalyzer();

    @Test
    void shouldCompareThreeSummariesAgainstDefault() {
        ReplayRunSummary defaultSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.HOLD, GateStatus.HOLD, 10, 10)
                ),
                List.of());
        ReplayRunSummary conservativeSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(
                        decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                        decision(1, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)
                ),
                List.of());
        ReplayRunSummary aggressiveSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "aggressive",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.SCALE_UP, GateStatus.CAPPED, 10, 12)
                ),
                List.of());

        SensitivityComparison comparison = analyzer.compare(
                "run-1", defaultSummary, conservativeSummary, aggressiveSummary);

        assertEquals("run-1", comparison.runId());
        assertEquals(defaultSummary, comparison.defaultSummary());
        assertEquals(conservativeSummary, comparison.conservativeSummary());
        assertEquals(aggressiveSummary, comparison.aggressiveSummary());
    }

    @Test
    void shouldProduceSignedDeltasVsDefault() {
        ReplayRunSummary defaultSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 10, 12)
                ),
                List.of());
        ReplayRunSummary conservativeSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(
                        decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8),
                        decision(1, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)
                ),
                List.of());
        ReplayRunSummary aggressiveSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "aggressive",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.SCALE_UP, GateStatus.CAPPED, 10, 12)
                ),
                List.of());

        SensitivityComparison comparison = analyzer.compare(
                "run-1", defaultSummary, conservativeSummary, aggressiveSummary);

        SensitivityComparison.SensitivityDelta delta = comparison.conservativeDeltaVsDefault();
        // conservative - default
        assertEquals(0 - 2, delta.scaleUpCountDelta());
        assertEquals(0 - 0, delta.scaleDownCountDelta());
        assertEquals(2 - 0, delta.holdCountDelta());

        SensitivityComparison.SensitivityDelta aggressiveDelta = comparison.aggressiveDeltaVsDefault();
        assertEquals(2 - 2, aggressiveDelta.scaleUpCountDelta());
        assertEquals(0 - 0, aggressiveDelta.scaleDownCountDelta());
    }

    @Test
    void shouldRejectNullLabelsOrSummaries() {
        ReplayRunSummary s = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(), List.of());
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.compare("", s, s, s));
        assertThrows(NullPointerException.class,
                () -> analyzer.compare("run-1", null, s, s));
        assertThrows(NullPointerException.class,
                () -> analyzer.compare("run-1", s, null, s));
        assertThrows(NullPointerException.class,
                () -> analyzer.compare("run-1", s, s, null));
    }

    @Test
    void shouldRejectMismatchedRunIdOnSummary() {
        ReplayRunSummary defaultSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(), List.of());
        ReplayRunSummary otherRun = builder.build(
                "run-2", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(), List.of());
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.compare("run-1", defaultSummary, otherRun, defaultSummary));
    }

    @Test
    void shouldRejectWrongLabelOnSummary() {
        ReplayRunSummary defaultSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(), List.of());
        ReplayRunSummary mislabeled = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "weird-label",
                List.of(), List.of());
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.compare("run-1", defaultSummary, mislabeled, defaultSummary));
    }

    @Test
    void shouldExposeAggressiveDeltaVsDefault() {
        ReplayRunSummary defaultSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary conservativeSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary aggressiveSummary = builder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "aggressive",
                List.of(decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10)),
                List.of());

        SensitivityComparison comparison = analyzer.compare(
                "run-1", defaultSummary, conservativeSummary, aggressiveSummary);

        assertNotNull(comparison.aggressiveDeltaVsDefault());
        assertEquals(1, comparison.aggressiveDeltaVsDefault().scaleUpCountDelta());
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
