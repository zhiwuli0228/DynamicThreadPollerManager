# Design: comparison-report-and-end-to-end-verification

## Overview

This change adds the comparison report artifact serialization and end-to-end verification to the v0.12.0 comparison framework. It depends on change 1/2 (`baseline-catalog-and-comparison-runner`) for `ComparableScenarioRunner`, `ComparisonResult`, and all model types.

## Module Boundaries

| Module | Change | Component |
|---|---|---|
| `experiment.scenario` | **New** | `ComparisonReportArtifact` (record) |
| `experiment.scenario` | **Modify** | `CommonExecutorPreset` — add `toMap()`/`fromMap()` |
| `experiment.scenario` | **Modify** | `NormalizedComparisonMetrics` — add `toMap()`/`fromMap()` |
| `experiment.scenario` | **Modify** | `MetricDelta` — add `toMap()`/`fromMap()` |
| `experiment.scenario` | **Modify** | `ComparisonResult` — add `toMap()`/`fromMap()` |
| `experiment.acquisition` | **New** | `ComparisonJsonWriter` (class) |
| `experiment.acquisition` | **Modify** | `AcquisitionReportPaths` — add comparison file path methods |

## ComparisonJsonWriter Design

```java
public final class ComparisonJsonWriter {
    private final AcquisitionReportPaths paths;

    public String writeComparisonReport(ComparisonReportArtifact artifact) {
        String json = AcquisitionJsonWriter.render(artifact.toMap());
        Path outputPath = Path.of(paths.outputDirectory())
                .resolve(comparisonReportFileName(artifact.comparisonId()));
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, json);
        return outputPath.toString();
    }

    public ComparisonReportArtifact readComparisonReport(Path filePath) {
        String json = Files.readString(filePath);
        Object parsed = AcquisitionJsonWriter.parse(json);
        return ComparisonReportArtifact.fromMap((Map<String, Object>) parsed);
    }
}
```

## JSON Schema

```json
{
  "comparisonId": "uuid",
  "scenarioId": "cpu-bound-100",
  "createdAt": "2026-06-14T10:30:00Z",
  "baselinePreset": {
    "presetId": "fixed-4",
    "executorType": "FIXED_THREAD_POOL",
    "corePoolSize": 4,
    "maxPoolSize": 4,
    "queueCapacity": -1,
    "description": "..."
  },
  "managedConfig": {
    "corePoolSize": 4,
    "maximumPoolSize": 8,
    "queueCapacity": 20,
    "keepAliveTime": 60,
    "keepAliveTimeUnit": "SECONDS",
    "threadMode": "PLATFORM"
  },
  "result": {
    "baselineMetrics": { /* 9 fields */ },
    "managedMetrics": { /* 9 fields */ },
    "deltas": { /* 9 MetricDelta entries */ }
  },
  "conclusion": "optional human-readable text"
}
```

## Testing Strategy

- Unit: ComparisonReportArtifact construction
- Unit: ComparisonJsonWriter write → read round-trip (fields identical)
- Unit: AcquisitionReportPaths comparison file path methods
- Integration: ComparableScenarioRunner.compare() → write report → read report → verify all deltas
- Regression: 646 existing tests pass
