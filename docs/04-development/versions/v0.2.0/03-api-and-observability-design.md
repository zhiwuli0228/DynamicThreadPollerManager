# v0.2.0 API and Observability Design

## Header

- Version name: `v0.2.0`
- Status: `EXECUTION_AUTHORIZED`
- Authorized change: `scenario-runner-and-baseline`

## 1. API Surface

No REST or external API is authorized.

The API surface is internal Java only:

- scenario model classes,
- planner interface,
- baseline executor preset,
- runner class,
- run outcome value object.

## 2. Internal Contract Expectations

### ScenarioPlan

Must expose:

- scenario id,
- ordered steps,
- total work units.

### ScenarioRunOutcome

Must expose:

- run id,
- scenario id,
- policy id,
- completed step count,
- total work units,
- recorded evidence count,
- final state or completion flag.

## 3. Observability

This version uses existing metrics evidence recording as its observability surface.

The runner must record snapshots through `EvidenceRecorder`. It must not write files, publish metrics externally, or expose a dashboard.

## 4. Logging

No logging framework changes are authorized. If implementation needs diagnostics, prefer test-visible return values over logs.

## 5. Future Extension Points

Later versions may add:

- persisted run output,
- CLI or REST trigger,
- richer analysis summary,
- comparison between baseline and adaptive policy.

These are explicitly not part of `v0.2.0`.
