# pressure-state-classification

## ADDED Requirements

### Requirement: System SHALL classify executor pressure state from snapshot sequences

The system MUST provide a `PressureClassifier` interface and `SnapshotPressureClassifier` implementation that analyzes `List<ObservedSnapshot>` time series and produces a `PressureClassification` with one of 6 semantic states, confidence score, and supporting evidence.

#### Scenario: Classify UNDER_UTILIZED state
- **GIVEN** a list of 5+ snapshots with activeThreads=0, queueSize=0, and rejectedTaskCount=0
- **WHEN** `classifier.classify(snapshots, config, 0, durationMs)` is called
- **THEN** a `PressureClassification` is returned with `state=UNDER_UTILIZED` and `confidence > 0.8`

#### Scenario: Classify NORMAL state
- **GIVEN** a list of 5+ snapshots with activeThreads=2, queueSize=2, maxPoolSize=8, and rejectedTaskCount=0
- **WHEN** `classifier.classify(snapshots, config, 0, durationMs)` is called
- **THEN** a `PressureClassification` is returned with `state=NORMAL`

#### Scenario: Classify QUEUE_BUILDUP state via trend
- **GIVEN** a list of 5+ snapshots with queueSize monotonically increasing (2→4→6→8→10), activeThreads=3, maxPoolSize=8, rejectedTaskCount=0
- **WHEN** `classifier.classify(snapshots, config, 0, durationMs)` is called
- **THEN** a `PressureClassification` is returned with `state=QUEUE_BUILDUP`

#### Scenario: Classify OVERLOAD state
- **GIVEN** a list of 5+ snapshots with activeThreads=7, queueSize=15, maxPoolSize=8, queueCapacity=20, rejectedTaskCount=0
- **WHEN** `classifier.classify(snapshots, config, 0, durationMs)` is called
- **THEN** a `PressureClassification` is returned with `state=OVERLOAD`

#### Scenario: Classify REJECTION_ACTIVE state
- **GIVEN** a list of snapshots and `rejectedTaskCount=3`
- **WHEN** `classifier.classify(snapshots, config, 3, durationMs)` is called
- **THEN** a `PressureClassification` is returned with `state=REJECTION_ACTIVE` and `confidence >= 0.95`

#### Scenario: Classify RECOVERY state via pure trend
- **GIVEN** a list of 5+ snapshots with queueSize monotonically decreasing (10→9→...→1), activeThreads decreasing (5→4→...→1), maxPoolSize=8, maxQueueDepth>0, rejectedTaskCount=0
- **WHEN** `classifier.classify(snapshots, config, 0, durationMs)` is called
- **THEN** a `PressureClassification` is returned with `state=RECOVERY`

#### Scenario: Empty snapshots returns NORMAL with zero confidence
- **GIVEN** an empty snapshot list
- **WHEN** `classifier.classify(emptyList, config, 0, 0)` is called
- **THEN** a `PressureClassification` is returned with `state=NORMAL` and `confidence=0.0`

#### Scenario: Short sequence degrades confidence
- **GIVEN** a list of 2 snapshots where trendWindowSize=5
- **WHEN** `classifier.classify(snapshots, config, 0, durationMs)` is called
- **THEN** the returned `PressureClassification.confidence()` is reduced (confidence * 2/5)

### Requirement: PressureState SHALL define 6 ordered states with descriptions

The `PressureState` enum MUST contain exactly 6 values in priority order (REJECTION_ACTIVE > OVERLOAD > QUEUE_BUILDUP > RECOVERY > UNDER_UTILIZED > NORMAL), each with a non-empty `description()`.

#### Scenario: Enum has 6 values in correct order
- **WHEN** `PressureState.values()` is called
- **THEN** 6 values are returned in declaration order: REJECTION_ACTIVE, OVERLOAD, QUEUE_BUILDUP, RECOVERY, UNDER_UTILIZED, NORMAL

#### Scenario: Each value has non-empty description
- **WHEN** `PressureState.UNDER_UTILIZED.description()` is called
- **THEN** a non-blank string is returned

### Requirement: ClassifierConfig SHALL carry classification parameters

The `ClassifierConfig` record MUST carry trendWindowSize (>=2), queueGrowthThreshold (>0), rejectionWindowSize (>=1), and queueCapacity (>=0 or MAX_VALUE). A `defaults()` factory MUST return sensible defaults.

#### Scenario: Defaults returns expected values
- **WHEN** `ClassifierConfig.defaults()` is called
- **THEN** config has trendWindowSize=5, queueGrowthThreshold=0.1, rejectionWindowSize=10, queueCapacity=Integer.MAX_VALUE

#### Scenario: Invalid trendWindowSize throws
- **WHEN** `new ClassifierConfig(1, 0.1, 10, 100)` is called
- **THEN** `IllegalArgumentException` is thrown
