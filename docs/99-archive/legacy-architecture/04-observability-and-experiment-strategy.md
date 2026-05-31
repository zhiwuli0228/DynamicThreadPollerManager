# Observability and Experiment Strategy

## 1. Experiment Objectives

The project should prove that dynamic thread-pool and scheduling ideas work under repeatable conditions, not just that interfaces exist.

## 2. Observation Model

Experiments should produce observable state through snapshots, metrics, records, or explicit outcomes that can be reviewed after execution.

## 3. Candidate Metrics

| Metric / Observation | Purpose | Candidate Phase |
|---|---|---|
| active thread count | Observe immediate concurrency usage | Executor experiment |
| current pool size | Observe actual expansion and shrinkage | Executor experiment |
| queue depth | Observe backlog risk | Executor experiment |
| completed task count | Observe throughput | Executor experiment |
| rejection count | Observe overload and policy effect | Executor experiment |
| task duration | Observe workload and timeout behavior | Workload experiment |
| last execution timestamp | Observe periodic task health | Scheduling experiment |
| schedule version mismatch count | Observe stale chain invalidation | Scheduling experiment |
| rebuild count | Observe recovery trigger | Recovery experiment |
| coordination acquisition result | Observe multi-node execution eligibility | Distributed experiment |

## 4. Controlled Workloads

- CPU-bound workload
- blocking / simulated I/O workload
- burst submission workload
- intentionally stalled scheduled task
- stale version invocation simulation
- later distributed contention simulation

## 5. Failure Injection Scenarios

Experiments should include invalid configuration, rejected work, missed execution windows, and stale version replay so the design can prove rejection and recovery behavior.

## 6. Verification Strategy

Actuator and Micrometer are candidate carrier technologies, but whether they belong in the first version is a unified design decision. Experiments should be repeatable and have clear expected outcomes. This phase is for design only; no metrics dependency or implementation should be added yet.

## 7. Tooling Decisions Deferred to V1 Design

- Which observation surfaces are public.
- Which metrics names and snapshot formats are exposed.
- Whether experiment controllers live inside the main app or in a separate harness path.

