package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

/**
 * Immutable threshold set for the mutation readiness gate. All
 * values are first-pass experience-based defaults and are exposed as
 * fields so they can be reviewed against the {@code default}
 * sensitivity report.
 */
public final class ReadinessThresholds {

    public static final ReadinessThresholds DEFAULTS = new ReadinessThresholds(
            0.25, 0.85, 2, 2,
            0.50, 0.95, 4, 4
    );

    private final double maxCappedRatioForReady;
    private final double maxHoldRatioForReady;
    private final int maxDirectionFlipCountForReady;
    private final int maxAlternatingStreakForReady;
    private final double maxCappedRatioForRisk;
    private final double maxHoldRatioForRisk;
    private final int maxDirectionFlipCountForRisk;
    private final int maxAlternatingStreakForRisk;

    public ReadinessThresholds(double maxCappedRatioForReady,
                               double maxHoldRatioForReady,
                               int maxDirectionFlipCountForReady,
                               int maxAlternatingStreakForReady,
                               double maxCappedRatioForRisk,
                               double maxHoldRatioForRisk,
                               int maxDirectionFlipCountForRisk,
                               int maxAlternatingStreakForRisk) {
        if (maxCappedRatioForReady < 0.0 || maxCappedRatioForReady > 1.0) {
            throw new IllegalArgumentException("maxCappedRatioForReady must be in [0,1]");
        }
        if (maxHoldRatioForReady < 0.0 || maxHoldRatioForReady > 1.0) {
            throw new IllegalArgumentException("maxHoldRatioForReady must be in [0,1]");
        }
        if (maxCappedRatioForRisk < 0.0 || maxCappedRatioForRisk > 1.0) {
            throw new IllegalArgumentException("maxCappedRatioForRisk must be in [0,1]");
        }
        if (maxHoldRatioForRisk < 0.0 || maxHoldRatioForRisk > 1.0) {
            throw new IllegalArgumentException("maxHoldRatioForRisk must be in [0,1]");
        }
        if (maxCappedRatioForRisk < maxCappedRatioForReady) {
            throw new IllegalArgumentException("risk ceiling must be >= ready ceiling for cappedRatio");
        }
        if (maxHoldRatioForRisk < maxHoldRatioForReady) {
            throw new IllegalArgumentException("risk ceiling must be >= ready ceiling for holdRatio");
        }
        if (maxDirectionFlipCountForReady < 0 || maxAlternatingStreakForReady < 0
                || maxDirectionFlipCountForRisk < 0 || maxAlternatingStreakForRisk < 0) {
            throw new IllegalArgumentException("flip and streak counts must be >= 0");
        }
        if (maxDirectionFlipCountForRisk < maxDirectionFlipCountForReady) {
            throw new IllegalArgumentException("risk ceiling must be >= ready ceiling for directionFlipCount");
        }
        if (maxAlternatingStreakForRisk < maxAlternatingStreakForReady) {
            throw new IllegalArgumentException("risk ceiling must be >= ready ceiling for alternatingStreak");
        }
        this.maxCappedRatioForReady = maxCappedRatioForReady;
        this.maxHoldRatioForReady = maxHoldRatioForReady;
        this.maxDirectionFlipCountForReady = maxDirectionFlipCountForReady;
        this.maxAlternatingStreakForReady = maxAlternatingStreakForReady;
        this.maxCappedRatioForRisk = maxCappedRatioForRisk;
        this.maxHoldRatioForRisk = maxHoldRatioForRisk;
        this.maxDirectionFlipCountForRisk = maxDirectionFlipCountForRisk;
        this.maxAlternatingStreakForRisk = maxAlternatingStreakForRisk;
    }

    public double maxCappedRatioForReady() { return maxCappedRatioForReady; }
    public double maxHoldRatioForReady() { return maxHoldRatioForReady; }
    public int maxDirectionFlipCountForReady() { return maxDirectionFlipCountForReady; }
    public int maxAlternatingStreakForReady() { return maxAlternatingStreakForReady; }
    public double maxCappedRatioForRisk() { return maxCappedRatioForRisk; }
    public double maxHoldRatioForRisk() { return maxHoldRatioForRisk; }
    public int maxDirectionFlipCountForRisk() { return maxDirectionFlipCountForRisk; }
    public int maxAlternatingStreakForRisk() { return maxAlternatingStreakForRisk; }
}
