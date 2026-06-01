package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.util.List;
import java.util.Set;

/**
 * Append-only evidence store for {@link ObservedSnapshot}s keyed by run
 * identity. Implementations must preserve insertion order and keep each
 * run's stream independently addressable.
 */
public interface EvidenceRecorder {

    void record(ObservedSnapshot snapshot);

    List<ObservedSnapshot> snapshots(String runId);

    Set<String> runIds();
}
