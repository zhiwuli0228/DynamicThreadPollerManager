package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

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
 * Writes replay summary, sensitivity, readiness, and composite
 * markdown artifacts to the controlled output directory
 * {@code outputs/reports/v0.4.0/} under a caller-supplied root.
 *
 * <p>Files follow a fixed naming pattern:
 * <ul>
 *   <li>{@code replay-run-summary-<runId>-<configLabel>.json}</li>
 *   <li>{@code replay-scenario-summary-<scenarioProfile>-<configLabel>.json}</li>
 *   <li>{@code replay-sensitivity-report-<runId>.json}</li>
 *   <li>{@code readiess-assessment-v0.4.0.json}</li>
 *   <li>{@code replay-report-v0.4.0.md}</li>
 * </ul>
 *
 * <p>Raw snapshot evidence is intentionally not copied; the report
 * only contains aggregated counters and ratios to keep the report
 * directory compact and reviewable.
 */
public final class ReplayReportWriter {

    public static final String OUTPUT_DIRECTORY = "outputs/reports/v0.4.0";

    private final Path outputDirectory;

    public ReplayReportWriter(Path outputRoot) {
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");
        Path resolved = outputRoot.resolve(OUTPUT_DIRECTORY);
        if (!resolved.startsWith(outputRoot.resolve("outputs/reports/v0.4.0"))
                && !resolved.equals(outputRoot.resolve(OUTPUT_DIRECTORY))) {
            throw new IllegalArgumentException(
                    "writer must target outputs/reports/v0.4.0 under " + outputRoot);
        }
        this.outputDirectory = resolved;
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public ReplayReportArtifact writeRunSummary(ReplayRunSummary run,
                                                ReadinessAssessment assessment) {
        Objects.requireNonNull(run, "run must not be null");
        Objects.requireNonNull(assessment, "assessment must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                "replay-run-summary-" + run.runId() + "-" + run.policyConfigLabel() + ".json");
        writeText(path, MinimalJsonWriter.render(runSummaryJson(run, assessment)));
        return new ReplayReportArtifact(path, null, null, null, null);
    }

    public ReplayReportArtifact writeScenarioSummary(ReplayScenarioSummary scenario) {
        Objects.requireNonNull(scenario, "scenario must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                "replay-scenario-summary-" + scenario.scenarioProfile().name()
                        + "-" + scenario.policyConfigLabel() + ".json");
        writeText(path, MinimalJsonWriter.render(scenarioSummaryJson(scenario)));
        return new ReplayReportArtifact(null, List.of(path), null, null, null);
    }

    public ReplayReportArtifact writeSensitivityReport(SensitivityComparison comparison) {
        Objects.requireNonNull(comparison, "comparison must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve(
                "replay-sensitivity-report-" + comparison.runId() + ".json");
        writeText(path, MinimalJsonWriter.render(sensitivityJson(comparison)));
        return new ReplayReportArtifact(null, null, path, null, null);
    }

    public ReplayReportArtifact writeReadinessAssessment(ReadinessAssessment assessment) {
        Objects.requireNonNull(assessment, "assessment must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve("readiness-assessment-v0.4.0.json");
        writeText(path, MinimalJsonWriter.render(assessmentJson(assessment)));
        return new ReplayReportArtifact(null, null, null, path, null);
    }

    public ReplayReportArtifact writeCompositeReport(SensitivityComparison comparison,
                                                     ReadinessAssessment assessment) {
        Objects.requireNonNull(comparison, "comparison must not be null");
        Objects.requireNonNull(assessment, "assessment must not be null");
        ensureDirectory();
        Path path = outputDirectory.resolve("replay-report-v0.4.0.md");
        writeText(path, renderMarkdown(comparison, assessment));
        return new ReplayReportArtifact(null, null, null, null, path);
    }

    public ReplayReportArtifact writeAll(ReplayRunSummary run,
                                         ReplayScenarioSummary scenario,
                                         SensitivityComparison comparison,
                                         ReadinessAssessment assessment) {
        ensureDirectory();
        Path runPath = writeSinglePath(MinimalJsonWriter.render(runSummaryJson(run, assessment)),
                "replay-run-summary-" + run.runId() + "-" + run.policyConfigLabel() + ".json");
        Path scenarioPath = writeSinglePath(MinimalJsonWriter.render(scenarioSummaryJson(scenario)),
                "replay-scenario-summary-" + scenario.scenarioProfile().name()
                        + "-" + scenario.policyConfigLabel() + ".json");
        Path sensitivityPath = writeSinglePath(MinimalJsonWriter.render(sensitivityJson(comparison)),
                "replay-sensitivity-report-" + comparison.runId() + ".json");
        Path readinessPath = writeSinglePath(MinimalJsonWriter.render(assessmentJson(assessment)),
                "readiness-assessment-v0.4.0.json");
        Path markdownPath = writeSinglePath(renderMarkdown(comparison, assessment),
                "replay-report-v0.4.0.md");
        return new ReplayReportArtifact(
                runPath, List.of(scenarioPath), sensitivityPath, readinessPath, markdownPath);
    }

    private Path writeSinglePath(String body, String name) {
        Path path = outputDirectory.resolve(name);
        writeText(path, body);
        return path;
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

    private static Map<String, Object> runSummaryJson(ReplayRunSummary run,
                                                     ReadinessAssessment assessment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("runId", run.runId());
        map.put("scenarioId", run.scenarioId());
        map.put("scenarioProfile", run.scenarioProfile().name());
        map.put("policyConfigLabel", run.policyConfigLabel());
        map.put("evidenceCount", run.evidenceCount());
        map.put("decisionCount", run.decisionCount());
        map.put("skippedCount", run.skippedCount());
        map.put("scaleUpCount", run.scaleUpCount());
        map.put("scaleDownCount", run.scaleDownCount());
        map.put("holdCount", run.holdCount());
        map.put("acceptedCount", run.acceptedCount());
        map.put("cappedCount", run.cappedCount());
        map.put("gateHoldCount", run.gateHoldCount());
        map.put("rejectedCount", run.rejectedCount());
        map.put("directionFlipCount", run.directionFlipCount());
        map.put("alternatingStreakMax", run.alternatingStreakMax());
        map.put("holdRatio", run.holdRatio());
        map.put("cappedRatio", run.cappedRatio());
        map.put("skippedReasons", run.skippedReasons());
        map.put("selectedConfigLabel", assessment.selectedConfigLabel());
        return map;
    }

    private static Map<String, Object> scenarioSummaryJson(ReplayScenarioSummary scenario) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scenarioProfile", scenario.scenarioProfile().name());
        map.put("policyConfigLabel", scenario.policyConfigLabel());
        map.put("runIds", scenario.runIds());
        map.put("totalEvidenceCount", scenario.totalEvidenceCount());
        map.put("totalDecisionCount", scenario.totalDecisionCount());
        map.put("totalSkippedCount", scenario.totalSkippedCount());
        map.put("aggregateDirectionFlipCount", scenario.aggregateDirectionFlipCount());
        map.put("aggregateAlternatingStreakMax", scenario.aggregateAlternatingStreakMax());
        map.put("aggregateHoldRatio", scenario.aggregateHoldRatio());
        map.put("aggregateCappedRatio", scenario.aggregateCappedRatio());
        return map;
    }

    private static Map<String, Object> sensitivityJson(SensitivityComparison comparison) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("runId", comparison.runId());
        map.put("default", runSummaryMap(comparison.defaultSummary()));
        map.put("conservative", runSummaryMap(comparison.conservativeSummary()));
        map.put("aggressive", runSummaryMap(comparison.aggressiveSummary()));
        map.put("conservativeDeltaVsDefault", deltaMap(comparison.conservativeDeltaVsDefault()));
        map.put("aggressiveDeltaVsDefault", deltaMap(comparison.aggressiveDeltaVsDefault()));
        return map;
    }

    private static Map<String, Object> runSummaryMap(ReplayRunSummary run) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scaleUpCount", run.scaleUpCount());
        map.put("scaleDownCount", run.scaleDownCount());
        map.put("holdCount", run.holdCount());
        map.put("acceptedCount", run.acceptedCount());
        map.put("cappedCount", run.cappedCount());
        map.put("directionFlipCount", run.directionFlipCount());
        map.put("alternatingStreakMax", run.alternatingStreakMax());
        map.put("holdRatio", run.holdRatio());
        map.put("cappedRatio", run.cappedRatio());
        return map;
    }

    private static Map<String, Object> deltaMap(SensitivityComparison.SensitivityDelta delta) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scaleUpCountDelta", delta.scaleUpCountDelta());
        map.put("scaleDownCountDelta", delta.scaleDownCountDelta());
        map.put("holdCountDelta", delta.holdCountDelta());
        map.put("acceptedCountDelta", delta.acceptedCountDelta());
        map.put("cappedCountDelta", delta.cappedCountDelta());
        map.put("directionFlipCountDelta", delta.directionFlipCountDelta());
        map.put("alternatingStreakMaxDelta", delta.alternatingStreakMaxDelta());
        map.put("holdRatioDelta", delta.holdRatioDelta());
        map.put("cappedRatioDelta", delta.cappedRatioDelta());
        return map;
    }

