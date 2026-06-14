package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ComparableScenarioRunnerTest {

    private final Supplier<Instant> clock = Instant::now;

    private static ScenarioDefinition createScenario(String id, int stepCount) {
        return new ScenarioDefinition(id, ScenarioProfile.STEADY, 0L,
                stepCount, 1, "");
    }

    @Test
    void compareShouldReturnComparisonResultWithDifferentRunIds() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = createScenario("test-scenario", 10);
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result = runner.compare(scenario, "fixed-4", config);

        assertNotNull(result);
        assertEquals("test-scenario", result.scenarioId());
        assertEquals("fixed-4", result.baselinePresetId());
        assertNotNull(result.baselineOutcome());
        assertNotNull(result.managedOutcome());
        assertNotEquals(result.baselineOutcome().runId(), result.managedOutcome().runId());
    }

    @Test
    void compareShouldReturnNineDeltas() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = createScenario("test-scenario", 5);
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result = runner.compare(scenario, "fixed-2-bounded", config);

        assertEquals(9, result.deltas().size());
        assertTrue(result.deltas().containsKey("completedTaskCount"));
        assertTrue(result.deltas().containsKey("rejectedTaskCount"));
        assertTrue(result.deltas().containsKey("throughputPerSecond"));
        assertTrue(result.deltas().containsKey("totalDurationMs"));
    }

    @Test
    void compareShouldFailFastOnNonexistentPreset() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = createScenario("test-scenario", 5);
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        assertThrows(NoSuchElementException.class,
                () -> runner.compare(scenario, "nonexistent", config));
    }

    @Test
    void compareWithDifferentPresetsShouldBothSucceed() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = createScenario("test-scenario", 3);
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result1 = runner.compare(scenario, "fixed-2", config);
        ComparisonResult result2 = runner.compare(scenario, "fixed-8", config);

        assertEquals("fixed-2", result1.baselinePresetId());
        assertEquals("fixed-8", result2.baselinePresetId());
        assertNotNull(result1.baselineMetrics());
        assertNotNull(result2.baselineMetrics());
    }

    @Test
    void compareShouldIncludeBaselineAndManagedMetrics() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = createScenario("test-scenario", 10);
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result = runner.compare(scenario, "fixed-4", config);

        assertTrue(result.baselineMetrics().snapshotCount() > 0);
        assertTrue(result.managedMetrics().snapshotCount() > 0);
        assertTrue(result.baselineMetrics().completedTaskCount() > 0);
        assertTrue(result.managedMetrics().completedTaskCount() > 0);
    }

    @Test
    void compareShouldHaveBaselineRejectedTaskCountZero() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        ScenarioPlanner planner = new DeterministicScenarioPlanner();
        ComparableScenarioRunner runner = new ComparableScenarioRunner(catalog, planner, clock);

        ScenarioDefinition scenario = createScenario("test-scenario", 5);
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();

        ComparisonResult result = runner.compare(scenario, "fixed-4", config);

        assertEquals(0L, result.baselineMetrics().rejectedTaskCount());
        assertTrue(result.managedMetrics().rejectedTaskCount() >= 0);
    }
}
