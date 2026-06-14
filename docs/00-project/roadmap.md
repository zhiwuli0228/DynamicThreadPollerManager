# Roadmap

## Status

This roadmap is a candidate sequencing reference only. It is not an approval order.
It records the current evolution blueprint for later version design, OpenSpec
decomposition, and implementation authorization.

## Evolution Thesis

`DynamicThreadPollerManager` should evolve from a runnable dynamic-thread-pool
experiment into an evidence-driven managed-executor system that can prove why it
is better than a generally configured thread pool under controlled workload
scenarios.

The project should not compete with `ThreadPoolExecutor` by merely wrapping the
same knobs. Its advantage should come from:

- controlled workload scenarios;
- repeatable baseline comparison against common executor configurations;
- live pressure sampling and persistent evidence;
- explainable adaptive decisions;
- bounded, safety-checked runtime adjustment;
- reproducible reports that connect a decision to measurable outcome changes.

## Current Baseline

The implemented baseline already contains the core foundation needed for later
comparative evolution:

- managed executor and registry boundaries;
- runtime adjustment adapters for pool-size, queue-capacity, and rejection-policy
  dimensions;
- safety gates for runtime control;
- scenario execution support;
- live pressure sampling;
- persistent evidence recording;
- acquisition quality gates and report paths;
- archived verification records for v0.1.0 through v0.11.0.

Future work should treat these as the platform for measurement and comparison,
not as proof by themselves.

## Strategic Proof Model

The project should be able to answer this question with recorded evidence:

> Under the same workload and safety constraints, when does the managed executor
> produce better stability, latency, rejection, recovery, or operability outcomes
> than a common thread-pool baseline?

The proof model should compare:

- JDK `ThreadPoolExecutor` with fixed manual configuration;
- common fixed and cached executor presets where relevant;
- Spring `ThreadPoolTaskExecutor` style configuration where relevant;
- this project's managed executor with safety gates and adaptive policy.

The comparison should avoid vague "dynamic is better" claims. A future report
should state the scenario, baseline, managed policy, observed pressure, action
taken, and measured result delta.

## Candidate Version Sequence

| Candidate version | Theme | Primary proof |
|---|---|---|
| v0.12.0 | Baseline comparison experiment framework | The project can run the same workload against common executor baselines and managed executors, then persist comparable result records. |
| v0.13.0 | Pressure classification and policy scoring | The project can classify overload, underutilization, queue buildup, rejection pressure, and recovery states before taking action. |
| v0.14.0 | Adaptive closed-loop adjustment | The project can safely adjust pool-size, queue-capacity, or rejection-policy dimensions based on sampled pressure and bounded policy decisions. |
| v0.15.0 | Strategy explanation and experiment reporting | The project can explain why a decision was made and how the outcome changed after the decision. |
| v0.16.0 | Complex workload and rollback verification | The project can handle burst, long-tail, mixed, and downstream-blocked scenarios with cooldown, rollback, and anti-oscillation controls. |

This sequence is intentionally evidence-first. It prioritizes comparison and
measurement before adding more runtime knobs.

## Candidate Directions

1. Establish a baseline comparison framework for common executor configurations.
2. Define scenario profiles for CPU-bound, IO-bound, burst, long-tail, mixed, and
   downstream-blocked workloads.
3. Normalize result metrics across baseline executors and managed executors.
4. Add pressure classification before adding new policy actions.
5. Build adaptive decisions around explicit safety gates, cooldown windows,
   bounded adjustment steps, and rollback semantics.
6. Generate evidence reports that connect scenario, pressure, decision, and
   observed result delta.
7. Defer broad platform features until the managed-executor advantage is proven
   by reproducible experiments.

## Evaluation Metrics

Future version designs should prefer measurable outcomes such as:

- throughput;
- average latency;
- p95 and p99 latency;
- rejected task count;
- queue depth and queue wait duration;
- active thread count and pool-size movement;
- time to recover after a burst;
- adjustment count and adjustment success rate;
- unsafe or blocked adjustment count;
- oscillation frequency;
- before-and-after deltas around each adaptive decision.

## Acceptance Standards

Future version designs should define acceptance criteria against this blueprint
before any OpenSpec decomposition. At minimum, a version that claims progress on
the managed-executor advantage should satisfy these standards:

- The compared executor baselines are named explicitly.
- The workload scenario is reproducible from documented inputs.
- The same workload is executed against both the baseline executor and the
  managed executor.
- Result records use the same metric names, units, and sampling windows across
  compared executors.
- The report states whether the managed executor improved, matched, or regressed
  against the baseline for each primary metric.
- Any adaptive decision includes the observed pressure signal, selected action,
  safety-gate outcome, and before-and-after metric delta.
- A regression or no-improvement result is retained as evidence instead of being
  hidden or treated as a failed run by default.
- Existing archived behavior remains protected by the regression test suite
  required by the authorized version design.

For `v0.12.0`, the first acceptable slice should prove that at least one common
thread-pool baseline and one managed-executor run can be compared under the same
scenario with persisted, reviewable result artifacts.

## Deferred Directions

The following areas should remain out of scope until a later version design
explicitly needs them:

- frontend console;
- authentication;
- Redis, Kafka, or database-backed coordination;
- multi-node management;
- external monitoring-stack integration;
- virtual threads as the default execution mode;
- general-purpose scheduling or workflow orchestration.

These areas may become useful later, but they do not directly prove the managed
executor's superiority over common thread-pool baselines.

## v0.12.0 Starting Point

The next strong candidate is a version design for `v0.12.0` focused on baseline
comparison. Its draft scope should answer:

- Which executor baselines are compared?
- Which workload scenarios are mandatory for the first comparison slice?
- Which metrics are normalized across all executor types?
- What result artifact proves one run is comparable to another?
- What report shape lets a reviewer inspect advantage or failure without reading
  implementation internals?

Candidate deliverables for that version design:

- baseline executor catalog;
- comparable scenario runner extension;
- normalized result model;
- baseline-vs-managed report artifact;
- acceptance criteria requiring at least one scenario where managed behavior is
  compared against a common thread-pool baseline.

## Notes

- Do not treat candidate directions as authorized work.
- Do not infer implementation scope from archived bootstrap files.
- Do not create OpenSpec changes until a version design explicitly authorizes decomposition.
- Do not implement v0.12.0 or later work until `docs/00-project/current-state.md`
  authorizes the relevant version design and execution stage.
