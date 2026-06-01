package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

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
}
