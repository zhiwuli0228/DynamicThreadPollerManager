# Design: policy-scoring-and-cpu-probe

## Overview

This change implements policy scoring/ranking and CPU utilization probe. The full functional design is documented in `docs/04-development/versions/v0.13.0/20-sr.md` §4.7-4.12.

## Module Boundaries

| Module | Change | Component |
|---|---|---|
| `experiment.classification` | **New** | `PolicyScore` (record) |
| `experiment.classification` | **New** | `PolicyScorer` (interface) |
| `experiment.classification` | **New** | `ThresholdPolicyScorer` (class) |
| `experiment.classification` | **New** | `PolicyRanker` (class) |
| `experiment.probe` | **New** | `SystemCpuProbe` (class) |
| `experiment.metrics` | **Modify** | `RuntimeObservation` — add `fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)` overload |

## Dependency Direction

```text
experiment.classification (scoring additions)
    ├── experiment.policy (ThresholdPolicyConfig — read-only)
    └── (self) PressureClassification, NormalizedPressureMetrics (from change 1)

experiment.probe (SystemCpuProbe)
    └── java.lang.management (ManagementFactory, OperatingSystemMXBean)

experiment.metrics (RuntimeObservation modification)
    └── experiment.probe (SystemCpuProbe — optional parameter in new overload)
```

## Component Design Summary

### PolicyScore
- Record: policyId, compositeScore, responsivenessScore, safetyScore, stabilityScore, efficiencyScore, explanation
- All scores validated as [0.0, 1.0] at construction
- No compositeScore ≈ weighted sum validation (defer to scorer implementation)

### PolicyScorer (interface)
```java
PolicyScore score(PressureClassification classification, ThresholdPolicyConfig config);
```
Implementation must guarantee compositeScore = weighted sum of dimension scores.

### ThresholdPolicyScorer
- Rule-based heuristic scorer with configurable weights (default: 0.35/0.30/0.20/0.15)
- Responsiveness: uses `threadUtilizationRatio` (consistent with classifier). OVERLOAD/QUEUE_BUILDUP → higher utilization → higher score. UNDER_UTILIZED/RECOVERY → lower utilization → higher score.
- Safety: capacity adequacy (maxPoolSize vs observed max), step reasonableness (scaleStep vs maxPoolSize*0.5), boundary reasonableness (maxPoolSize vs 128 cap)
- Stability: volatility match — high volatility (>0.5) + large step (>4) → low score
- Efficiency: config max vs observed max ratio — ratio<=1.5 → high, ratio<=3.0 → medium, ratio>3.0 → low

### PolicyRanker
- Constructor accepts PolicyScorer
- `rank(PressureClassification, List<ThresholdPolicyConfig>)` → List\<PolicyScore\> sorted by compositeScore descending
- `best(PressureClassification, List<ThresholdPolicyConfig>)` → Optional\<PolicyScore\>
- Stable sort: equal scores preserve input order

### SystemCpuProbe
- Constructor calls `ManagementFactory.getOperatingSystemMXBean()`
- `sampleProcessCpuLoad()`: via `com.sun.management.OperatingSystemMXBean.getProcessCpuLoad()`; returns 0.0 if unavailable
- `sampleSystemCpuLoad()`: via `OperatingSystemMXBean.getSystemLoadAverage()`; returns 0.0 if unavailable
- Zero external dependencies

### RuntimeObservation Modification
- New overload: `fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)` — uses probe for cpuUtilization
- Original 2-arg method delegates: `fromExecutor(executor, timestamp, new SystemCpuProbe())`
- CPU read failure → `MetricValue.absent()` (graceful degradation)
- `DefaultSnapshotAssembler` unchanged — already handles absent → 0.0
