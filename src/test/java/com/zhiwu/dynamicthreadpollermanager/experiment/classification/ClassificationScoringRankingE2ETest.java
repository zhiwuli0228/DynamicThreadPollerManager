package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationScoringRankingE2ETest {

    private ManagedExecutor executor;
    private ManagedExecutorConfig managedConfig;

    @BeforeEach
    void setUp() {
        managedConfig = new ManagedExecutorConfig(2, 4, 10, 60,
                java.util.concurrent.TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.close();
        }
    }

    @Test
    void shouldClassifyScoreAndRankEndToEnd() {
        ScenarioDefinition scenario = new ScenarioDefinition(
                "e2e-test", ScenarioProfile.STEADY, System.currentTimeMillis(),
                20, 1, "E2E classification test");
        DeterministicScenarioPlanner planner = new DeterministicScenarioPlanner();
        ScenarioPlan plan = planner.plan(scenario);

        ExperimentCoordinator coordinator = new ExperimentCoordinator();
        InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();
        ManualPressureSampler sampler = new ManualPressureSampler(
                new DefaultSnapshotAssembler());
        Supplier<Instant> clock = Instant::now;

        ManagedExecutorScenarioRunner runner = new ManagedExecutorScenarioRunner(
                coordinator, planner, sampler, recorder, clock);

        long startMs = clock.get().toEpochMilli();
        ScenarioRunOutcome outcome = runner.run(scenario, managedConfig);
        long endMs = clock.get().toEpochMilli();

        List<ObservedSnapshot> snapshots = recorder.snapshots(outcome.runId());
        assertFalse(snapshots.isEmpty(), "should have captured snapshots");

        // Classify
        SnapshotPressureClassifier classifier = new SnapshotPressureClassifier();
        ClassifierConfig config = ClassifierConfig.defaults();
        PressureClassification classification = classifier.classify(
                snapshots, config, outcome.rejectedTaskCount(),
                endMs - startMs);

        assertNotNull(classification);
        assertNotNull(classification.state());
        assertTrue(classification.confidence() > 0.0);

        // Score 3 policies
        ThresholdPolicyScorer scorer = new ThresholdPolicyScorer();
        ThresholdPolicyConfig conservative = new ThresholdPolicyConfig(
                "conservative", 2, 8, 24, 16, 4, 2);
        ThresholdPolicyConfig moderate = ThresholdPolicyConfig.defaultAdaptive();
        ThresholdPolicyConfig aggressive = new ThresholdPolicyConfig(
                "aggressive", 2, 16, 4, 8, 2, 4);

        PolicyScore s1 = scorer.score(classification, conservative);
        PolicyScore s2 = scorer.score(classification, moderate);
        PolicyScore s3 = scorer.score(classification, aggressive);

        assertFalse(s1.explanation().isBlank());
        assertFalse(s2.explanation().isBlank());
        assertFalse(s3.explanation().isBlank());

        // Rank
        PolicyRanker ranker = new PolicyRanker(scorer);
        List<PolicyScore> ranked = ranker.rank(
                classification, List.of(conservative, moderate, aggressive));

        assertEquals(3, ranked.size());
        for (int i = 1; i < ranked.size(); i++) {
            assertTrue(ranked.get(i - 1).compositeScore()
                            >= ranked.get(i).compositeScore(),
                    "scores should be descending");
        }

        // best() should not be empty
        assertTrue(ranker.best(classification,
                List.of(conservative, moderate, aggressive)).isPresent());
    }
}
