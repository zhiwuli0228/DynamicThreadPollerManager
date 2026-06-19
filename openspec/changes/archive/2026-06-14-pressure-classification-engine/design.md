# Design: pressure-classification-engine

## Overview

This change implements the pressure classification engine: a 6-state pressure classifier with trend-aware time-series analysis. The full functional design is documented in `docs/04-development/versions/v0.13.0/20-sr.md` §4.1-4.6.

## Module Boundaries

| Module | Change | Component |
|---|---|---|
| `experiment.classification` | **New** | `PressureState` (enum) |
| `experiment.classification` | **New** | `ClassifierConfig` (record) |
| `experiment.classification` | **New** | `NormalizedPressureMetrics` (record) |
| `experiment.classification` | **New** | `PressureClassification` (record) |
| `experiment.classification` | **New** | `PressureClassifier` (interface) |
| `experiment.classification` | **New** | `SnapshotPressureClassifier` (class) |

## Dependency Direction

```text
experiment.classification (new package)
    ├── experiment.metrics (ObservedSnapshot, PressureSnapshot — read-only)
    └── (no other project dependencies)

experiment.metrics → experiment.classification: NO (classification is consumer of metrics)
experiment.policy → experiment.classification: NO (classification is orthogonal)
experiment.scenario → experiment.classification: NO (scenario is unchanged)
```

## Component Design Summary

### PressureState
- Enum: REJECTION_ACTIVE, OVERLOAD, QUEUE_BUILDUP, RECOVERY, UNDER_UTILIZED, NORMAL
- Declaration order = classification priority (highest first)
- Each value carries `description()` for human-readable evidence

### ClassifierConfig
- Record: trendWindowSize (>=2, default 5), queueGrowthThreshold (>0, default 0.1), rejectionWindowSize (>=1, default 10), queueCapacity (>=0 or MAX_VALUE, default MAX_VALUE)
- Static factory `defaults()` returns default instance
- queueCapacity used for OVERLOAD relative threshold (MAX_VALUE degrades to absolute condition)

### NormalizedPressureMetrics
- Record: 11 fields — 9 base (completedTaskCount, rejectedTaskCount, avgQueueDepth, maxQueueDepth, totalDurationMs, throughputPerSecond, avgActiveThreads, maxPoolSize, snapshotCount) + 2 derived (queueGrowthRate, threadUtilizationRatio)
- `fromSnapshots(List<ObservedSnapshot>, long totalDurationMs, int fallbackPoolSize, int trendWindowSize)`: computes all 11 metrics. Base 9 use same logic as NormalizedComparisonMetrics.fromSnapshots()
- `withRejectedTaskCount(long)`: returns new instance with updated rejectedTaskCount
- `toMap()`: 11-field LinkedHashMap for debug/assertion support
- `queueGrowthRate`: simple linear regression slope over recent trendWindowSize snapshots
- `threadUtilizationRatio`: avgActiveThreads / maxPoolSize (0.0 when maxPoolSize==0)

### PressureClassification
- Record: state (PressureState), confidence (double [0.0-1.0]), evidence (List\<String\>), metrics (NormalizedPressureMetrics), classifiedAt (Instant)

### PressureClassifier (interface)
```java
PressureClassification classify(
    List<ObservedSnapshot> snapshots,
    ClassifierConfig config,
    long rejectedTaskCount,
    long totalDurationMs);
```

### SnapshotPressureClassifier
- Stateless implementation of PressureClassifier
- Classification algorithm (priority order):
  1. rejectedTaskCount > 0 → REJECTION_ACTIVE (confidence 0.95)
  2. threadUtilizationRatio >= 0.8 + queue pressure → OVERLOAD
  3. queueGrowthRate > threshold + utilization < 0.8 → QUEUE_BUILDUP
  4. queueGrowthRate < -threshold + utilization < 0.5 + maxQueueDepth > 0 → RECOVERY (pure trend, no state)
  5. utilization < 0.3 + maxQueueDepth == 0 + no rejections → UNDER_UTILIZED
  6. Fallback → NORMAL
- Short-sequence confidence decay applied automatically via private `shortSequenceConfidenceFactor()`
