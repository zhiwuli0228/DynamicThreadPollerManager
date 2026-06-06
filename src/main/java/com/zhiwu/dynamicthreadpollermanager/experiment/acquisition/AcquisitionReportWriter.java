package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes acquisition report artifacts to the versioned output
 * directory {@code outputs/reports/v0.6.0/} under a caller-supplied
 * root. Each artifact is written as a standalone JSON file; a
 * composite markdown report is also emitted for human review.
 *
 * <p>The writer is intentionally pure: it serializes the
 * contract models produced by the acquisition boundary and
 * never carries any runtime mutation authorization.
 */
public final class AcquisitionReportWriter {

    private final Path outputDirectory;

    public AcquisitionReportWriter(Path outputRoot) {
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");
        this.outputDirectory = outputRoot.resolve(AcquisitionReportPaths.OUTPUT_DIRECTORY);
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public Path writeRunManifest(RunManifest manifest) {
        Objects.requireNonNull(manifest, "manifest must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                AcquisitionReportPaths.runManifestFileName(manifest.runId()));
        writeText(path, AcquisitionJsonWriter.render(manifestJson(manifest)));
        return path;
    }

    public Path writePressureSummary(PressureSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                AcquisitionReportPaths.pressureSummaryFileName(summary.runId()));
        writeText(path, AcquisitionJsonWriter.render(pressureSummaryJson(summary)));
        return path;
    }

    public Path writeReplaySummary(ReplaySummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                AcquisitionReportPaths.replaySummaryFileName(summary.runId()));
        writeText(path, AcquisitionJsonWriter.render(replaySummaryJson(summary)));
        return path;
    }

