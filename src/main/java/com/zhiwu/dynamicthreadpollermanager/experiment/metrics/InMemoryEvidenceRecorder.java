package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link EvidenceRecorder} that appends snapshots per run. The
 * returned snapshot list is an immutable snapshot taken at call time, so
 * later mutations on the recorder are not visible to earlier readers.
 */
public final class InMemoryEvidenceRecorder implements EvidenceRecorder {

    private final Map<String, List<ObservedSnapshot>> store = new ConcurrentHashMap<>();

    @Override
    public void record(ObservedSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        store.computeIfAbsent(snapshot.runId(), key -> new CopyOnWriteArrayList<>()).add(snapshot);
    }

    @Override
    public List<ObservedSnapshot> snapshots(String runId) {
        Objects.requireNonNull(runId, "runId must not be null");
        List<ObservedSnapshot> recorded = store.get(runId);
        if (recorded == null) {
            return Collections.emptyList();
        }
        return List.copyOf(recorded);
    }

    @Override
    public Set<String> runIds() {
        return Set.copyOf(store.keySet());
    }
}
