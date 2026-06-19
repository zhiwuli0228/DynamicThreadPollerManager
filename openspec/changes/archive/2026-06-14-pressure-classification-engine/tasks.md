# Tasks: pressure-classification-engine

## 1. PressureState

- [ ] 1.1 Create `PressureState` enum in `experiment.classification` with 6 values: REJECTION_ACTIVE, OVERLOAD, QUEUE_BUILDUP, RECOVERY, UNDER_UTILIZED, NORMAL
- [ ] 1.2 Add `description()` method returning human-readable Chinese description for each value
- [ ] 1.3 Write unit test: verify 6 values, priority order (ordinal), description() non-empty

## 2. ClassifierConfig

- [ ] 2.1 Create `ClassifierConfig` record in `experiment.classification` with fields: trendWindowSize (int, >=2, default 5), queueGrowthThreshold (double, >0, default 0.1), rejectionWindowSize (int, >=1, default 10), queueCapacity (int, >=0 or MAX_VALUE, default MAX_VALUE)
- [ ] 2.2 Add compact constructor validation
- [ ] 2.3 Add `defaults()` static factory returning default configuration
- [ ] 2.4 Write unit tests: valid construction, window<2 throws, threshold<=0 throws, capacity<0 throws, defaults() returns correct values

## 3. NormalizedPressureMetrics

- [ ] 3.1 Create `NormalizedPressureMetrics` record in `experiment.classification` with 11 fields: completedTaskCount (long), rejectedTaskCount (long), avgQueueDepth (double), maxQueueDepth (int), totalDurationMs (long), throughputPerSecond (double), avgActiveThreads (double), maxPoolSize (int), snapshotCount (int), queueGrowthRate (double), threadUtilizationRatio (double)
- [ ] 3.2 Implement `fromSnapshots(List<ObservedSnapshot>, long totalDurationMs, int fallbackPoolSize, int trendWindowSize)`: compute 9 base metrics (same logic as NormalizedComparisonMetrics), compute queueGrowthRate via simple linear regression over trendWindowSize, compute threadUtilizationRatio = avgActiveThreads/maxPoolSize
- [ ] 3.3 Implement `withRejectedTaskCount(long)`: returns new NormalizedPressureMetrics with updated rejectedTaskCount, all other fields preserved
- [ ] 3.4 Add `toMap()` method: 11-field LinkedHashMap for debug/assertion
- [ ] 3.5 Write unit tests: non-empty list computes all 11 fields, empty list returns zero defaults, zero totalDurationMs yields zero throughput, queueGrowthRate positive for increasing queue, negative for decreasing, ~0 for stable, withRejectedTaskCount() preserves other fields

## 4. PressureClassification

- [ ] 4.1 Create `PressureClassification` record in `experiment.classification` with fields: state (PressureState), confidence (double [0.0-1.0]), evidence (List\<String\>), metrics (NormalizedPressureMetrics), classifiedAt (Instant)
- [ ] 4.2 Add compact constructor validation: non-null state, confidence in [0.0-1.0], non-null evidence, non-null metrics, non-null classifiedAt
- [ ] 4.3 Write unit tests: valid construction, confidence out of range throws, null fields throw

## 5. PressureClassifier + SnapshotPressureClassifier

- [ ] 5.1 Create `PressureClassifier` interface in `experiment.classification` with method: `PressureClassification classify(List<ObservedSnapshot> snapshots, ClassifierConfig config, long rejectedTaskCount, long totalDurationMs)`
- [ ] 5.2 Create `SnapshotPressureClassifier` class implementing PressureClassifier — stateless
- [ ] 5.3 Implement classification algorithm (priority order): REJECTION_ACTIVE (rejectedTaskCount>0) → OVERLOAD (utilization>=0.8 + queue pressure) → QUEUE_BUILDUP (growth>threshold + utilization<0.8) → RECOVERY (growth<-threshold + utilization<0.5 + maxQueue>0) → UNDER_UTILIZED (utilization<0.3 + empty queue + no rejections) → NORMAL (fallback)
- [ ] 5.4 OVERLOAD: handle queueCapacity==MAX_VALUE (unbounded → absolute check), queueCapacity==0 (SynchronousQueue → utilization only), queueCapacity>0 (relative check)
- [ ] 5.5 Implement shortSequenceConfidenceFactor() as private method, applied automatically in build() helper
- [ ] 5.6 Implement confidence calculation: condition hit → 0.85-0.95, borderline → 0.60-0.80; short-sequence decay applied automatically
- [ ] 5.7 Write unit tests: classify UNDER_UTILIZED (activeThreads=0, queueEmpty), NORMAL (activeThreads=2, queue=2), QUEUE_BUILDUP (increasing queue 2→10), OVERLOAD (utilization>=0.8 + maxQueue>=capacity*0.5), REJECTION_ACTIVE (rejectedTaskCount>0), RECOVERY (decreasing queue from previous elevated state), empty list→NORMAL, short-sequence→lower confidence

## 6. Full Test Verification

- [ ] 6.1 Run `mvn test` — verify all 708 existing tests pass (zero regression)
- [ ] 6.2 Verify all new tests pass
