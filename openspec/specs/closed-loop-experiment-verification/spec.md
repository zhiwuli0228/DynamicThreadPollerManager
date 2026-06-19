# closed-loop-experiment-verification Specification

## Purpose

The `closed-loop-experiment-verification` capability provides the final end-to-end verification that the full experiment pipeline works on a real `ManagedExecutor` wrapping `ThreadPoolExecutor`. A single focused test orchestrates the complete flow: workload submission → executor state observation → policy evaluation → adjustment command creation → adapter application → result verification. This is the culmination of v0.7.0, proving that the experiment infrastructure built over v0.1.0–v0.6.0 successfully bridges to a real thread pool executor.

## Requirements

### Requirement: Closed-loop experiment verifies full pipeline on real ManagedExecutor
The system MUST provide an end-to-end test that proves the full experiment pipeline works on a real `ManagedExecutor` wrapping `ThreadPoolExecutor`: workload submission → state observation → policy evaluation → adjustment → verification.

#### Scenario: Full closed-loop experiment with scale-up
- **GIVEN** a `ManagedExecutor` with core=2, max=4, queue=10 registered as "experiment-executor"
- **AND** a `ManagedExecutorAdjustmentAdapter` with `DefaultRuntimeAdjustmentSafetyGate` and READY assessment
- **AND** a `ThresholdPolicyConfig` with scaleUpQueueSizeThreshold=1, scaleStep=3
- **WHEN** long-running tasks are submitted to consume core threads and fill the queue
- **AND** executor state is collected via `adapter.currentState()`
- **AND** a `PressureSnapshot` is built from the executor state
- **AND** `ThresholdPolicyEvaluator.evaluate()` produces a policy decision
- **AND** the decision is converted to a `ScaleAdjustmentCommand`
- **AND** `adapter.apply(command)` is called
- **THEN** the adjustment result status MUST be `APPLIED`
- **AND** `afterState.corePoolSize()` MUST equal the target pool size from the command
- **AND** `afterState.maximumPoolSize()` MUST be >= `afterState.corePoolSize()`

#### Scenario: Before-state and after-state are consistent
- **GIVEN** the same setup as the closed-loop experiment
- **WHEN** adjustment is applied
- **THEN** `beforeState.corePoolSize()` MUST be 2 (the initial value)
- **AND** `afterState.corePoolSize()` MUST be greater than `beforeState.corePoolSize()`
- **AND** both `beforeState` and `afterState` MUST have non-null values for all extended fields (poolSize, completedTaskCount, keepAliveTimeSeconds, largestPoolSize, taskCount)

#### Scenario: Executor cleanup after test
- **GIVEN** a closed-loop experiment test
- **WHEN** the test completes (including `@AfterEach`)
- **THEN** the `ManagedExecutor` MUST be shut down and terminated
- **AND** no threads MUST leak from the test
