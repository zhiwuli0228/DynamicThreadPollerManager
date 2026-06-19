package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates the minimum data-quality gates for an
 * acquisition dataset. The validator is intentionally
 * deterministic and pure: it never mutates the dataset, and
 * it never carries any runtime mutation authorization.
 *
 * <p>Gates enforced (all must pass for a {@code VALID}
 * verdict):
 * <ul>
 *   <li>{@code G1} Required profiles: every dataset MUST
 *       include {@code STEADY}, {@code RAMP}, and
 *       {@code BURST} profile coverage.</li>
 *   <li>{@code G2} Repetition: each required profile MUST
 *       appear in at least three runs.</li>
 *   <li>{@code G3} Snapshots: each run MUST have at least
 *       three snapshots.</li>
 *   <li>{@code G4} Timestamp ordering: snapshot timestamps
 *       within a run MUST be non-decreasing.</li>
 *   <li>{@code G5} Run identity: the {@code runId} of every
 *       snapshot within a run MUST match the run's
 *       {@code runId}. (Cross-run consistency is a
 *       separate gate handled in the report index.)</li>
 *   <li>{@code G6} Metadata completeness: scenario, seed,
 *       baseline policy, and environment metadata MUST
 *       all be present for every run.</li>
 * </ul>
 */
public final class AcquisitionDataQualityValidator {

    public static final int MIN_RUNS_PER_PROFILE = 3;
    public static final int MIN_SNAPSHOTS_PER_RUN = 3;
    public static final String GATE_PROFILES = "G1_profiles";
    public static final String GATE_REPETITION = "G2_repetition";
    public static final String GATE_SNAPSHOTS = "G3_snapshots";
    public static final String GATE_ORDERING = "G4_ordering";
    public static final String GATE_RUN_IDENTITY = "G5_run_identity";
    public static final String GATE_METADATA = "G6_metadata";
    public static final String GATE_EXTENDED_FIELDS = "G7_extended_fields";
    public static final String GATE_QUEUE_PRESSURE = "G8_queue_pressure";
    public static final String GATE_THREAD_LEAK = "G9_thread_leak";

    static final Set<String> REQUIRED_EXTENDED_FIELDS = Set.of(
            "poolSize", "completedTaskCount", "keepAliveTimeSeconds",
            "largestPoolSize", "taskCount"
    );

