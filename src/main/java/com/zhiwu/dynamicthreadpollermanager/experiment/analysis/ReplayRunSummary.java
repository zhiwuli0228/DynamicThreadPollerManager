package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate counts and oscillation signals for a single
 * (run, policyConfigLabel) pair.
 *
 * <p>Invariant: {@code decisionCount + skippedCount == evidenceCount}.
 * Oscillation signals ({@code directionFlipCount} and
 * {@code alternatingStreakMax}) are computed on the non-{@code HOLD}
 * action subsequence only.
 */
public final class ReplayRunSummary {

    private final String runId;
    private final String scenarioId;
    private final ScenarioProfile scenarioProfile;
    private final String policyConfigLabel;
    private final int evidenceCount;
    private final int decisionCount;
    private final int skippedCount;
    private final int scaleUpCount;
    private final int scaleDownCount;
    private final int holdCount;
    private final int acceptedCount;
    private final int cappedCount;
    private final int gateHoldCount;
    private final int rejectedCount;
    private final int directionFlipCount;
    private final int alternatingStreakMax;
    private final double holdRatio;
    private final double cappedRatio;
    private final List<ReplayDecisionEvidence> decisionEvidence;
    private final List<String> skippedReasons;

    public ReplayRunSummary(String runId,
                            String scenarioId,
                            ScenarioProfile scenarioProfile,
                            String policyConfigLabel,
                            int evidenceCount,
                            int decisionCount,
                            int skippedCount,
                            int scaleUpCount,
                            int scaleDownCount,
                            int holdCount,
                            int acceptedCount,
                            int cappedCount,
                            int gateHoldCount,
                            int rejectedCount,
                            int directionFlipCount,
                            int alternatingStreakMax,
                            double holdRatio,
                            double cappedRatio,
                            List<ReplayDecisionEvidence> decisionEvidence,
                            List<String> skippedReasons) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile must not be null");
        if (policyConfigLabel == null || policyConfigLabel.isBlank()) {
            throw new IllegalArgumentException("policyConfigLabel must not be blank");
        }
        if (evidenceCount < 0 || decisionCount < 0 || skippedCount < 0) {
            throw new IllegalArgumentException("counts must be >= 0");
        }
        if (decisionCount + skippedCount != evidenceCount) {
            throw new IllegalArgumentException(
                    "decisionCount + skippedCount must equal evidenceCount");
        }
        if (holdRatio < 0.0 || holdRatio > 1.0 || cappedRatio < 0.0 || cappedRatio > 1.0) {
            throw new IllegalArgumentException("ratios must be in [0.0, 1.0]");
        }
        this.runId = runId;
        this.scenarioId = scenarioId;
        this.policyConfigLabel = policyConfigLabel;
        this.evidenceCount = evidenceCount;
        this.decisionCount = decisionCount;
        this.skippedCount = skippedCount;
        this.scaleUpCount = scaleUpCount;
        this.scaleDownCount = scaleDownCount;
        this.holdCount = holdCount;
        this.acceptedCount = acceptedCount;
        this.cappedCount = cappedCount;
        this.gateHoldCount = gateHoldCount;
        this.rejectedCount = rejectedCount;
        this.directionFlipCount = directionFlipCount;
        this.alternatingStreakMax = alternatingStreakMax;
        this.holdRatio = holdRatio;
        this.cappedRatio = cappedRatio;
        this.decisionEvidence = List.copyOf(Objects.requireNonNull(decisionEvidence, "decisionEvidence must not be null"));
        this.skippedReasons = List.copyOf(Objects.requireNonNull(skippedReasons, "skippedReasons must not be null"));
    }

    public String runId() { return runId; }
    public String scenarioId() { return scenarioId; }
    public ScenarioProfile scenarioProfile() { return scenarioProfile; }
    public String policyConfigLabel() { return policyConfigLabel; }
    public int evidenceCount() { return evidenceCount; }
    public int decisionCount() { return decisionCount; }
    public int skippedCount() { return skippedCount; }
    public int scaleUpCount() { return scaleUpCount; }
    public int scaleDownCount() { return scaleDownCount; }
    public int holdCount() { return holdCount; }
    public int acceptedCount() { return acceptedCount; }
    public int cappedCount() { return cappedCount; }
    public int gateHoldCount() { return gateHoldCount; }
    public int rejectedCount() { return rejectedCount; }
    public int directionFlipCount() { return directionFlipCount; }
    public int alternatingStreakMax() { return alternatingStreakMax; }
    public double holdRatio() { return holdRatio; }
    public double cappedRatio() { return cappedRatio; }
    public List<ReplayDecisionEvidence> decisionEvidence() { return decisionEvidence; }
    public List<String> skippedReasons() { return skippedReasons; }
}
