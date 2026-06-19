package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Pressure profile summary for a single acquisition run. The
 * summary is descriptive: it records observed counters and
 * ratios from the metrics layer but never carries any
 * mutation authorization.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code totalSnapshotCount == sum of profileSnapshotCounts}</li>
 *   <li>All profile counts are non-negative</li>
 *   <li>{@code scaleEventCount == scaleUpCount + scaleDownCount}</li>
 * </ul>
 */
public final class PressureSummary {

    private final String runId;
    private final ScenarioProfile scenarioProfile;
    private final int totalSnapshotCount;
    private final List<ProfileCount> profileSnapshotCounts;
    private final int scaleEventCount;
    private final int scaleUpCount;
    private final int scaleDownCount;
    private final double peakObservedQueueDepth;
    private final double meanObservedQueueDepth;

    public PressureSummary(String runId,
                           ScenarioProfile scenarioProfile,
                           int totalSnapshotCount,
                           List<ProfileCount> profileSnapshotCounts,
                           int scaleEventCount,
                           int scaleUpCount,
                           int scaleDownCount,
                           double peakObservedQueueDepth,
                           double meanObservedQueueDepth) {
        this.runId = requireNonBlank(runId, "runId");
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile");
        if (totalSnapshotCount < 0) {
            throw new IllegalArgumentException(
                    "totalSnapshotCount must be non-negative, was " + totalSnapshotCount);
        }
        this.totalSnapshotCount = totalSnapshotCount;
        this.profileSnapshotCounts = List.copyOf(Objects.requireNonNull(
                profileSnapshotCounts, "profileSnapshotCounts"));
        this.profileSnapshotCounts.forEach(p -> Objects.requireNonNull(p, "profileSnapshotCounts must not contain null"));
        if (scaleEventCount < 0 || scaleUpCount < 0 || scaleDownCount < 0) {
            throw new IllegalArgumentException("scale counts must be non-negative");
        }
        if (scaleUpCount + scaleDownCount != scaleEventCount) {
            throw new IllegalArgumentException(
                    "scaleUpCount + scaleDownCount must equal scaleEventCount");
        }
        if (peakObservedQueueDepth < 0.0 || meanObservedQueueDepth < 0.0) {
            throw new IllegalArgumentException("queue depth metrics must be non-negative");
        }
        this.scaleEventCount = scaleEventCount;
        this.scaleUpCount = scaleUpCount;
        this.scaleDownCount = scaleDownCount;
        this.peakObservedQueueDepth = peakObservedQueueDepth;
        this.meanObservedQueueDepth = meanObservedQueueDepth;
    }

    public String runId() { return runId; }
    public ScenarioProfile scenarioProfile() { return scenarioProfile; }
    public int totalSnapshotCount() { return totalSnapshotCount; }
    public List<ProfileCount> profileSnapshotCounts() { return profileSnapshotCounts; }
    public int scaleEventCount() { return scaleEventCount; }
    public int scaleUpCount() { return scaleUpCount; }
    public int scaleDownCount() { return scaleDownCount; }
    public double peakObservedQueueDepth() { return peakObservedQueueDepth; }
    public double meanObservedQueueDepth() { return meanObservedQueueDepth; }

    /**
     * Per-profile snapshot count within a single acquisition run.
     * Used to cross-check against the run manifest's
     * {@code scenarioProfile} field.
     */
    public record ProfileCount(ScenarioProfile profile, int count) {
        public ProfileCount {
            Objects.requireNonNull(profile, "profile");
            if (count < 0) {
                throw new IllegalArgumentException("count must be non-negative, was " + count);
            }
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
