package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.util.List;
import java.util.Set;

/**
 * Append-only evidence store for {@link ObservedSnapshot}s keyed by run
 * identity. Implementations must preserve insertion order and keep each
 * run's stream independently addressable.
 *
 * <h3>Thread-safety contract</h3>
 * Implementations of this interface MUST be safe for concurrent use:
 * {@code record()} may be called from multiple threads simultaneously,
 * and {@code snapshots()} may be called concurrently with {@code record()}
 * without data loss or corruption. Callers may safely invoke any combination
 * of methods from any number of threads.
 */
public interface EvidenceRecorder {

    void record(ObservedSnapshot snapshot);

    List<ObservedSnapshot> snapshots(String runId);

    Set<String> runIds();
}
