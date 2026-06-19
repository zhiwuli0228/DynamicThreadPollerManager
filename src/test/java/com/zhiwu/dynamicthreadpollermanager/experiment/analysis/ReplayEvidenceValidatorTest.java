package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceSummary;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayEvidenceValidatorTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T00:01:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T00:02:00Z");

    private final ReplayEvidenceValidator validator = new ReplayEvidenceValidator();

    @Test
    void shouldMarkValidInputAsValid() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-1", T1, 4, 8, 0, 10L),
                        observed("run-1", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.VALID, result.status());
        assertTrue(result.isValid());
        assertTrue(result.failureCodes().isEmpty());
        assertTrue(result.failureReasons().isEmpty());
        assertEquals(3, result.acceptedSnapshotCount());
        assertEquals(0, result.rejectedSnapshotCount());
    }

    @Test
    void shouldMarkInputWithEmptySnapshotsAsInvalid() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY, List.of());

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.EMPTY_SNAPSHOTS));
    }

    @Test
    void shouldMarkInputWithInsufficientSnapshotsAsInvalid() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", T0, 4, 8, 0, 10L)));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.INSUFFICIENT_SNAPSHOTS));
        assertFalse(result.failureCodes().contains(ReplayFailureCode.EMPTY_SNAPSHOTS),
                () -> "INSUFFICIENT_SNAPSHOTS subsumes EMPTY_SNAPSHOTS when size == 1");
    }

    @Test
    void shouldMarkInputWithRunIdMismatchAsInvalid() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-2", T1, 4, 8, 0, 10L),
                        observed("run-1", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.RUN_ID_MISMATCH));
    }

    @Test
    void shouldMarkInputWithUnorderedTimestampsAsInvalid() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-1", T2, 4, 8, 0, 10L),
                        observed("run-1", T1, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.UNORDERED_TIMESTAMP));
    }

    @Test
    void shouldAcceptEqualTimestampsAsOrdered() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-1", T1, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.VALID, result.status());
    }

    @Test
    void shouldMarkInputWithMissingPressureFieldsAsInvalid() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observedWithAbsentMetrics("run-1", T1),
                        observed("run-1", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.MISSING_PRESSURE_FIELDS));
    }

    @Test
    void shouldMarkInputWithMissingPoolSizeAsInvalid() {
        // poolSize is absent, but activeThreads / queueSize / completedTaskCount are present
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observedWithAbsent("run-1", T0, Set.of("poolSize")),
                        observedWithAbsent("run-1", T1, Set.of("poolSize")),
                        observedWithAbsent("run-1", T2, Set.of("poolSize"))
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.MISSING_PRESSURE_FIELDS),
                () -> "missing poolSize must trigger MISSING_PRESSURE_FIELDS");
        // the failure reason must mention poolSize
        assertTrue(result.failureReasons().stream()
                        .anyMatch(reason -> reason.contains("poolSize")),
                () -> "failure reason must name the missing field poolSize; got: "
                        + result.failureReasons());
    }

    @Test
    void shouldMarkInputWithMissingCompletedTaskCountAsInvalid() {
        // completedTaskCount is absent, but activeThreads / poolSize / queueSize are present
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observedWithAbsent("run-1", T0, Set.of("completedTaskCount")),
                        observedWithAbsent("run-1", T1, Set.of("completedTaskCount")),
                        observedWithAbsent("run-1", T2, Set.of("completedTaskCount"))
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.MISSING_PRESSURE_FIELDS),
                () -> "missing completedTaskCount must trigger MISSING_PRESSURE_FIELDS");
        // the failure reason must mention completedTaskCount
        assertTrue(result.failureReasons().stream()
                        .anyMatch(reason -> reason.contains("completedTaskCount")),
                () -> "failure reason must name the missing field completedTaskCount; got: "
                        + result.failureReasons());
    }

    @Test
    void shouldMarkInputWithMissingActiveThreadsAsInvalid() {
        // activeThreads is absent, the other three are present
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observedWithAbsent("run-1", T0, Set.of("activeThreads")),
                        observedWithAbsent("run-1", T1, Set.of("activeThreads")),
                        observedWithAbsent("run-1", T2, Set.of("activeThreads"))
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.MISSING_PRESSURE_FIELDS));
        assertTrue(result.failureReasons().stream()
                        .anyMatch(reason -> reason.contains("activeThreads")),
                () -> "failure reason must name the missing field activeThreads");
    }

    @Test
    void shouldMarkInputWithMissingQueueSizeAsInvalid() {
        // queueSize is absent, the other three are present
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observedWithAbsent("run-1", T0, Set.of("queueSize")),
                        observedWithAbsent("run-1", T1, Set.of("queueSize")),
                        observedWithAbsent("run-1", T2, Set.of("queueSize"))
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.MISSING_PRESSURE_FIELDS));
        assertTrue(result.failureReasons().stream()
                        .anyMatch(reason -> reason.contains("queueSize")),
                () -> "failure reason must name the missing field queueSize");
    }

    @Test
    void shouldNameAllMissingFieldsWhenMultipleAreAbsent() {
        // snapshot 0: all required fields present
        // snapshot 1: poolSize + completedTaskCount absent
        // snapshot 2: all required fields present
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observedWithAbsent("run-1", T1, Set.of("poolSize", "completedTaskCount")),
                        observed("run-1", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.MISSING_PRESSURE_FIELDS));
        // the failure reason must list both missing fields
        assertTrue(result.failureReasons().stream()
                        .anyMatch(reason -> reason.contains("poolSize")
                                && reason.contains("completedTaskCount")),
                () -> "failure reason must name both missing fields; got: "
                        + result.failureReasons());
        // the failure reason must identify the offending snapshot index
        assertTrue(result.failureReasons().stream()
                        .anyMatch(reason -> reason.contains("snapshot[1]")),
                () -> "failure reason must identify the snapshot index; got: "
                        + result.failureReasons());
    }

    @Test
    void shouldProduceZeroAcceptedAndFullRejectedCountForInvalidInput() {
        // run-level blocking semantic: when validation fails, accepted=0, rejected=snapshots.size()
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observedWithAbsent("run-1", T1, Set.of("poolSize")),
                        observed("run-1", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertEquals(0, result.acceptedSnapshotCount(),
                () -> "INVALID must yield zero accepted snapshots");
        assertEquals(3, result.rejectedSnapshotCount(),
                () -> "INVALID must reject every snapshot in the run");
    }

    @Test
    void shouldProduceZeroAcceptedAndFullRejectedCountForEmptySnapshots() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY, List.of());

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertEquals(0, result.acceptedSnapshotCount());
        assertEquals(0, result.rejectedSnapshotCount(),
                () -> "EMPTY_SNAPSHOTS has no snapshots to reject");
    }

    @Test
    void shouldProduceZeroAcceptedAndFullRejectedCountForInsufficientSnapshots() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", T0, 4, 8, 0, 10L)));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertEquals(0, result.acceptedSnapshotCount());
        assertEquals(1, result.rejectedSnapshotCount());
    }

    @Test
    void shouldProduceZeroAcceptedAndFullRejectedCountForUnorderedTimestamps() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-1", T2, 4, 8, 0, 10L),
                        observed("run-1", T1, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertEquals(0, result.acceptedSnapshotCount());
        assertEquals(3, result.rejectedSnapshotCount());
    }

    @Test
    void shouldProduceZeroAcceptedAndFullRejectedCountForRunIdMismatch() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-2", T1, 4, 8, 0, 10L),
                        observed("run-1", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertEquals(0, result.acceptedSnapshotCount());
        assertEquals(3, result.rejectedSnapshotCount());
    }

    @Test
    void shouldReportAllIndependentFailureCodesWhenMultipleIssuesPresent() {
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0, 10L),
                        observed("run-2", T2, 4, 8, 0, 10L)
                ));

        ReplayEvidenceValidationResult result = validator.validate(input);

        assertEquals(ReplayValidationStatus.INVALID, result.status());
        assertTrue(result.failureCodes().contains(ReplayFailureCode.RUN_ID_MISMATCH));
        assertTrue(result.failureCodes().contains(ReplayFailureCode.INSUFFICIENT_SNAPSHOTS));
    }

    @Test
    void shouldExposeAllDesignFailureCodes() {
        // failure codes that the validator never emits because ReplayRunInput blocks them upstream
        // are still part of the public contract and must be present in the enum.
        ReplayFailureCode.valueOf("MISSING_RUN_ID");
        ReplayFailureCode.valueOf("MISSING_SCENARIO_ID");
        ReplayFailureCode.valueOf("MISSING_SCENARIO_PROFILE");
    }

    private static ReplayRunInput input(String runId,
                                        ScenarioProfile profile,
                                        List<ObservedSnapshot> snapshots) {
        return new ReplayRunInput(
                runId, "scenario-1", profile, "policy-1",
                snapshots, evidenceSummary(runId), snapshots.size(), 10);
    }

    private static EvidenceSummary evidenceSummary(String runId) {
        return new EvidenceSummary(runId, 3, Optional.of(T0), Optional.of(T2));
    }

    /**
     * Builds a fully-valid observation: all four required pressure fields
     * (activeThreads, poolSize, queueSize, completedTaskCount) and the
     * optional cpuUtilization are present.
     */
    private static ObservedSnapshot observed(String runId, Instant ts,
                                             int active, int pool, int queue, long completed) {
        RuntimeObservation observation = new RuntimeObservation(
                ts,
                MetricValue.present(active),
                MetricValue.present(pool),
                MetricValue.present(queue),
                MetricValue.present(completed),
                MetricValue.present(0.5)
        );
        PressureSnapshot snapshot = new PressureSnapshot(ts, active, pool, queue, completed, 0.5);
        return new ObservedSnapshot(runId, snapshot, observation);
    }

    /**
     * Builds an observation with every metric absent. Used to assert that
     * the validator still flags MISSING_PRESSURE_FIELDS when all required
     * fields are missing simultaneously.
     */
    private static ObservedSnapshot observedWithAbsentMetrics(String runId, Instant ts) {
        RuntimeObservation observation = new RuntimeObservation(
                ts,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        );
        PressureSnapshot snapshot = new PressureSnapshot(ts, 0, 0, 0, 0L, 0.0);
        return new ObservedSnapshot(runId, snapshot, observation);
    }

    /**
     * Builds an observation where the named fields are absent and all other
     * required pressure fields are present. {@code absentFields} may include
     * any of {@code activeThreads, poolSize, queueSize, completedTaskCount}.
     */
    private static ObservedSnapshot observedWithAbsent(String runId,
                                                      Instant ts,
                                                      Set<String> absentFields) {
        MetricValue<Integer> active = absentFields.contains("activeThreads")
                ? MetricValue.absent() : MetricValue.present(4);
        MetricValue<Integer> pool = absentFields.contains("poolSize")
                ? MetricValue.absent() : MetricValue.present(8);
        MetricValue<Integer> queue = absentFields.contains("queueSize")
                ? MetricValue.absent() : MetricValue.present(0);
        MetricValue<Long> completed = absentFields.contains("completedTaskCount")
                ? MetricValue.absent() : MetricValue.present(10L);

        RuntimeObservation observation = new RuntimeObservation(
                ts, active, pool, queue, completed, MetricValue.present(0.5));
        PressureSnapshot snapshot = new PressureSnapshot(
                ts,
                active.asOptional().orElse(0),
                pool.asOptional().orElse(0),
                queue.asOptional().orElse(0),
                completed.asOptional().orElse(0L),
                0.5);
        return new ObservedSnapshot(runId, snapshot, observation);
    }
}
