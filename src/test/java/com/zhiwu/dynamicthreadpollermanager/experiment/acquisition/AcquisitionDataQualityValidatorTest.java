package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionDataQualityValidatorTest {

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

    private static Map<String, String> completeMetadata() {
        Map<String, String> m = new HashMap<>();
        m.put("environment", "test");
        return m;
    }

    @Test
    void validDatasetShouldPassAllGates() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
            runs.add(snapshot("ramp-" + i, ScenarioProfile.RAMP, 3));
            runs.add(snapshot("burst-" + i, ScenarioProfile.BURST, 3));
        }
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-001", runs, completeMetadata());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertTrue(result.isValid());
        assertEquals(AcquisitionDataQualityResult.Status.VALID, result.status());
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_PROFILES));
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_REPETITION));
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_SNAPSHOTS));
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_ORDERING));
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_RUN_IDENTITY));
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_METADATA));
        assertTrue(result.failedGateCodes().isEmpty());
        assertTrue(result.missingScenarioProfiles().isEmpty());
    }

    @Test
    void missingRequiredProfileShouldBlock() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
        }
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-missing", runs, completeMetadata());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(result.isValid());
        assertTrue(result.failedGateCodes().contains(AcquisitionDataQualityValidator.GATE_PROFILES));
        assertTrue(result.missingScenarioProfiles().contains(ScenarioProfile.RAMP));
        assertTrue(result.missingScenarioProfiles().contains(ScenarioProfile.BURST));
    }

    @Test
    void insufficientRepetitionShouldBlock() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
        }
        runs.add(snapshot("ramp-0", ScenarioProfile.RAMP, 3));
        runs.add(snapshot("burst-0", ScenarioProfile.BURST, 3));
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-low", runs, completeMetadata());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(result.isValid());
        assertTrue(result.failedGateCodes().contains(AcquisitionDataQualityValidator.GATE_REPETITION));
    }

    @Test
    void insufficientSnapshotsShouldBlock() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
            runs.add(snapshot("ramp-" + i, ScenarioProfile.RAMP, 3));
            runs.add(snapshot("burst-" + i, ScenarioProfile.BURST, 2));
        }
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-shallow", runs, completeMetadata());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(result.isValid());
        assertTrue(result.failedGateCodes().contains(AcquisitionDataQualityValidator.GATE_SNAPSHOTS));
    }

    @Test
    void unorderedTimestampsShouldBlock() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
        }
        // Single RAMP run with out-of-order timestamps
        runs.add(new AcquisitionDataSet.RunSnapshot(
                "ramp-0", "scenario-ramp", ScenarioProfile.RAMP, 1L, "baseline-policy-v1",
                List.of(BASE.plus(Duration.ofSeconds(2)),
                        BASE.plus(Duration.ofSeconds(1)),
                        BASE.plus(Duration.ofSeconds(3)))));
        for (int i = 0; i < 2; i++) {
            runs.add(snapshot("ramp-" + (i + 1), ScenarioProfile.RAMP, 3));
        }
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("burst-" + i, ScenarioProfile.BURST, 3));
        }
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-ord", runs, completeMetadata());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(result.isValid());
        assertTrue(result.failedGateCodes().contains(AcquisitionDataQualityValidator.GATE_ORDERING));
    }

    @Test
    void duplicateRunIdsShouldBlock() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        runs.add(snapshot("dup-1", ScenarioProfile.STEADY, 3));
        runs.add(snapshot("dup-1", ScenarioProfile.STEADY, 3));
        runs.add(snapshot("dup-1", ScenarioProfile.STEADY, 3));
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("ramp-" + i, ScenarioProfile.RAMP, 3));
            runs.add(snapshot("burst-" + i, ScenarioProfile.BURST, 3));
        }
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-dup", runs, completeMetadata());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(result.isValid());
        assertTrue(result.failedGateCodes().contains(AcquisitionDataQualityValidator.GATE_RUN_IDENTITY));
    }

    @Test
    void missingEnvironmentMetadataShouldBlock() {
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(snapshot("steady-" + i, ScenarioProfile.STEADY, 3));
            runs.add(snapshot("ramp-" + i, ScenarioProfile.RAMP, 3));
            runs.add(snapshot("burst-" + i, ScenarioProfile.BURST, 3));
        }
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-meta", runs, new HashMap<>());
        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(result.isValid());
        assertTrue(result.failedGateCodes().contains(AcquisitionDataQualityValidator.GATE_METADATA));
    }
}
