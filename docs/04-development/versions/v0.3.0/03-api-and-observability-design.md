# v0.3.0 API and Observability Design

## Header

- Version name: `v0.3.0`
- Status: `EXECUTION_AUTHORIZED`
- Change candidate: `adaptive-policy-and-control-gate`

## 1. API Surface

No REST, CLI, UI, or external API is authorized.

The API surface is internal Java only:

- policy config,
- policy input,
- policy decision,
- policy evaluator,
- gate evaluator.

## 2. Internal Contract Expectations

### PolicyEvaluator

Expected shape:

```java
PolicyDecision evaluate(PolicyEvaluationInput input, ThresholdPolicyConfig config);
```

Equivalent shapes are acceptable if they preserve deterministic input and explicit config.

### PolicyDecision

Must expose:

- run id,
- policy id,
- timestamp,
- action,
- gate status,
- current pool size,
- proposed pool size,
- reason.

## 3. Observability

Observability is represented by explicit decision output, not logs or metrics.

The decision reason must be human-readable enough to explain:

- which threshold triggered the action,
- whether the gate accepted, capped, held, or rejected,
- what pool size was proposed.

## 4. Logging

No logging change is authorized. Prefer return-value evidence and tests.

## 5. Future Extension Points

Later versions may add:

- decision recording into result series,
- executor adapter consumption,
- cooldown windows,
- trend detection,
- comparison reports.

These are not part of `v0.3.0`.
