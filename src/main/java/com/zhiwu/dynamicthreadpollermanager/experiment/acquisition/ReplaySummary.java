package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Read-only replay summary scoped to a single acquisition run.
 * The summary is descriptive — it records counts and ratios
 * aggregated from the offline analysis layer for the run, and
 * never carries any mutation authorization.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code evidenceCount == decisionCount + skippedCount}</li>
 *   <li>All counts are non-negative</li>
 *   <li>{@code holdRatio} and {@code cappedRatio} are in {@code [0.0, 1.0]}</li>
 * </ul>
 */
public final class ReplaySummary {

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
    private final int rejectedCount;
    private final double holdRatio;
    private final double cappedRatio;
    private final List<String> skippedReasons;

    public ReplaySummary(String runId,
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
                         int rejectedCount,
                         double holdRatio,
                         double cappedRatio,
                         List<String> skippedReasons) {
        this.runId = requireNonBlank(runId, "runId");
        this.scenarioId = requireNonBlank(scenarioId, "scenarioId");
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile");
        this.policyConfigLabel = requireNonBlank(policyConfigLabel, "policyConfigLabel");
        if (evidenceCount < 0 || decisionCount < 0 || skippedCount < 0) {
            throw new IllegalArgumentException("counts must be non-negative");
        }
        if (decisionCount + skippedCount != evidenceCount) {
            throw new IllegalArgumentException(
                    "decisionCount + skippedCount must equal evidenceCount");
        }
        if (scaleUpCount < 0 || scaleDownCount < 0 || holdCount < 0
                || acceptedCount < 0 || cappedCount < 0 || rejectedCount < 0) {
            throw new IllegalArgumentException("decision counts must be non-negative");
        }
        if (holdRatio < 0.0 || holdRatio > 1.0 || cappedRatio < 0.0 || cappedRatio > 1.0) {
            throw new IllegalArgumentException("ratios must be in [0.0, 1.0]");
        }
        this.evidenceCount = evidenceCount;
        this.decisionCount = decisionCount;
        this.skippedCount = skippedCount;
        this.scaleUpCount = scaleUpCount;
        this.scaleDownCount = scaleDownCount;
        this.holdCount = holdCount;
        this.acceptedCount = acceptedCount;
        this.cappedCount = cappedCount;
        this.rejectedCount = rejectedCount;
        this.holdRatio = holdRatio;
        this.cappedRatio = cappedRatio;
        this.skippedReasons = List.copyOf(Objects.requireNonNull(skippedReasons, "skippedReasons"));
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
    public int rejectedCount() { return rejectedCount; }
    public double holdRatio() { return holdRatio; }
    public double cappedRatio() { return cappedRatio; }
    public List<String> skippedReasons() { return skippedReasons; }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
