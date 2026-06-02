package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

import java.util.Objects;

/**
 * Default {@link SnapshotAssembler} that maps each present metric directly
 * into a {@link PressureSnapshot} and substitutes zero for any absent metric
 * so the snapshot is always usable while the original absence is retained on
 * the {@link RuntimeObservation}.
 */
public final class DefaultSnapshotAssembler implements SnapshotAssembler {

    @Override
    public ObservedSnapshot assemble(String runId, RuntimeObservation observation) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(observation, "observation must not be null");

        PressureSnapshot snapshot = new PressureSnapshot(
                observation.timestamp(),
                SnapshotAssembler.resolveIntOrDefault(observation.activeThreads(), 0),
                SnapshotAssembler.resolveIntOrDefault(observation.poolSize(), 0),
                SnapshotAssembler.resolveIntOrDefault(observation.queueSize(), 0),
                SnapshotAssembler.resolveLongOrDefault(observation.completedTaskCount(), 0L),
                SnapshotAssembler.resolveDoubleOrDefault(observation.cpuUtilization(), 0.0)
        );
        return new ObservedSnapshot(runId, snapshot, observation);
    }
}
