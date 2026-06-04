# v0.3.0 Version Design

## Header

- Version name: `v0.3.0`
- Authoring date: `2026-06-03`
- Status: `IMPLEMENTED`
- Authoritative branch: `claude_master`

## Purpose

`v0.3.0` defines the next bounded capability after deterministic baseline execution: `adaptive-policy-and-control-gate`.

The version introduces policy evaluation and safety gating only. It must produce reasoned `ScaleDecision` records from recorded pressure evidence, but it must not mutate executors, resize queues, schedule scenarios, persist output, or expose external APIs.

## Document Set

1. `00-objectives-and-scope.md`
2. `01-requirements-and-use-cases.md`
3. `02-solution-design.md`
4. `03-api-and-observability-design.md`
5. `04-testing-and-acceptance-design.md`
6. `05-change-decomposition-plan.md`
7. `decision-log.md`

## Next Change Candidate

- OpenSpec change candidate: `adaptive-policy-and-control-gate`
- Schema: `superspec`
- Current authorization: `ARCHIVED`

This version package authorized `adaptive-policy-and-control-gate`; the change has been implemented, archived, and synchronized to `openspec/specs/adaptive-policy-and-control-gate/spec.md`. It does not authorize executor mutation or a neighboring change.

## Boundary Summary

Allowed:

- policy configuration,
- pressure evaluation,
- threshold-based scale recommendation,
- safety gate evaluation,
- `ScaleDecision` creation,
- rejection / hold decisions with explicit reasons,
- tests for deterministic policy behavior and boundary isolation.

Not allowed:

- applying decisions to executors,
- queue capacity resizing,
- scenario generation,
- workload execution,
- external metrics dependencies,
- persistence, UI, REST API, or production thread-pool management.
