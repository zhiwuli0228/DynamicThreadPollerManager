# v0.3.0 Change Decomposition Plan

## Header

- Version name: `v0.3.0`
- Status: `READY_FOR_CHANGE_DECOMPOSITION`
- Authorization status: one OpenSpec change may be created from this design

## 1. Change Candidate

### Change: `adaptive-policy-and-control-gate`

Core responsibility:

- evaluate pressure evidence,
- apply deterministic threshold rules,
- apply safety gates,
- produce reasoned policy decisions,
- optionally convert accepted decisions to `ScaleDecision`.

Key boundary:

- no executor mutation,
- no queue resizing,
- no scenario execution,
- no persistence,
- no external API.

## 2. Dependencies

The change depends on delivered capabilities:

- `experiment-foundation`,
- `metrics-snapshot-and-recording`,
- `scenario-runner-and-baseline`.

## 3. Deferred Work

The following remain deferred:

- `executor-adapter-and-queue-resizing`,
- decision persistence,
- trend detection,
- cooldown state,
- analysis reports,
- production integration.

## 4. Execution Path

Next step after this version design:

1. Create `openspec/changes/adaptive-policy-and-control-gate/` using `superspec`.
2. Generate full artifacts: brainstorm, proposal, design, specs, tasks, plan.
3. Only then run apply.

## 5. Decomposition Guardrail

Do not split this version into multiple changes unless the user explicitly asks. The first policy change should stay small enough for a weak implementation agent:

- threshold config,
- gate evaluator,
- policy evaluator,
- decision output,
- tests.
