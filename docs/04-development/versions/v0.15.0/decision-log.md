# v0.15.0 Decision Log

## D1: Coordination Model — Centralized GroupCoordinator vs Distributed Peer-to-Peer

**Background**: Multiple `AdjustmentLoop` instances running concurrently need to coordinate resource allocation. The coordination model determines how adjustment decisions are serialized and conflicts resolved.

**Options**:
- A: Centralized `GroupCoordinator` — one coordinator instance receives all adjustment decisions, evaluates them against the shared budget, and returns approved/modified/rejected results. Individual loops call `coordinator.coordinate(decision, executor)` before applying decisions.
- B: Distributed peer-to-peer — each executor's loop communicates directly with other loops, negotiating resource allocation through a consensus protocol (e.g., each loop broadcasts its intent, waits for responses, resolves conflicts locally).
- C: Token-based — a rotating coordination token; only the token-holder can adjust, others wait.

**Decision**: A — Centralized `GroupCoordinator`.

**Rationale**:
- Centralized coordination (A) provides a single serialization point for all adjustment decisions — no distributed consensus protocol needed, no message passing overhead, no split-brain scenarios.
- Distributed peer-to-peer (B) introduces significant complexity (consensus, timeout handling, partial failure) with no benefit for a single-JVM scenario. All executors are in the same process — there is no network partition risk that would justify distributed coordination.
- Token-based (C) is too restrictive — it serializes ALL adjustments, preventing independent scale-down decisions that don't compete for resources. This creates unnecessary adjustment latency.
- Centralized coordinator is a natural extension of v0.7.0's `ExecutorRegistry` — the registry already knows about all executors. The coordinator adds decision-making on top of that knowledge.
- For v0.15.0, all executors are in-process. If cross-JVM coordination is ever needed (v0.16.0+), the centralized coordinator can be extended with a remoting layer without changing the coordination algorithm.
- Testing is simpler: mock the coordinator, verify the loop calls `coordinate()` with the right decision. No need to simulate multi-node consensus.

**Implications**: `GroupCoordinator` is a single instance per `ExecutorGroup`. It is thread-safe (synchronized methods or lock-based) since multiple loop threads call `coordinate()` concurrently. The coordinator does NOT block the caller's thread — it evaluates quickly (budget check + oscillation pattern check) and returns immediately.

---

## D2: Resource Budgeting Strategy — Static vs Dynamic vs Priority-Based

**Background**: When multiple executors share a resource budget (e.g., max total threads), the coordinator needs a strategy to allocate resources when demand exceeds supply.

**Options**:
- A: Static allocation — each executor gets a fixed share of the total budget, defined upfront. No runtime reallocation.
- B: Dynamic allocation — budget shifts between executors based on real-time pressure. An executor under high pressure can claim budget from one under low pressure, regardless of priority.
- C: Priority-based allocation — each executor has a fixed priority (CRITICAL/HIGH/NORMAL/LOW). Higher-priority executors can preempt lower-priority ones when budget is constrained. Otherwise, first-come-first-served.

**Decision**: C — Priority-based allocation, with a static floor for each executor.

**Rationale**:
- Static allocation (A) is too rigid — it can't respond to shifting workload patterns. If executor-A is under overload while executor-B is idle, static allocation leaves resources stranded.
- Dynamic allocation (B) without priority can lead to undesirable preemption — a background task executor should not preempt a request-processing executor under any workload condition.
- Priority-based allocation (C) provides clear, predictable semantics: critical workloads always get resources first; low-priority workloads yield under contention. This matches real-world operational priorities.
- Priority is assigned per executor, not per adjustment — it reflects the business importance of the workload, not the current pressure state.
- The "static floor" ensures no executor can be preempted below its `minPoolSize` — even a LOW priority executor retains its minimum capacity.
- Priority is a v0.15.0 simplification; a future version could add dynamic priority based on real-time metrics (v0.16.0+ candidate).

**Preemption rules**:
| Requestor priority | Can preempt |
|---|---|
| CRITICAL | HIGH, NORMAL, LOW |
| HIGH | NORMAL, LOW |
| NORMAL | LOW |
| LOW | (none) |

**Implications**: `AdjustmentPriority` enum with 4 levels. `ResourceBudget.reserve()` accepts an optional priority parameter. Preemption is communicated to the preempted executor's loop via a `PREEMPT` signal, triggering an immediate adjustment cycle.

