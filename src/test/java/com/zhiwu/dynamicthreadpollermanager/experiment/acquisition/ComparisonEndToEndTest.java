package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonEndToEndTest {

    private final Supplier<Instant> clock = Instant::now;

    @Test
    void e2eCompareReportReadback(@TempDir Path tempDir) {
        // Step 1: Catalog
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        assertEquals(6, catalog.size());

        // Step 2: Compare
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = new ScenarioDefinition(
                "e2e-scenario", ScenarioProfile.STEADY, 0L, 10, 1, "");
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result = runner.compare(scenario, "fixed-4", config);

        assertNotNull(result);
        assertEquals(9, result.deltas().size());

        // Step 3: Build artifact
        ComparisonReportArtifact artifact = new ComparisonReportArtifact(
                result.comparisonId(),
                result.scenarioId(),
                result.createdAt(),
                catalog.get("fixed-4"),
                config,
                result,
                "End-to-end test: baseline fixed-4 vs managed executor"
        );

        // Step 4: Write report
        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        Path reportPath = tempDir.resolve("e2e-report.json");
        writer.writeComparisonReport(artifact, reportPath);

        // Step 5: Read back
        ComparisonReportArtifact restored = writer.readComparisonReport(reportPath);

        // Step 6: Verify
        assertEquals(artifact.comparisonId(), restored.comparisonId());
        assertEquals(artifact.scenarioId(), restored.scenarioId());
        assertEquals(artifact.baselinePreset().presetId(), restored.baselinePreset().presetId());
        assertEquals(artifact.conclusion(), restored.conclusion());

        assertEquals(artifact.result().baselineMetrics().completedTaskCount(),
                restored.result().baselineMetrics().completedTaskCount());
        assertEquals(artifact.result().managedMetrics().completedTaskCount(),
                restored.result().managedMetrics().completedTaskCount());
        assertEquals(9, restored.result().deltas().size());

        // Step 7: Verify deltas
        assertTrue(restored.result().deltas().containsKey("throughputPerSecond"));
        assertTrue(restored.result().deltas().containsKey("rejectedTaskCount"));
        MetricDelta throughputDelta = restored.result().deltas().get("throughputPerSecond");
        assertNotNull(throughputDelta.direction());
    }

    @Test
    void e2eRegressionResultPreserved(@TempDir Path tempDir) {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = new ScenarioDefinition(
                "regression-test", ScenarioProfile.STEADY, 0L, 5, 1, "");
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result = runner.compare(scenario, "fixed-8", config);

        ComparisonReportArtifact artifact = new ComparisonReportArtifact(
                result.comparisonId(), result.scenarioId(), result.createdAt(),
                catalog.get("fixed-8"), config, result,
                "Regression test: managed may regress against fixed-8");

        ComparisonJsonWriter writer = new ComparisonJsonWriter(
                AcquisitionReportPaths.forVersion("v0.12.0"));
        Path reportPath = tempDir.resolve("regression-report.json");
        writer.writeComparisonReport(artifact, reportPath);

        ComparisonReportArtifact restored = writer.readComparisonReport(reportPath);

        // Verify all deltas present — including regression direction
        MetricDelta tpDelta = restored.result().deltas().get("throughputPerSecond");
        assertNotNull(tpDelta);
        // direction could be IMPROVED, REGRESSED, or NEUTRAL — all are valid evidence
        assertTrue(tpDelta.direction().equals("IMPROVED")
                || tpDelta.direction().equals("REGRESSED")
                || tpDelta.direction().equals("NEUTRAL"));
    }

    @Test
    void e2eComparisonReportFileNaming(@TempDir Path tempDir) {
        String comparisonId = "test-cmp-123";
        String fileName = AcquisitionReportPaths.comparisonReportFileName(comparisonId);
        assertEquals("test-cmp-123-comparison.json", fileName);

        Path filePath = AcquisitionReportPaths.comparisonReportFile(tempDir, comparisonId);
        assertTrue(filePath.toString().contains("outputs") && filePath.toString().contains("reports")
                && filePath.toString().contains("v0.12.0"));
        assertTrue(filePath.toString().endsWith("test-cmp-123-comparison.json"));
    }
}
