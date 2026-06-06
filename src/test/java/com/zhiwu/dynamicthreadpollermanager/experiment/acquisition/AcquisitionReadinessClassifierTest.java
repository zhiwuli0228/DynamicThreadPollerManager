package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionReadinessClassifierTest {

    private static final Instant BASE = Instant.parse("2026-06-06T10:00:00Z");

    private static AcquisitionDataSet.RunSnapshot snapshot(String runId,
                                                           ScenarioProfile profile,
                                                           int snapshotCount) {
        List<Instant> ts = new ArrayList<>();
        for (int i = 0; i < snapshotCount; i++) {
            ts.add(BASE.plus(Duration.ofSeconds(i)));
        }
        return new AcquisitionDataSet.RunSnapshot(
                runId, "scenario-" + profile.name().toLowerCase(), profile, 1L,
                "baseline-policy-v1", ts);
    }

    private static AcquisitionDataSet validDataset() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
            runs.add(snapshot("ramp-" + i, ScenarioProfile.RAMP, 3));
            runs.add(snapshot("burst-" + i, ScenarioProfile.BURST, 3));
        }
        HashMap<String, String> meta = new HashMap<>();
        meta.put("environment", "test");
        return new AcquisitionDataSet("ds-001", runs, meta);
    }

    private static AcquisitionDataQualityResult validQuality() {
        return new AcquisitionDataQualityValidator().validate(validDataset());
    }

    @Test
    void validDatasetWithNoRisksShouldClassifyAsReady() {
        ReadinessSummary summary = new AcquisitionReadinessClassifier()
                .classify("run-001", validQuality(), List.of());
        assertEquals(ReadinessStatus.READY, summary.status());
        assertEquals(ReadinessSummary.NEXT_STEP_REPLAY, summary.recommendedNextStep());
        assertTrue(summary.riskReasons().isEmpty());
        assertTrue(summary.blockingReasons().isEmpty());
        assertFalse(summary.isMutationAuthorizing());
    }

    @Test
    void validDatasetWithRiskSignalsShouldClassifyAsReadyWithRisk() {
        ReadinessSummary summary = new AcquisitionReadinessClassifier()
                .classify("run-002", validQuality(),
                        List.of(new AcquisitionReadinessClassifier.RiskSignal(
                                ScenarioProfile.BURST, "holdRatio above 0.5")));
        assertEquals(ReadinessStatus.READY_WITH_RISK, summary.status());
        assertEquals(ReadinessSummary.NEXT_STEP_REPLAY_WITH_CAUTION, summary.recommendedNextStep());
        assertEquals(1, summary.riskReasons().size());
        assertTrue(summary.riskReasons().get(0).contains("BURST"));
        assertTrue(summary.riskReasons().get(0).contains("holdRatio above 0.5"));
        assertFalse(summary.isMutationAuthorizing());
    }

    @Test
    void invalidDatasetShouldClassifyAsNotReady() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
        }
        HashMap<String, String> meta = new HashMap<>();
        meta.put("environment", "test");
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-bad", runs, meta);
        AcquisitionDataQualityResult quality =
                new AcquisitionDataQualityValidator().validate(dataset);
        ReadinessSummary summary = new AcquisitionReadinessClassifier()
                .classify("run-bad", quality, List.of());
        assertEquals(ReadinessStatus.NOT_READY, summary.status());
        assertEquals(ReadinessSummary.NEXT_STEP_COLLECT_MORE, summary.recommendedNextStep());
        assertTrue(summary.missingScenarioProfiles().contains(ScenarioProfile.RAMP));
        assertTrue(summary.missingScenarioProfiles().contains(ScenarioProfile.BURST));
        assertFalse(summary.blockingReasons().isEmpty());
        assertFalse(summary.isMutationAuthorizing());
    }
}
