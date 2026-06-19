## Purpose

Define end-to-end verification behavior for the queue resize pipeline delivered in `queue-resize-command-and-rebuild`. Verifies the full chain — QueueResizeCommand → QueueResizeSafetyGate → QueueResizeAdjustmentAdapter → ExecutorRebuildStrategy → ResizeEvidence — using real ManagedExecutor instances and the existing ManagedExecutorScenarioRunner.

## Requirements

### Requirement: EXPAND resize end-to-end with post-resize scenario re-run
The system MUST support end-to-end EXPAND resize that increases queue capacity and preserves executor health for subsequent scenario execution.

#### Scenario: EXPAND 10 to 20 with STEADY re-run
- **WHEN** a ManagedExecutor with queueCapacity=10 is created and STEADY scenario passes
- **AND** QueueResizeAdjustmentAdapter.apply() is called with targetQueueCapacity=20
- **THEN** AdjustmentResult.success() MUST be true
- **AND** the executor in registry MUST have queueCapacity=20
- **AND** corePoolSize and maximumPoolSize MUST remain unchanged
- **AND** re-running STEADY scenario on the new executor MUST pass

#### Scenario: Thread configuration preserved after EXPAND
- **WHEN** a resize EXPAND completes on an executor with core=2, max=4, keepAlive=60s
- **THEN** the new executor MUST have corePoolSize=2, maximumPoolSize=4, and keepAliveTime=60000ms

---

### Requirement: SHRINK resize end-to-end
The system MUST support end-to-end SHRINK resize that reduces queue capacity.

#### Scenario: SHRINK 20 to 5 succeeds
- **WHEN** a ManagedExecutor with queueCapacity=20 is created
- **AND** QueueResizeAdjustmentAdapter.apply() is called with targetQueueCapacity=5
- **THEN** AdjustmentResult.success() MUST be true
- **AND** the executor in registry MUST have queueCapacity=5

#### Scenario: Old executor terminated after SHRINK
- **WHEN** a SHRINK resize completes
- **THEN** the old ThreadPoolExecutor MUST be in terminated state
- **AND** the new executor MUST be registered under the same executorId

---

### Requirement: Safety gate DENY end-to-end
The system MUST deny SHRINK operations when current queue depth exceeds the target capacity.

#### Scenario: SHRINK denied when queue has pending tasks
- **WHEN** a ManagedExecutor with queueCapacity=10 has 8 tasks blocked in its queue
- **AND** QueueResizeAdjustmentAdapter.apply() is called with targetQueueCapacity=5 (SHRINK)
- **THEN** AdjustmentResult.success() MUST be false
- **AND** AdjustmentResult.failureCode() MUST be "SAFETY_GATE_DENIED"
- **AND** the original executor MUST still be RUNNING with queueCapacity=10

---

### Requirement: ResizeEvidence completeness verification
The system MUST produce complete ResizeEvidence for every resize operation.

#### Scenario: All evidence fields populated on success
- **WHEN** an EXPAND resize from 10 to 20 completes successfully
- **THEN** ResizeEvidence.success() MUST be true
- **AND** ResizeEvidence.beforeState().queueCapacity() MUST be 10
- **AND** ResizeEvidence.afterState().queueCapacity() MUST be 20
- **AND** ResizeEvidence.direction() MUST be "EXPAND"
- **AND** ResizeEvidence.rebuildDurationMs() MUST be > 0
- **AND** ResizeEvidence.errorMessage() MUST be null

#### Scenario: Evidence on failure
- **WHEN** a resize operation fails
- **THEN** ResizeEvidence.success() MUST be false
- **AND** ResizeEvidence.afterState() MUST be null
- **AND** ResizeEvidence.errorMessage() MUST be non-null

---

### Requirement: No-op detection for unchanged capacity
The system MUST detect and short-circuit no-op resize requests.

#### Scenario: Same capacity returns empty command
- **WHEN** QueueResizeCommand.fromCurrent(10, 10, "no change") is called
- **THEN** Optional.empty() MUST be returned

---

### Requirement: Full regression — existing tests unaffected
All existing tests MUST continue to pass without modification.

#### Scenario: mvn test passes with zero failures
- **WHEN** `mvn test` is executed after both v0.9.0 changes are implemented
- **THEN** all 433 existing tests MUST pass
- **AND** all new tests from both changes MUST pass
- **AND** ManagedExecutorAdjustmentAdapter tests MUST pass unchanged
- **AND** ManagedExecutorScenarioRunner tests MUST pass unchanged
