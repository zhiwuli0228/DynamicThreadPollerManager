package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionContractsTest {

    @Test
    void runManifestShouldExposeAllRequiredFields() {
        Instant now = Instant.parse("2026-06-06T10:00:00Z");
        RunManifest.BaselinePresetSummary preset =
                new RunManifest.BaselinePresetSummary("baseline-fixed", 2, 4, 50);
        RunManifest manifest = new RunManifest(
                "run-001",
                "scenario-A",
                ScenarioProfile.STEADY,
                42L,
                20,
                "baseline-policy-v1",
                preset,
                Map.of("java.runtime.version", "21.0.1"),
                List.of("java", "-jar", "run.jar"),
                now);

        assertAll(
                () -> assertEquals("run-001", manifest.runId()),
                () -> assertEquals("scenario-A", manifest.scenarioId()),
                () -> assertEquals(ScenarioProfile.STEADY, manifest.scenarioProfile()),
                () -> assertEquals(42L, manifest.seed()),
                () -> assertEquals(20, manifest.stepCount()),
                () -> assertEquals("baseline-policy-v1", manifest.baselinePolicyId()),
                () -> assertEquals(preset, manifest.baselinePreset()),
                () -> assertEquals(Map.of("java.runtime.version", "21.0.1"),
                        manifest.environmentSummary()),
                () -> assertEquals(List.of("java", "-jar", "run.jar"), manifest.commandLine()),
                () -> assertEquals(now, manifest.createdAt())
        );
    }

    @Test
    void runManifestShouldRejectBlankRunIdAndInvalidStepCount() {
        RunManifest.BaselinePresetSummary preset =
                new RunManifest.BaselinePresetSummary("baseline-fixed", 2, 4, 50);
        Instant now = Instant.parse("2026-06-06T10:00:00Z");
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new RunManifest(
                        "", "scenario-A", ScenarioProfile.STEADY, 1L, 1,
                        "p", preset, Map.of(), List.of(), now)),
                () -> assertThrows(IllegalArgumentException.class, () -> new RunManifest(
                        "run-001", "scenario-A", ScenarioProfile.STEADY, 1L, 0,
                        "p", preset, Map.of(), List.of(), now)),
                () -> assertThrows(NullPointerException.class, () -> new RunManifest(
                        "run-001", "scenario-A", null, 1L, 1,
                        "p", preset, Map.of(), List.of(), now))
        );
    }

    @Test
    void pressureSummaryShouldEnforceScaleEventInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new PressureSummary(
                "run-001",
                ScenarioProfile.STEADY,
                10,
                List.of(new PressureSummary.ProfileCount(ScenarioProfile.STEADY, 10)),
                5,
                3,
                3,
                10.0,
                4.5));
    }

    @Test
    void pressureSummaryShouldExposeAllFields() {
        PressureSummary summary = new PressureSummary(
                "run-001",
                ScenarioProfile.RAMP,
                20,
                List.of(new PressureSummary.ProfileCount(ScenarioProfile.RAMP, 20)),
                6,
                4,
                2,
                9.5,
                3.2);
        assertAll(
                () -> assertEquals("run-001", summary.runId()),
                () -> assertEquals(ScenarioProfile.RAMP, summary.scenarioProfile()),
                () -> assertEquals(20, summary.totalSnapshotCount()),
                () -> assertEquals(1, summary.profileSnapshotCounts().size()),
                () -> assertEquals(6, summary.scaleEventCount()),
                () -> assertEquals(4, summary.scaleUpCount()),
                () -> assertEquals(2, summary.scaleDownCount()),
                () -> assertEquals(9.5, summary.peakObservedQueueDepth()),
                () -> assertEquals(3.2, summary.meanObservedQueueDepth())
        );
    }

    @Test
    void replaySummaryShouldEnforceEvidenceInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new ReplaySummary(
                "run-001", "scenario-A", ScenarioProfile.STEADY, "default",
                10, 5, 4, 1, 1, 3, 2, 1, 1,
                0.3, 0.2, List.of()));
    }

    @Test
    void replaySummaryShouldRejectOutOfRangeRatios() {
        assertThrows(IllegalArgumentException.class, () -> new ReplaySummary(
                "run-001", "scenario-A", ScenarioProfile.STEADY, "default",
                5, 5, 0, 1, 1, 3, 2, 1, 1,
                1.5, 0.2, List.of()));
    }

    @Test
    void evidenceIndexShouldValidateRetentionRecord() {
        RetentionRecord good = RetentionRecord.defaultNonVersioned("run-001");
        EvidenceIndex index = new EvidenceIndex(
                "run-001",
                "outputs/reports/v0.6.0",
                "outputs/reports/v0.6.0/run-001-run-manifest.json",
                "outputs/reports/v0.6.0/run-001-pressure-summary.json",
                "outputs/reports/v0.6.0/run-001-replay-summary.json",
                "outputs/reports/v0.6.0/run-001-readiness-summary.json",
                good);
        assertEquals(4, index.artifactPaths().size());
        assertFalse(good.isRetained());
    }

    @Test
    void evidenceIndexShouldRejectBlankArtifactPaths() {
        RetentionRecord good = RetentionRecord.defaultNonVersioned("run-001");
        assertThrows(IllegalArgumentException.class, () -> new EvidenceIndex(
                "run-001",
                "outputs/reports/v0.6.0",
                "",
                "outputs/reports/v0.6.0/run-001-pressure-summary.json",
                "outputs/reports/v0.6.0/run-001-replay-summary.json",
                "outputs/reports/v0.6.0/run-001-readiness-summary.json",
                good));
    }

    @Test
    void retentionRecordShouldDefaultToNonVersioned() {
        RetentionRecord r = RetentionRecord.defaultNonVersioned("run-001");
        assertFalse(r.isRetained());
        assertNull(r.retentionLocation());
        assertNull(r.responsibleOwner());
        assertNull(r.retainedAt());
        assertNull(r.cleanupPlan());
        assertEquals(RetentionRecord.DEFAULT_POLICY,
                r.toString().contains(RetentionRecord.DEFAULT_POLICY) ? RetentionRecord.DEFAULT_POLICY : null);
    }

    @Test
    void retentionRecordShouldRequireOwnerAndCleanupWhenRetained() {
        Instant now = Instant.parse("2026-06-06T10:00:00Z");
        RetentionRecord incomplete = new RetentionRecord("run-001", "/tmp/raw", null, now, "delete on 2026-07-01");
        IllegalStateException ex = assertThrows(IllegalStateException.class, incomplete::validate);
        assertTrue(ex.getMessage().contains("responsibleOwner"));

        RetentionRecord complete = new RetentionRecord(
                "run-001",
                "/tmp/raw",
                "data-platform",
                now,
                "delete on 2026-07-01");
        complete.validate();
        assertTrue(complete.isRetained());
    }

    @Test
    void readinessSummaryShouldClassifyAndBoundedNextStep() {
        ReadinessSummary ready = new ReadinessSummary(
                "run-001", ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of(),
                ReadinessSummary.NEXT_STEP_REPLAY);
        assertFalse(ready.isMutationAuthorizing());
        assertEquals(ReadinessStatus.READY, ready.status());

        ReadinessSummary risky = new ReadinessSummary(
                "run-002", ReadinessStatus.READY_WITH_RISK,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of("BURST holdRatio above 0.5"),
                ReadinessSummary.NEXT_STEP_REPLAY_WITH_CAUTION);
        assertFalse(risky.isMutationAuthorizing());

        ReadinessSummary notReady = new ReadinessSummary(
                "run-003", ReadinessStatus.NOT_READY,
                List.of(ScenarioProfile.STEADY),
                List.of(ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of("missing RAMP profile", "missing BURST profile"),
                List.of(),
                ReadinessSummary.NEXT_STEP_COLLECT_MORE);
        assertFalse(notReady.isMutationAuthorizing());
    }

    @Test
    void readinessSummaryShouldRejectUnboundedNextStep() {
        assertThrows(IllegalArgumentException.class, () -> new ReadinessSummary(
                "run-001", ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of(),
                List.of(),
                "proceed_to_mutation"));
    }

    @Test
    void readinessSummaryShouldRequireBlockingReasonsForNotReady() {
        assertThrows(IllegalArgumentException.class, () -> new ReadinessSummary(
                "run-001", ReadinessStatus.NOT_READY,
                List.of(ScenarioProfile.STEADY),
                List.of(ScenarioProfile.RAMP),
                List.of(),
                List.of(),
                ReadinessSummary.NEXT_STEP_COLLECT_MORE));
    }

    @Test
    void readinessSummaryShouldForbidRiskReasonsWhenReady() {
        assertThrows(IllegalArgumentException.class, () -> new ReadinessSummary(
                "run-001", ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of("anything"),
                ReadinessSummary.NEXT_STEP_REPLAY));
    }

    @Test
    void readinessSummaryShouldRequireRiskReasonsForReadyWithRisk() {
        assertThrows(IllegalArgumentException.class, () -> new ReadinessSummary(
                "run-001", ReadinessStatus.READY_WITH_RISK,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of(),
                ReadinessSummary.NEXT_STEP_REPLAY_WITH_CAUTION));
    }

    @Test
    void reportPathsShouldCentralizeNaming() {
        assertAll(
                () -> assertEquals("run-001-run-manifest.json",
                        AcquisitionReportPaths.runManifestFileName("run-001")),
                () -> assertEquals("run-001-pressure-summary.json",
                        AcquisitionReportPaths.pressureSummaryFileName("run-001")),
                () -> assertEquals("run-001-replay-summary.json",
                        AcquisitionReportPaths.replaySummaryFileName("run-001")),
                () -> assertEquals("run-001-evidence-index.json",
                        AcquisitionReportPaths.evidenceIndexFileName("run-001")),
                () -> assertEquals("run-001-readiness-summary.json",
                        AcquisitionReportPaths.readinessSummaryFileName("run-001")),
                () -> assertEquals("run-001-acquisition-report.md",
                        AcquisitionReportPaths.compositeReportFileName("run-001"))
        );
    }

    @Test
    void reportPathsShouldRejectPathTraversal() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AcquisitionReportPaths.runManifestFileName("../etc")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AcquisitionReportPaths.runManifestFileName("a/b")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AcquisitionReportPaths.runManifestFileName("a\\b")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AcquisitionReportPaths.runManifestFileName(""))
        );
    }

    @Test
    void reportPathsShouldResolveToVersionedDirectory() {
        String resolved = AcquisitionReportPaths.reportDirectory(java.nio.file.Path.of("."))
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace('\\', '/');
        assertTrue(resolved.endsWith("outputs/reports/v0.6.0"),
                "expected to end with outputs/reports/v0.6.0, was " + resolved);
        assertNotNull(AcquisitionReportPaths.VERSION_TAG);
        assertEquals("v0.6.0", AcquisitionReportPaths.VERSION_TAG);
    }
}
