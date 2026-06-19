# policy-scoring-and-ranking

## ADDED Requirements

### Requirement: PolicyScorer SHALL score policy configurations against pressure classifications

The system MUST provide a `PolicyScorer` interface and `ThresholdPolicyScorer` implementation that evaluates a `ThresholdPolicyConfig` against a `PressureClassification` and produces a `PolicyScore` with 4-dimensional breakdown.

#### Scenario: OVERLOAD state favors aggressive scale-up policy
- **GIVEN** a PressureClassification with state=OVERLOAD and high threadUtilizationRatio
- **WHEN** scoring an aggressive policy (low scaleUpActiveThreadsThreshold) and a conservative policy (high threshold)
- **THEN** the aggressive policy has higher `responsivenessScore`

#### Scenario: UNDER_UTILIZED state favors conservative scale-down policy
- **GIVEN** a PressureClassification with state=UNDER_UTILIZED and low threadUtilizationRatio
- **WHEN** scoring a conservative policy and an aggressive policy
- **THEN** the conservative policy has higher `efficiencyScore`

#### Scenario: Capacity-insufficient policy has reduced safety score
- **GIVEN** a PressureClassification where metrics.maxPoolSize() exceeds config.maxPoolSize()
- **WHEN** scoring that config
- **THEN** `safetyScore` is reduced from 1.0

#### Scenario: All dimension scores within valid range
- **GIVEN** any valid PressureClassification and any valid ThresholdPolicyConfig
- **WHEN** `scorer.score(classification, config)` is called
- **THEN** all scores (composite, responsiveness, safety, stability, efficiency) are in [0.0, 1.0]

#### Scenario: Composite score equals weighted sum
- **GIVEN** default weights (0.35/0.30/0.20/0.15)
- **WHEN** `scorer.score(classification, config)` is called
- **THEN** `compositeScore` equals `responsivenessScore*0.35 + safetyScore*0.30 + stabilityScore*0.20 + efficiencyScore*0.15` within floating-point tolerance

### Requirement: PolicyRanker SHALL rank multiple policy configs by score

The `PolicyRanker` class MUST score and sort multiple `ThresholdPolicyConfig` candidates against a single `PressureClassification`, returning results in descending composite score order.

#### Scenario: Rank returns descending order
- **GIVEN** 3 different ThresholdPolicyConfig instances and a PressureClassification
- **WHEN** `ranker.rank(classification, candidates)` is called
- **THEN** the returned list has 3 PolicyScore entries sorted by `compositeScore` descending

#### Scenario: best returns highest scoring policy
- **GIVEN** 3 ranked policy configs
- **WHEN** `ranker.best(classification, candidates)` is called
- **THEN** the returned Optional contains the PolicyScore with the highest compositeScore

#### Scenario: Empty candidates returns empty results
- **GIVEN** an empty list of candidates
- **WHEN** `ranker.rank(classification, emptyList)` is called
- **THEN** an empty list is returned
- **WHEN** `ranker.best(classification, emptyList)` is called
- **THEN** `Optional.empty()` is returned