    private static final Set<ScenarioProfile> REQUIRED_PROFILES =
            EnumSet.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST);

    public AcquisitionDataQualityValidator() {
    }

    public AcquisitionDataQualityResult validate(AcquisitionDataSet dataset) {
        Objects.requireNonNull(dataset, "dataset must not be null");
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> blocking = new ArrayList<>();
        Set<ScenarioProfile> evaluated = EnumSet.noneOf(ScenarioProfile.class);
        Set<ScenarioProfile> missing = EnumSet.copyOf(REQUIRED_PROFILES);

        // G1 — required profile coverage at the dataset level
        Map<ScenarioProfile, Integer> profileCounts = new HashMap<>();
        for (var run : dataset.runs()) {
            profileCounts.merge(run.profile(), 1, Integer::sum);
            evaluated.add(run.profile());
        }
        for (ScenarioProfile required : REQUIRED_PROFILES) {
            if (!profileCounts.containsKey(required)) {
                failed.add(GATE_PROFILES);
                blocking.add("missing required profile " + required.name());
            }
        }
        if (!failed.contains(GATE_PROFILES)) {
            passed.add(GATE_PROFILES);
        }
        missing.removeAll(evaluated);

        // G2 — repetition per profile
        boolean repetitionOk = true;
        for (ScenarioProfile required : REQUIRED_PROFILES) {
            int count = profileCounts.getOrDefault(required, 0);
            if (count < MIN_RUNS_PER_PROFILE) {
                repetitionOk = false;
                failed.add(GATE_REPETITION);
                blocking.add("profile " + required.name() + " has " + count
                        + " runs, minimum is " + MIN_RUNS_PER_PROFILE);
            }
        }
        if (repetitionOk) {
            passed.add(GATE_REPETITION);
        }

        // G3 / G4 / G5 / G6 — per-run checks
        boolean snapshotsOk = true;
        boolean orderingOk = true;
        boolean identityOk = true;
        boolean metadataOk = true;
        Set<String> runIds = new HashSet<>();
        for (var run : dataset.runs()) {
            if (!runIds.add(run.runId())) {
                identityOk = false;
                failed.add(GATE_RUN_IDENTITY);
                blocking.add("duplicate runId " + run.runId() + " within dataset");
            }
            if (run.snapshotTimestamps().size() < MIN_SNAPSHOTS_PER_RUN) {
                snapshotsOk = false;
                failed.add(GATE_SNAPSHOTS);
                blocking.add("run " + run.runId() + " has "
                        + run.snapshotTimestamps().size()
                        + " snapshots, minimum is " + MIN_SNAPSHOTS_PER_RUN);
            }
            Instant previous = null;
            for (Instant ts : run.snapshotTimestamps()) {
                if (previous != null && ts.isBefore(previous)) {
                    orderingOk = false;
                    failed.add(GATE_ORDERING);
                    blocking.add("run " + run.runId()
                            + " snapshot timestamps are not non-decreasing");
                    break;
                }
                previous = ts;
            }
            if (run.scenarioId().isBlank()) {
                metadataOk = false;
                failed.add(GATE_METADATA);
                blocking.add("run " + run.runId() + " missing scenarioId");
            }
            if (run.baselinePolicyId().isBlank()) {
                metadataOk = false;
                failed.add(GATE_METADATA);
                blocking.add("run " + run.runId() + " missing baselinePolicyId");
            }
            if (!dataset.metadata().containsKey("environment")) {
                metadataOk = false;
                failed.add(GATE_METADATA);
                blocking.add("dataset metadata missing environment entry");
            }
        }
        if (snapshotsOk) {
            passed.add(GATE_SNAPSHOTS);
        }
        if (orderingOk) {
            passed.add(GATE_ORDERING);
        }
        if (identityOk) {
            passed.add(GATE_RUN_IDENTITY);
        }
        if (metadataOk) {
            passed.add(GATE_METADATA);
        }

        // G7 — extended field presence (per-run)
        boolean extendedFieldsOk = true;
        for (var run : dataset.runs()) {
            Map<String, Boolean> presence = run.extendedFieldPresence();
            if (presence.isEmpty()) {
                continue; // skip for pre-v0.8.0 data
            }
            for (String field : REQUIRED_EXTENDED_FIELDS) {
                if (!presence.getOrDefault(field, false)) {
                    extendedFieldsOk = false;
                    failed.add(GATE_EXTENDED_FIELDS);
                    blocking.add("run " + run.runId()
                            + " missing extended field " + field);
                }
            }
        }
        if (extendedFieldsOk && !failed.contains(GATE_EXTENDED_FIELDS)) {
            passed.add(GATE_EXTENDED_FIELDS);
        }

        // G8 — per-profile queue pressure evidence (per-run)
        boolean queuePressureOk = true;
        for (var run : dataset.runs()) {
            Integer pressureCount = run.queuePressureSnapshotCount();
            if (pressureCount == null) {
                continue; // skip for pre-v0.8.0 data
            }
            switch (run.profile()) {
                case STEADY:
                    break; // exempt — steady queue is expected behavior
                case RAMP:
                    if (pressureCount < 1) {
                        queuePressureOk = false;
                        failed.add(GATE_QUEUE_PRESSURE);
                        blocking.add("RAMP run " + run.runId()
                                + " has no snapshot with queueSize > 0");
                    }
                    break;
                case BURST:
                    if (pressureCount < 2) {
                        queuePressureOk = false;
                        failed.add(GATE_QUEUE_PRESSURE);
                        blocking.add("BURST run " + run.runId()
                                + " has only " + pressureCount
                                + " snapshots with queueSize > 0, minimum 2");
                    }
                    break;
            }
        }
        if (queuePressureOk && !failed.contains(GATE_QUEUE_PRESSURE)) {
            passed.add(GATE_QUEUE_PRESSURE);
        }

        // G9 — thread leak check (per-run)
        boolean threadLeakOk = true;
        for (var run : dataset.runs()) {
            Boolean leakFree = run.threadLeakFree();
            if (leakFree == null) {
                continue; // skip for pre-v0.8.0 data
            }
            if (!leakFree) {
                threadLeakOk = false;
                failed.add(GATE_THREAD_LEAK);
                blocking.add("run " + run.runId() + " thread leak detected");
            }
        }
        if (threadLeakOk && !failed.contains(GATE_THREAD_LEAK)) {
            passed.add(GATE_THREAD_LEAK);
        }

        boolean valid = failed.isEmpty();
        return new AcquisitionDataQualityResult(
                dataset.datasetId(),
                valid ? AcquisitionDataQualityResult.Status.VALID
                        : AcquisitionDataQualityResult.Status.INVALID,
                List.copyOf(evaluated),
                List.copyOf(missing),
                List.copyOf(passed),
                List.copyOf(failed),
                List.copyOf(blocking));
    }
}
