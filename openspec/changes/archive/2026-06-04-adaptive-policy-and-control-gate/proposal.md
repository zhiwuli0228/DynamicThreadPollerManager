## Why

The project can now run deterministic baseline scenarios and record pressure snapshots, but it cannot yet turn that evidence into a reasoned scaling recommendation. This change adds the missing policy and safety-gate layer so future executor-adapter work can consume audited decisions instead of inventing control behavior inline.

## What Changes

**Policy evaluation**
- From: pressure snapshots are recorded but not evaluated.
- To: a deterministic threshold evaluator will inspect a snapshot and recommend scale up, scale down, or hold.
- Reason: later adaptive execution needs an independently testable decision layer.
- Impact: non-breaking internal addition.

**Safety gating**
- From: no explicit gate exists between pressure evidence and target pool size.
- To: a gate will cap or hold proposals based on configured min/max bounds and no-op conditions.
- Reason: unsafe recommendations must be visible before any executor adapter can apply them.
- Impact: non-breaking internal addition.

**Policy decision output**
- From: existing `ScaleDecision` only captures current/proposed pool size and reasoning.
- To: a richer policy decision will include action, gate status, timestamp, current size, proposed size, and reason.
- Reason: hold and capped decisions need more semantics than `ScaleDecision` can represent.
- Impact: non-breaking; optional conversion to `ScaleDecision` is allowed only for accepted/capped decisions.

## Capabilities

### New Capabilities

- `adaptive-policy-and-control-gate`: deterministic threshold policy evaluation, explicit safety gates, and reasoned policy decision output from pressure evidence.

### Modified Capabilities

- none

## Impact

- Affected code: new policy package under `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/`.
- Affected APIs: internal Java contracts for config, input, gate, evaluator, and decision result.
- Affected dependencies: none.
- Affected systems: future executor adapter and queue resizing changes will consume the policy decision output.
