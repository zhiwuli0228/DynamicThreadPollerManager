package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

/**
 * Identifies the deterministic workload pattern a scenario produces.
 * The profile is a pure data label — the planner owns the step
 * formula for each value.
 */
public enum ScenarioProfile {
    STEADY,
    RAMP,
    BURST
}
