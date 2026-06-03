# v0.3.0 Solution Design

## Header

- Version name: `v0.3.0`
- Status: `EXECUTION_AUTHORIZED`
- Change candidate: `adaptive-policy-and-control-gate`

## 1. Context

Delivered capabilities:

- `experiment-foundation`: run lifecycle and shared domain model.
- `metrics-snapshot-and-recording`: normalized pressure evidence.
- `scenario-runner-and-baseline`: deterministic baseline scenario execution.

Missing capability:

- policy evaluation that turns observed pressure into a reasoned scaling recommendation.

## 2. Design Principles

- Evaluate, do not mutate.
- Keep policy deterministic.
- Make gates explicit.
- Keep decisions bounded.
- Separate decision action from executor application.
- Prefer small immutable value objects and simple services.

## 3. Proposed Package Shape

Recommended package:

```text
src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/policy/
```

Recommended classes/interfaces:

- `PolicyAction`
- `GateStatus`
- `PolicyEvaluationInput`
- `ThresholdPolicyConfig`
- `PolicyDecision`
- `PolicyEvaluator`
- `ThresholdPolicyEvaluator`
- `ControlGate`
- `DefaultControlGate`

## 4. Component Responsibilities

### 4.1 PolicyAction

Enum:

```java
SCALE_UP, SCALE_DOWN, HOLD
```

No `APPLY` action is allowed.

### 4.2 GateStatus

Enum:

```java
ACCEPTED, CAPPED, HOLD, REJECTED
```

This makes gate outcomes explicit without mutating state.

### 4.3 ThresholdPolicyConfig

Immutable configuration.

Minimum fields:

- `policyId`
- `minPoolSize`
- `maxPoolSize`
- `scaleUpActiveThreadsThreshold`
- `scaleUpQueueSizeThreshold`
- `scaleDownActiveThreadsThreshold`
- `scaleStep`

Validation:

- policy id non-null and non-blank,
- min > 0,
- max >= min,
- thresholds non-negative,
- scaleStep > 0.

### 4.4 PolicyEvaluationInput

Immutable input:

- `runId`
- `PressureSnapshot snapshot`
- `Instant evaluatedAt`

The evaluator must use `evaluatedAt` from the caller instead of calling `Instant.now()`.

### 4.5 PolicyDecision

Immutable output:

- `runId`
- `policyId`
- `Instant timestamp`
- `PolicyAction action`
- `GateStatus gateStatus`
- `int currentPoolSize`
- `int proposedPoolSize`
- `String reason`

It may also expose conversion to the existing `ScaleDecision` when `gateStatus` is `ACCEPTED` or `CAPPED`. If conversion is added, tests must prove that hold/rejected decisions are not converted into executor-applicable decisions.

### 4.6 ThresholdPolicyEvaluator

Algorithm:

1. Read current pool size, active threads, and queue size from `PressureSnapshot`.
2. If active threads >= scale-up active threshold OR queue size >= scale-up queue threshold, propose `currentPoolSize + scaleStep`.
3. Else if active threads <= scale-down active threshold AND queue size == 0, propose `currentPoolSize - scaleStep`.
4. Else propose hold at current pool size.
5. Pass proposed action to `ControlGate`.
6. Return `PolicyDecision`.

### 4.7 DefaultControlGate

Gate rules:

- If action is `HOLD`, return hold status and current size.
- If proposed size > max, cap to max and return `CAPPED` if capped size differs from current; otherwise `HOLD`.
- If proposed size < min, cap to min and return `CAPPED` if capped size differs from current; otherwise `HOLD`.
- If proposed size == current, return `HOLD`.
- Otherwise return `ACCEPTED`.

`REJECTED` is reserved for invalid inputs that cannot be evaluated safely.

## 5. Integration With Existing Model

The evaluator consumes `PressureSnapshot`.

The existing `ControlPolicy` can remain a descriptive foundation object. Do not force all policy behavior into it unless tests show a need.

The existing `ScaleDecision` is narrower than the desired policy result. Prefer adding `PolicyDecision` and optionally converting accepted/capped decisions into `ScaleDecision`.

## 6. Risks and Mitigations

- Risk: policy accidentally mutates executor state. Mitigation: boundary test bans executor adapter and mutation references.
- Risk: threshold semantics are ambiguous. Mitigation: encode formulas in tests.
- Risk: `ScaleDecision` is too narrow. Mitigation: keep `PolicyDecision` as richer output.
- Risk: downstream agent overbuilds trend detection. Mitigation: no trend detection in this version.

## 7. Implementation Guidance for Weak Agents

Implement in this order:

1. enums,
2. config and input validation,
3. decision value object,
4. gate rules,
5. evaluator rules,
6. boundary tests.

Avoid:

- background tasks,
- stateful cooldown,
- scenario package dependencies,
- executor adapter dependencies,
- real-time calls inside evaluator,
- new dependencies.
