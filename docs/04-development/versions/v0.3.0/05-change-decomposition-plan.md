# v0.3.0 Change Decomposition Plan

## Header

- Version name: `v0.3.0`
- Status: `IMPLEMENTED`
- Authorization status: `adaptive-policy-and-control-gate` has been implemented, archived, and synchronized to `openspec/specs/`

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

## 4. Execution Result

Result after this version design:

1. `adaptive-policy-and-control-gate` was implemented.
2. The change was verified with the tests and evidence listed in `04-testing-and-acceptance-design.md`.
3. The delta spec was synchronized to `openspec/specs/adaptive-policy-and-control-gate/spec.md`.
4. The change was archived under `openspec/changes/archive/2026-06-04-adaptive-policy-and-control-gate/`.

## 5. Decomposition Guardrail

Do not split this version into multiple changes unless the user explicitly asks. The first policy change should stay small enough for a weak implementation agent:

- threshold config,
- gate evaluator,
- policy evaluator,
- decision output,
- tests.
