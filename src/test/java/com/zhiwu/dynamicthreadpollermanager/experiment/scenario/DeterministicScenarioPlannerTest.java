package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterministicScenarioPlannerTest {

    private final DeterministicScenarioPlanner planner = new DeterministicScenarioPlanner();

    @Test
    void shouldProduceIdenticalPlansForSameSteadyDefinition() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "steady", ScenarioProfile.STEADY, 0L, 4, 3, "");

        ScenarioPlan first = planner.plan(definition);
        ScenarioPlan second = planner.plan(definition);

        assertEquals(first, second);
        assertEquals(4, first.stepCount());
        for (ScenarioStep step : first.steps()) {
            assertEquals(3, step.workUnits());
        }
        assertEquals(12L, first.totalWorkUnits());
    }

    @Test
    void shouldUseBaseWorkUnitsPlusIndexForRamp() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "ramp", ScenarioProfile.RAMP, 0L, 5, 10, "");

        ScenarioPlan plan = planner.plan(definition);

        for (int i = 0; i < plan.stepCount(); i++) {
            assertEquals(10 + i, plan.steps().get(i).workUnits());
        }
        // 10 + 11 + 12 + 13 + 14 = 60
        assertEquals(60L, plan.totalWorkUnits());
    }

    @Test
    void shouldSpikeEveryThirdStepForBurst() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "burst", ScenarioProfile.BURST, 0L, 7, 4, "");

        ScenarioPlan plan = planner.plan(definition);

        // indexes 0, 3, 6 spike (3 * base); indexes 1, 2, 4, 5 are base.
        int[] indexes = {0, 1, 2, 3, 4, 5, 6};
        int[] expected = {12, 4, 4, 12, 4, 4, 12};
        long expectedTotal = 0L;
        for (int i = 0; i < indexes.length; i++) {
            assertEquals(expected[i], plan.steps().get(indexes[i]).workUnits());
            expectedTotal += expected[i];
        }
        assertEquals(expectedTotal, plan.totalWorkUnits());
    }

    @Test
    void shouldProduceSamePlanForSameDefinitionRegardlessOfSeed() {
        ScenarioDefinition a = new ScenarioDefinition(
                "ramp", ScenarioProfile.RAMP, 1L, 6, 2, "");
        ScenarioDefinition b = new ScenarioDefinition(
                "ramp", ScenarioProfile.RAMP, 999L, 6, 2, "");

        ScenarioPlan planA = planner.plan(a);
        ScenarioPlan planB = planner.plan(b);

        assertEquals(planA, planB);
    }

    @Test
    void shouldProduceDifferentPlansForDifferentStepCounts() {
        ScenarioDefinition a = new ScenarioDefinition(
                "ramp", ScenarioProfile.RAMP, 0L, 3, 2, "");
        ScenarioDefinition b = new ScenarioDefinition(
                "ramp", ScenarioProfile.RAMP, 0L, 4, 2, "");

        assertNotEquals(planner.plan(a), planner.plan(b));
    }
}
