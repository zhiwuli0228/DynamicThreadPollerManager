## ADDED Requirements

### Requirement: AntiOscillationGuard SHALL block non-emergency adjustments when sustained oscillation is detected

A new class `AntiOscillationGuard` MUST consult the `OscillationDetector` history and block non-emergency adjustments when sustained oscillation is detected. The guard MUST return a `SafetyGateDecision`-like result with a block reason when activated.

#### Scenario: Sustained oscillation blocks non-emergency adjustment

- **WHEN** `OscillationDetector.wouldOscillate()` returns `true` for more than a configurable `blockThreshold` consecutive evaluations
- **AND** the incoming command is a non-emergency adjustment
- **THEN** the guard rejects the command with `AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE`

#### Scenario: No oscillation allows adjustment

- **WHEN** `OscillationDetector.wouldOscillate()` returns `false` for the pending decision
- **AND** the incoming command is a non-emergency adjustment
- **THEN** the guard allows the command to proceed

### Requirement: AntiOscillationGuard SHALL allow emergency rollback to bypass the guard

Emergency rollback commands MUST bypass the anti-oscillation guard. The guard MUST NOT block a rollback command targeting the previous safe state, even when the guard is activated.

#### Scenario: Emergency rollback bypasses active anti-oscillation guard

- **WHEN** the anti-oscillation guard is activated (sustained oscillation detected)
- **AND** the incoming command is an emergency rollback targeting the previous safe state
- **THEN** the guard allows the command to proceed

#### Scenario: Non-emergency command is blocked when guard is active

- **WHEN** the anti-oscillation guard is activated
- **AND** the incoming command is a non-emergency scale-up
- **THEN** the guard rejects the command with `AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE`

### Requirement: Anti-oscillation block reason SHALL be recorded via LoopEvidenceRecorder

When the anti-oscillation guard blocks an adjustment, the block reason MUST be recorded via the `LoopEvidenceRecorder`. The recording MUST include the oscillation pattern type and the consecutive oscillation count.

#### Scenario: Blocked adjustment is recorded in evidence

- **WHEN** the anti-oscillation guard blocks a non-emergency adjustment
- **THEN** the `LoopEvidenceRecorder` records an iteration entry with the block reason and `AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE`

### Requirement: AntiOscillationGuard SHALL reset on stable adjustment success

The guard MUST reset its consecutive oscillation counter when a stable adjustment (no oscillation detected) succeeds. This prevents the guard from remaining permanently blocked after a transient oscillation period.

#### Scenario: Guard resets after stable adjustment

- **WHEN** the guard has `consecutiveOscillations = blockThreshold + 1` (activated)
- **AND** a new decision does NOT trigger oscillation detection
- **THEN** `consecutiveOscillations` resets to 0 and the guard is deactivated

#### Scenario: Guard remains active during continued oscillation

- **WHEN** the guard has `consecutiveOscillations = blockThreshold + 1` (activated)
- **AND** a new decision DOES trigger oscillation detection
- **THEN** `consecutiveOscillations` increments and the guard remains active

### Requirement: AntiOscillationGuard SHALL accept configurable block threshold

The guard MUST accept a `blockThreshold` configuration that specifies how many consecutive oscillation detections are required before the guard activates. The threshold MUST be configurable at construction time.

#### Scenario: Threshold controls activation

- **WHEN** the guard is constructed with `blockThreshold = 3`
- **AND** oscillation is detected for 2 consecutive evaluations
- **THEN** the guard is NOT yet activated and allows non-emergency adjustments

#### Scenario: Threshold exceeded activates guard

- **WHEN** the guard is constructed with `blockThreshold = 3`
- **AND** oscillation is detected for 3 consecutive evaluations
- **THEN** the guard IS activated and blocks non-emergency adjustments

### Requirement: AntiOscillationGuard SHALL integrate between oscillation check and safety gate in AdjustmentLoop

The guard MUST be evaluated in the `AdjustmentLoop` lifecycle after the oscillation check step and before the safety gate evaluation step. This ensures that the guard can block adjustments before they reach the safety gate.

#### Scenario: Guard is consulted before safety gate

- **WHEN** the `AdjustmentLoop` processes an iteration
- **THEN** the anti-oscillation guard is evaluated after `oscillationDetector.wouldOscillate()` and before `safetyGate.evaluate()`

### Requirement: ANTI_OSCILLATION_ACTIVE SHALL be added to AdjustmentFailureCode

The `AdjustmentFailureCode` enum MUST contain a new constant `ANTI_OSCILLATION_ACTIVE` to represent rejections by the anti-oscillation guard.

#### Scenario: ANTI_OSCILLATION_ACTIVE failure code exists

- **WHEN** `AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE` is referenced
- **THEN** the constant exists and can be used in `SafetyGateDecision.rejected()`
