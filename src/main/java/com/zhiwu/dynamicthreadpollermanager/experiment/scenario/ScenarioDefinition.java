package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.Objects;

/**
 * Immutable input model for a deterministic scenario run.
 */
public final class ScenarioDefinition {

    private final String scenarioId;
    private final ScenarioProfile profile;
    private final long seed;
    private final int stepCount;
    private final int baseWorkUnits;
    private final String description;

    public ScenarioDefinition(String scenarioId,
                              ScenarioProfile profile,
                              long seed,
                              int stepCount,
                              int baseWorkUnits,
                              String description) {
        this.scenarioId = requireNonBlank(scenarioId, "scenarioId");
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        if (stepCount <= 0) {
            throw new IllegalArgumentException("stepCount must be positive, was " + stepCount);
        }
        if (baseWorkUnits <= 0) {
            throw new IllegalArgumentException("baseWorkUnits must be positive, was " + baseWorkUnits);
        }
        this.seed = seed;
        this.stepCount = stepCount;
        this.baseWorkUnits = baseWorkUnits;
        this.description = description == null ? "" : description;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public ScenarioProfile profile() {
        return profile;
    }

    public long seed() {
        return seed;
    }

    public int stepCount() {
        return stepCount;
    }

    public int baseWorkUnits() {
        return baseWorkUnits;
    }

    public String description() {
        return description;
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ScenarioDefinition that
                && seed == that.seed
                && stepCount == that.stepCount
                && baseWorkUnits == that.baseWorkUnits
                && scenarioId.equals(that.scenarioId)
                && profile == that.profile
                && description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId, profile, seed, stepCount, baseWorkUnits, description);
    }

    @Override
    public String toString() {
        return "ScenarioDefinition{scenarioId='%s', profile=%s, seed=%d, stepCount=%d, baseWorkUnits=%d, description='%s'}"
                .formatted(scenarioId, profile, seed, stepCount, baseWorkUnits, description);
    }
}
