package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import java.util.Objects;

/**
 * Composite policy score with 4-dimensional breakdown.
 */
public record PolicyScore(
        String policyId,
        double compositeScore,
        double responsivenessScore,
        double safetyScore,
        double stabilityScore,
        double efficiencyScore,
        String explanation
) {
    public PolicyScore {
        Objects.requireNonNull(policyId, "policyId must not be null");
        if (policyId.isBlank()) {
            throw new IllegalArgumentException("policyId must not be blank");
        }
        validateScore(compositeScore, "compositeScore");
        validateScore(responsivenessScore, "responsivenessScore");
        validateScore(safetyScore, "safetyScore");
        validateScore(stabilityScore, "stabilityScore");
        validateScore(efficiencyScore, "efficiencyScore");
        Objects.requireNonNull(explanation, "explanation must not be null");
    }

    private static void validateScore(double score, String fieldName) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be in [0.0, 1.0], was " + score);
        }
    }
}
