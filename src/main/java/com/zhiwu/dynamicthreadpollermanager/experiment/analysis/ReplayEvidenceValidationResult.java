package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Result of validating a {@link ReplayRunInput} for offline replay.
 *
 * <p>An input with status {@link ReplayValidationStatus#VALID} carries
 * zero failure codes; an {@link ReplayValidationStatus#INVALID} result
 * carries one or more failure codes and human-readable reasons.
 */
public final class ReplayEvidenceValidationResult {

    private final ReplayValidationStatus status;
    private final List<ReplayFailureCode> failureCodes;
    private final List<String> failureReasons;
    private final int acceptedSnapshotCount;
    private final int rejectedSnapshotCount;

    public ReplayEvidenceValidationResult(ReplayValidationStatus status,
                                          List<ReplayFailureCode> failureCodes,
                                          List<String> failureReasons,
                                          int acceptedSnapshotCount,
                                          int rejectedSnapshotCount) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.failureCodes = List.copyOf(Objects.requireNonNull(failureCodes, "failureCodes must not be null"));
        this.failureReasons = List.copyOf(Objects.requireNonNull(failureReasons, "failureReasons must not be null"));
        if (acceptedSnapshotCount < 0) {
            throw new IllegalArgumentException("acceptedSnapshotCount must be >= 0");
        }
        if (rejectedSnapshotCount < 0) {
            throw new IllegalArgumentException("rejectedSnapshotCount must be >= 0");
        }
        this.acceptedSnapshotCount = acceptedSnapshotCount;
        this.rejectedSnapshotCount = rejectedSnapshotCount;
    }

    public static ReplayEvidenceValidationResult valid(int acceptedCount) {
        return new ReplayEvidenceValidationResult(
                ReplayValidationStatus.VALID, List.of(), List.of(), acceptedCount, 0);
    }

    public static ReplayEvidenceValidationResult invalid(List<ReplayFailureCode> codes,
                                                         List<String> reasons,
                                                         int acceptedCount,
                                                         int rejectedCount) {
        return new ReplayEvidenceValidationResult(
                ReplayValidationStatus.INVALID, codes, reasons, acceptedCount, rejectedCount);
    }

    public ReplayValidationStatus status() {
        return status;
    }

    public List<ReplayFailureCode> failureCodes() {
        return failureCodes;
    }

    public List<String> failureReasons() {
        return failureReasons;
    }

    public int acceptedSnapshotCount() {
        return acceptedSnapshotCount;
    }

    public int rejectedSnapshotCount() {
        return rejectedSnapshotCount;
    }

    public boolean isValid() {
        return status == ReplayValidationStatus.VALID;
    }
}
