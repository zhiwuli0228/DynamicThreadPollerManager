package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Observation output that pairs a canonical {@link PressureSnapshot} with the
 * original {@link RuntimeObservation} so absent metric values remain explicit
 * downstream of the assembler.
 */
public final class ObservedSnapshot {

    private final String runId;
    private final PressureSnapshot snapshot;
    private final RuntimeObservation observation;

    public ObservedSnapshot(String runId, PressureSnapshot snapshot, RuntimeObservation observation) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.observation = Objects.requireNonNull(observation, "observation must not be null");
    }

    public String runId() {
        return runId;
    }

    public PressureSnapshot snapshot() {
        return snapshot;
    }

    public RuntimeObservation observation() {
        return observation;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("runId", runId);
        map.put("snapshot", snapshot.toMap());
        map.put("observation", observation.toMap());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ObservedSnapshot fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        Object runIdObj = map.get("runId");
        if (!(runIdObj instanceof String runId)) {
            throw new IllegalArgumentException(
                    "map must contain String 'runId', got "
                            + (runIdObj == null ? "null" : runIdObj.getClass().getSimpleName()));
        }
        Object snapshotObj = map.get("snapshot");
        if (!(snapshotObj instanceof Map)) {
            throw new IllegalArgumentException(
                    "map must contain Map 'snapshot', got "
                            + (snapshotObj == null ? "null" : snapshotObj.getClass().getSimpleName()));
        }
        Object observationObj = map.get("observation");
        if (!(observationObj instanceof Map)) {
            throw new IllegalArgumentException(
                    "map must contain Map 'observation', got "
                            + (observationObj == null ? "null" : observationObj.getClass().getSimpleName()));
        }
        PressureSnapshot snapshot = PressureSnapshot.fromMap(
                (Map<String, Object>) snapshotObj);
        RuntimeObservation observation = RuntimeObservation.fromMap(
                (Map<String, Object>) observationObj);
        return new ObservedSnapshot(runId, snapshot, observation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObservedSnapshot that)) return false;
        return runId.equals(that.runId)
                && snapshot.equals(that.snapshot)
                && observation.equals(that.observation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, snapshot, observation);
    }

    @Override
    public String toString() {
        return "ObservedSnapshot{runId='%s', snapshot=%s, observation=%s}"
                .formatted(runId, snapshot, observation);
    }
}
