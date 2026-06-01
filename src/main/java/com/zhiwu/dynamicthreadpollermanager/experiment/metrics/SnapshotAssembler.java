package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

/**
 * Maps a raw {@link RuntimeObservation} into an {@link ObservedSnapshot} for a
 * given run. Absent metric values are preserved on the observation while the
 * canonical {@link PressureSnapshot} receives safe fallbacks so callers can
 * always read a non-null snapshot.
 */
public interface SnapshotAssembler {

    ObservedSnapshot assemble(String runId, RuntimeObservation observation);

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
     * Resolves a {@link MetricValue} to a {@code double}, falling back to the
     * supplied default when the value is absent.
     */
    static double resolveDoubleOrDefault(MetricValue<Double> value, double fallback) {
        return value.isPresent() ? value.asOptional().orElseThrow() : fallback;
    }
}
