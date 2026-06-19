# Statistical Significance

## Overview
In-project statistical tests: paired t-test, Cohen's d effect size, and 95% confidence intervals. No external library dependencies.

## ADDED Requirements

### Requirement: Paired t-Test Computation
StatisticalSignificanceCalculator SHALL compute paired t-test p-values using Abramowitz & Stegun 26.2.17 approximation.

#### Scenario: Significant difference detected
- GIVEN modeA values significantly higher than modeB values (t > 2.0, n=30)
- WHEN compare(modeA, modeB, "throughput") is called
- THEN pValue < 0.05
- AND isSignificant == true

#### Scenario: No significant difference
- GIVEN modeA values nearly identical to modeB values (t ~ 0, n=30)
- WHEN compare() is called
- THEN pValue > 0.05
- AND isSignificant == false

### Requirement: Cohen's d Effect Size
StatisticalSignificanceCalculator SHALL compute Cohen's d effect size.

#### Scenario: Large effect
- GIVEN modeA mean significantly higher than modeB with small variance
- WHEN compare() is called
- THEN effectSize > 0.8 (large effect)

### Requirement: Edge Cases
StatisticalSignificanceCalculator SHALL handle edge cases gracefully.

#### Scenario: Insufficient samples
- GIVEN n < 2 samples
- WHEN compare() is called
- THEN isSignificant == false
- AND sampleSize == actual n

#### Scenario: Zero variance
- GIVEN all values identical in both modes (zero variance)
- WHEN compare() is called
- THEN isSignificant == false
- AND does not throw (no division by zero)

### Requirement: Accuracy
StatisticalSignificanceCalculator SHALL produce p-values accurate to ±0.01 of reference for n >= 10.

#### Scenario: Accuracy at df=10
- GIVEN reference p-value = 0.042 for t=2.228, df=10
- WHEN compare() computes p-value
- THEN computed p-value is within 0.032 and 0.052 (±0.01)
