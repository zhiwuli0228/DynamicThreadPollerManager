package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result of a single coordination decision. Carries the original command,
 * the approved (possibly modified) command, the outcome, and metadata.
 */
public record GroupCoordinationResult(
        ScaleAdjustmentCommand command,
        ScaleAdjustmentCommand approvedCommand,
        CoordinationOutcome outcome,
        String rationale,
        List<String> conflicts,
        boolean crossOscillationDetected,
        Instant coordinatedAt) {

    public GroupCoordinationResult {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(approvedCommand, "approvedCommand must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
        if (rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        Objects.requireNonNull(conflicts, "conflicts must not be null");
        Objects.requireNonNull(coordinatedAt, "coordinatedAt must not be null");
        conflicts = List.copyOf(conflicts);
    }

    public boolean isApproved() {
        return outcome == CoordinationOutcome.APPROVED_AS_IS
                || outcome == CoordinationOutcome.MODIFIED;
    }

    public boolean isRejected() {
        return outcome == CoordinationOutcome.REJECTED;
    }
}
