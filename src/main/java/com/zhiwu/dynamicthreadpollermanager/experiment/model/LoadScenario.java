package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.util.Objects;

/**
 * Immutable description of a load scenario used in an experiment run.
 */
public final class LoadScenario {

    private final String scenarioId;
    private final String description;

    public LoadScenario(String scenarioId, String description) {
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        this.description = description;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof LoadScenario that && Objects.equals(scenarioId, that.scenarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId);
    }

    @Override
    public String toString() {
        return "LoadScenario{scenarioId='%s', description='%s'}".formatted(scenarioId, description);
    }
}
