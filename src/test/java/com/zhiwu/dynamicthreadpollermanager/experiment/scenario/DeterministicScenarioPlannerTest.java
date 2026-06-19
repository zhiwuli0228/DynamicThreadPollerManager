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

    @Test
    void longTailWithSeedDivisibleBy3ProducesSpikeSteps() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "long-tail-spike", ScenarioProfile.LONG_TAIL, 6L, 4, 100, "");

        ScenarioPlan plan = planner.plan(definition);

        assertEquals(4, plan.stepCount());
        for (ScenarioStep step : plan.steps()) {
            assertEquals(600, step.workUnits());
            assertEquals(0L, step.plannedDelayMillis());
        }
    }

    @Test
    void longTailWithSeedNotDivisibleBy3ProducesBaseSteps() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "long-tail-base", ScenarioProfile.LONG_TAIL, 7L, 4, 100, "");

        ScenarioPlan plan = planner.plan(definition);

        assertEquals(4, plan.stepCount());
        for (ScenarioStep step : plan.steps()) {
            assertEquals(100, step.workUnits());
            assertEquals(0L, step.plannedDelayMillis());
        }
    }

    @Test
    void longTailPlanIsDeterministicAcrossInvocations() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "long-tail-det", ScenarioProfile.LONG_TAIL, 6L, 4, 100, "");

        ScenarioPlan first = planner.plan(definition);
        ScenarioPlan second = planner.plan(definition);

        assertEquals(first, second);
    }

    @Test
    void mixedCpuIoAlternatesCpuAndIoSteps() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "mixed", ScenarioProfile.MIXED_CPU_IO, 0L, 4, 50, "");

        ScenarioPlan plan = planner.plan(definition);

        assertEquals(4, plan.stepCount());
        assertEquals(150, plan.steps().get(0).workUnits());
        assertEquals(0L, plan.steps().get(0).plannedDelayMillis());
        assertEquals(50, plan.steps().get(1).workUnits());
        assertEquals(100L, plan.steps().get(1).plannedDelayMillis());
        assertEquals(150, plan.steps().get(2).workUnits());
        assertEquals(0L, plan.steps().get(2).plannedDelayMillis());
        assertEquals(50, plan.steps().get(3).workUnits());
        assertEquals(100L, plan.steps().get(3).plannedDelayMillis());
    }

    @Test
    void mixedCpuIoPlanIsDeterministicAcrossInvocations() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "mixed-det", ScenarioProfile.MIXED_CPU_IO, 7L, 8, 50, "");

        ScenarioPlan first = planner.plan(definition);
        ScenarioPlan second = planner.plan(definition);

        assertEquals(first, second);
    }

    @Test
    void downstreamBlockedUsesConstantWorkWithHighDelay() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "blocked", ScenarioProfile.DOWNSTREAM_BLOCKED, 0L, 3, 200, "");

        ScenarioPlan plan = planner.plan(definition);

        assertEquals(3, plan.stepCount());
        for (ScenarioStep step : plan.steps()) {
            assertEquals(200, step.workUnits());
            assertEquals(2000L, step.plannedDelayMillis());
        }
    }

    @Test
    void downstreamBlockedPlanIsDeterministicAcrossInvocations() {
        ScenarioDefinition definition = new ScenarioDefinition(
                "blocked-det", ScenarioProfile.DOWNSTREAM_BLOCKED, 99L, 6, 200, "");

        ScenarioPlan first = planner.plan(definition);
        ScenarioPlan second = planner.plan(definition);

        assertEquals(first, second);
    }

    @Test
    void differentSeedsProduceDifferentLongTailPlansWhenSeedMod3Differs() {
        ScenarioDefinition defA = new ScenarioDefinition(
                "lt-a", ScenarioProfile.LONG_TAIL, 3L, 4, 100, "");
        ScenarioDefinition defB = new ScenarioDefinition(
                "lt-b", ScenarioProfile.LONG_TAIL, 4L, 4, 100, "");

        ScenarioPlan planA = planner.plan(defA);
        ScenarioPlan planB = planner.plan(defB);

        for (ScenarioStep step : planA.steps()) {
            assertEquals(600, step.workUnits());
        }
        for (ScenarioStep step : planB.steps()) {
            assertEquals(100, step.workUnits());
        }
        assertNotEquals(planA, planB);
    }

    @Test
    void sameSeedProducesIdenticalPlansForDownstreamBlocked() {
        ScenarioDefinition defA = new ScenarioDefinition(
                "db", ScenarioProfile.DOWNSTREAM_BLOCKED, 99L, 3, 200, "");
        ScenarioDefinition defB = new ScenarioDefinition(
                "db", ScenarioProfile.DOWNSTREAM_BLOCKED, 99L, 3, 200, "");

        assertEquals(planner.plan(defA), planner.plan(defB));
    }
}