---

## D3: Cross-Validation Methodology — Paired Comparison vs Independent Runs

**Background**: To prove closed-loop effectiveness, we must compare closed-loop outcomes against static-policy and no-adjustment baselines. The methodology determines how these three modes are executed and compared.

**Options**:
- A: Paired comparison — run the same workload sequentially through all three modes on the same executor configuration, then compare side-by-side metrics. Reuse v0.12.0's `ComparableScenarioRunner`.
- B: Independent runs — run each mode independently with its own executor and workload instance. Compare aggregate statistics.
- C: Concurrent comparison — run all three modes simultaneously on separate executors, sharing the same workload generator.

**Decision**: A — Paired comparison (sequential, same workload).

**Rationale**:
- Paired comparison (A) ensures the workload is identical across all three modes — same task arrival pattern, same task duration distribution, same warmup. This eliminates workload variance as a confounding factor.
- v0.12.0's `ComparableScenarioRunner` already supports paired comparison with identical workloads — v0.15.0 reuses this infrastructure directly.
- Independent runs (B) introduce workload variance — the random seed, timing, and system state differ between runs. This requires larger sample sizes to achieve statistical power.
- Concurrent comparison (C) introduces resource contention between modes — they compete for CPU, memory, and I/O, making it impossible to attribute differences to the adjustment strategy vs resource starvation.
- Sequential execution (A) is acceptable because each mode runs for a configurable duration (default 60s) and the total validation time (~3 minutes for 3 × 60s) is reasonable for a development/test context.
- The primary limitation of (A) is that system state (JVM warmup, GC patterns) can drift across sequential runs. Mitigation: warmup period before each mode, and randomized mode execution order to cancel ordering effects.

**Implications**: `ClosedLoopValidationRunner` executes modes sequentially. Mode order is configurable but defaults to: baseline → static-policy → closed-loop (increasing sophistication). Each mode includes a warmup period (default 10s) before measurements begin.

---

## D4: Adjustment Conflict Resolution — Serialize vs Merge vs Reject

**Background**: When two executors simultaneously request scale-up and the budget cannot satisfy both, the coordinator must resolve the conflict.

**Options**:
- A: Serialize — process decisions in arrival order. First request gets budget; second waits or gets rejected.
- B: Merge — combine both requests into a single allocation that partially satisfies each.
- C: Priority reject — lower-priority request is rejected (or capped); higher-priority gets full allocation.

**Decision**: C — Priority-based rejection with optional cap.

**Rationale**:
- Serialize (A) is unfair — a LOW priority executor that arrives first can block a CRITICAL executor that arrives 1ms later. Order of arrival should not override business priority.
- Merge (B) is complex and can produce suboptimal outcomes — both executors get partial allocations, potentially leaving both under-provisioned. "Half-allocating to both" is worse than "fully allocating to one" in many scenarios.
- Priority reject (C) aligns with D2's priority-based budgeting. The coordinator evaluates decisions in priority order, not arrival order.
- Within the same priority level, first-come-first-served is used as a tiebreaker.
- The "cap" variant: if the requestor has higher priority but the budget gap is small, the lower-priority executor is partially preempted (not fully rejected). This is more nuanced than binary reject/approve.

**Coordination queue semantics**:
```
coordinate(decision, executor):
  1. If delta <= 0 → approve immediately (no conflict possible)
  2. If budget.available >= delta → reserve, approve
  3. If budget.available < delta:
     a. Scan all executors with lower priority, sorted by priority (lowest first)
     b. Calculate total preemptible budget from lower-priority executors
     c. If preemptible >= delta → preempt, allocate, return MODIFIED for preempted executors
     d. If preemptible < delta but > 0 → preempt what we can, cap the rest, return CAPPED
     e. If preemptible == 0 → return REJECTED
```

**Implications**: `GroupCoordinationResult.outcome()` can be `APPROVED_AS_IS`, `MODIFIED` (preempted others), `CAPPED` (partial allocation), or `REJECTED`. Preempted executors receive a `PREEMPT` signal with the required scale-down target.

---

## D5: Integration Pattern — Decorator Adapter vs Modify AdjustmentLoop

