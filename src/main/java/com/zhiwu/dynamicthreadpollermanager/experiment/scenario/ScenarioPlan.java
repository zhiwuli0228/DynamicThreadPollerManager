package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable ordered result of planning a {@link ScenarioDefinition}.
 * The total work units is derived from the step list to keep callers
 * from recomputing it.
 */
public final class ScenarioPlan {

    private final String scenarioId;
    private final List<ScenarioStep> steps;
    private final long totalWorkUnits;

    public ScenarioPlan(String scenarioId, List<ScenarioStep> steps) {
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(steps, "steps must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        List<ScenarioStep> copy = List.copyOf(steps);
        long total = 0L;
        for (ScenarioStep step : copy) {
            total += step.workUnits();
        }
        this.steps = Collections.unmodifiableList(copy);
        this.totalWorkUnits = total;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public List<ScenarioStep> steps() {
        return steps;
    }

    public long totalWorkUnits() {
        return totalWorkUnits;
    }

    public int stepCount() {
        return steps.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ScenarioPlan that
                && totalWorkUnits == that.totalWorkUnits
                && scenarioId.equals(that.scenarioId)
                && steps.equals(that.steps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId, steps, totalWorkUnits);
    }

    @Override
    public String toString() {
        return "ScenarioPlan{scenarioId='%s', stepCount=%d, totalWorkUnits=%d}"
                .formatted(scenarioId, steps.size(), totalWorkUnits);
    }
}