    private static Map<String, Object> assessmentJson(ReadinessAssessment assessment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", assessment.status().name());
        map.put("selectedConfigLabel", assessment.selectedConfigLabel());
        List<String> evaluated = new ArrayList<>();
        for (var profile : assessment.evaluatedScenarioProfiles()) {
            evaluated.add(profile.name());
        }
        List<String> missing = new ArrayList<>();
        for (var profile : assessment.missingScenarioProfiles()) {
            missing.add(profile.name());
        }
        map.put("evaluatedScenarioProfiles", evaluated);
        map.put("missingScenarioProfiles", missing);
        map.put("blockingReasons", assessment.blockingReasons());
        map.put("riskReasons", assessment.riskReasons());
        map.put("inputRunIds", assessment.inputRunIds());
        return map;
    }

    private static String renderMarkdown(SensitivityComparison comparison,
                                         ReadinessAssessment assessment) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Replay Report v0.4.0\n\n");
        sb.append("## Sensitivity Comparison (run ").append(comparison.runId()).append(")\n\n");
        sb.append("| Config | scaleUp | scaleDown | hold | accepted | capped | flips | streak | holdRatio | cappedRatio |\n");
        sb.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        appendMarkdownRow(sb, comparison.defaultSummary());
        appendMarkdownRow(sb, comparison.conservativeSummary());
        appendMarkdownRow(sb, comparison.aggressiveSummary());
        sb.append('\n');
        sb.append("### Conservative vs Default\n\n");
        appendMarkdownDelta(sb, comparison.conservativeDeltaVsDefault());
        sb.append('\n');
        sb.append("### Aggressive vs Default\n\n");
        appendMarkdownDelta(sb, comparison.aggressiveDeltaVsDefault());
        sb.append('\n');

