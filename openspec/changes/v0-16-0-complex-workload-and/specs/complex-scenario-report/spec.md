## ADDED Requirements

### Requirement: ComplexScenarioReport SHALL be an immutable record containing scenario identification

A new record `ComplexScenarioReport` MUST contain `reportId`, `scenarioId`, `seed`, and `scenarioConfig` fields for scenario identification. The record MUST be immutable.

#### Scenario: ComplexScenarioReport is created with all identification fields

- **WHEN** a `ComplexScenarioReport` is constructed with `reportId = "r1"`, `scenarioId = "s1"`, `seed = 42`, and `scenarioConfig = {profile: LONG_TAIL, stepCount: 10}`
- **THEN** `report.reportId()` returns `"r1"`, `report.scenarioId()` returns `"s1"`, `report.seed()` returns `42`, and `report.scenarioConfig()` returns the provided config

### Requirement: ComplexScenarioReport SHALL contain adjustment, blocked, and rollback counts

The report MUST contain `adjustmentCount`, `blockedCount`, and `rollbackCount` fields representing the total number of adjustments attempted, blocked by safety gates or anti-oscillation, and rollbacks performed during the scenario run.

#### Scenario: Counts reflect scenario execution

- **WHEN** a scenario run performs 10 adjustments, blocks 3, and triggers 2 rollbacks
- **THEN** the report has `adjustmentCount = 10`, `blockedCount = 3`, and `rollbackCount = 2`

### Requirement: ComplexScenarioReport SHALL contain rollback success rate

The report MUST contain a `rollbackSuccessRate` field as a `double` between 0.0 and 1.0, computed as `successfulRollbacks / totalRollbackAttempts`.

#### Scenario: Rollback success rate is computed correctly

- **WHEN** 3 rollbacks are attempted and 2 succeed
- **THEN** `report.rollbackSuccessRate()` returns `2.0 / 3.0` (approximately 0.667)

#### Scenario: Zero rollbacks yields zero success rate

- **WHEN** no rollbacks are attempted
- **THEN** `report.rollbackSuccessRate()` returns `0.0`

### Requirement: ComplexScenarioReport SHALL contain recovery time

The report MUST contain a `recoveryTimeMs` field representing the time in milliseconds from a degradation event to the point where metrics return to pre-degradation levels.

#### Scenario: Recovery time is recorded

- **WHEN** a degradation event occurs at `T0` and metrics recover at `T0 + 5000ms`
- **THEN** `report.recoveryTimeMs()` returns `5000`

### Requirement: ComplexScenarioReport SHALL contain p95 and p99 latency

The report MUST contain `p95LatencyMs` and `p99LatencyMs` fields computed from real observation data (not synthetic proxies).

#### Scenario: Percentile latencies are computed from real snapshots

- **WHEN** a scenario run produces 100 observed latency values
- **THEN** `p95LatencyMs` is the 95th percentile and `p99LatencyMs` is the 99th percentile of those real values

### Requirement: ComplexScenarioReport SHALL contain queue depth delta and throughput delta

The report MUST contain `queueDepthDelta` and `throughputDelta` fields representing the change in queue depth and throughput from the start to the end of the scenario run.

#### Scenario: Deltas reflect start-to-end change

- **WHEN** initial queue depth is 10 and final queue depth is 25, initial throughput is 100/s and final throughput is 80/s
- **THEN** `report.queueDepthDelta()` returns `15` and `report.throughputDelta()` returns `-20`

### Requirement: ComplexScenarioReport SHALL contain per-decision observation windows

The report MUST contain a `List<ObservationWindow> decisionWindows` field. Each `ObservationWindow` MUST contain `decisionIndex`, `preDecisionSnapshots`, `postDecisionSnapshots`, and `decisionTimestamp`. Observation windows MUST be derived from real snapshot arrays.

#### Scenario: Observation windows are populated from real data

- **WHEN** a scenario run makes 5 decisions and each decision has 3 pre-snapshots and 3 post-snapshots
- **THEN** `report.decisionWindows()` contains 5 `ObservationWindow` entries, each with 3 pre and 3 post snapshots

### Requirement: ComplexScenarioReportGenerator SHALL read from real evidence sources

A new class `ComplexScenarioReportGenerator` MUST read from `EvidenceRecorder` (real snapshot arrays), `LoopEvidenceRecorder`, and `AdjustmentHistory` to compute all metrics. The generator MUST NOT use synthetic data or proxy arrays.

#### Scenario: Generator produces report from real evidence

- **WHEN** `generate()` is called with a populated `EvidenceRecorder`, `LoopEvidenceRecorder`, and `AdjustmentHistory`
- **THEN** the returned `ComplexScenarioReport` contains metrics computed entirely from the real recorded data

#### Scenario: Generator rejects null evidence sources

- **WHEN** `generate()` is called with a `null` `EvidenceRecorder`
- **THEN** a `NullPointerException` is thrown

### Requirement: ComplexScenarioReport SHALL contain rejection count

The report MUST contain a `rejectionCount` field representing the total number of tasks rejected by the executor during the scenario run.

#### Scenario: Rejection count reflects executor saturation

- **WHEN** the executor rejects 15 tasks during a scenario run
- **THEN** `report.rejectionCount()` returns `15`

### Requirement: ComplexScenarioReport SHALL contain generation timestamp

The report MUST contain a `generatedAt` field of type `Instant` recording when the report was generated.

#### Scenario: Generation timestamp is set

- **WHEN** a report is generated at a known instant
- **THEN** `report.generatedAt()` returns that instant
