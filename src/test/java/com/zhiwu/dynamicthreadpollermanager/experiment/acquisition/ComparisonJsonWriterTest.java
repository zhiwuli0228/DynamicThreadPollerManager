package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonJsonWriterTest {

    @TempDir
    Path tempDir;

    private static ScenarioRunOutcome createOutcome(String runId) {
        return new ScenarioRunOutcome(runId, "test", "policy", 10, 10L, 10, RunState.FINALIZED);
    }

    private ComparisonReportArtifact createTestArtifact() {
        CommonExecutorPreset preset = new CommonExecutorPreset(
                "fixed-4", "FIXED_THREAD_POOL", 4, 4, -1, "test preset");
        ManagedExecutorConfig config = new ManagedExecutorConfig(
                2, 4, 10, 60, TimeUnit.SECONDS);

        NormalizedComparisonMetrics bMetrics = new NormalizedComparisonMetrics(
                100L, 0L, 2.5, 8, 5000L, 20.0, 1.5, 4, 10);
        NormalizedComparisonMetrics mMetrics = new NormalizedComparisonMetrics(
                95L, 3L, 4.0, 10, 5200L, 18.3, 2.0, 4, 10);

        Map<String, MetricDelta> deltas = new LinkedHashMap<>();
        deltas.put("completedTaskCount", MetricDelta.compute("completedTaskCount", 100.0, 95.0, true));
        deltas.put("throughputPerSecond", MetricDelta.compute("throughputPerSecond", 20.0, 18.3, true));

        ComparisonResult result = new ComparisonResult(
                "cmp-001", "test-scenario", "fixed-4", "managed-default",
                createOutcome("run-baseline"), createOutcome("run-managed"),
                bMetrics, mMetrics, deltas, Instant.now());

        return new ComparisonReportArtifact(
                "cmp-001", "test-scenario", Instant.now(),
                preset, config, result, "Managed executor shows slight regression in throughput");
    }

    @Test
    void writeShouldProduceFile() {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        ComparisonReportArtifact artifact = createTestArtifact();
        Path outputPath = tempDir.resolve("test-output");

        String path = writer.writeComparisonReport(artifact, outputPath);

        assertTrue(java.nio.file.Files.exists(Path.of(path)));
    }

    @Test
    void roundTripShouldPreserveAllFields() {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        ComparisonReportArtifact original = createTestArtifact();
        Path outputPath = tempDir.resolve("roundtrip-test.json");

        writer.writeComparisonReport(original, outputPath);
        ComparisonReportArtifact restored = writer.readComparisonReport(outputPath);

        assertEquals(original.comparisonId(), restored.comparisonId());
        assertEquals(original.scenarioId(), restored.scenarioId());
        assertEquals(original.baselinePreset().presetId(), restored.baselinePreset().presetId());
        assertEquals(original.conclusion(), restored.conclusion());

        assertEquals(original.result().baselineMetrics().completedTaskCount(),
                restored.result().baselineMetrics().completedTaskCount());
        assertEquals(original.result().managedMetrics().rejectedTaskCount(),
                restored.result().managedMetrics().rejectedTaskCount());
    }

    @Test
    void roundTripShouldPreserveMetrics() {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        ComparisonReportArtifact original = createTestArtifact();
        Path outputPath = tempDir.resolve("metrics-test.json");

        writer.writeComparisonReport(original, outputPath);
        ComparisonReportArtifact restored = writer.readComparisonReport(outputPath);

        NormalizedComparisonMetrics origB = original.result().baselineMetrics();
        NormalizedComparisonMetrics restB = restored.result().baselineMetrics();
        assertEquals(origB.throughputPerSecond(), restB.throughputPerSecond(), 0.01);
        assertEquals(origB.avgQueueDepth(), restB.avgQueueDepth(), 0.01);
        assertEquals(origB.maxPoolSize(), restB.maxPoolSize());
        assertEquals(origB.snapshotCount(), restB.snapshotCount());
    }

    @Test
    void roundTripShouldPreserveDeltas() {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        ComparisonReportArtifact original = createTestArtifact();
        Path outputPath = tempDir.resolve("deltas-test.json");

        writer.writeComparisonReport(original, outputPath);
        ComparisonReportArtifact restored = writer.readComparisonReport(outputPath);

        assertEquals(original.result().deltas().size(), restored.result().deltas().size());
        assertTrue(restored.result().deltas().containsKey("completedTaskCount"));
        assertTrue(restored.result().deltas().containsKey("throughputPerSecond"));
    }

    @Test
    void writeShouldProduceValidJson(@TempDir Path jsonTempDir) {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        ComparisonReportArtifact artifact = createTestArtifact();
        Path outputPath = jsonTempDir.resolve("valid-json-test.json");

        String path = writer.writeComparisonReport(artifact, outputPath);
        String content;
        try {
            content = java.nio.file.Files.readString(Path.of(path));
        } catch (Exception e) {
            fail("Failed to read output file", e);
            return;
        }

        // Verify it's valid JSON by parsing
        Object parsed = AcquisitionJsonWriter.parse(content);
        assertNotNull(parsed);
        assertTrue(parsed instanceof Map);
    }

    @Test
    void readInvalidJsonShouldThrow() {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        Path invalidPath = tempDir.resolve("invalid.json");
        try {
            java.nio.file.Files.writeString(invalidPath, "not valid json {{{");
        } catch (Exception e) {
            fail("Failed to write test file", e);
        }

        assertThrows(RuntimeException.class, () -> writer.readComparisonReport(invalidPath));
    }

    @Test
    void conclusionNullShouldRoundTrip() {
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        ComparisonReportArtifact artifact = new ComparisonReportArtifact(
                "cmp-002", "test", Instant.now(),
                createTestArtifact().baselinePreset(),
                createTestArtifact().managedConfig(),
                createTestArtifact().result(),
                null);
        Path outputPath = tempDir.resolve("null-conclusion.json");

        writer.writeComparisonReport(artifact, outputPath);
        ComparisonReportArtifact restored = writer.readComparisonReport(outputPath);

        assertNull(restored.conclusion());
    }
}
