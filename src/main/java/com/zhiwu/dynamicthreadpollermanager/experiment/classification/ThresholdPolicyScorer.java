package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.util.Objects;

/**
 * Rule-based heuristic policy scorer with 4 dimensions:
 * responsiveness, safety, stability, efficiency.
 */
public final class ThresholdPolicyScorer implements PolicyScorer {

    private static final int MAX_SAFE_POOL_SIZE = 128;

    private final double wResponsiveness;
    private final double wSafety;
    private final double wStability;
    private final double wEfficiency;

    public ThresholdPolicyScorer(
            double wResponsiveness, double wSafety,
            double wStability, double wEfficiency) {
        double sum = wResponsiveness + wSafety + wStability + wEfficiency;
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException(
                    "weights must sum to 1.0, was " + sum);
        }
        this.wResponsiveness = wResponsiveness;
        this.wSafety = wSafety;
        this.wStability = wStability;
        this.wEfficiency = wEfficiency;
    }

    public ThresholdPolicyScorer() {
        this(0.35, 0.30, 0.20, 0.15);
    }

    @Override
    public PolicyScore score(PressureClassification classification,
                              ThresholdPolicyConfig config) {
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(config, "config must not be null");

        NormalizedPressureMetrics metrics = classification.metrics();
        PressureState state = classification.state();

        double responsiveness = scoreResponsiveness(state, config, metrics);
        double safety = scoreSafety(config, metrics);
        double stability = scoreStability(config, metrics);
        double efficiency = scoreEfficiency(config, metrics);

        double composite = responsiveness * wResponsiveness
                + safety * wSafety
                + stability * wStability
                + efficiency * wEfficiency;

        String explanation = buildExplanation(state, config.policyId(),
                composite, responsiveness, safety, stability, efficiency);

        return new PolicyScore(
                config.policyId(), composite,
                responsiveness, safety, stability, efficiency,
                explanation);
    }

    private double scoreResponsiveness(PressureState state,
                                        ThresholdPolicyConfig config,
                                        NormalizedPressureMetrics metrics) {
        return switch (state) {
            case OVERLOAD, QUEUE_BUILDUP -> {
                double uScore = utilizationProximity(
                        metrics.threadUtilizationRatio(),
                        config.scaleUpActiveThreadsThreshold(),
                        config.maxPoolSize());
                double qScore = thresholdProximity(
                        metrics.maxQueueDepth(),
                        config.scaleUpQueueSizeThreshold(), false);
                yield (uScore + qScore) / 2.0;
            }
            case UNDER_UTILIZED, RECOVERY -> {
                double uScore = utilizationProximity(
                        metrics.threadUtilizationRatio(),
                        config.scaleDownActiveThreadsThreshold(),
                        config.maxPoolSize());
                yield 1.0 - uScore;
            }
            case REJECTION_ACTIVE -> 1.0;
            case NORMAL -> 0.7;
        };
    }

    private double scoreSafety(ThresholdPolicyConfig config,
                                NormalizedPressureMetrics metrics) {
        double score = 1.0;

        if (config.maxPoolSize() < metrics.maxPoolSize()) {
            score -= 0.4;
        }

        if (config.scaleStep() > config.maxPoolSize() * 0.5) {
            score -= 0.3;
        }

        if (config.maxPoolSize() > MAX_SAFE_POOL_SIZE || config.maxPoolSize() < 1) {
            score -= 0.3;
        }

        return clamp(score);
    }

    private double scoreStability(ThresholdPolicyConfig config,
                                   NormalizedPressureMetrics metrics) {
        double volatility = Math.abs(metrics.queueGrowthRate());
        int step = config.scaleStep();

        if (volatility > 0.5 && step > 4) return 0.2;
        if (volatility > 0.5 && step <= 4) return 0.6;
        if (volatility <= 0.5 && step > 4) return 0.7;
        return 0.9;
    }

    private double scoreEfficiency(ThresholdPolicyConfig config,
                                    NormalizedPressureMetrics metrics) {
        int observedMax = metrics.maxPoolSize();
        int configMax = config.maxPoolSize();

        if (observedMax == 0) return 0.7;

        double ratio = (double) configMax / observedMax;

        if (ratio <= 1.5) return 0.9;
        if (ratio <= 3.0) return 0.6;
        return 0.3;
    }

    private static double utilizationProximity(
            double utilizationRatio, int scaleThreshold, int maxPoolSize) {
        if (maxPoolSize == 0) return 0.5;
        double thresholdRatio = (double) scaleThreshold / maxPoolSize;
        double proximity = 1.0 - Math.abs(utilizationRatio - thresholdRatio);
        return clamp(proximity);
    }

    private static double thresholdProximity(
            double actual, double threshold, boolean higherIsBetter) {
        if (threshold == 0) return 0.5;
        double ratio = actual / threshold;
        if (higherIsBetter) {
            return ratio >= 1.0 ? 0.9 : 0.9 * ratio;
        } else {
            return ratio >= 1.0 ? 0.9 : 0.9 * (1.0 - Math.min(ratio, 1.0));
        }
    }

    private static String buildExplanation(PressureState state, String policyId,
                                            double composite, double r, double s,
                                            double t, double e) {
        return String.format(
                "[%s] Policy '%s': composite=%.2f (R=%.2f, S=%.2f, St=%.2f, E=%.2f)",
                state.name(), policyId, composite, r, s, t, e);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