    public Path writeReadinessSummary(ReadinessSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                AcquisitionReportPaths.readinessSummaryFileName(summary.runId()));
        writeText(path, AcquisitionJsonWriter.render(readinessSummaryJson(summary)));
        return path;
    }

    public Path writeEvidenceIndex(EvidenceIndex index) {
        Objects.requireNonNull(index, "index must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                AcquisitionReportPaths.evidenceIndexFileName(index.runId()));
        writeText(path, AcquisitionJsonWriter.render(evidenceIndexJson(index)));
        return path;
    }

    public AcquisitionReportArtifact writeAll(RunManifest manifest,
                                               PressureSummary pressureSummary,
                                               ReplaySummary replaySummary,
                                               ReadinessSummary readinessSummary,
                                               EvidenceIndex evidenceIndex) {
        Objects.requireNonNull(manifest, "manifest must not be null");
        Objects.requireNonNull(pressureSummary, "pressureSummary must not be null");
        Objects.requireNonNull(replaySummary, "replaySummary must not be null");
        Objects.requireNonNull(readinessSummary, "readinessSummary must not be null");
        Objects.requireNonNull(evidenceIndex, "evidenceIndex must not be null");
        ensureDirectory();
        Path manifestPath = writeRunManifest(manifest);
        Path pressurePath = writePressureSummary(pressureSummary);
        Path replayPath = writeReplaySummary(replaySummary);
        Path readinessPath = writeReadinessSummary(readinessSummary);
        Path indexPath = writeEvidenceIndex(evidenceIndex);
        return new AcquisitionReportArtifact(
                manifestPath, pressurePath, replayPath, readinessPath, indexPath);
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "failed to create output directory " + outputDirectory, e);
        }
    }

    private void writeText(Path path, String body) {
        try {
            Files.writeString(path, body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to write artifact " + path, e);
        }
    }

    private static Map<String, Object> manifestJson(RunManifest m) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("runId", m.runId());
        map.put("scenarioId", m.scenarioId());
        map.put("scenarioProfile", m.scenarioProfile().name());
        map.put("seed", m.seed());
        map.put("stepCount", m.stepCount());
        map.put("baselinePolicyId", m.baselinePolicyId());
        map.put("baselinePreset", presetMap(m.baselinePreset()));
        map.put("environmentSummary", m.environmentSummary());
        map.put("commandLine", m.commandLine());
        map.put("createdAt", m.createdAt().toString());
        return map;
    }

    private static Map<String, Object> presetMap(RunManifest.BaselinePresetSummary p) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("policyId", p.policyId());
        map.put("corePoolSize", p.corePoolSize());
        map.put("maximumPoolSize", p.maximumPoolSize());
        map.put("queueCapacity", p.queueCapacity());
        return map;
    }

    private static Map<String, Object> pressureSummaryJson(PressureSummary s) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("runId", s.runId());
        map.put("scenarioProfile", s.scenarioProfile().name());
        map.put("totalSnapshotCount", s.totalSnapshotCount());
        map.put("profileSnapshotCounts", profileCountList(s.profileSnapshotCounts()));
        map.put("scaleEventCount", s.scaleEventCount());
        map.put("scaleUpCount", s.scaleUpCount());
        map.put("scaleDownCount", s.scaleDownCount());
        map.put("peakObservedQueueDepth", s.peakObservedQueueDepth());
        map.put("meanObservedQueueDepth", s.meanObservedQueueDepth());
        return map;
    }

    private static List<Map<String, Object>> profileCountList(
            List<PressureSummary.ProfileCount> counts) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PressureSummary.ProfileCount pc : counts) {
            Map<String, Object> map = AcquisitionJsonWriter.map();
            map.put("profile", pc.profile().name());
            map.put("count", pc.count());
            list.add(map);
        }
        return list;
    }

    private static Map<String, Object> replaySummaryJson(ReplaySummary r) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("runId", r.runId());
        map.put("scenarioId", r.scenarioId());
        map.put("scenarioProfile", r.scenarioProfile().name());
        map.put("policyConfigLabel", r.policyConfigLabel());
        map.put("evidenceCount", r.evidenceCount());
        map.put("decisionCount", r.decisionCount());
        map.put("skippedCount", r.skippedCount());
        map.put("scaleUpCount", r.scaleUpCount());
        map.put("scaleDownCount", r.scaleDownCount());
        map.put("holdCount", r.holdCount());
        map.put("acceptedCount", r.acceptedCount());
        map.put("cappedCount", r.cappedCount());
        map.put("rejectedCount", r.rejectedCount());
        map.put("holdRatio", r.holdRatio());
        map.put("cappedRatio", r.cappedRatio());
        map.put("skippedReasons", r.skippedReasons());
        return map;
    }

    private static Map<String, Object> readinessSummaryJson(ReadinessSummary r) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("runId", r.runId());
        map.put("status", r.status().name());
        List<String> evaluated = new ArrayList<>();
        for (var p : r.evaluatedScenarioProfiles()) {
            evaluated.add(p.name());
        }
        map.put("evaluatedScenarioProfiles", evaluated);
        List<String> missing = new ArrayList<>();
        for (var p : r.missingScenarioProfiles()) {
            missing.add(p.name());
        }
        map.put("missingScenarioProfiles", missing);
        map.put("blockingReasons", r.blockingReasons());
        map.put("riskReasons", r.riskReasons());
        map.put("recommendedNextStep", r.recommendedNextStep());
        return map;
    }

    private static Map<String, Object> evidenceIndexJson(EvidenceIndex e) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("runId", e.runId());
        map.put("reportDirectory", e.reportDirectory());
        map.put("runManifestPath", e.runManifestPath());
        map.put("pressureSummaryPath", e.pressureSummaryPath());
        map.put("replaySummaryPath", e.replaySummaryPath());
        map.put("readinessSummaryPath", e.readinessSummaryPath());
        map.put("retention", retentionJson(e.retention()));
        return map;
    }

    private static Map<String, Object> retentionJson(RetentionRecord r) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("runId", r.runId());
        map.put("policy", RetentionRecord.DEFAULT_POLICY);
        map.put("isRetained", r.isRetained());
        if (r.isRetained()) {
            map.put("retentionLocation", r.retentionLocation());
            map.put("responsibleOwner", r.responsibleOwner());
            map.put("retainedAt", r.retainedAt() != null ? r.retainedAt().toString() : null);
            map.put("cleanupPlan", r.cleanupPlan());
        }
        return map;
    }
}
