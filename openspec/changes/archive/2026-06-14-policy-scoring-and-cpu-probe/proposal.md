## Why

The system can now classify pressure states (pressure-classification-engine, change 1/2), but cannot evaluate which policy configuration is best suited for the current state. When multiple `ThresholdPolicyConfig` variants exist (different thresholds, different step sizes), there is no mechanism to score or rank them. Additionally, `PressureSnapshot.cpuUtilization` has been defined since v0.1.0 but always returns 0.0 because no real data source feeds it (v0.12.0 DFR-01). This change closes the diagnostic layer: policy scoring for state-appropriate strategy selection, and CPU utilization as a real data source.

## What Changes

- **PolicyScore record**: Composite score [0.0-1.0] with 4-dimensional breakdown (responsiveness, safety, stability, efficiency) and human-readable explanation
- **PolicyScorer interface + ThresholdPolicyScorer**: Rule-based heuristic scorer with configurable weights (default: 0.35/0.30/0.20/0.15). Uses utilizationRatio for consistency with classifier metrics
- **PolicyRanker**: Ranks multiple `ThresholdPolicyConfig` candidates by composite score, with `best()` convenience method
- **SystemCpuProbe**: JDK `ManagementFactory.getOperatingSystemMXBean()` wrapper providing `sampleProcessCpuLoad()` and `sampleSystemCpuLoad()`
- **RuntimeObservation modification**: Add `fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)` overload; original 2-arg method delegates internally to new overload with `new SystemCpuProbe()`

All new scoring components are in `experiment.classification` package. CPU probe is in new `experiment.probe` package. `RuntimeObservation` modification is backward-compatible.

## Capabilities

### New Capabilities
- `policy-scoring-and-ranking`: Multi-dimensional policy scoring (4 dimensions) with composite scoring and ranking of multiple policy configurations
- `cpu-utilization-probe`: Real CPU utilization data source via JDK ManagementFactory, integrated into RuntimeObservation → PressureSnapshot pipeline

### Modified Capabilities
- `runtime-observation-cpu-integration`: `RuntimeObservation.fromExecutor()` now provides real CPU utilization via optional SystemCpuProbe injection (backward-compatible — original 2-arg signature unchanged)

## Impact

- **New package**: `experiment.probe` (1 class)
- **Addition to existing package**: `experiment.classification` (4 types: 1 record, 1 interface, 1 class, 1 class)
- **New source files**: ~6 files (~400 lines)
- **Modified source files**: `RuntimeObservation.java` (~20 lines)
- **New test files**: ~7 unit/integration tests (~350 lines)
- **No changes to**: `ManagedExecutor`, `PressureSnapshot`, `DefaultSnapshotAssembler`, `ThresholdPolicyConfig`, `ThresholdPolicyEvaluator`
- **Breaking changes**: None (all additions are new or backward-compatible)
- **Dependencies**: No new external dependencies (JDK `java.lang.management` and `com.sun.management` only)
