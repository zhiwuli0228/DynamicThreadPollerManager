package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import java.util.Objects;

/**
 * Comparison of the three fixed replay configurations
 * ({@code default}, {@code conservative}, {@code aggressive}) for a
 * single run. The {@code deltasVsDefault} block captures the signed
 * difference of each non-default config against {@code default}.
 */
public final class SensitivityComparison {

    private final String runId;
    private final ReplayRunSummary defaultSummary;
    private final ReplayRunSummary conservativeSummary;
    private final ReplayRunSummary aggressiveSummary;
    private final SensitivityDelta deltasVsDefault;
    private final SensitivityDelta aggressiveDeltaVsDefault;

    public SensitivityComparison(String runId,
                                 ReplayRunSummary defaultSummary,
                                 ReplayRunSummary conservativeSummary,
                                 ReplayRunSummary aggressiveSummary) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        this.runId = runId;
        this.defaultSummary = Objects.requireNonNull(defaultSummary, "defaultSummary must not be null");
        this.conservativeSummary = Objects.requireNonNull(conservativeSummary, "conservativeSummary must not be null");
        this.aggressiveSummary = Objects.requireNonNull(aggressiveSummary, "aggressiveSummary must not be null");
        this.deltasVsDefault = SensitivityDelta.between(defaultSummary, conservativeSummary);
        this.aggressiveDeltaVsDefault = SensitivityDelta.between(defaultSummary, aggressiveSummary);
    }

    public String runId() { return runId; }
    public ReplayRunSummary defaultSummary() { return defaultSummary; }
    public ReplayRunSummary conservativeSummary() { return conservativeSummary; }
    public ReplayRunSummary aggressiveSummary() { return aggressiveSummary; }
    public SensitivityDelta conservativeDeltaVsDefault() { return deltasVsDefault; }
    public SensitivityDelta aggressiveDeltaVsDefault() { return aggressiveDeltaVsDefault; }

    /**
     * Signed deltas of a non-default config relative to {@code default}.
     */
    public static final class SensitivityDelta {
        private final int scaleUpCountDelta;
        private final int scaleDownCountDelta;
        private final int holdCountDelta;
        private final int acceptedCountDelta;
        private final int cappedCountDelta;
        private final int directionFlipCountDelta;
        private final int alternatingStreakMaxDelta;
        private final double holdRatioDelta;
        private final double cappedRatioDelta;

        public SensitivityDelta(int scaleUpCountDelta,
                                int scaleDownCountDelta,
                                int holdCountDelta,
                                int acceptedCountDelta,
                                int cappedCountDelta,
                                int directionFlipCountDelta,
                                int alternatingStreakMaxDelta,
                                double holdRatioDelta,
                                double cappedRatioDelta) {
            this.scaleUpCountDelta = scaleUpCountDelta;
            this.scaleDownCountDelta = scaleDownCountDelta;
            this.holdCountDelta = holdCountDelta;
            this.acceptedCountDelta = acceptedCountDelta;
            this.cappedCountDelta = cappedCountDelta;
            this.directionFlipCountDelta = directionFlipCountDelta;
            this.alternatingStreakMaxDelta = alternatingStreakMaxDelta;
            this.holdRatioDelta = holdRatioDelta;
            this.cappedRatioDelta = cappedRatioDelta;
        }

        static SensitivityDelta between(ReplayRunSummary baseline, ReplayRunSummary other) {
            return new SensitivityDelta(
                    other.scaleUpCount() - baseline.scaleUpCount(),
                    other.scaleDownCount() - baseline.scaleDownCount(),
                    other.holdCount() - baseline.holdCount(),
                    other.acceptedCount() - baseline.acceptedCount(),
                    other.cappedCount() - baseline.cappedCount(),
                    other.directionFlipCount() - baseline.directionFlipCount(),
                    other.alternatingStreakMax() - baseline.alternatingStreakMax(),
                    other.holdRatio() - baseline.holdRatio(),
                    other.cappedRatio() - baseline.cappedRatio()
            );
        }

        public int scaleUpCountDelta() { return scaleUpCountDelta; }
        public int scaleDownCountDelta() { return scaleDownCountDelta; }
        public int holdCountDelta() { return holdCountDelta; }
        public int acceptedCountDelta() { return acceptedCountDelta; }
        public int cappedCountDelta() { return cappedCountDelta; }
        public int directionFlipCountDelta() { return directionFlipCountDelta; }
        public int alternatingStreakMaxDelta() { return alternatingStreakMaxDelta; }
        public double holdRatioDelta() { return holdRatioDelta; }
        public double cappedRatioDelta() { return cappedRatioDelta; }
    }
}
