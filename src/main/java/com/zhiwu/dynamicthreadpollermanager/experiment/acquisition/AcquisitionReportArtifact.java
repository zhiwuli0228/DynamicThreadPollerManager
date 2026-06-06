package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Record of the artifact paths produced by
 * {@link AcquisitionReportWriter#writeAll}. The artifact is
 * the output receipt: every path is absolute and points to a
 * file written under {@code outputs/reports/v0.6.0/}.
 */
public record AcquisitionReportArtifact(Path runManifestPath,
                                         Path pressureSummaryPath,
                                         Path replaySummaryPath,
                                         Path readinessSummaryPath,
                                         Path evidenceIndexPath) {

    public AcquisitionReportArtifact {
        Objects.requireNonNull(runManifestPath, "runManifestPath");
        Objects.requireNonNull(pressureSummaryPath, "pressureSummaryPath");
        Objects.requireNonNull(replaySummaryPath, "replaySummaryPath");
        Objects.requireNonNull(readinessSummaryPath, "readinessSummaryPath");
        Objects.requireNonNull(evidenceIndexPath, "evidenceIndexPath");
    }

    public List<Path> allPaths() {
        return List.of(
                runManifestPath,
                pressureSummaryPath,
                replaySummaryPath,
                readinessSummaryPath,
                evidenceIndexPath);
    }
}
