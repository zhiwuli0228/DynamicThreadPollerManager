# v0.3.0 Decision Log

## Decision 1: Authorize policy evaluation before executor mutation

- Date: `2026-06-03`
- Decision: `v0.3.0` defines `adaptive-policy-and-control-gate` before any executor adapter work.
- Rationale: decisions must be testable and auditable before they are applied to runtime state.
- Consequence: executor mutation remains deferred.

## Decision 2: Use deterministic threshold policy first

- Date: `2026-06-03`
- Decision: the first policy implementation should be threshold-based.
- Rationale: weak downstream agents need concrete formulas and testable behavior.
- Consequence: trend detection, cooldown state, and learned policies are deferred.

## Decision 3: Keep policy output richer than ScaleDecision

- Date: `2026-06-03`
- Decision: add a `PolicyDecision` shape if `ScaleDecision` cannot carry action, gate status, and reason.
- Rationale: `ScaleDecision` is useful for accepted target sizes but too narrow for hold/rejected decisions.
- Consequence: accepted/capped decisions may convert to `ScaleDecision`, but policy verification should use `PolicyDecision`.

## Decision 4: No time source inside evaluator

- Date: `2026-06-03`
- Decision: evaluator receives timestamps from input.
- Rationale: deterministic tests and repeatability matter more than convenience.
- Consequence: `Instant.now()` inside policy evaluator is a design violation.
