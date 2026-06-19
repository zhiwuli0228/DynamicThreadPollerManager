package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioPlanTest {

    @Test
    void shouldExposeStepsAndTotalWorkUnits() {
        List<ScenarioStep> steps = List.of(
                new ScenarioStep(0, 5, 0L),
                new ScenarioStep(1, 7, 0L),
                new ScenarioStep(2, 11, 0L)
        );

        ScenarioPlan plan = new ScenarioPlan("steady-3", steps);

        assertEquals("steady-3", plan.scenarioId());
        assertEquals(3, plan.stepCount());
        assertEquals(23L, plan.totalWorkUnits());
        assertEquals(steps, plan.steps());
    }

    @Test
    void shouldRejectEmptySteps() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioPlan("empty", List.of()));
    }

    @Test
    void shouldRejectNullScenarioId() {
        assertThrows(NullPointerException.class,
                () -> new ScenarioPlan(null, List.of(new ScenarioStep(0, 1, 0L))));
    }

    @Test
    void shouldRejectNullSteps() {
        assertThrows(NullPointerException.class, () -> new ScenarioPlan("x", null));
    }
}
