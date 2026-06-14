# comparison-report-artifact

## ADDED Requirements

### Requirement: ComparisonReportArtifact SHALL carry complete comparison data for serialization

The system MUST provide a `ComparisonReportArtifact` record containing the full comparison context: preset config, managed config, comparison result, and optional conclusion.

#### Scenario: Artifact contains all required fields
- **WHEN** a `ComparisonReportArtifact` is created with valid inputs
- **THEN** all fields are accessible: `comparisonId`, `scenarioId`, `createdAt`, `baselinePreset`, `managedConfig`, `result`, `conclusion`

#### Scenario: Null conclusion is allowed
- **WHEN** a `ComparisonReportArtifact` is created with `conclusion=null`
- **THEN** construction succeeds and `conclusion()` returns null

### Requirement: ComparisonJsonWriter SHALL serialize and deserialize comparison reports via JSON

The system MUST provide a `ComparisonJsonWriter` class that writes `ComparisonReportArtifact` to a JSON file and reads it back, using `AcquisitionJsonWriter.render()` and `AcquisitionJsonWriter.parse()`.

#### Scenario: Write produces valid JSON file
- **WHEN** `writeComparisonReport(artifact)` is called with a valid artifact
- **THEN** a file is created at `outputs/reports/v0.12.0/{comparisonId}-comparison.json` containing valid JSON

#### Scenario: Round-trip preserves all field values
- **WHEN** an artifact is written via `writeComparisonReport()` and read back via `readComparisonReport()`
- **THEN** all fields are identical: same comparisonId, scenarioId, baselinePreset fields, managedConfig fields, result metrics values, and conclusion

#### Scenario: Invalid JSON throws on read
- **WHEN** `readComparisonReport()` is called on a file containing malformed JSON
- **THEN** an exception is thrown (not returning null or a partial object)

#### Scenario: Write to specified output path
- **WHEN** `writeComparisonReport(artifact, customPath)` is called with a specific Path
- **THEN** the file is written at the specified path and the returned path string matches

### Requirement: AcquisitionReportPaths SHALL provide comparison report file paths

The system MUST extend `AcquisitionReportPaths` with methods for comparison report file naming and path resolution.

#### Scenario: comparisonReportFileName follows naming convention
- **WHEN** `comparisonReportFileName("abc123")` is called
- **THEN** the returned string is `"abc123-comparison.json"`

#### Scenario: comparisonReportFile resolves to versioned output directory
- **WHEN** `comparisonReportFile(outputRoot, "abc123")` is called
- **THEN** the returned path is `outputRoot/outputs/reports/v0.12.0/abc123-comparison.json`

### Requirement: Comparison model records SHALL support toMap and fromMap serialization

The records `CommonExecutorPreset`, `NormalizedComparisonMetrics`, `MetricDelta`, `ComparisonResult`, and `ComparisonReportArtifact` MUST each provide `toMap()` and `fromMap()` methods following the pattern established in v0.11.0.

#### Scenario: CommonExecutorPreset round-trip via toMap/fromMap
- **WHEN** `CommonExecutorPreset.fromMap(preset.toMap())` is called
- **THEN** the result equals the original preset

#### Scenario: NormalizedComparisonMetrics round-trip via toMap/fromMap
- **WHEN** `NormalizedComparisonMetrics` is serialized via `toMap()` and deserialized via `fromMap()`
- **THEN** all 9 fields match the original values

#### Scenario: ComparisonReportArtifact round-trip via toMap/fromMap
- **WHEN** a full artifact is serialized via `toMap()` and deserialized via `fromMap()`
- **THEN** all nested records (baselinePreset, managedConfig, result.baselineMetrics, result.managedMetrics, all 9 deltas) match the original values

## MODIFIED Requirements

### Requirement: baseline-executor-catalog CommonExecutorPreset SHALL support toMap/fromMap

The `CommonExecutorPreset` record MUST provide `toMap()` returning a Map with keys presetId, executorType, corePoolSize, maxPoolSize, queueCapacity, and description (if non-null), and `fromMap(Map)` constructing an equivalent record.

#### Scenario: toMap includes description when non-null
- **WHEN** `toMap()` is called on a preset with `description="test"`
- **THEN** the returned map contains key `"description"` with value `"test"`

#### Scenario: toMap omits description when null
- **WHEN** `toMap()` is called on a preset with `description=null`
- **THEN** the returned map does not contain key `"description"`

### Requirement: normalized-comparison-metrics records SHALL support toMap/fromMap

The `NormalizedComparisonMetrics` and `MetricDelta` records MUST each provide `toMap()` and `fromMap()` methods.

#### Scenario: MetricDelta toMap includes all 6 fields
- **WHEN** `MetricDelta.toMap()` is called
- **THEN** the returned map contains keys: `metricName`, `baselineValue`, `managedValue`, `absoluteDelta`, `relativeDelta`, `direction`

### Requirement: comparable-scenario-runner ComparisonResult SHALL support toMap/fromMap

The `ComparisonResult` record MUST provide `toMap()` serializing baselineMetrics, managedMetrics as nested maps, and deltas as a nested map of metric name to MetricDelta map; and `fromMap()` reconstructing the full object graph.

#### Scenario: ComparisonResult toMap produces nested maps for metrics and deltas
- **WHEN** `result.toMap()` is called on a valid `ComparisonResult`
- **THEN** the returned map contains `"baselineMetrics"` and `"managedMetrics"` as Map values, and `"deltas"` as a Map of String to Map
