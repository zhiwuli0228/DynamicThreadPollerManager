package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.Objects;

/**
 * Immutable ordered unit of work inside a {@link ScenarioPlan}.
 * The step index is the zero-based position of the step within its
 * plan, the work units count the work the step should perform, and
 * the planned delay is recorded data only — runners must not sleep.
 */
public final class ScenarioStep {

    private final int index;
    private final int workUnits;
    private final long plannedDelayMillis;

    public ScenarioStep(int index, int workUnits, long plannedDelayMillis) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative, was " + index);
        }
        if (workUnits < 0) {
            throw new IllegalArgumentException("workUnits must be non-negative, was " + workUnits);
        }
        if (plannedDelayMillis < 0) {
            throw new IllegalArgumentException("plannedDelayMillis must be non-negative, was " + plannedDelayMillis);
        }
        this.index = index;
        this.workUnits = workUnits;
        this.plannedDelayMillis = plannedDelayMillis;
    }

    public int index() {
        return index;
    }

    public int workUnits() {
        return workUnits;
    }

    public long plannedDelayMillis() {
        return plannedDelayMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ScenarioStep that
                && index == that.index
                && workUnits == that.workUnits
                && plannedDelayMillis == that.plannedDelayMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, workUnits, plannedDelayMillis);
    }

    @Override
    public String toString() {
        return "ScenarioStep{index=%d, workUnits=%d, plannedDelayMillis=%d}"
                .formatted(index, workUnits, plannedDelayMillis);
    }
}
