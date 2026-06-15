# decision-orchestration

## Purpose

Orchestrate the classification→ranking→evaluation pipeline to produce a single AdjustmentDecision per loop iteration.

## ADDED Requirements

### Requirement: AdjustmentDecision Record

The system SHALL provide an `AdjustmentDecision` record carrying the full decision chain output with nullable score/policy fields for NO_OP decisions.

#### Scenario: Non-NO_OP decision
- **GIVEN** a PressureClassification, PolicyScore, ThresholdPolicyConfig, PolicyDecision with SCALE_UP action
- **WHEN** AdjustmentDecision is constructed
- **THEN** isNoOp() returns false and toCommand() returns a non-no-op ScaleAdjustmentCommand

#### Scenario: NO_OP decision
- **GIVEN** a PressureClassification and PolicyDecision with HOLD action
- **WHEN** AdjustmentDecision is constructed with null selectedScore and null selectedPolicy
- **THEN** construction succeeds (no NPE) and isNoOp() returns true

### Requirement: DecisionOrchestrator Class

The system SHALL provide an immutable `DecisionOrchestrator` that executes classify→rank→find→input→evaluate→assemble pipeline.

#### Scenario: OVERLOAD state selects aggressive policy
- **GIVEN** snapshots showing OVERLOAD conditions (high active threads, high queue)
- **AND** 3 candidate policies: conservative (high thresholds), moderate, aggressive (low thresholds)
- **WHEN** decide() is called
- **THEN** the selected policy is the aggressive one (lowest thresholds → highest responsivenessScore)
- **AND** the decision rationale contains "OVERLOAD"

#### Scenario: Empty snapshots returns NO_OP
- **GIVEN** an empty snapshot list
- **WHEN** decide() is called
- **THEN** the returned AdjustmentDecision has isNoOp() == true

#### Scenario: PolicyEvaluationInput uses snapshot timestamp
- **GIVEN** snapshots with known timestamps
- **WHEN** decide() is called
- **THEN** the PolicyEvaluationInput.evaluatedAt equals the last snapshot's timestamp (not wall-clock)
