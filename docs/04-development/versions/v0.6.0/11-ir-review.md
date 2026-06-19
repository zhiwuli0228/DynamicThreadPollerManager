# v0.6.0 IR Review

## Header

- Document type: IR review
- Version name: `v0.6.0`
- Review date: `2026-06-06`
- Review scope: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `15-experiment-data-acquisition-plan.md`, `decision-log.md`, and current-state authority
- Review mode: independent documentation review
- Conclusion: `ready for disposition`

## 输入包

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/04-development/versions/v0.6.0/README.md`
- `docs/04-development/versions/v0.6.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.6.0/10-ir.md`
- `docs/04-development/versions/v0.6.0/15-experiment-data-acquisition-plan.md`
- `docs/04-development/versions/v0.6.0/decision-log.md`
- `openspec/specs/scenario-runner-and-baseline/spec.md`
- `openspec/specs/metrics-snapshot-and-recording/spec.md`
- `openspec/specs/adaptive-policy-and-control-gate/spec.md`
- `openspec/specs/offline-replay-and-readiness-gate/spec.md`
- `openspec/specs/executor-adapter-and-adjustment-evidence/spec.md`

## Findings

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |

No P0/P1 findings were identified in this review pass.

## Non-Blocking Notes

- `10-ir.md` clearly keeps the scope at IR level and does not authorize Java implementation, OpenSpec change creation, or actual pressure test execution.
- `15-experiment-data-acquisition-plan.md` now explicitly defines scenario coverage, run repetition, environment fingerprint fields, raw evidence hygiene, and quality gates.
- `10-ir.md` and `15-experiment-data-acquisition-plan.md` together make the baseline pressure acquisition ask specific enough for a later SR without over-authorizing the current stage.
- `decision-log.md` correctly records the decision to start with pressure data acquisition rather than immediate mutation.
- The current-state file now authorizes `v0.6.0` SR functional design only and still does not authorize any OpenSpec change or Java implementation.

## 结论

`v0.6.0` IR 需求草案可以进入 disposition。没有阻塞性 findings；后续是否进入 SR 功能设计仍必须由 closure verification 和 current-state authority 共同确认。
