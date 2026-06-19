## ADDED Requirements

### Requirement: TimeBasedCooldownSafetyGate SHALL implement RuntimeAdjustmentSafetyGate

A new class `TimeBasedCooldownSafetyGate` MUST implement the `RuntimeAdjustmentSafetyGate` interface. It MUST accept a `Supplier<Instant>` for an injectable time source in its constructor.

#### Scenario: TimeBasedCooldownSafetyGate evaluates allowed command

- **WHEN** `evaluate()` is called with a valid command, a ready state, and no cooldown active
- **THEN** the decision outcome is `ALLOW`

#### Scenario: TimeBasedCooldownSafetyGate rejects null clock

- **WHEN** a `TimeBasedCooldownSafetyGate` is constructed with a `null` `Supplier<Instant>`
- **THEN** a `NullPointerException` is thrown

### Requirement: Cooldown SHALL be time-based using injectable clock

The gate MUST maintain a `Map<String, Instant> lastAppliedInstant` per executor. The cooldown check MUST compare `Duration.between(lastAppliedInstant, clock.get())` against a configurable `cooldownDuration`. If the duration is less than the cooldown duration, the command MUST be rejected with `AdjustmentFailureCode.COOLDOWN_ACTIVE`.

#### Scenario: Command rejected during cooldown window

- **WHEN** an adjustment was applied at `T0` and `evaluate()` is called at `T0 + 500ms` with a cooldown duration of 1 second
- **THEN** the decision outcome is `REJECTED` with failure code `COOLDOWN_ACTIVE`

#### Scenario: Command allowed after cooldown expires

- **WHEN** an adjustment was applied at `T0` and `evaluate()` is called at `T0 + 1500ms` with a cooldown duration of 1 second
- **THEN** the decision outcome is `ALLOW`

### Requirement: Emergency rollback SHALL bypass cooldown

When a command carries an emergency rollback flag (or is issued via a dedicated emergency rollback path targeting the previous safe state), the cooldown check MUST be skipped. The gate MUST allow the emergency rollback command even if the cooldown window is active.

#### Scenario: Emergency rollback bypasses active cooldown

- **WHEN** an adjustment was applied at `T0` and an emergency rollback command is evaluated at `T0 + 100ms` with a cooldown duration of 1 second
- **THEN** the decision outcome is `ALLOW` (cooldown is bypassed)

#### Scenario: Non-emergency command is still blocked during cooldown

- **WHEN** an adjustment was applied at `T0` and a non-emergency command is evaluated at `T0 + 100ms` with a cooldown duration of 1 second
- **THEN** the decision outcome is `REJECTED` with failure code `COOLDOWN_ACTIVE`

### Requirement: Emergency bypass SHALL only apply to rollback commands

The emergency bypass MUST only apply to commands that target the previous safe state (rollback commands). Arbitrary scale-up commands MUST NOT be able to exploit the emergency bypass to circumvent cooldown.

#### Scenario: Emergency flag on non-rollback command does not bypass cooldown

- **WHEN** an adjustment was applied at `T0` scaling UP from 4 to 8, and a new UP command (4 → 12) with emergency flag is evaluated at `T0 + 100ms`
- **THEN** the decision outcome is `REJECTED` with failure code `COOLDOWN_ACTIVE`

#### Scenario: Emergency flag on rollback command (target == previous safe state) bypasses cooldown

- **WHEN** an adjustment was applied at `T0` scaling from 4 to 8, and a rollback command (8 → 4) with emergency flag is evaluated at `T0 + 100ms`
- **THEN** the decision outcome is `ALLOW`

### Requirement: TimeBasedCooldownSafetyGate SHALL preserve all other safety checks

All other safety gate checks from `DefaultRuntimeAdjustmentSafetyGate` — readiness, per-run limit, opposite direction, no-op — MUST be preserved identically. The time-based cooldown gate is a replacement for the counter-based cooldown only.

#### Scenario: NOT_READY rejection still applies

- **WHEN** `evaluate()` is called with a command and `ReadinessStatus.NOT_READY`
- **THEN** the decision outcome is `REJECTED` with failure code `NOT_READY`

#### Scenario: Per-run limit rejection still applies

- **WHEN** the per-run adjustment count has reached the maximum and `evaluate()` is called with a non-emergency command
- **THEN** the decision outcome is `REJECTED` with failure code `RUN_LIMIT_EXCEEDED`

#### Scenario: No-op detection still applies

- **WHEN** `evaluate()` is called with a command where `targetPoolSize == currentPoolSize`
- **THEN** the decision outcome is `NO_OP`

### Requirement: TimeBasedCooldownSafetyGate SHALL be testable with controllable clock

The gate MUST accept a `Supplier<Instant>` that can be controlled in tests (e.g., `AtomicReference<Instant>`) without depending on `Thread.sleep()` or real wall-clock time.

#### Scenario: Test controls time advancement

- **WHEN** a test constructs the gate with an `AtomicReference<Instant>` set to `T0`
- **AND** applies a command at `T0`
- **AND** advances the clock to `T0 + cooldownDuration + 1`
- **AND** evaluates a new command
- **THEN** the new command is allowed without any real sleep
