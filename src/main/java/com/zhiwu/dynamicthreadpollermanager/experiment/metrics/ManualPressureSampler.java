package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import java.time.Instant;
import java.util.Objects;

/**
 * Deterministic {@link PressureSampler} that records snapshots only when a
 * caller supplies the observation and the timestamp. Suitable for unit tests
 * and for runtime callers that already hold the source observation.
 */
public final class ManualPressureSampler implements PressureSampler {

    private final SnapshotAssembler assembler;

    public ManualPressureSampler(SnapshotAssembler assembler) {
        this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
    }

    public ManualPressureSampler() {
        this(new DefaultSnapshotAssembler());
    }

    @Override
    public ObservedSnapshot sample(String runId, RuntimeObservation observation, Instant at) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(observation, "observation must not be null");
        Objects.requireNonNull(at, "at must not be null");
        return assembler.assemble(runId, observation.withTimestamp(at));
    }

    public ObservedSnapshot sampleFromExecutorState(String runId, ExecutorStateSnapshot state) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(state, "state must not be null");
        return assembler.fromExecutorState(runId, state);
    }
}
