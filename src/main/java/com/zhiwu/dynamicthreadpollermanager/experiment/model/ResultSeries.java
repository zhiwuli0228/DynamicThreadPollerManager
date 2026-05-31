package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable collection of pressure snapshots recorded during an experiment run.
 */
public final class ResultSeries {

    private final String runId;
    private final List<PressureSnapshot> snapshots;

    public ResultSeries(String runId, List<PressureSnapshot> snapshots) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.snapshots = List.copyOf(snapshots);
    }

    public String runId() {
        return runId;
    }

    public List<PressureSnapshot> snapshots() {
        return snapshots;
    }

    public int size() {
        return snapshots.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ResultSeries that && Objects.equals(runId, that.runId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId);
    }

    @Override
    public String toString() {
        return "ResultSeries{runId='%s', snapshotCount=%d}".formatted(runId, snapshots.size());
    }
}
