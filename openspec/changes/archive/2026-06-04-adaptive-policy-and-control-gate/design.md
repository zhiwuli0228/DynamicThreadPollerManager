## Context

Delivered capabilities:

- `experiment-foundation` provides run lifecycle and foundation model types, including `ControlPolicy` and `ScaleDecision`.
- `metrics-snapshot-and-recording` provides normalized `PressureSnapshot` evidence.
- `scenario-runner-and-baseline` provides deterministic baseline runs that can produce pressure snapshots.

The next capability must evaluate evidence and produce decisions, but it must not apply them.

## Goals / Non-Goals

**Goals:**

- Add a small policy package under the experiment boundary.
- Define deterministic threshold policy configuration.
- Define policy evaluation input with caller-supplied timestamp.
- Produce policy decisions with action, gate status, current size, proposed size, and reason.
- Apply min/max/no-op safety gates.
- Optionally convert accepted/capped policy decisions to `ScaleDecision`.
- Add tests that cover formulas, gates, conversion, and boundary isolation.

**Non-Goals:**

- No executor mutation.
- No queue capacity resizing.
- No scenario execution.
- No decision persistence.
- No cooldown or trend detection.
- No external API, UI, CLI, scheduler, or metrics registry.
- No new dependencies.

## Decisions

### 1. Package boundary

Create:

```text
com.zhiwu.dynamicthreadpollermanager.experiment.policy
```

Recommended files:

- `PolicyAction.java`
- `GateStatus.java`
- `PolicyEvaluationInput.java`
- `ThresholdPolicyConfig.java`
- `PolicyDecision.java`
- `PolicyEvaluator.java`
- `ThresholdPolicyEvaluator.java`
- `ControlGate.java`
- `DefaultControlGate.java`

### 2. PolicyAction

Use exactly:

```java
SCALE_UP, SCALE_DOWN, HOLD
```

Do not add `APPLY`, `RESIZE`, or mutation-oriented action names.

### 3. GateStatus

Use:

```java
ACCEPTED, CAPPED, HOLD, REJECTED
```

Meaning:

- `ACCEPTED`: proposal is safe and changes pool size.
- `CAPPED`: proposal exceeded bounds and was changed to a safe bound.
- `HOLD`: no change should be made.
- `REJECTED`: invalid input or unsafe state prevents a meaningful decision.

### 4. ThresholdPolicyConfig

Immutable fields:

- `String policyId`
- `int minPoolSize`
- `int maxPoolSize`
- `int scaleUpActiveThreadsThreshold`
- `int scaleUpQueueSizeThreshold`
- `int scaleDownActiveThreadsThreshold`
- `int scaleStep`

Validation:

- `policyId` non-null and non-blank.
- `minPoolSize > 0`.
- `maxPoolSize >= minPoolSize`.
- thresholds `>= 0`.
- `scaleStep > 0`.

Optional static factory:

```java
ThresholdPolicyConfig.defaultAdaptive()
```

If implemented, tests must assert its exact values.

### 5. PolicyEvaluationInput

Immutable fields:

- `String runId`
- `PressureSnapshot snapshot`
- `Instant evaluatedAt`

Validation:

- `runId` non-null and non-blank.
- `snapshot` non-null.
- `evaluatedAt` non-null.

The evaluator must use `evaluatedAt` from input. Calling `Instant.now()` inside evaluator or gate code is not allowed.

### 6. PolicyDecision

Immutable fields:

- `String runId`
- `String policyId`
- `Instant timestamp`
- `PolicyAction action`
- `GateStatus gateStatus`
- `int currentPoolSize`
- `int proposedPoolSize`
- `String reason`

Validation:

- required references non-null.
- ids non-blank.
- pool sizes non-negative.
- reason non-null and non-blank.

Optional conversion:

```java
ScaleDecision toScaleDecision()
```

Allowed only when `gateStatus` is `ACCEPTED` or `CAPPED` and action is not `HOLD`. It must throw for `HOLD` and `REJECTED`.

### 7. ControlGate and DefaultControlGate

Recommended interface:

```java
PolicyDecision apply(PolicyEvaluationInput input,
                     ThresholdPolicyConfig config,
                     PolicyAction action,
                     int proposedPoolSize,
                     String reason);
```

Equivalent shape is acceptable if it keeps the gate explicit and testable.

Gate rules:

1. If action is `HOLD`, return `GateStatus.HOLD` with current pool size.
2. If proposed size equals current size, return `GateStatus.HOLD`.
3. If proposed size > max:
   - cap proposed size to max,
   - return `CAPPED` if capped size differs from current,
   - otherwise return `HOLD`.
4. If proposed size < min:
   - cap proposed size to min,
   - return `CAPPED` if capped size differs from current,
   - otherwise return `HOLD`.
5. Otherwise return `ACCEPTED`.

### 8. ThresholdPolicyEvaluator

Recommended interface:

```java
PolicyDecision evaluate(PolicyEvaluationInput input, ThresholdPolicyConfig config);
```

Algorithm:

1. `currentPoolSize = input.snapshot().poolSize()`.
2. If `activeThreads >= scaleUpActiveThreadsThreshold` OR `queueSize >= scaleUpQueueSizeThreshold`:
   - action `SCALE_UP`,
   - proposed `currentPoolSize + scaleStep`,
   - reason mentions which threshold triggered.
3. Else if `activeThreads <= scaleDownActiveThreadsThreshold` AND `queueSize == 0`:
   - action `SCALE_DOWN`,
   - proposed `currentPoolSize - scaleStep`,
   - reason mentions low active threads and empty queue.
4. Else:
   - action `HOLD`,
   - proposed `currentPoolSize`,
   - reason mentions normal pressure.
5. Pass action/proposal/reason to `ControlGate`.
6. Return the gate result.

If both scale-up and scale-down conditions appear true, scale-up wins. This prevents high queue pressure from being hidden by low active-thread count.

## Risks / Trade-offs

- [Risk] Threshold rules are too simple. -> [Mitigation] This version is intentionally first-pass and deterministic.
- [Risk] Policy output duplicates `ScaleDecision`. -> [Mitigation] `PolicyDecision` captures richer gate semantics; conversion is optional.
- [Risk] Downstream implementation accidentally mutates executor state. -> [Mitigation] boundary tests must ban mutation and executor classes.
- [Risk] Evaluator becomes time-dependent. -> [Mitigation] tests must prove timestamp comes from input.
- [Risk] Ambiguous cap semantics. -> [Mitigation] gate tests must assert max/min capping exactly.

## Migration Plan

This is additive.

Implementation order:

1. enums,
2. config and input validation,
3. policy decision,
4. gate,
5. evaluator,
6. conversion tests if conversion is implemented,
7. boundary tests.

Rollback strategy:

- If conversion to `ScaleDecision` complicates implementation, omit conversion and keep `PolicyDecision` only.
- If gate behavior becomes unclear, keep only `ACCEPTED`, `CAPPED`, and `HOLD` for valid inputs and reserve `REJECTED` for validation failures.

## Open Questions

- Should conversion to `ScaleDecision` be required now? Current design says optional.
- Should `REJECTED` be created as a decision or represented by thrown validation exceptions? Current design allows both, but tests must document the chosen behavior.
