package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaselineWorkloadExecutorTest {

    private final BaselineExecutorPreset preset = BaselineExecutorPreset.fixedSmall();

    @Test
    void shouldStartAtZero() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);

        assertEquals(0, executor.completedStepCount());
        assertEquals(0L, executor.completedWorkUnits());
        assertEquals(0, executor.activeThreads());
        assertEquals(preset.corePoolSize(), executor.poolSize());
        assertEquals(0, executor.queueSize());
        assertEquals(0L, executor.completedTaskCount());
    }

    @Test
    void shouldAccumulateCompletedStepsAndWorkUnits() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        executor.executeStep(new ScenarioStep(0, 5, 0L));
        executor.executeStep(new ScenarioStep(1, 7, 0L));
        executor.executeStep(new ScenarioStep(2, 9, 0L));

        assertEquals(3, executor.completedStepCount());
        assertEquals(21L, executor.completedWorkUnits());
        assertEquals(21L, executor.completedTaskCount());
    }

    @Test
    void shouldExecuteEveryStepInAPlan() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        ScenarioPlan plan = new ScenarioPlan("steady", List.of(
                new ScenarioStep(0, 3, 0L),
                new ScenarioStep(1, 3, 0L),
                new ScenarioStep(2, 3, 0L),
                new ScenarioStep(3, 3, 0L)
        ));

        BaselineWorkloadExecutor returned = executor;
        ScenarioPlan executedPlan = executor.executePlan(plan);

        // Method returns the plan for fluent use; counters reflect all steps.
        assertEquals(plan, executedPlan);
        assertEquals(4, returned.completedStepCount());
        assertEquals(12L, returned.completedWorkUnits());
    }

    @Test
    void shouldNotResizePoolOrQueue() {
        BaselineWorkloadExecutor executor = new BaselineWorkloadExecutor(preset);
        for (int i = 0; i < 10; i++) {
            executor.executeStep(new ScenarioStep(i, 2, 0L));
        }

        // The synchronous executor must never change pool sizing or
        // accumulate a queue, regardless of how many steps run.
        assertEquals(preset.corePoolSize(), executor.poolSize());
        assertEquals(0, executor.queueSize());
        assertEquals(0, executor.activeThreads());
    }
}
