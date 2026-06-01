## Why

The delivered experiment foundation defines the run and result contracts, but later policy and executor changes still lack runtime evidence to consume. This change adds a bounded observation layer so experiments can collect, normalize, and summarize pressure snapshots before any adaptive decision or mutation logic is introduced.

## What Changes

**Runtime snapshot collection**
- From: experiment runs have lifecycle metadata but no real evidence stream.
- To: the system will collect timestamped runtime pressure snapshots associated with a run.
- Reason: adaptive policy and comparison work need observed data rather than guessed state.
- Impact: non-breaking addition that builds on the foundation contracts.

**Append-only evidence recording**
- From: result series exists as a foundation concept but has no recording owner.
- To: an observation component will append snapshots into a result series and expose the recorded stream for summary generation.
- Reason: later analysis requires ordered evidence with stable provenance.
- Impact: non-breaking; no external storage dependency is introduced.

**Summary generation from snapshots**
- From: summaries can describe lifecycle metadata only.
- To: summaries will include basic observation-derived signals such as sample count, time bounds, and executor pressure ranges.
- Reason: experiment results need enough evidence to compare fixed and adaptive behavior later.
- Impact: non-breaking internal capability.

## Capabilities

### New Capabilities

- `metrics-snapshot-and-recording`: runtime pressure sampling, normalized snapshot assembly, append-only evidence recording, and minimal summary generation for experiment runs.

### Modified Capabilities

- none

## Impact

- Affected code: new observation package under `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/`.
- Affected APIs: internal Java contracts for sampler, recorder, and summary builder.
- Affected dependencies: none planned.
- Affected systems: later scenario runner, adaptive policy, and executor adapter changes will consume this evidence stream.
