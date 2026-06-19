package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

/**
 * Builds a {@link ScenarioPlan} from a {@link ScenarioDefinition}.
 * Implementations must be deterministic with respect to their
 * inputs.
 */
public interface ScenarioPlanner {

    ScenarioPlan plan(ScenarioDefinition definition);
}