**Background**: The `GroupCoordinator` needs to intercept adjustment decisions between `AdjustmentLoop` and `AdjustmentAdapter`. The integration pattern determines whether v0.14.0 code needs modification.

**Options**:
- A: Decorator adapter — create `CoordinatedAdjustmentAdapter` that wraps `ExecutorAdjustmentAdapter` and injects coordination before delegation. `AdjustmentLoop` is unchanged.
- B: Modify `AdjustmentLoop` — add an optional `GroupCoordinator` field to `AdjustmentLoop`, call `coordinator.coordinate()` in the main loop before `adapter.apply()`.
- C: Callback/Interceptor — define a `DecisionInterceptor` interface. `AdjustmentLoop` accepts an optional interceptor. `GroupCoordinator` implements the interceptor.

**Decision**: A — Decorator adapter (wrapper pattern).

**Rationale**:
- Decorator (A) maintains zero code modification to v0.14.0 components. `AdjustmentLoop` continues to call `adapter.apply(command)` — it doesn't need to know about coordination.
- Modifying `AdjustmentLoop` (B) adds a coordination concern to a class whose single responsibility is "run the closed loop for one executor." Coordination is a separate concern and belongs in a separate component.
- Callback/Interceptor (C) is essentially option B with a different name — it still requires `AdjustmentLoop` to carry an interceptor reference and call it.
- The decorator approach is consistent with the project's established pattern: v0.10.0's `RejectionPolicyAdjustmentAdapter` wrapped the base adapter; v0.9.0's `QueueResizeAdjustmentAdapter` wrapped it. The adapter chain is a proven extension point.
- The decorator is transparent to `AdjustmentLoop` — the loop's constructor receives an `ExecutorAdjustmentAdapter`, which could be the raw adapter or the coordinated wrapper.

**Decorator chain**:
```
AdjustmentLoop
  → ExecutorAdjustmentAdapter (interface)
    → CoordinatedAdjustmentAdapter (new — intercepts, calls GroupCoordinator)
      → ManagedExecutorAdjustmentAdapter (existing — applies to TPE)
```

**Implications**: `CoordinatedAdjustmentAdapter` is part of Change 1. It composes `GroupCoordinator` and delegates to the real adapter. `AdjustmentLoop` source code is unchanged.

---

## D6: Change Decomposition Strategy

**Background**: v0.15.0 contains two sub-capabilities: multi-executor coordination (group, coordinator, budget, priority) and closed-loop validation (3-way comparison runner, statistical tests, reports).

**Options**:
- A: Single change (`multi-executor-coordination-and-validation`)
- B: Dual change (`multi-executor-coordination` → `closed-loop-validation-and-evidence`)

**Decision**: B — Dual change.

**Rationale**:
- Change 1 (`multi-executor-coordination`) can be independently compiled and tested: `ExecutorGroup`, `GroupCoordinator`, `ResourceBudget`, `GroupLoopOrchestrator` all work with mock `AdjustmentLoop` instances. Budget enforcement and priority preemption are algorithmic — testable without real executors.
- Change 2 (`closed-loop-validation-and-evidence`) depends on Change 1 for `ExecutorGroup` types, and on v0.12.0 for `ComparableScenarioRunner`. The validation logic (3-mode execution, metric comparison, statistical tests) is independent of coordination internals.
- Single change (A) would be large — coordination (~8 types) + validation (~4 types) + tests (~80+ tests). Dual change makes review and testing more manageable.
- Follows the established pattern from v0.11.0 through v0.14.0 (all dual-change versions).
- Each change has a distinct verification target:
  - Change 1: "Can we coordinate multiple executors under a shared budget with priority-based allocation?"
  - Change 2: "Does closed-loop adjustment produce measurably better outcomes than alternatives?"

**Implications**: 2 OpenSpec changes. Change 1 creates `experiment.coordination` package. Change 2 creates `experiment.validation` package and depends on both Change 1 and v0.12.0 comparison infrastructure.

---

## D7: Statistical Significance Framework

**Background**: The cross-validation report must include statistical significance tests to distinguish real improvement from random variation.

