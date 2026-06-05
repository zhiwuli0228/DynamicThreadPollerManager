package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates a {@link ReplayRunInput} before it is fed to the
 * offline replay pipeline. The validator is purely structural and
 * never inspects the policy layer.
 *
 * <p>An input with no failure codes is marked
 * {@link ReplayValidationStatus#VALID}; any failure code flips the
 * status to {@link ReplayValidationStatus#INVALID} and the reasons
 * are surfaced to the readiness gate so a blocked run never reaches
 * the {@code READY} or {@code READY_WITH_RISK} verdict.
 *
 * <p>Counting semantic: the validator operates at run scope. A
 * {@code VALID} result reports every input snapshot as accepted and
 * zero as rejected. An {@code INVALID} result reports zero accepted
 * and every input snapshot as rejected, because no individual
 * snapshot can be replayed when the run is blocked. For inputs with
 * no snapshots (e.g. {@code EMPTY_SNAPSHOTS}) both counts are zero
 * because there is nothing to reject.
 */
public final class ReplayEvidenceValidator {

    private static final int MIN_SNAPSHOTS = 3;

    private static final List<String> REQUIRED_PRESSURE_FIELDS = List.of(
            "activeThreads", "poolSize", "queueSize", "completedTaskCount");

    public ReplayEvidenceValidationResult validate(ReplayRunInput input) {
        Objects.requireNonNull(input, "input must not be null");

        List<ReplayFailureCode> codes = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        if (input.runId() == null || input.runId().isBlank()) {
            codes.add(ReplayFailureCode.MISSING_RUN_ID);
            reasons.add("runId is blank");
        }
        if (input.scenarioId() == null || input.scenarioId().isBlank()) {
            codes.add(ReplayFailureCode.MISSING_SCENARIO_ID);
            reasons.add("scenarioId is blank");
        }
        if (input.scenarioProfile() == null) {
            codes.add(ReplayFailureCode.MISSING_SCENARIO_PROFILE);
            reasons.add("scenarioProfile is null");
        }

        List<ObservedSnapshot> snapshots = input.snapshots();
        if (snapshots.isEmpty()) {
            codes.add(ReplayFailureCode.EMPTY_SNAPSHOTS);
            reasons.add("snapshots list is empty");
        } else if (snapshots.size() < MIN_SNAPSHOTS) {
            codes.add(ReplayFailureCode.INSUFFICIENT_SNAPSHOTS);
            reasons.add("snapshots count " + snapshots.size()
                    + " is below minimum " + MIN_SNAPSHOTS);
        }

        String expectedRunId = input.runId();
        if (expectedRunId != null && !expectedRunId.isBlank()) {
            for (int i = 0; i < snapshots.size(); i++) {
                ObservedSnapshot snap = snapshots.get(i);
                if (!expectedRunId.equals(snap.runId())) {
                    codes.add(ReplayFailureCode.RUN_ID_MISMATCH);
                    reasons.add("snapshot[" + i + "].runId=" + snap.runId()
                            + " does not match input.runId=" + expectedRunId);
                    break;
                }
            }
        }

        if (checkTimestampOrder(snapshots) != null) {
            codes.add(ReplayFailureCode.UNORDERED_TIMESTAMP);
            reasons.add(checkTimestampOrder(snapshots));
        }

        String missingFieldsReason = checkMissingPressureFields(snapshots);
        if (missingFieldsReason != null) {
            codes.add(ReplayFailureCode.MISSING_PRESSURE_FIELDS);
            reasons.add(missingFieldsReason);
        }

        if (codes.isEmpty()) {
            return ReplayEvidenceValidationResult.valid(snapshots.size());
        }
        // run-level blocking: an INVALID run cannot replay any of its
        // snapshots, so accepted=0 and rejected=snapshots.size(). When
        // there are no snapshots to begin with (EMPTY_SNAPSHOTS) both
        // counts stay at zero.
        return ReplayEvidenceValidationResult.invalid(codes, reasons, 0, snapshots.size());
    }

    private static String checkTimestampOrder(List<ObservedSnapshot> snapshots) {
        Instant previous = null;
        for (int i = 0; i < snapshots.size(); i++) {
            Instant current = snapshots.get(i).snapshot().timestamp();
            if (previous != null && current.isBefore(previous)) {
                return "snapshot[" + i + "].timestamp=" + current
                        + " is before previous timestamp " + previous;
            }
            previous = current;
        }
        return null;
    }

    /**
     * Verifies that every snapshot carries each of the four required
     * pressure observation fields: {@code activeThreads},
     * {@code poolSize}, {@code queueSize}, {@code completedTaskCount}.
     * Returns a human-readable reason naming the offending snapshot
     * and the missing field names, or {@code null} if every snapshot
     * has all required fields present.
     */
    private static String checkMissingPressureFields(List<ObservedSnapshot> snapshots) {
        for (int i = 0; i < snapshots.size(); i++) {
            RuntimeObservation observation = snapshots.get(i).observation();
            List<String> missing = missingRequiredFields(observation);
            if (!missing.isEmpty()) {
                return "snapshot[" + i + "] is missing required pressure fields: "
                        + String.join(", ", missing);
            }
        }
        return null;
    }

    private static List<String> missingRequiredFields(RuntimeObservation observation) {
        List<String> missing = new ArrayList<>();
        if (isAbsent(observation.activeThreads())) {
            missing.add("activeThreads");
        }
        if (isAbsent(observation.poolSize())) {
            missing.add("poolSize");
        }
        if (isAbsent(observation.queueSize())) {
            missing.add("queueSize");
        }
        if (isAbsent(observation.completedTaskCount())) {
            missing.add("completedTaskCount");
        }
        return missing;
    }

    private static <T> boolean isAbsent(MetricValue<T> value) {
        return value == null || value.isAbsent();
    }

    /**
     * Exposed for tests: the canonical list of required pressure
     * observation fields. Order is fixed and stable for failure-reason
     * rendering.
     */
    public static List<String> requiredPressureFields() {
        return REQUIRED_PRESSURE_FIELDS;
    }
}
