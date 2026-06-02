# v0.2.0 Version Design

## Header

- Version name: `v0.2.0`
- Authoring date: `2026-06-02`
- Status: `BASELINE_DELIVERED`
- Authoritative branch: `claude_master`

## Purpose

`v0.2.0` is a narrow successor version that authorizes the third experimental capability: `scenario-runner-and-baseline`.

The version intentionally does not authorize adaptive policy, executor mutation, queue resizing, or external observability. Its only implementation goal is to make experiments repeatable by adding deterministic scenario definitions, workload playback, a fixed baseline executor preset, and an orchestration runner that can connect the existing foundation and metrics layers.

## Document Set

1. `00-objectives-and-scope.md`
2. `01-requirements-and-use-cases.md`
3. `02-solution-design.md`
4. `03-api-and-observability-design.md`
5. `04-testing-and-acceptance-design.md`
6. `05-change-decomposition-plan.md`
7. `decision-log.md`

## Authorized Change

- OpenSpec change: `scenario-runner-and-baseline`
- Schema: `superspec`
- Authorization: `BASELINE_DELIVERED` (archived 2026-06-03)

## Boundary Summary

Allowed:

- deterministic scenario definitions,
- bounded workload step generation,
- baseline executor preset,
- scenario playback orchestration,
- integration with existing `ExperimentCoordinator`, `PressureSampler`, and `EvidenceRecorder`,
- tests for repeatability, lifecycle, and metrics recording handoff.

Not allowed:

- adaptive policy evaluation,
- scale decisions,
- executor resizing,
- queue capacity mutation,
- external metrics dependencies,
- UI or REST API.
