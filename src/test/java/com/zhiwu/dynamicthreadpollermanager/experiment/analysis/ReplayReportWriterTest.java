package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayReportWriterTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ReplaySummaryBuilder summaryBuilder = new ReplaySummaryBuilder();

    @Test
    void shouldForceOutputDirectoryToV040(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReadinessAssessment assessment = new ReadinessAssessment(
                ReadinessStatus.READY, List.of(ScenarioProfile.STEADY), List.of(),
                List.of(), List.of(), "default", List.of("run-1"));

        ReplayReportArtifact artifact = writer.writeRunSummary(run, assessment);

        assertTrue(artifact.runSummaryPath().startsWith(tempDir.resolve("outputs/reports/v0.4.0")),
                () -> "run summary must be under outputs/reports/v0.4.0, got: " + artifact.runSummaryPath());
        assertTrue(Files.exists(artifact.runSummaryPath()));
    }

    @Test
    void shouldUseCanonicalRunSummaryFileName(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReadinessAssessment assessment = readinessFor(run);

        ReplayReportArtifact artifact = writer.writeRunSummary(run, assessment);

        assertEquals(
                "replay-run-summary-run-1-default.json",
                artifact.runSummaryPath().getFileName().toString());
    }

    @Test
    void shouldUseCanonicalScenarioSummaryFileName(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReplayScenarioSummary scenario = ReplaySummaryBuilder.summarizeScenario(
                ScenarioProfile.STEADY, "default", List.of(run));

        ReplayReportArtifact artifact = writer.writeScenarioSummary(scenario);

        assertEquals(
                "replay-scenario-summary-STEADY-default.json",
                artifact.scenarioSummaryPaths().get(0).getFileName().toString());
    }

    @Test
    void shouldUseCanonicalSensitivityReportFileName(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReplayRunSummary conservative = summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary aggressive = summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "aggressive",
                List.of(decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10)),
                List.of());
        SensitivityComparison comparison = new SensitivityComparison("run-1", run, conservative, aggressive);

        ReplayReportArtifact artifact = writer.writeSensitivityReport(comparison);

        assertEquals(
                "replay-sensitivity-report-run-1.json",
                artifact.sensitivityReportPath().getFileName().toString());
    }

    @Test
    void shouldUseCanonicalReadinessAssessmentFileName(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReplayRunSummary runRamp = summaryBuilder.build(
                "run-ramp", "scenario-2", ScenarioProfile.RAMP, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary runBurst = summaryBuilder.build(
                "run-burst", "scenario-3", ScenarioProfile.BURST, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());

        MutationReadinessGate gate = new MutationReadinessGate();
        ReadinessAssessment assessment = gate.assess(List.of(run, runRamp, runBurst));

        ReplayReportArtifact artifact = writer.writeReadinessAssessment(assessment);

        assertEquals(
                "readiness-assessment-v0.4.0.json",
                artifact.readinessAssessmentPath().getFileName().toString());
    }

    @Test
    void shouldWriteMinimalFieldsInJsonArtifact(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReadinessAssessment assessment = readinessFor(run);

        ReplayReportArtifact artifact = writer.writeRunSummary(run, assessment);

        String body = Files.readString(artifact.runSummaryPath());
        assertTrue(body.contains("\"runId\""), () -> "expected runId field in: " + body);
        assertTrue(body.contains("\"policyConfigLabel\""), () -> "expected policyConfigLabel field: " + body);
        assertTrue(body.contains("\"evidenceCount\""), () -> "expected evidenceCount field: " + body);
        assertTrue(body.contains("\"holdRatio\""), () -> "expected holdRatio field: " + body);
        assertTrue(body.contains("\"cappedRatio\""), () -> "expected cappedRatio field: " + body);
    }

    @Test
    void shouldNotCopyRawSnapshotEvidence(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReadinessAssessment assessment = readinessFor(run);

        ReplayReportArtifact artifact = writer.writeRunSummary(run, assessment);

        // The raw decision evidence is large and is intentionally excluded from the report
        // body. The summary aggregates are enough for the readiness audit.
        String body = Files.readString(artifact.runSummaryPath());
        assertTrue(!body.contains("snapshotTimestamp"),
                () -> "report must not include raw decision evidence: " + body);
    }

    @Test
    void shouldWriteCompositeMarkdownReport(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReplayRunSummary conservative = summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary aggressive = summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "aggressive",
                List.of(decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10)),
                List.of());
        SensitivityComparison comparison = new SensitivityComparison("run-1", run, conservative, aggressive);
        ReplayRunSummary runRamp = summaryBuilder.build(
                "run-ramp", "scenario-2", ScenarioProfile.RAMP, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary runBurst = summaryBuilder.build(
                "run-burst", "scenario-3", ScenarioProfile.BURST, "default",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());

        MutationReadinessGate gate = new MutationReadinessGate();
        ReadinessAssessment assessment = gate.assess(List.of(run, runRamp, runBurst));

        ReplayReportArtifact artifact = writer.writeCompositeReport(comparison, assessment);

        assertEquals("replay-report-v0.4.0.md", artifact.markdownReportPath().getFileName().toString());
        String body = Files.readString(artifact.markdownReportPath());
        assertTrue(body.contains("Replay Report v0.4.0"));
        assertTrue(body.contains("Readiness"));
    }

    @Test
    void shouldAlwaysResolveToV040Subdirectory(@TempDir Path tempDir) {
        // The writer must always target outputs/reports/v0.4.0/ under
        // the given root, even when the root has additional path segments.
        Path customRoot = tempDir.resolve("elsewhere");
        ReplayReportWriter writer = new ReplayReportWriter(customRoot);
        assertEquals(customRoot.resolve("outputs/reports/v0.4.0"),
                writer.outputDirectory());
    }

    @Test
    void shouldRefuseNullArguments(@TempDir Path tempDir) {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        assertThrows(NullPointerException.class, () -> writer.writeRunSummary(null, null));
        assertThrows(NullPointerException.class, () -> writer.writeRunSummary(run, null));
    }

    @Test
    void shouldExposeAllPathsInArtifact(@TempDir Path tempDir) throws IOException {
        ReplayReportWriter writer = new ReplayReportWriter(tempDir);
        ReplayRunSummary run = sampleRunSummary();
        ReplayScenarioSummary scenario = ReplaySummaryBuilder.summarizeScenario(
                ScenarioProfile.STEADY, "default", List.of(run));
        ReplayRunSummary conservative = summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "conservative",
                List.of(decision(0, PolicyAction.HOLD, GateStatus.HOLD, 8, 8)),
                List.of());
        ReplayRunSummary aggressive = summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "aggressive",
                List.of(decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10)),
                List.of());
        SensitivityComparison comparison = new SensitivityComparison("run-1", run, conservative, aggressive);
        ReadinessAssessment assessment = readinessFor(run);

        ReplayReportArtifact artifact = writer.writeAll(run, scenario, comparison, assessment);

        assertNotNull(artifact.runSummaryPath());
        assertNotNull(artifact.scenarioSummaryPaths());
        assertEquals(1, artifact.scenarioSummaryPaths().size());
        assertNotNull(artifact.sensitivityReportPath());
        assertNotNull(artifact.readinessAssessmentPath());
        assertNotNull(artifact.markdownReportPath());
    }

    private ReplayRunSummary sampleRunSummary() {
        return summaryBuilder.build(
                "run-1", "scenario-1", ScenarioProfile.STEADY, "default",
                List.of(
                        decision(0, PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 8, 10),
                        decision(1, PolicyAction.HOLD, GateStatus.HOLD, 10, 10),
                        decision(2, PolicyAction.HOLD, GateStatus.HOLD, 10, 10)
                ),
                List.of());
    }

    private ReadinessAssessment readinessFor(ReplayRunSummary run) {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of(),
                List.of(),
                "default",
                List.of(run.runId()));
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
