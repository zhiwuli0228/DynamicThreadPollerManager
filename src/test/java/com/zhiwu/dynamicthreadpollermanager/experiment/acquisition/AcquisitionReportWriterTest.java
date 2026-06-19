package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionReportWriterTest {

    private static final Instant BASE = Instant.parse("2026-06-06T10:00:00Z");

    @TempDir
    Path tempDir;

    private RunManifest sampleManifest(String runId) {
        RunManifest.BaselinePresetSummary preset =
                new RunManifest.BaselinePresetSummary("baseline-fixed", 2, 4, 50);
        return new RunManifest(
                runId,
                "scenario-steady",
                ScenarioProfile.STEADY,
                42L,
                20,
                "baseline-policy-v1",
                preset,
                Map.of("java.runtime.version", "21.0.1"),
                List.of("java", "-jar", "run.jar"),
                BASE);
    }

    private PressureSummary samplePressureSummary(String runId) {
        return new PressureSummary(
                runId,
                ScenarioProfile.STEADY,
                10,
                List.of(new PressureSummary.ProfileCount(ScenarioProfile.STEADY, 10)),
                4,
                2,
                2,
                8.0,
                3.5);
    }

    private ReplaySummary sampleReplaySummary(String runId) {
        return new ReplaySummary(
                runId,
                "scenario-steady",
                ScenarioProfile.STEADY,
                "default",
                10,
                8,
                2,
                2,
                1,
                5,
                6,
                1,
                1,
                0.5,
                0.1,
                List.of("insufficient_data"));
    }

    private ReadinessSummary sampleReadinessSummary(String runId) {
        return new ReadinessSummary(
                runId,
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of(),
                ReadinessSummary.NEXT_STEP_REPLAY);
    }

    private EvidenceIndex sampleEvidenceIndex(String runId) {
        return new EvidenceIndex(
                runId,
                "outputs/reports/v0.6.0",
                "outputs/reports/v0.6.0/" + AcquisitionReportPaths.runManifestFileName(runId),
                "outputs/reports/v0.6.0/" + AcquisitionReportPaths.pressureSummaryFileName(runId),
                "outputs/reports/v0.6.0/" + AcquisitionReportPaths.replaySummaryFileName(runId),
                "outputs/reports/v0.6.0/" + AcquisitionReportPaths.readinessSummaryFileName(runId),
                RetentionRecord.defaultNonVersioned(runId));
    }

    @Test
    void writeAllShouldProduceAllRequiredArtifacts() throws IOException {
        String runId = "run-001";
        AcquisitionReportWriter writer = new AcquisitionReportWriter(tempDir);

        AcquisitionReportArtifact artifact = writer.writeAll(
                sampleManifest(runId),
                samplePressureSummary(runId),
                sampleReplaySummary(runId),
                sampleReadinessSummary(runId),
                sampleEvidenceIndex(runId));

        assertAll(
                () -> assertNotNull(artifact.runManifestPath()),
                () -> assertNotNull(artifact.pressureSummaryPath()),
                () -> assertNotNull(artifact.replaySummaryPath()),
                () -> assertNotNull(artifact.readinessSummaryPath()),
                () -> assertNotNull(artifact.evidenceIndexPath()),
                () -> assertEquals(5, artifact.allPaths().size()));

        for (Path p : artifact.allPaths()) {
            assertTrue(Files.exists(p), "expected file to exist: " + p);
            assertTrue(Files.size(p) > 0, "expected non-empty file: " + p);
        }

        // Verify manifest content
        String manifestJson = Files.readString(artifact.runManifestPath());
        assertTrue(manifestJson.contains("\"runId\""));
        assertTrue(manifestJson.contains(runId));
        assertTrue(manifestJson.contains("\"STEADY\""));

        // Verify pressure summary content
        String pressureJson = Files.readString(artifact.pressureSummaryPath());
        assertTrue(pressureJson.contains("\"totalSnapshotCount\""));

        // Verify replay summary content
        String replayJson = Files.readString(artifact.replaySummaryPath());
        assertTrue(replayJson.contains("\"evidenceCount\""));

        // Verify readiness summary content
        String readinessJson = Files.readString(artifact.readinessSummaryPath());
        assertTrue(readinessJson.contains("\"READY\""));
        assertTrue(readinessJson.contains("\"proceed_to_replay_review\""));

        // Verify evidence index content
        String indexJson = Files.readString(artifact.evidenceIndexPath());
        assertTrue(indexJson.contains("\"non_versioned_no_retention\""));
    }

    @Test
    void writeAllShouldTargetVersionedOutputDirectory() {
        AcquisitionReportWriter writer = new AcquisitionReportWriter(tempDir);
        String resolved = writer.outputDirectory().toAbsolutePath().normalize().toString()
                .replace('\\', '/');
        assertTrue(resolved.endsWith("outputs/reports/v0.6.0"),
                "expected to end with outputs/reports/v0.6.0, was " + resolved);
    }

    @Test
    void writeRunManifestShouldCreateDirectoryAndFile() throws IOException {
        AcquisitionReportWriter writer = new AcquisitionReportWriter(tempDir);
        Path path = writer.writeRunManifest(sampleManifest("run-dir-test"));
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(writer.outputDirectory()));
    }

    @Test
    void reportArtifactShouldExposeAllPaths() {
        AcquisitionReportArtifact artifact = new AcquisitionReportArtifact(
                Path.of("a"), Path.of("b"), Path.of("c"), Path.of("d"), Path.of("e"));
        assertEquals(5, artifact.allPaths().size());
    }

    @Test
    void fullPipelineShouldProduceValidReportForValidDataset() throws IOException {
        // Build a valid dataset
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(makeSnapshot("steady-" + i, ScenarioProfile.STEADY, 3));
            runs.add(makeSnapshot("ramp-" + i, ScenarioProfile.RAMP, 3));
            runs.add(makeSnapshot("burst-" + i, ScenarioProfile.BURST, 3));
        }
        Map<String, String> meta = new HashMap<>();
        meta.put("environment", "test");
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-pipeline", runs, meta);

        // Validate quality
        AcquisitionDataQualityResult quality =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertTrue(quality.isValid(), "dataset should be valid");

        // Classify readiness
        ReadinessSummary readiness = new AcquisitionReadinessClassifier()
                .classify("run-pipeline", quality, List.of());
        assertEquals(ReadinessStatus.READY, readiness.status());
        assertFalse(readiness.isMutationAuthorizing());

        // Write reports
        AcquisitionReportWriter writer = new AcquisitionReportWriter(tempDir);
        AcquisitionReportArtifact artifact = writer.writeAll(
                sampleManifest("run-pipeline"),
                samplePressureSummary("run-pipeline"),
                sampleReplaySummary("run-pipeline"),
                readiness,
                sampleEvidenceIndex("run-pipeline"));

        for (Path p : artifact.allPaths()) {
            assertTrue(Files.exists(p), "expected file: " + p);
        }

        // Verify readiness report doesn't imply mutation authorization
        String readinessJson = Files.readString(artifact.readinessSummaryPath());
        assertFalse(readinessJson.contains("\"mutationAuthorizing\": true"));
        assertTrue(readinessJson.contains("\"READY\""));
    }

    @Test
    void invalidDatasetShouldBlockBeforeReportWriting() {
        // Build dataset missing RAMP and BURST
        List<AcquisitionDataSet.RunSnapshot> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runs.add(makeSnapshot("steady-" + i, ScenarioProfile.STEADY, 3));
        }
        Map<String, String> meta = new HashMap<>();
        meta.put("environment", "test");
        AcquisitionDataSet dataset = new AcquisitionDataSet("ds-bad", runs, meta);

        AcquisitionDataQualityResult quality =
                new AcquisitionDataQualityValidator().validate(dataset);
        assertFalse(quality.isValid());

        ReadinessSummary readiness = new AcquisitionReadinessClassifier()
                .classify("run-bad", quality, List.of());
        assertEquals(ReadinessStatus.NOT_READY, readiness.status());
        assertEquals(ReadinessSummary.NEXT_STEP_COLLECT_MORE, readiness.recommendedNextStep());
        assertFalse(readiness.isMutationAuthorizing());
    }

    @Test
    void readinessOutputShouldNeverImplyMutationAuthorization() {
        // READY
        ReadinessSummary ready = new ReadinessSummary(
                "run-ready", ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(), List.of(), List.of(),
                ReadinessSummary.NEXT_STEP_REPLAY);
        assertFalse(ready.isMutationAuthorizing());

        // READY_WITH_RISK
        ReadinessSummary risky = new ReadinessSummary(
                "run-risky", ReadinessStatus.READY_WITH_RISK,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(), List.of(),
                List.of("BURST holdRatio high"),
                ReadinessSummary.NEXT_STEP_REPLAY_WITH_CAUTION);
        assertFalse(risky.isMutationAuthorizing());

        // NOT_READY
        ReadinessSummary notReady = new ReadinessSummary(
                "run-not", ReadinessStatus.NOT_READY,
                List.of(ScenarioProfile.STEADY),
                List.of(ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of("missing profiles"), List.of(),
                ReadinessSummary.NEXT_STEP_COLLECT_MORE);
        assertFalse(notReady.isMutationAuthorizing());
    }

    @Test
    void readinessNextStepShouldStayBounded() {
        // All three allowed next steps are bounded
        assertEquals("proceed_to_replay_review", ReadinessSummary.NEXT_STEP_REPLAY);
        assertEquals("proceed_with_acknowledged_risk", ReadinessSummary.NEXT_STEP_REPLAY_WITH_CAUTION);
        assertEquals("collect_additional_evidence", ReadinessSummary.NEXT_STEP_COLLECT_MORE);
    }

    private static AcquisitionDataSet.RunSnapshot makeSnapshot(String runId,
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
}
