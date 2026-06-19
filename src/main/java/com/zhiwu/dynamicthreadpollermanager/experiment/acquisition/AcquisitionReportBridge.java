package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioDefinition;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioRunOutcome;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bridge that aggregates a single acquisition run into its four
 * versioned report artifacts (manifest, pressure summary, replay
 * summary, evidence index). No {@link ReadinessSummary} is
 * produced — the bridge operates in acquisition-only mode.
 *
 * <p>The bridge is pure: it never carries runtime mutation
 * authorization and never accesses the executor directly.
 */
public final class AcquisitionReportBridge {

    private final AcquisitionReportPaths paths;
    private final AcquisitionReportWriter writer;

    public AcquisitionReportBridge(Path outputRoot, String versionTag) {
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");
        Objects.requireNonNull(versionTag, "versionTag must not be null");
        this.paths = AcquisitionReportPaths.forVersion(versionTag);
        this.writer = new AcquisitionReportWriter(outputRoot, paths);
    }

    public AcquisitionReportArtifact bridge(ScenarioRunOutcome outcome,
                                             ScenarioDefinition definition,
                                             ManagedExecutorConfig config,
                                             List<ObservedSnapshot> snapshots) {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(snapshots, "snapshots must not be null");

        RunManifest manifest = buildManifest(outcome, definition, config);
        PressureSummary pressureSummary = buildPressureSummary(outcome.runId(), definition, snapshots);
        ReplaySummary replaySummary = buildReplaySummary(outcome.runId(), definition);
        EvidenceIndex evidenceIndex = buildEvidenceIndex(outcome.runId());

        Path manifestPath = writer.writeRunManifest(manifest);
        Path pressurePath = writer.writePressureSummary(pressureSummary);
        Path replayPath = writer.writeReplaySummary(replaySummary);
        Path indexPath = writer.writeEvidenceIndex(evidenceIndex);

        return new AcquisitionReportArtifact(
                manifestPath, pressurePath, replayPath, null, indexPath);
    }

    private RunManifest buildManifest(ScenarioRunOutcome outcome,
                                       ScenarioDefinition definition,
                                       ManagedExecutorConfig config) {
        return new RunManifest(
                outcome.runId(),
                definition.scenarioId(),
                definition.profile(),
                definition.seed(),
                definition.stepCount(),
                config.toPresetSummary().policyId(),
                config.toPresetSummary(),
                environmentSummary(),
                List.of(),
                Instant.now());
    }

    private PressureSummary buildPressureSummary(String runId,
                                                   ScenarioDefinition definition,
                                                   List<ObservedSnapshot> snapshots) {
        int total = snapshots.size();
        List<PressureSummary.ProfileCount> profileCounts = List.of(
                new PressureSummary.ProfileCount(definition.profile(), total));

        double peak = 0.0;
        double sum = 0.0;
        for (ObservedSnapshot ss : snapshots) {
            int qs = ss.snapshot().queueSize();
            if (qs > peak) peak = qs;
            sum += qs;
        }
        double mean = total > 0 ? sum / total : 0.0;

        return new PressureSummary(
                runId, definition.profile(), total, profileCounts,
                0, 0, 0, peak, mean);
    }

    private ReplaySummary buildReplaySummary(String runId, ScenarioDefinition definition) {
        return new ReplaySummary(
                runId,
                definition.scenarioId(),
                definition.profile(),
                "acquisition-only-v0.8.0",
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0.0, 0.0,
                List.of());
    }

    private EvidenceIndex buildEvidenceIndex(String runId) {
        String reportDir = paths.outputDirectory();
        return new EvidenceIndex(
                runId,
                reportDir,
                AcquisitionReportPaths.runManifestFileName(runId),
                AcquisitionReportPaths.pressureSummaryFileName(runId),
                AcquisitionReportPaths.replaySummaryFileName(runId),
                "",
                RetentionRecord.defaultNonVersioned(runId));
    }

    private static Map<String, String> environmentSummary() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("java.version", System.getProperty("java.version"));
        env.put("os.name", System.getProperty("os.name"));
        env.put("availableProcessors",
                String.valueOf(Runtime.getRuntime().availableProcessors()));
        return env;
    }
}