        sb.append("## Readiness Assessment\n\n");
        sb.append("- status: **").append(assessment.status().name()).append("**\n");
        sb.append("- selectedConfigLabel: ").append(assessment.selectedConfigLabel()).append('\n');
        sb.append("- evaluatedScenarioProfiles: ");
        sb.append(assessment.evaluatedScenarioProfiles()).append('\n');
        sb.append("- missingScenarioProfiles: ");
        sb.append(assessment.missingScenarioProfiles()).append('\n');
        sb.append("- inputRunIds: ").append(assessment.inputRunIds()).append('\n');
        if (!assessment.riskReasons().isEmpty()) {
            sb.append("- riskReasons:\n");
            for (String reason : assessment.riskReasons()) {
                sb.append("  - ").append(reason).append('\n');
            }
        }
        if (!assessment.blockingReasons().isEmpty()) {
            sb.append("- blockingReasons:\n");
            for (String reason : assessment.blockingReasons()) {
                sb.append("  - ").append(reason).append('\n');
            }
        }
        return sb.toString();
    }

    private static void appendMarkdownRow(StringBuilder sb, ReplayRunSummary run) {
        sb.append("| ").append(run.policyConfigLabel())
                .append(" | ").append(run.scaleUpCount())
                .append(" | ").append(run.scaleDownCount())
                .append(" | ").append(run.holdCount())
                .append(" | ").append(run.acceptedCount())
                .append(" | ").append(run.cappedCount())
                .append(" | ").append(run.directionFlipCount())
                .append(" | ").append(run.alternatingStreakMax())
                .append(" | ").append(String.format("%.3f", run.holdRatio()))
                .append(" | ").append(String.format("%.3f", run.cappedRatio()))
                .append(" |\n");
    }

    private static void appendMarkdownDelta(StringBuilder sb, SensitivityComparison.SensitivityDelta delta) {
        sb.append("- scaleUpCountDelta: ").append(delta.scaleUpCountDelta()).append('\n');
        sb.append("- scaleDownCountDelta: ").append(delta.scaleDownCountDelta()).append('\n');
        sb.append("- holdCountDelta: ").append(delta.holdCountDelta()).append('\n');
        sb.append("- acceptedCountDelta: ").append(delta.acceptedCountDelta()).append('\n');
        sb.append("- cappedCountDelta: ").append(delta.cappedCountDelta()).append('\n');
        sb.append("- directionFlipCountDelta: ").append(delta.directionFlipCountDelta()).append('\n');
        sb.append("- alternatingStreakMaxDelta: ").append(delta.alternatingStreakMaxDelta()).append('\n');
        sb.append("- holdRatioDelta: ").append(String.format("%.3f", delta.holdRatioDelta())).append('\n');
        sb.append("- cappedRatioDelta: ").append(String.format("%.3f", delta.cappedRatioDelta())).append('\n');
    }
}
