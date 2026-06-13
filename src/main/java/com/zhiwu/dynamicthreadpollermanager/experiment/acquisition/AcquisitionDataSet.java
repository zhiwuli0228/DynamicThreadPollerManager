package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure data shape for an acquisition dataset passed to
 * {@link AcquisitionDataQualityValidator}. A dataset groups
 * the run inputs the validator needs without forcing the
 * caller to materialize a {@link RunManifest} for every
 * run.
 *
 * <p>The dataset is intentionally narrow: it carries
 * identity, per-run snapshot timestamps, the profile each
 * run produced, and a metadata map. Raw evidence bytes are
 * never carried here; raw evidence is governed by
 * {@link RetentionRecord}.
 */
public final class AcquisitionDataSet {

    private final String datasetId;
    private final List<RunSnapshot> runs;
    private final Map<String, String> metadata;

    public AcquisitionDataSet(String datasetId,
                              List<RunSnapshot> runs,
                              Map<String, String> metadata) {
        this.datasetId = requireNonBlank(datasetId, "datasetId");
        this.runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        this.runs.forEach(r -> Objects.requireNonNull(r, "runs must not contain null"));
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    public String datasetId() { return datasetId; }
    public List<RunSnapshot> runs() { return runs; }
    public Map<String, String> metadata() { return metadata; }

    /**
     * One acquisition run worth of evidence. The
     * {@code snapshotTimestamps} MUST be ordered; the
     * validator cross-checks the order invariant.
     */
    public record RunSnapshot(String runId,
                              String scenarioId,
                              ScenarioProfile profile,
                              long seed,
                              String baselinePolicyId,
                              List<Instant> snapshotTimestamps,
                              Map<String, Boolean> extendedFieldPresence,
                              Integer queuePressureSnapshotCount,
                              Boolean threadLeakFree) {
        public RunSnapshot {
            AcquisitionDataSet.requireNonBlank(runId, "runId");
            AcquisitionDataSet.requireNonBlank(scenarioId, "scenarioId");
            Objects.requireNonNull(profile, "profile");
            AcquisitionDataSet.requireNonBlank(baselinePolicyId, "baselinePolicyId");
            snapshotTimestamps = List.copyOf(Objects.requireNonNull(
                    snapshotTimestamps, "snapshotTimestamps"));
            if (extendedFieldPresence == null) {
                extendedFieldPresence = Map.of();
            }
        }

        public RunSnapshot(String runId,
                           String scenarioId,
                           ScenarioProfile profile,
                           long seed,
                           String baselinePolicyId,
                           List<Instant> snapshotTimestamps) {
            this(runId, scenarioId, profile, seed, baselinePolicyId,
                    snapshotTimestamps, Map.of(), null, null);
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
