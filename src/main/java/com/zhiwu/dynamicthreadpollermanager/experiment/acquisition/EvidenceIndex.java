package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.util.List;
import java.util.Objects;

/**
 * Index that maps a single {@code runId} to the artifacts
 * produced for that run under {@code outputs/reports/v0.6.0/}.
 * The index is the trace contract: every acquisition run
 * MUST emit a corresponding {@code EvidenceIndex} that lists
 * the manifest, pressure summary, replay summary, and
 * readiness summary paths relative to the run, so that a
 * reviewer can recover the run by {@code runId} alone.
 *
 * <p>The index MUST NOT list raw snapshot evidence paths;
 * raw evidence is governed by {@link RetentionRecord}.
 */
public final class EvidenceIndex {

    private final String runId;
    private final String reportDirectory;
    private final String runManifestPath;
    private final String pressureSummaryPath;
    private final String replaySummaryPath;
    private final String readinessSummaryPath;
    private final RetentionRecord retention;

    public EvidenceIndex(String runId,
                         String reportDirectory,
                         String runManifestPath,
                         String pressureSummaryPath,
                         String replaySummaryPath,
                         String readinessSummaryPath,
                         RetentionRecord retention) {
        this.runId = requireNonBlank(runId, "runId");
        this.reportDirectory = requireNonBlank(reportDirectory, "reportDirectory");
        this.runManifestPath = requireNonBlank(runManifestPath, "runManifestPath");
        this.pressureSummaryPath = requireNonBlank(pressureSummaryPath, "pressureSummaryPath");
        this.replaySummaryPath = requireNonBlank(replaySummaryPath, "replaySummaryPath");
        this.readinessSummaryPath = readinessSummaryPath == null ? "" : readinessSummaryPath;
        this.retention = Objects.requireNonNull(retention, "retention");
        this.retention.validate();
    }

    public String runId() { return runId; }
    public String reportDirectory() { return reportDirectory; }
    public String runManifestPath() { return runManifestPath; }
    public String pressureSummaryPath() { return pressureSummaryPath; }
    public String replaySummaryPath() { return replaySummaryPath; }
    public String readinessSummaryPath() { return readinessSummaryPath; }
    public RetentionRecord retention() { return retention; }

    public List<String> artifactPaths() {
        return List.of(
                runManifestPath,
                pressureSummaryPath,
                replaySummaryPath,
                readinessSummaryPath);
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
