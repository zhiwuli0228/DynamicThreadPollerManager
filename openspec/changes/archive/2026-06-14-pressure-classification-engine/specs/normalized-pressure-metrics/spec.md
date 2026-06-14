# normalized-pressure-metrics

## ADDED Requirements

### Requirement: NormalizedPressureMetrics SHALL compute 11 metrics from snapshot lists

The `NormalizedPressureMetrics` record MUST provide `fromSnapshots()` factory that computes 9 base metrics (matching NormalizedComparisonMetrics calculation logic) plus 2 derived signals (queueGrowthRate via linear regression, threadUtilizationRatio as avgActiveThreads/maxPoolSize).

#### Scenario: Compute all 11 metrics from non-empty snapshot list
- **GIVEN** a list of 10 snapshots from a managed executor run
- **WHEN** `NormalizedPressureMetrics.fromSnapshots(snapshots, 5000L, 8, 5)` is called
- **THEN** all 11 fields are computed: completedTaskCount from last snapshot, avgQueueDepth/maxQueueDepth from all snapshots, throughputPerSecond calculated, queueGrowthRate reflects queue trend, threadUtilizationRatio = avgActiveThreads/maxPoolSize

#### Scenario: queueGrowthRate is positive for increasing queue
- **GIVEN** a list of 5 snapshots with queueSize values [2, 4, 6, 8, 10]
- **WHEN** `fromSnapshots()` is called with trendWindowSize=5
- **THEN** `metrics.queueGrowthRate() > 0`

#### Scenario: queueGrowthRate is negative for decreasing queue
- **GIVEN** a list of 5 snapshots with queueSize values [10, 8, 6, 4, 2]
- **WHEN** `fromSnapshots()` is called with trendWindowSize=5
- **THEN** `metrics.queueGrowthRate() < 0`

#### Scenario: queueGrowthRate is near zero for stable queue
- **GIVEN** a list of 5 snapshots with queueSize values [3, 3, 3, 3, 3]
- **WHEN** `fromSnapshots()` is called with trendWindowSize=5
- **THEN** abs(metrics.queueGrowthRate()) < 0.01

#### Scenario: Empty snapshots returns zero defaults
- **GIVEN** an empty snapshot list
- **WHEN** `fromSnapshots(emptyList, 1000L, 4, 5)` is called
- **THEN** metrics has all zeros (completedTaskCount=0, rejectedTaskCount=0, avgQueueDepth=0.0, maxQueueDepth=0, totalDurationMs=1000, throughputPerSecond=0.0, avgActiveThreads=0.0, maxPoolSize=4, snapshotCount=0, queueGrowthRate=0.0, threadUtilizationRatio=0.0)

#### Scenario: Zero duration yields zero throughput
- **GIVEN** a non-empty snapshot list with totalDurationMs=0
- **WHEN** `fromSnapshots()` is called
- **THEN** `metrics.throughputPerSecond() == 0.0` (no division by zero)

### Requirement: NormalizedPressureMetrics SHALL support rejectedTaskCount injection

The `withRejectedTaskCount(long)` method MUST return a new instance with updated rejectedTaskCount, preserving all other 10 fields unchanged.

#### Scenario: withRejectedTaskCount updates only rejectedTaskCount
- **GIVEN** a NormalizedPressureMetrics with rejectedTaskCount=0
- **WHEN** `metrics.withRejectedTaskCount(5)` is called
- **THEN** returned metrics has rejectedTaskCount=5, all other fields match original

### Requirement: NormalizedPressureMetrics SHALL provide toMap() for debug support

The `toMap()` method MUST return a LinkedHashMap with all 11 fields as key-value pairs.

#### Scenario: toMap returns 11 entries
- **WHEN** `metrics.toMap()` is called
- **THEN** a Map with exactly 11 entries is returned, containing all field names as keys
