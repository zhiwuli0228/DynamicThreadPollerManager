package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Observation data captured for a single decision during a scenario run.
 *
 * @param decisionIndex       zero-based index of this decision in the run
 * @param preDecisionSnapshots  snapshots captured before the decision
 * @param postDecisionSnapshots snapshots captured after the decision
 * @param decisionTimestamp     when the decision was made
 */
public record ObservationWindow(
        int decisionIndex,
        List<ExecutorStateSnapshot> preDecisionSnapshots,
        List<ExecutorStateSnapshot> postDecisionSnapshots,
        Instant decisionTimestamp
) {
    public ObservationWindow {
        if (decisionIndex < 0) {
            throw new IllegalArgumentException("decisionIndex must be >= 0, was " + decisionIndex);
        }
        Objects.requireNonNull(preDecisionSnapshots, "preDecisionSnapshots must not be null");
        Objects.requireNonNull(postDecisionSnapshots, "postDecisionSnapshots must not be null");
        Objects.requireNonNull(decisionTimestamp, "decisionTimestamp must not be null");
        preDecisionSnapshots = List.copyOf(preDecisionSnapshots);
        postDecisionSnapshots = List.copyOf(postDecisionSnapshots);
    }
}
