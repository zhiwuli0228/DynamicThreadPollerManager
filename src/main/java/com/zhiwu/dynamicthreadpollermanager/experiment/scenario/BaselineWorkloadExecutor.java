package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.Objects;

/**
 * Synchronous, fixed-size baseline workload executor. The executor
 * counts completed steps and completed work units so the runner can
 * map them into {@code RuntimeObservation} values. It does not own
 * a real thread pool, never resizes, and never schedules work —
 * steps run in the calling thread.
 */
public final class BaselineWorkloadExecutor {

    private final BaselineExecutorPreset preset;
    private int completedStepCount;
    private long completedWorkUnits;

    public BaselineWorkloadExecutor(BaselineExecutorPreset preset) {
        this.preset = Objects.requireNonNull(preset, "preset must not be null");
        this.completedStepCount = 0;
        this.completedWorkUnits = 0L;
    }

    public BaselineExecutorPreset preset() {
        return preset;
    }

    public void executeStep(ScenarioStep step) {
        Objects.requireNonNull(step, "step must not be null");
        completedStepCount += 1;
        completedWorkUnits += step.workUnits();
    }

    public ScenarioPlan executePlan(ScenarioPlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        for (ScenarioStep step : plan.steps()) {
            executeStep(step);
        }
        return plan;
    }

    public int completedStepCount() {
        return completedStepCount;
    }

    public long completedWorkUnits() {
        return completedWorkUnits;
    }

    public int activeThreads() {
        return 0;
    }

    public int poolSize() {
        return preset.corePoolSize();
    }

    public int queueSize() {
        return 0;
    }

    public long completedTaskCount() {
        return completedWorkUnits;
    }
}
