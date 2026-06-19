## ADDED Requirements

### Requirement: RollbackAwareAdjustmentAdapter SHALL implement ExecutorAdjustmentAdapter

A new class `RollbackAwareAdjustmentAdapter` MUST implement the `ExecutorAdjustmentAdapter` interface. It MUST accept a delegate `ExecutorAdjustmentAdapter` in its constructor and delegate `currentState()` calls to the wrapped adapter unchanged.

#### Scenario: currentState delegates to wrapped adapter

- **WHEN** `currentState()` is called on a `RollbackAwareAdjustmentAdapter` wrapping a delegate
- **THEN** the returned `ExecutorStateSnapshot` is identical to the delegate's `currentState()` result

#### Scenario: RollbackAwareAdjustmentAdapter rejects null delegate

- **WHEN** a `RollbackAwareAdjustmentAdapter` is constructed with a `null` delegate
- **THEN** a `NullPointerException` is thrown

### Requirement: RollbackAwareAdjustmentAdapter SHALL capture pre-adjustment state before apply

Before delegating `apply()` to the wrapped adapter, the `RollbackAwareAdjustmentAdapter` MUST capture the current `ExecutorStateSnapshot` as the pre-adjustment baseline.

#### Scenario: Pre-adjustment snapshot is captured before delegate apply

- **WHEN** `apply()` is called with a valid `ScaleAdjustmentCommand`
- **THEN** the adapter captures the `ExecutorStateSnapshot` from `currentState()` before calling `delegate.apply()`

### Requirement: RollbackAwareAdjustmentAdapter SHALL detect post-adjustment degradation and trigger rollback

After the delegate `apply()` completes, the adapter MUST sample a post-adjustment snapshot. If a configurable degradation metric worsens beyond a threshold (e.g., queue depth increase > threshold, throughput drop > threshold), the adapter MUST issue a rollback `ScaleAdjustmentCommand` restoring the prior `corePoolSize` through the safety gate.

#### Scenario: Degradation detected triggers rollback

- **WHEN** `apply()` is called and post-adjustment queue depth exceeds pre-adjustment queue depth by more than the degradation threshold
- **THEN** a rollback command targeting the pre-adjustment `corePoolSize` is issued through the safety gate

#### Scenario: No degradation does not trigger rollback

- **WHEN** `apply()` is called and post-adjustment metrics are within the degradation threshold
- **THEN** no rollback command is issued and the original `AdjustmentResult` is returned

### Requirement: Rollback attempts SHALL be bounded to 1 per adjustment decision

The adapter MUST limit rollback attempts to at most 1 per original adjustment decision. If the rollback itself degrades metrics, the adapter MUST NOT enter an infinite rollback loop. Instead, it MUST return the rollback result (success or failure) and record the outcome.

#### Scenario: Rollback does not recurse

- **WHEN** the initial apply causes degradation AND the rollback also causes degradation
- **THEN** the adapter returns the rollback result without attempting a second rollback

### Requirement: Rollback MUST pass through the existing safety gate

The rollback `ScaleAdjustmentCommand` MUST be evaluated by the `RuntimeAdjustmentSafetyGate` before being applied. If the safety gate rejects the rollback command, the adapter MUST record the rejection and return the original (non-rolled-back) result.

#### Scenario: Safety gate allows rollback

- **WHEN** a rollback command is issued and the safety gate evaluates it as ALLOW
- **THEN** the rollback is applied and the rollback `AdjustmentResult` is returned

#### Scenario: Safety gate rejects rollback

- **WHEN** a rollback command is issued and the safety gate evaluates it as REJECTED
- **THEN** the rollback is not applied and the original `AdjustmentResult` is returned with a note that rollback was rejected

### Requirement: Rollback actions SHALL be recorded via LoopEvidenceRecorder

All rollback actions — success, failure, skip, and rejection — MUST be recorded via the `LoopEvidenceRecorder` with the failure reason and rollback details.

#### Scenario: Successful rollback is recorded in evidence

- **WHEN** a rollback is successfully applied
- **THEN** the `LoopEvidenceRecorder` records an iteration entry with the rollback result and reason

#### Scenario: Failed rollback is recorded in evidence

- **WHEN** a rollback is attempted but fails (safety gate rejection or apply failure)
- **THEN** the `LoopEvidenceRecorder` records an iteration entry with the failure reason and `AdjustmentFailureCode`

### Requirement: RollbackAwareAdjustmentAdapter SHALL accept configurable degradation threshold

The adapter MUST accept a degradation configuration that specifies the metric thresholds for triggering rollback. The configuration MUST be injectable at construction time.

#### Scenario: Custom threshold controls rollback behavior

- **WHEN** the adapter is constructed with a queue depth degradation threshold of 50
- **THEN** a post-adjustment queue depth increase of 49 does NOT trigger rollback
- **AND** a post-adjustment queue depth increase of 51 DOES trigger rollback
