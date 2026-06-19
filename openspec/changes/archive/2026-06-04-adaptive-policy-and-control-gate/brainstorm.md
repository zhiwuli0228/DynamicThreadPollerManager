## Design Summary

The next capability should introduce policy evaluation without crossing into runtime mutation. The project already has a foundation model, metrics snapshots, evidence recording, and deterministic baseline scenarios. What is missing is a deterministic policy layer that can inspect `PressureSnapshot` evidence and produce a reasoned recommendation for a target pool size.

The safest design is a small threshold policy package. It should define policy action and gate status enums, an immutable threshold config, an immutable evaluation input, a richer `PolicyDecision` output, a `ControlGate`, and a `ThresholdPolicyEvaluator`. The evaluator produces decisions only. It must never resize an executor, mutate a queue, run a scenario, schedule background work, or call the wall clock internally.

This change is intentionally bounded. It creates the decision layer that later executor-adapter work can consume, but it does not apply decisions.

## Alternatives Considered

### Alternative A: Mutating adaptive executor in one change

- **Approach**: Evaluate pressure and immediately resize the executor or queue in the same implementation.
- **Pros**: Produces visible adaptive behavior sooner.
- **Cons**: Couples policy to executor implementation; makes failures harder to diagnose; violates the current version boundary; raises safety risk before decisions are independently testable.
- **Why not chosen**: The project needs auditable decisions before runtime mutation.

### Alternative B: Stateful trend/cooldown policy

- **Approach**: Track previous snapshots or decisions and use trend or cooldown windows to avoid overreaction.
- **Pros**: More realistic adaptive behavior; may reduce oscillation later.
- **Cons**: Requires state management, time semantics, and history rules; much harder for downstream weak agents; risks nondeterministic tests.
- **Why not chosen**: First policy version should be deterministic and stateless.

### Alternative C: Deterministic threshold policy with explicit gates

- **Approach**: Evaluate one snapshot with explicit thresholds, produce `SCALE_UP`, `SCALE_DOWN`, or `HOLD`, then run the recommendation through safety gates.
- **Pros**: Small and testable; deterministic; produces clear reasons; isolates decision generation from decision application.
- **Cons**: Simple thresholds may not be enough for production behavior.
- **Why chosen**: Best balance of safety, clarity, and implementability for this version.

## Agreed Approach

Use Alternative C. Implement a small `experiment.policy` package that consumes `PressureSnapshot` and emits `PolicyDecision`. The policy should support scale-up, scale-down, and hold decisions with explicit gate statuses. Accepted or capped decisions may be converted to the existing `ScaleDecision`, but hold/rejected decisions must not become executor-applicable decisions.

## Key Decisions

- The capability name is `adaptive-policy-and-control-gate`.
- The policy is deterministic and threshold-based.
- The evaluator receives timestamps through `PolicyEvaluationInput`; it must not call `Instant.now()`.
- `PolicyDecision` is the primary result because existing `ScaleDecision` cannot represent action and gate status.
- `ScaleDecision` conversion is optional and allowed only for accepted or capped decisions.
- `DefaultControlGate` owns min/max/hold/cap behavior.
- The policy package must not depend on scenario runner or executor mutation classes.
- No Java implementation is authorized until the user approves the artifact set.

## Open Questions

- Should `REJECTED` be used only for invalid inputs, or also for unsafe proposals? Current design reserves it for invalid inputs and uses `CAPPED`/`HOLD` for bounded proposals.
- Should scale-down require queue size exactly zero or below a threshold? Current design uses queue size exactly zero for the first version.
- Should accepted/capped `PolicyDecision` conversion to `ScaleDecision` be implemented now? The downstream agent may implement it if the tests prove hold/rejected decisions cannot convert.
