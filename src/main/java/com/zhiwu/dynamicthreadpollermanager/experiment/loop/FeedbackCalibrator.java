package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PolicyScore;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.ThresholdPolicyScorer;

import java.util.List;
import java.util.function.Function;

/**
 * Calibrates {@link ThresholdPolicyScorer} dimension weights based on actual
 * adjustment outcomes using median-split correlation analysis.
 *
 * <p>Returns new scorer instances (immutable pattern). If the correlation
 * between a dimension score and success rate exceeds ±0.1, the corresponding
 * weight is adjusted by {@code maxAdjustmentPerCycle}. Weights are normalized
 * and clamped to [{@code minWeight}, {@code maxWeight}] after adjustment.
 */
public final class FeedbackCalibrator {

    private final double maxAdjustmentPerCycle;
    private final double minWeight;
    private final double maxWeight;

    private static final Function<PolicyScore, Double> DIM_RESPONSIVENESS = PolicyScore::responsivenessScore;
    private static final Function<PolicyScore, Double> DIM_SAFETY = PolicyScore::safetyScore;
    private static final Function<PolicyScore, Double> DIM_STABILITY = PolicyScore::stabilityScore;
    private static final Function<PolicyScore, Double> DIM_EFFICIENCY = PolicyScore::efficiencyScore;

    public FeedbackCalibrator(double maxAdjustmentPerCycle, double minWeight, double maxWeight) {
        if (maxAdjustmentPerCycle <= 0 || maxAdjustmentPerCycle > 0.2) {
            throw new IllegalArgumentException(
                    "maxAdjustmentPerCycle must be in (0, 0.2], was " + maxAdjustmentPerCycle);
        }
        if (minWeight < 0.05 || minWeight > 0.2) {
            throw new IllegalArgumentException(
                    "minWeight must be in [0.05, 0.20], was " + minWeight);
        }
        if (maxWeight < 0.3 || maxWeight > 0.6) {
            throw new IllegalArgumentException(
                    "maxWeight must be in [0.30, 0.60], was " + maxWeight);
        }
        if (minWeight >= maxWeight) {
            throw new IllegalArgumentException(
                    "minWeight must be < maxWeight, was min=" + minWeight + " max=" + maxWeight);
        }
        this.maxAdjustmentPerCycle = maxAdjustmentPerCycle;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    public FeedbackCalibrator() {
        this(0.05, 0.10, 0.50);
    }

    /**
     * Calibrate weights based on adjustment history.
     *
     * @param history       adjustment history
     * @param currentScorer current scorer (for reading weights via getters)
     * @param windowSize    number of recent adjustments to analyze
     * @return new ThresholdPolicyScorer with calibrated weights,
     *         or currentScorer if insufficient data
     */
    public ThresholdPolicyScorer calibrate(
            AdjustmentHistory history,
            ThresholdPolicyScorer currentScorer,
            int windowSize) {

        List<HistoryEntry> recent = history.recent(windowSize);
        if (recent.size() < windowSize) {
            return currentScorer;
        }

        double wR = currentScorer.wResponsiveness();
        double wS = currentScorer.wSafety();
        double wSt = currentScorer.wStability();
        double wE = currentScorer.wEfficiency();

        wR = adjustWeight(wR, correlation(recent, DIM_RESPONSIVENESS));
        wS = adjustWeight(wS, correlation(recent, DIM_SAFETY));
        wSt = adjustWeight(wSt, correlation(recent, DIM_STABILITY));
        wE = adjustWeight(wE, correlation(recent, DIM_EFFICIENCY));

        double sum = wR + wS + wSt + wE;
        wR /= sum;
        wS /= sum;
        wSt /= sum;
        wE /= sum;

        wR = clamp(wR);
        wS = clamp(wS);
        wSt = clamp(wSt);
        wE = clamp(wE);

        sum = wR + wS + wSt + wE;
        wR /= sum;
        wS /= sum;
        wSt /= sum;
        wE /= sum;

        return new ThresholdPolicyScorer(wR, wS, wSt, wE);
    }

    private double correlation(List<HistoryEntry> entries,
                               Function<PolicyScore, Double> dimensionExtractor) {
        List<Double> scores = entries.stream()
                .map(e -> dimensionExtractor.apply(e.decision().selectedScore()))
                .sorted()
                .toList();
        double median = scores.get(scores.size() / 2);

        long highSuccess = 0, highTotal = 0;
        long lowSuccess = 0, lowTotal = 0;

        for (HistoryEntry e : entries) {
            double score = dimensionExtractor.apply(e.decision().selectedScore());
            boolean success = AdjustmentHistory.isImprovement(
                    e.beforeClassification().state(),
                    e.afterClassification().state());
            if (score >= median) {
                highTotal++;
                if (success) highSuccess++;
            } else {
                lowTotal++;
                if (success) lowSuccess++;
            }
        }

        double highRate = highTotal > 0 ? (double) highSuccess / highTotal : 0.5;
        double lowRate = lowTotal > 0 ? (double) lowSuccess / lowTotal : 0.5;

        // When all scores are identical, one group is empty and the split is
        // meaningless — return 0.0 to avoid phantom correlation.
        if (highTotal == 0 || lowTotal == 0) return 0.0;

        return highRate - lowRate;
    }

    private double adjustWeight(double currentWeight, double correlation) {
        if (correlation > 0.1) return currentWeight + maxAdjustmentPerCycle;
        if (correlation < -0.1) return currentWeight - maxAdjustmentPerCycle;
        return currentWeight;
    }

    private double clamp(double w) {
        return Math.max(minWeight, Math.min(maxWeight, w));
    }
}
