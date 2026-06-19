package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ManualPressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.DeterministicScenarioPlanner;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ManagedExecutorScenarioRunner;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioDefinition;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioRunOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataAcquisitionNineRunTest {

    private ExperimentCoordinator coordinator;
    private DeterministicScenarioPlanner planner;
    private ManualPressureSampler sampler;
    private InMemoryEvidenceRecorder recorder;
    private ManagedExecutorScenarioRunner runner;
    private ManagedExecutorConfig config;

    @BeforeEach
    void setUp() {
        coordinator = new ExperimentCoordinator();
        planner = new DeterministicScenarioPlanner();
        sampler = new ManualPressureSampler();
        recorder = new InMemoryEvidenceRecorder();
        runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, Instant::now);
        config = ManagedExecutorConfig.defaultConfig();
    }

    @Test
    void nineRunAcquisitionShouldPassAllGates() {
        List<AcquisitionDataSet.RunSnapshot> runSnapshots = new ArrayList<>();

        for (long seed = 1L; seed <= 3L; seed++) {
            runSnapshots.add(runAndCapture("steady-" + seed, ScenarioProfile.STEADY, seed));
            runSnapshots.add(runAndCapture("ramp-" + seed, ScenarioProfile.RAMP, seed));
            runSnapshots.add(runAndCapture("burst-" + seed, ScenarioProfile.BURST, seed));
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("environment", "test");

        AcquisitionDataSet dataset = new AcquisitionDataSet(
                "ds-v0.8.0-nine-run", runSnapshots, metadata);

        AcquisitionDataQualityResult result =
                new AcquisitionDataQualityValidator().validate(dataset);

        assertTrue(result.isValid(), "All gates should pass, blocking: " + result.blockingReasons());
        assertEquals(AcquisitionDataQualityResult.Status.VALID, result.status());

        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_PROFILES),
                "G1 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_REPETITION),
                "G2 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_SNAPSHOTS),
                "G3 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_ORDERING),
                "G4 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_RUN_IDENTITY),
                "G5 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_METADATA),
                "G6 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_EXTENDED_FIELDS),
                "G7 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_QUEUE_PRESSURE),
                "G8 should pass");
        assertTrue(result.passedGateCodes().contains(AcquisitionDataQualityValidator.GATE_THREAD_LEAK),
                "G9 should pass");

        assertTrue(result.failedGateCodes().isEmpty(),
                "No gates should fail, failed: " + result.failedGateCodes());
        assertTrue(result.missingScenarioProfiles().isEmpty());
    }

    private AcquisitionDataSet.RunSnapshot runAndCapture(
            String scenarioId, ScenarioProfile profile, long seed) {

        ScenarioDefinition definition = new ScenarioDefinition(
                scenarioId, profile, seed, 8, 2,
                profile.name() + " acquisition run seed=" + seed);

        ScenarioRunOutcome outcome = runner.run(definition, config);
        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());

        List<Instant> timestamps = new ArrayList<>();
        int queuePressureCount = 0;
        Map<String, Boolean> extendedFields = null;

        for (ObservedSnapshot ss : snapshots) {
            timestamps.add(ss.snapshot().timestamp());
            if (ss.snapshot().queueSize() > 0) {
                queuePressureCount++;
            }
            // Capture extended field presence from the first snapshot's observation
            if (extendedFields == null) {
                RuntimeObservation obs = ss.observation();
                extendedFields = new HashMap<>();
                extendedFields.put("poolSize", obs.poolSize().isPresent());
                extendedFields.put("completedTaskCount", obs.completedTaskCount().isPresent());
                extendedFields.put("keepAliveTimeSeconds",
                        obs.keepAliveTimeSeconds().isPresent());
                extendedFields.put("largestPoolSize", obs.largestPoolSize().isPresent());
                extendedFields.put("taskCount", obs.taskCount().isPresent());
            }
        }

        if (extendedFields == null) {
            extendedFields = Map.of();
        }

        return new AcquisitionDataSet.RunSnapshot(
                outcome.runId(),
                outcome.scenarioId(),
                profile,
                seed,
                outcome.policyId(),
                timestamps,
                extendedFields,
                queuePressureCount,
                true);
    }
}
