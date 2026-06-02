## Why

The experiment foundation and metrics layers are present, but the project still lacks a deterministic way to run workload scenarios. Without a repeatable baseline runner, future adaptive policy work cannot compare behavior against fixed execution evidence. This change creates that repeatable baseline without adding control or mutation logic.

## What Changes

**Deterministic scenario planning**
- From: scenarios are represented only as simple identifiers and descriptions.
- To: scenarios will have profiles, seeds, step counts, and deterministic ordered workload steps.
- Reason: future policy evaluation needs repeatable workload inputs.
- Impact: non-breaking internal addition.

**Fixed baseline execution**
- From: no baseline execution path exists.
- To: a fixed baseline executor preset and workload executor will run scenario steps without resizing.
- Reason: adaptive comparison requires a non-adaptive baseline.
- Impact: non-breaking internal addition.

**Scenario experiment runner**
- From: experiment lifecycle and metrics recording exist separately.
- To: a runner will create/start/stop/finalize runs while sampling and recording evidence during scenario playback.
- Reason: downstream changes need an integrated baseline run path.
- Impact: non-breaking internal addition that depends on delivered foundation and metrics capabilities.

## Capabilities

### New Capabilities

- `scenario-runner-and-baseline`: deterministic scenario definitions, repeatable scenario planning, fixed baseline execution, and runner orchestration that records metrics evidence.

### Modified Capabilities

- none

## Impact

- Affected code: new scenario package under `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/`.
- Affected APIs: internal Java model and runner contracts only.
- Affected dependencies: none.
- Affected systems: later adaptive policy and executor adapter changes will use baseline scenario evidence.
