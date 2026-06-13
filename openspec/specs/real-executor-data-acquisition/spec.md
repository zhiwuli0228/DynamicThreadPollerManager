## Purpose

Define the behavior of `ManagedExecutorConfig`, `ManagedExecutorScenarioRunner`, and `SnapshotAssembler.fromExecutorState()` for real ThreadPoolExecutor-based data acquisition across STEADY/RAMP/BURST profiles.

## Requirements

### Requirement: ManagedExecutorConfig invariants and creation
The system MUST provide an immutable `ManagedExecutorConfig` record that validates creation parameters and produces `ManagedExecutor` instances.

#### Scenario: Valid config creates ManagedExecutor
- **WHEN** `ManagedExecutorConfig` is created with core=2, max=4, queue=10, keepAlive=60s
- **THEN** `toManagedExecutor()` MUST return a `ManagedExecutor` wrapping a `ThreadPoolExecutor` with those parameters

#### Scenario: Invalid corePoolSize is rejected
- **WHEN** `ManagedExecutorConfig` is created with corePoolSize=0
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: Invalid maxPoolSize is rejected
- **WHEN** `ManagedExecutorConfig` is created with maxPoolSize < corePoolSize
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: defaultConfig provides standard values
- **WHEN** `ManagedExecutorConfig.defaultConfig()` is called
- **THEN** corePoolSize=2, maxPoolSize=4, queueCapacity=10, keepAliveTime=60, keepAliveTimeUnit=SECONDS

#### Scenario: toPresetSummary maps to manifest format
- **WHEN** `toPresetSummary()` is called on a config with core=2, max=4, queue=10
- **THEN** the returned `BaselinePresetSummary` MUST have policyId="managed-executor-v0.8.0", corePoolSize=2, maxPoolSize=4, queueCapacity=10

---

### Requirement: ManagedExecutorScenarioRunner profiles and execution
The system MUST provide a `ManagedExecutorScenarioRunner` that runs STEADY, RAMP, and BURST scenarios on a real `ManagedExecutor`, collecting pressure snapshots through the standard metrics pipeline.

#### Scenario: STEADY profile produces expected snapshot count
- **WHEN** `run()` is called with STEADY profile (8 steps, 2 tasks/step)
- **THEN** the `ScenarioRunOutcome` MUST have completedStepCount=8, and `EvidenceRecorder` MUST contain snapshots for the run

#### Scenario: RAMP profile shows increasing queue pressure
- **WHEN** `run()` is called with RAMP profile (8 steps, task count 2→9)
- **THEN** at least one later step MUST have `queueSize > 0` in its snapshot

#### Scenario: BURST profile shows burst-step queue pressure
- **WHEN** `run()` is called with BURST profile (9 steps, alternating 6/2 tasks)
- **THEN** burst steps (index 0, 3, 6) MUST show higher `queueSize` than adjacent non-burst steps

#### Scenario: startedLatch ensures threads started before sampling
- **WHEN** the runner submits tasks for a step
- **THEN** sampling MUST NOT occur until all submitted tasks have called `startedLatch.countDown()`, or 5 seconds have elapsed

#### Scenario: Executor is terminated after run
- **WHEN** `run()` completes (success or exception)
- **THEN** the `ManagedExecutor` MUST be shutdown, and `isTerminated()` MUST return true before `registry.remove()`

#### Scenario: Exception during run triggers cleanup
- **WHEN** a `RuntimeException` occurs mid-run
- **THEN** the runner MUST shutdown the executor, attempt termination, and NOT leak threads

#### Scenario: RAMP task count respects cap
- **WHEN** stepIndex is large enough that `2 + stepIndex > queueCapacity + maxPoolSize`
- **THEN** the task count MUST be capped at `queueCapacity + maxPoolSize` and MUST NOT throw `RejectedExecutionException`

---

### Requirement: SnapshotAssembler bridges ExecutorStateSnapshot to metrics pipeline
The system MUST provide a `fromExecutorState()` method on `SnapshotAssembler` that converts `ExecutorStateSnapshot` to `ObservedSnapshot`.

#### Scenario: All present fields map correctly
- **WHEN** `fromExecutorState()` is called with a snapshot where activeCount=3, poolSize=4, queueSize=2, completedTaskCount=100
- **THEN** the resulting `ObservedSnapshot` MUST have activeThreads=3, poolSize=4, queueSize=2, completedTaskCount=100

#### Scenario: Null fields map to absent
- **WHEN** `fromExecutorState()` is called with a snapshot where poolSize=null
- **THEN** the resulting `ObservedSnapshot` MUST have poolSize=MetricValue.absent()

#### Scenario: cpuUtilization is always absent
- **WHEN** `fromExecutorState()` is called with any snapshot
- **THEN** the resulting `ObservedSnapshot` MUST have cpuUtilization=MetricValue.absent()

#### Scenario: Default method preserves binary compatibility
- **WHEN** an existing `SnapshotAssembler` implementation does not override `fromExecutorState()`
- **THEN** the default implementation MUST be used and MUST compile without changes to the implementation class
