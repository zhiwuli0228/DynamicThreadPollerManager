package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical record of a single pressure data acquisition run.
 * The manifest is the audit anchor: every summary and the
 * {@link EvidenceIndex} MUST be traceable back to the same
 * {@code runId} that this record carries.
 *
 * <p>The manifest intentionally does not encode any runtime
 * mutation intent. Its presence is a statement that evidence
 * was collected, not that the system is permitted to mutate
 * the executor.
 */
public final class RunManifest {

    private final String runId;
    private final String scenarioId;
    private final ScenarioProfile scenarioProfile;
    private final long seed;
    private final int stepCount;
    private final String baselinePolicyId;
    private final BaselinePresetSummary baselinePreset;
    private final Map<String, String> environmentSummary;
    private final List<String> commandLine;
    private final Instant createdAt;

    public RunManifest(String runId,
                       String scenarioId,
                       ScenarioProfile scenarioProfile,
                       long seed,
                       int stepCount,
                       String baselinePolicyId,
                       BaselinePresetSummary baselinePreset,
                       Map<String, String> environmentSummary,
                       List<String> commandLine,
                       Instant createdAt) {
        this.runId = requireNonBlank(runId, "runId");
        this.scenarioId = requireNonBlank(scenarioId, "scenarioId");
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile");
        this.seed = seed;
        if (stepCount <= 0) {
            throw new IllegalArgumentException("stepCount must be positive, was " + stepCount);
        }
        this.stepCount = stepCount;
        this.baselinePolicyId = requireNonBlank(baselinePolicyId, "baselinePolicyId");
        this.baselinePreset = Objects.requireNonNull(baselinePreset, "baselinePreset");
        this.environmentSummary = Map.copyOf(Objects.requireNonNull(environmentSummary,
                "environmentSummary"));
        this.commandLine = List.copyOf(Objects.requireNonNull(commandLine, "commandLine"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public String runId() { return runId; }
    public String scenarioId() { return scenarioId; }
    public ScenarioProfile scenarioProfile() { return scenarioProfile; }
    public long seed() { return seed; }
    public int stepCount() { return stepCount; }
    public String baselinePolicyId() { return baselinePolicyId; }
    public BaselinePresetSummary baselinePreset() { return baselinePreset; }
    public Map<String, String> environmentSummary() { return environmentSummary; }
    public List<String> commandLine() { return commandLine; }
    public Instant createdAt() { return createdAt; }

    /**
     * Read-only summary of the baseline executor preset used
     * by the acquisition run. Only the sizing is captured;
     * no runtime state or mutation hooks are exposed.
     */
    public record BaselinePresetSummary(String policyId,
                                        int corePoolSize,
                                        int maximumPoolSize,
                                        int queueCapacity) {
        public BaselinePresetSummary {
            Objects.requireNonNull(policyId, "policyId");
            if (corePoolSize <= 0) {
                throw new IllegalArgumentException(
                        "corePoolSize must be positive, was " + corePoolSize);
            }
            if (maximumPoolSize < corePoolSize) {
                throw new IllegalArgumentException(
                        "maximumPoolSize must be >= corePoolSize, was " + maximumPoolSize);
            }
            if (queueCapacity < 0) {
                throw new IllegalArgumentException(
                        "queueCapacity must be non-negative, was " + queueCapacity);
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