**Options**:
- A: Full statistical framework (t-test, Mann-Whitney U, Cohen's d, confidence intervals) — implemented in-project
- B: Simple heuristics (mean difference > X%, report is "significant")
- C: External library (Apache Commons Math) for statistical tests

**Decision**: A — In-project implementation of core statistical tests (paired t-test + Cohen's d + 95% CI).

**Rationale**:
- Simple heuristics (B) lack rigor — a 5% mean difference could be noise. The project needs credible evidence, not hand-waving.
- External library (C) violates the project's no-external-dependency constraint (see `operational-and-evolution-boundaries.md`).
- The required statistics are straightforward to implement:
  - Paired t-test: t = mean_diff / (stddev_diff / sqrt(n)), p-value from t-distribution CDF
  - Cohen's d: (mean_A - mean_B) / pooled_stddev
  - 95% CI: mean_diff ± t_critical * (stddev_diff / sqrt(n))
- t-distribution CDF can be approximated using Abramowitz and Stegun approximation (or a simple lookup table for common n values).
- Sample sizes are small (30-60 snapshots per mode), so the approximations are adequate.
- Statistical code is isolated in `StatisticalSignificanceCalculator` — can be replaced with a library later without changing the validation runner.

**Limitations acknowledged**:
- t-test assumes normality of differences. For small samples (n < 30), this assumption matters. Mitigation: require minimum 30 snapshots per mode.
- Paired t-test is sensitive to outliers. Mitigation: report both mean and median comparisons.
- Multiple comparison correction (Bonferroni) is not applied — the report compares 7 metrics, increasing familywise error rate. This is acceptable for a development/experiment context; a production system would apply correction.

**Implications**: `StatisticalSignificanceCalculator` is a standalone utility class with pure functions. No external dependencies.

---

## DFR: Deferred Items

| ID | Description | Rationale | Target version |
|---|---|---|---|
| DFR-01 | Cross-JVM / distributed coordination | Single-JVM coordination is the natural first step. Distributed coordination introduces network partition tolerance, RPC serialization, and leader election — each a significant design effort. | Candidate v0.16.0+ |
| DFR-02 | Dynamic priority rebalancing | Priority is static per executor in v0.15.0. Real-time priority adjustment based on workload criticality (e.g., request-processing priority increases during peak load) requires additional classification infrastructure. | Candidate v0.17.0+ |
| DFR-03 | Group-level feedback weight calibration | Per-executor weight calibration (v0.14.0) continues to operate. Group-level calibration could detect that a policy works well for one executor but poorly for another, and adjust weights accordingly. | Candidate v0.17.0+ |
| DFR-04 | Closed-loop performance benchmarks (DFR-02 from v0.14.0) | Benchmarking the coordination overhead (coordinator latency, budget check cost) is valuable but lower priority than proving correctness. | Candidate v0.16.0 |
| DFR-05 | Multi-group coordination (groups of groups) | Hierarchical coordination where executors belong to subgroups, and subgroups belong to a parent group. Adds complexity without a clear use case in the current scope. | Candidate v0.18.0+ |
| DFR-06 | Automated best-static-policy selection for validation | The "best static policy" for validation is manually selected in v0.15.0. An automated search (run all candidate policies, pick the best-performing one) would be more rigorous. | Candidate v0.16.0 |
| DFR-07 | Policy auto-generation (DFR-03 from v0.14.0) | Still deferred — requires larger-scale historical data from coordinated multi-executor runs. | Candidate v0.17.0+ |
| DFR-08 | Multi-dimension simultaneous adjustment (DFR-06 from v0.14.0) | Still deferred — single-dimension adjustment per cycle remains the model. | Candidate v0.17.0+ |

## Carried-Forward Deferred Items

| Source | ID | Description | v0.15.0 disposition |
|---|---|---|---|
| v0.14.0 | DFR-01 | Multi-executor parallel closed-loop coordination | ✅ Addressed — ExecutorGroup + GroupCoordinator (Change 1) |
| v0.14.0 | DFR-02 | Closed-loop performance benchmarks | ❌ Deferred to v0.16.0 |
| v0.14.0 | DFR-03 | Policy auto-generation | ❌ Still deferred — v0.16.0+ |
| v0.14.0 | DFR-04 | Cross-validation via comparison runs | ✅ Addressed — ClosedLoopValidationRunner (Change 2) |
| v0.14.0 | DFR-06 | Multi-dimension simultaneous adjustment | ❌ Still deferred — v0.17.0+ |
