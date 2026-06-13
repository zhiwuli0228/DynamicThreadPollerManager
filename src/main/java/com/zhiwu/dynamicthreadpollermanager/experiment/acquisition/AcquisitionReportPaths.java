package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Centralized naming and path rules for acquisition
 * report artifacts. The class exposes deterministic,
 * file-system-safe names and supports versioned output
 * directories via {@link #forVersion(String)}.
 *
 * <p>Naming convention:
 * <ul>
 *   <li>{@code run-manifest-<runId>.json}</li>
 *   <li>{@code pressure-summary-<runId>.json}</li>
 *   <li>{@code replay-summary-<runId>.json}</li>
 *   <li>{@code evidence-index-<runId>.json}</li>
 *   <li>{@code readiness-summary-<runId>.json}</li>
 *   <li>{@code acquisition-report-<runId>.md}</li>
 * </ul>
 */
public final class AcquisitionReportPaths {

    public static final String OUTPUT_DIRECTORY = "outputs/reports/v0.6.0";
    public static final String VERSION_TAG = "v0.6.0";

    private final String outputDirectory;
    private final String versionTag;

    private AcquisitionReportPaths() {
        this.outputDirectory = OUTPUT_DIRECTORY;
        this.versionTag = VERSION_TAG;
    }

    private AcquisitionReportPaths(String versionTag) {
        this.versionTag = versionTag;
        this.outputDirectory = "outputs/reports/" + versionTag;
    }

    public static AcquisitionReportPaths forVersion(String versionTag) {
        if (versionTag == null || versionTag.isBlank()) {
            throw new IllegalArgumentException("versionTag must not be null or blank");
        }
        if (versionTag.contains("/") || versionTag.contains("\\")
                || versionTag.contains("..")) {
            throw new IllegalArgumentException(
                    "versionTag must not contain path separators or traversal");
        }
        return new AcquisitionReportPaths(versionTag);
    }

    public String outputDirectory() { return outputDirectory; }
    public String versionTag() { return versionTag; }

    public static Path reportDirectory(Path outputRoot) {
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");
        return outputRoot.resolve(OUTPUT_DIRECTORY);
    }

    public static String runManifestFileName(String runId) {
        return requireSafeRunId(runId, "runId") + ".json"
                .replace(".json", "-run-manifest.json");
    }

    public static String pressureSummaryFileName(String runId) {
        return requireSafeRunId(runId, "runId") + ".json"
                .replace(".json", "-pressure-summary.json");
    }

    public static String replaySummaryFileName(String runId) {
        return requireSafeRunId(runId, "runId") + ".json"
                .replace(".json", "-replay-summary.json");
    }

    public static String evidenceIndexFileName(String runId) {
        return requireSafeRunId(runId, "runId") + ".json"
                .replace(".json", "-evidence-index.json");
    }

    public static String readinessSummaryFileName(String runId) {
        return requireSafeRunId(runId, "runId") + ".json"
                .replace(".json", "-readiness-summary.json");
    }

    public static String compositeReportFileName(String runId) {
        return requireSafeRunId(runId, "runId") + ".md"
                .replace(".md", "-acquisition-report.md");
    }

    public static Path runManifest(Path outputRoot, String runId) {
        return reportDirectory(outputRoot).resolve(runManifestFileName(runId));
    }

    public static Path pressureSummary(Path outputRoot, String runId) {
        return reportDirectory(outputRoot).resolve(pressureSummaryFileName(runId));
    }

    public static Path replaySummary(Path outputRoot, String runId) {
        return reportDirectory(outputRoot).resolve(replaySummaryFileName(runId));
    }

    public static Path evidenceIndex(Path outputRoot, String runId) {
        return reportDirectory(outputRoot).resolve(evidenceIndexFileName(runId));
    }

    public static Path readinessSummary(Path outputRoot, String runId) {
        return reportDirectory(outputRoot).resolve(readinessSummaryFileName(runId));
    }

    public static Path compositeReport(Path outputRoot, String runId) {
        return reportDirectory(outputRoot).resolve(compositeReportFileName(runId));
    }

    public static String evidenceFileName(String runId) {
        return requireSafeRunId(runId, "runId") + "-evidence.jsonl";
    }

    public static String sessionMetadataFileName(String runId) {
        return requireSafeRunId(runId, "runId") + "-session.json";
    }

    public static Path evidenceFile(Path outputRoot, String runId) {
        return outputRoot.resolve("outputs/reports/v0.11.0")
                .resolve(evidenceFileName(runId));
    }

    public static Path sessionMetadataFile(Path outputRoot, String runId) {
        return outputRoot.resolve("outputs/reports/v0.11.0")
                .resolve(sessionMetadataFileName(runId));
    }

    private static String requireSafeRunId(String runId, String name) {
        Objects.requireNonNull(runId, name + " must not be null");
        if (runId.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (runId.contains("/") || runId.contains("\\") || runId.contains("..")) {
            throw new IllegalArgumentException(
                    name + " must not contain path separators or traversal");
        }
        return runId;
    }
}
