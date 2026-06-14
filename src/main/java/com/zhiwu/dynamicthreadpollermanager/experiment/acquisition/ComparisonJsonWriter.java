package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ComparisonReportArtifact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class ComparisonJsonWriter {

    private final AcquisitionReportPaths paths;

    public ComparisonJsonWriter(AcquisitionReportPaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths must not be null");
    }

    public String writeComparisonReport(ComparisonReportArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact must not be null");
        Map<String, Object> map = artifact.toMap();
        String json = AcquisitionJsonWriter.render(map);
        Path outputPath = Path.of(paths.outputDirectory())
                .resolve(AcquisitionReportPaths.comparisonReportFileName(artifact.comparisonId()));
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, json);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(
                    "failed to write comparison report: " + outputPath, e);
        }
        return outputPath.toString();
    }

    public String writeComparisonReport(ComparisonReportArtifact artifact, Path outputPath) {
        Objects.requireNonNull(artifact, "artifact must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        Map<String, Object> map = artifact.toMap();
        String json = AcquisitionJsonWriter.render(map);
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, json);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(
                    "failed to write comparison report: " + outputPath, e);
        }
        return outputPath.toString();
    }

    @SuppressWarnings("unchecked")
    public ComparisonReportArtifact readComparisonReport(Path filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        String json;
        try {
            json = Files.readString(filePath);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(
                    "failed to read comparison report: " + filePath, e);
        }
        Object parsed = AcquisitionJsonWriter.parse(json);
        return ComparisonReportArtifact.fromMap((Map<String, Object>) parsed);
    }
}
