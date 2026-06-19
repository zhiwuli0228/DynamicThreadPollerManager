# normalized-comparison-metrics

## ADDED Requirements

### Requirement: NormalizedComparisonMetrics SHALL compute 9 cross-executor metrics from snapshots

The system MUST provide a `NormalizedComparisonMetrics` record with 9 normalized fields and a `fromSnapshots()` factory method that computes them from a list of `ObservedSnapshot`.

#### Scenario: fromSnapshots computes all 9 fields from non-empty snapshot list
- **WHEN** `fromSnapshots(snapshots, totalDurationMs, fallbackPoolSize)` is called with a non-empty snapshot list
- **THEN** all 9 fields (`completedTaskCount`, `rejectedTaskCount`, `avgQueueDepth`, `maxQueueDepth`, `totalDurationMs`, `throughputPerSecond`, `avgActiveThreads`, `maxPoolSize`, `snapshotCount`) have non-default computed values

#### Scenario: Empty snapshot list returns zero metrics with fallback pool size
- **WHEN** `fromSnapshots(emptyList, 1000L, 4)` is called
- **THEN** all numeric fields are 0, `totalDurationMs=1000`, `maxPoolSize=4`, `snapshotCount=0`

#### Scenario: Zero totalDurationMs produces zero throughput
- **WHEN** `fromSnapshots(snapshots, 0L, 4)` is called with a non-empty snapshot list
- **THEN** `throughputPerSecond=0.0` (no division by zero)

#### Scenario: withRejectedTaskCount overrides the default zero
- **WHEN** `metrics.withRejectedTaskCount(5L)` is called
- **THEN** the returned record has `rejectedTaskCount=5` and all other fields unchanged

### Requirement: MetricDelta SHALL compute per-metric comparison deltas

The system MUST provide a `MetricDelta` record and a `compute()` static factory that calculates absolute delta, relative delta, and direction for a single metric.

#### Scenario: Improved direction when higher is better and managed exceeds baseline
- **WHEN** `MetricDelta.compute("throughputPerSecond", 1000.0, 1200.0, true)` is called
- **THEN** a `MetricDelta` is returned with `absoluteDelta=200.0`, `relativeDelta=20.0`, `direction="IMPROVED"`

#### Scenario: Regressed direction when lower is better and managed exceeds baseline
- **WHEN** `MetricDelta.compute("avgQueueDepth", 5.0, 10.0, false)` is called
- **THEN** a `MetricDelta` is returned with `direction="REGRESSED"`

#### Scenario: Neutral direction when change is below 1% threshold
- **WHEN** `MetricDelta.compute("throughputPerSecond", 1000.0, 1005.0, true)` is called
- **THEN** a `MetricDelta` is returned with `relativeDelta=0.5`, `direction="NEUTRAL"`

#### Scenario: Zero relativeDelta when baselineValue is zero
- **WHEN** `MetricDelta.compute("rejectedTaskCount", 0.0, 5.0, false)` is called
- **THEN** a `MetricDelta` is returned with `relativeDelta=0.0`

### Requirement: ComparisonResult SHALL contain two sets of metrics and nine deltas

The system MUST provide a `ComparisonResult` record that pairs baseline and managed outcomes with normalized metrics and per-metric deltas.

#### Scenario: ComparisonResult contains exactly 9 delta entries
- **WHEN** a `ComparisonResult` is created with a `deltas` map
- **THEN** the `deltas` map has exactly 9 entries (one per normalized metric)

### Requirement: NormalizedComparisonMetrics and MetricDelta SHALL support toMap and fromMap

The `NormalizedComparisonMetrics` and `MetricDelta` records MUST each provide `toMap()` and `fromMap()` methods for JSON serialization.

#### Scenario: MetricDelta toMap includes all 6 fields
- **WHEN** `MetricDelta.toMap()` is called
- **THEN** the returned map contains keys: `metricName`, `baselineValue`, `managedValue`, `absoluteDelta`, `relativeDelta`, `direction`
