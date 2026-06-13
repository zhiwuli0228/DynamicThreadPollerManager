package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

/**
 * Maps a raw {@link RuntimeObservation} into an {@link ObservedSnapshot} for a
 * given run. Absent metric values are preserved on the observation while the
 * canonical {@link PressureSnapshot} receives safe fallbacks so callers can
 * always read a non-null snapshot.
 */
public interface SnapshotAssembler {

    ObservedSnapshot assemble(String runId, RuntimeObservation observation);

    default ObservedSnapshot fromExecutorState(String runId, ExecutorStateSnapshot state) {
        RuntimeObservation observation = new RuntimeObservation(
                state.observedAt(),
                state.activeCount() != null
                        ? MetricValue.present(state.activeCount())
                        : MetricValue.absent(),
                state.poolSize() != null
                        ? MetricValue.present(state.poolSize())
                        : MetricValue.absent(),
                state.queueSize() != null
                        ? MetricValue.present(state.queueSize())
                        : MetricValue.absent(),
                state.completedTaskCount() != null
                        ? MetricValue.present(state.completedTaskCount())
                        : MetricValue.absent(),
                MetricValue.absent(),
                state.keepAliveTimeSeconds() != null
                        ? MetricValue.present(state.keepAliveTimeSeconds())
                        : MetricValue.absent(),
                state.largestPoolSize() != null
                        ? MetricValue.present(state.largestPoolSize())
                        : MetricValue.absent(),
                state.taskCount() != null
                        ? MetricValue.present(state.taskCount())
                        : MetricValue.absent()
        );
        return assemble(runId, observation);
    }

    /**
     * Resolves a {@link MetricValue} to a primitive value, falling back to the
     * supplied default when the value is absent.
     */
    static <T> T resolveOrDefault(MetricValue<T> value, T fallback) {
        return value.isPresent() ? value.asOptional().orElseThrow() : fallback;
    }

    /**
     * Resolves a {@link MetricValue} to an {@code int}, falling back to the
     * supplied default when the value is absent.
     */
    static int resolveIntOrDefault(MetricValue<Integer> value, int fallback) {
        return value.isPresent() ? value.asOptional().orElseThrow() : fallback;
    }

    /**
     * Resolves a {@link MetricValue} to a {@code long}, falling back to the
     * supplied default when the value is absent.
     */
    static long resolveLongOrDefault(MetricValue<Long> value, long fallback) {
        return value.isPresent() ? value.asOptional().orElseThrow() : fallback;
    }

    /**
     * Resolves a {@link MetricValue} to a {@code double}, falling back to the
     * supplied default when the value is absent.
     */
    static double resolveDoubleOrDefault(MetricValue<Double> value, double fallback) {
        return value.isPresent() ? value.asOptional().orElseThrow() : fallback;
    }
}
