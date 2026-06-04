# adaptive-policy-and-control-gate Specification

## Purpose
TBD - created by archiving change adaptive-policy-and-control-gate. Update Purpose after archive.
## Requirements
### Requirement: Threshold policy configuration
The system MUST define immutable threshold policy configuration with a policy identity, minimum pool size, maximum pool size, scale-up thresholds, scale-down threshold, and scale step.

#### Scenario: Create valid threshold policy configuration
- **WHEN** a policy configuration is created with a non-blank policy id, positive minimum pool size, maximum pool size greater than or equal to minimum, non-negative thresholds, and positive scale step
- **THEN** the system MUST retain those values for deterministic policy evaluation

#### Scenario: Reject invalid threshold policy configuration
- **WHEN** a policy configuration is created with a blank policy id, invalid min/max bounds, negative thresholds, or non-positive scale step
- **THEN** the system MUST reject the configuration before evaluation

---

### Requirement: Policy evaluation input
The system MUST define immutable policy evaluation input containing a run identity, a pressure snapshot, and a caller-supplied evaluation timestamp.

#### Scenario: Create valid policy evaluation input
- **WHEN** policy input is created with a non-blank run id, a pressure snapshot, and an evaluation timestamp
- **THEN** the system MUST expose those values to the evaluator

#### Scenario: Preserve deterministic timestamp
- **WHEN** a policy decision is produced from policy input
- **THEN** the decision timestamp MUST match the input evaluation timestamp rather than a wall-clock timestamp created inside the evaluator

---

### Requirement: Threshold policy evaluation
The system MUST evaluate pressure snapshots into deterministic scale-up, scale-down, or hold policy actions.

#### Scenario: Scale up on high active threads
- **WHEN** active threads are greater than or equal to the configured scale-up active-thread threshold
- **THEN** the evaluator MUST propose a scale-up action by adding the configured scale step to the current pool size

#### Scenario: Scale up on high queue size
- **WHEN** queue size is greater than or equal to the configured scale-up queue-size threshold
- **THEN** the evaluator MUST propose a scale-up action by adding the configured scale step to the current pool size

#### Scenario: Scale down on low pressure
- **WHEN** active threads are less than or equal to the configured scale-down active-thread threshold and queue size is zero
- **THEN** the evaluator MUST propose a scale-down action by subtracting the configured scale step from the current pool size

#### Scenario: Hold on normal pressure
- **WHEN** neither scale-up nor scale-down conditions are met
- **THEN** the evaluator MUST return a hold action with the current pool size as the proposed pool size

---

### Requirement: Control gate bounds
The system MUST apply explicit control gates that keep proposed pool sizes within configured minimum and maximum bounds.

#### Scenario: Accept safe proposal
- **WHEN** a proposed pool size is within configured bounds and differs from the current pool size
- **THEN** the gate MUST return an accepted decision preserving the proposed pool size

#### Scenario: Cap proposal above maximum
- **WHEN** a proposed pool size exceeds the configured maximum pool size
- **THEN** the gate MUST cap the proposed pool size to the configured maximum and mark the decision as capped unless the capped value equals the current pool size

#### Scenario: Cap proposal below minimum
- **WHEN** a proposed pool size is below the configured minimum pool size
- **THEN** the gate MUST cap the proposed pool size to the configured minimum and mark the decision as capped unless the capped value equals the current pool size

#### Scenario: Hold no-op proposal
- **WHEN** a proposed pool size is equal to the current pool size
- **THEN** the gate MUST return a hold decision

---

### Requirement: Policy decision output
The system MUST produce immutable policy decisions containing run id, policy id, timestamp, action, gate status, current pool size, proposed pool size, and reason.

#### Scenario: Produce reasoned decision
- **WHEN** the evaluator returns a policy decision
- **THEN** the decision MUST include a non-blank reason explaining the triggering threshold or gate result

#### Scenario: Convert applicable decision to scale decision
- **WHEN** conversion to `ScaleDecision` is implemented and a decision is accepted or capped with a non-hold action
- **THEN** the system MUST convert it to a `ScaleDecision` preserving timestamp, run id, current pool size, proposed pool size, and reason

#### Scenario: Prevent non-applicable scale decision conversion
- **WHEN** conversion to `ScaleDecision` is implemented and a decision is hold or rejected
- **THEN** the system MUST prevent conversion because no executor-applicable scale target exists

---

### Requirement: Policy boundary isolation
The system MUST keep policy evaluation independent from scenario execution and executor mutation.

#### Scenario: Verify forbidden dependencies
- **WHEN** the policy package source is inspected
- **THEN** it MUST NOT reference scenario runner classes, executor adapter classes, queue mutation controllers, thread pool executors, scheduled executors, or adjustment event creation

