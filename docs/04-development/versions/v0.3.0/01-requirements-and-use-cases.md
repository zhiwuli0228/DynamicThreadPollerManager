# v0.3.0 Requirements and Use Cases

## Header

- Version name: `v0.3.0`
- Status: `IMPLEMENTED`
- Change candidate: `adaptive-policy-and-control-gate`

## 1. Primary Use Case

A developer runs a deterministic baseline scenario and records pressure snapshots. The policy layer then evaluates one or more snapshots and produces a recommended target pool size with a clear reason, without mutating any executor.

## 2. Actors

- Experiment developer: invokes policy evaluation in tests or local experiments.
- Future executor adapter implementer: consumes accepted decisions later.
- Verification agent: checks that policy decisions are deterministic and non-mutating.

## 3. Functional Requirements

### 3.1 Policy configuration

The system must define explicit configuration for:

- minimum pool size,
- maximum pool size,
- scale-up active-thread threshold,
- scale-up queue-size threshold,
- scale-down active-thread threshold,
- scale step.

### 3.2 Policy input

The system must evaluate:

- run id,
- policy identity,
- current `PressureSnapshot`,
- caller-supplied evaluation timestamp.

### 3.3 Policy evaluation

The evaluator must:

- recommend scale up when pressure exceeds configured thresholds,
- recommend scale down or hold when pressure is low,
- keep recommendations within min/max pool bounds,
- include reasoning.

### 3.4 Safety gate

The gate must hold or reject when:

- current pool size is below minimum or above maximum,
- proposed pool size would exceed configured bounds,
- input is invalid or missing required identity,
- no meaningful change is needed.

### 3.5 Decision output

The output must expose:

- run id,
- policy id,
- decision timestamp,
- current pool size,
- proposed pool size,
- decision action,
- gate status,
- reason.

If existing `ScaleDecision` cannot carry all fields, this version should add a policy-specific wrapper while preserving the foundation model.

## 4. Non-Functional Requirements

- Deterministic: no wall-clock calls inside evaluator unless timestamp is supplied by caller.
- Non-mutating: policy must not change executor or queue state.
- Small: no framework wiring required.
- No new dependencies.

## 5. Acceptance Use Cases

### Use Case A: Scale up under pressure

- Given active threads at or above the scale-up threshold or queue size above threshold.
- When the policy evaluates the snapshot.
- Then it recommends a larger pool size within bounds and records the reason.

### Use Case B: Hold within normal pressure

- Given active threads and queue size within normal range.
- When the policy evaluates the snapshot.
- Then it returns a hold decision with no executor mutation.

### Use Case C: Gate unsafe proposals

- Given a policy proposal above the maximum pool size.
- When safety gates run.
- Then the final result is capped or held according to the documented gate rule.

### Use Case D: Preserve boundary

- Given the policy package.
- When verification scans source.
- Then it does not reference scenario runner classes, executor adapter classes, queue mutation, or scheduling components.
