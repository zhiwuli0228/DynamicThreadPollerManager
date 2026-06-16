# feedback-weight-calibration

## Purpose

Calibrate ThresholdPolicyScorer dimension weights based on actual adjustment outcomes using median-split correlation analysis.

## Requirements

### Requirement: FeedbackCalibrator Class

The system SHALL provide `FeedbackCalibrator` that returns new `ThresholdPolicyScorer` instances with calibrated weights.

#### Scenario: Calibration produces weight changes
- **GIVEN** an AdjustmentHistory with 10 entries where high responsiveness scores correlate with success
- **WHEN** calibrate() is called
- **THEN** the returned scorer has different weights from the input scorer
- **AND** the responsiveness weight increased (positive correlation)

#### Scenario: Weight sum equals 1.0
- **GIVEN** any valid calibration input
- **WHEN** calibrate() returns a new scorer
- **THEN** wR+wS+wSt+wE == 1.0 (within 0.001 tolerance)

#### Scenario: Weights stay within bounds
- **GIVEN** any valid calibration input
- **WHEN** calibrate() returns a new scorer
- **THEN** each weight is in [0.10, 0.50]

#### Scenario: Insufficient data returns same scorer
- **GIVEN** an AdjustmentHistory with fewer than windowSize entries
- **WHEN** calibrate() is called
- **THEN** the returned scorer is the same instance as the input scorer

#### Scenario: All-success history produces no correlation
- **GIVEN** 10 entries where all adjustments were successful
- **WHEN** calibrate() is called
- **THEN** weights remain close to original values (correlation ~0 for all dimensions)

### Requirement: ThresholdPolicyScorer Weight Getters

The system SHALL provide public weight getters on ThresholdPolicyScorer for FeedbackCalibrator access.

#### Scenario: Weight getters return constructor values
- **GIVEN** a ThresholdPolicyScorer constructed with (0.35, 0.30, 0.20, 0.15)
- **WHEN** wResponsiveness(), wSafety(), wStability(), wEfficiency() are called
- **THEN** returns 0.35, 0.30, 0.20, 0.15 respectively

#### Scenario: Getters accessible cross-package
- **GIVEN** a class outside the experiment.classification package
- **WHEN** calling wResponsiveness() on a ThresholdPolicyScorer instance
- **THEN** compiles and returns the value
