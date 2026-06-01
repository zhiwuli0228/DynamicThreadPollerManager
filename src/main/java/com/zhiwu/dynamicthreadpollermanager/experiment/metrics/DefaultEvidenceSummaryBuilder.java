package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link EvidenceSummaryBuilder} that returns an empty summary for
 * streams with no snapshots and otherwise reports the recorded count and
 * first/last timestamps from insertion order.
 */
public final class DefaultEvidenceSummaryBuilder implements EvidenceSummaryBuilder {

    @Override
    public EvidenceSummary summarize(String runId, List<ObservedSnapshot> snapshots) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(snapshots, "snapshots must not be null");

        if (snapshots.isEmpty()) {
            return EvidenceSummary.empty(runId);
        }
        Instant first = snapshots.get(0).snapshot().timestamp();
        Instant last = snapshots.get(snapshots.size() - 1).snapshot().timestamp();
        return new EvidenceSummary(runId, snapshots.size(), Optional.of(first), Optional.of(last));
    }
}
