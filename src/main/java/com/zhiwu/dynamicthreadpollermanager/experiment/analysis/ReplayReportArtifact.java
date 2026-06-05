package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Paths of the artifacts produced by a single
 * {@link ReplayReportWriter} invocation. Any path may be null if the
 * corresponding artifact was not requested.
 */
public final class ReplayReportArtifact {

    private final Path runSummaryPath;
    private final List<Path> scenarioSummaryPaths;
    private final Path sensitivityReportPath;
    private final Path readinessAssessmentPath;
    private final Path markdownReportPath;

    public ReplayReportArtifact(Path runSummaryPath,
                                List<Path> scenarioSummaryPaths,
                                Path sensitivityReportPath,
                                Path readinessAssessmentPath,
                                Path markdownReportPath) {
        this.runSummaryPath = runSummaryPath;
        this.scenarioSummaryPaths = scenarioSummaryPaths == null
                ? List.of()
                : List.copyOf(scenarioSummaryPaths);
        this.sensitivityReportPath = sensitivityReportPath;
        this.readinessAssessmentPath = readinessAssessmentPath;
        this.markdownReportPath = markdownReportPath;
    }

    public Path runSummaryPath() {
        return runSummaryPath;
    }

    public List<Path> scenarioSummaryPaths() {
        return Objects.requireNonNull(scenarioSummaryPaths);
    }

    public Path sensitivityReportPath() {
        return sensitivityReportPath;
    }

    public Path readinessAssessmentPath() {
        return readinessAssessmentPath;
    }

    public Path markdownReportPath() {
        return markdownReportPath;
    }
}
