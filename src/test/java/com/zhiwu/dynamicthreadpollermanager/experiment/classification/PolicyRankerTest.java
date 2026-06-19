package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PolicyRankerTest {

    private PolicyRanker ranker;
    private PressureClassification classification;

    @BeforeEach
    void setUp() {
        ranker = new PolicyRanker(new ThresholdPolicyScorer());

        List<ObservedSnapshot> snapshots = List.of(
                snapshot(8, 8, 15, 10L), snapshot(8, 8, 15, 20L),
                snapshot(8, 8, 15, 30L), snapshot(8, 8, 15, 40L),
                snapshot(8, 8, 15, 50L));
        classification = new SnapshotPressureClassifier()
                .classify(snapshots,
                        new ClassifierConfig(5, 0.1, 10, 20), 0L, 5000L);
    }

    private static ObservedSnapshot snapshot(
            int activeThreads, int poolSize, int queueSize, long completed) {
        Instant now = Instant.now();
        PressureSnapshot ps = new PressureSnapshot(
                now, activeThreads, poolSize, queueSize, completed, 0.0);
        RuntimeObservation obs = new RuntimeObservation(
                now, MetricValue.present(activeThreads),
                MetricValue.present(queueSize), MetricValue.absent());
        return new ObservedSnapshot("run-1", ps, obs);
    }

    @Test
    void shouldRankInDescendingOrder() {
        ThresholdPolicyConfig c1 = new ThresholdPolicyConfig(
                "p1", 2, 8, 24, 16, 4, 2);
        ThresholdPolicyConfig c2 = new ThresholdPolicyConfig(
                "p2", 2, 16, 4, 8, 2, 4);
        ThresholdPolicyConfig c3 = ThresholdPolicyConfig.defaultAdaptive();

        List<PolicyScore> ranked = ranker.rank(
                classification, List.of(c1, c2, c3));

        assertEquals(3, ranked.size());
        for (int i = 1; i < ranked.size(); i++) {
            assertTrue(ranked.get(i - 1).compositeScore()
                            >= ranked.get(i).compositeScore(),
                    "scores should be descending");
        }
    }

    @Test
    void shouldBestReturnHighestScore() {
        ThresholdPolicyConfig c1 = new ThresholdPolicyConfig(
                "p1", 2, 8, 24, 16, 4, 2);
        ThresholdPolicyConfig c2 = new ThresholdPolicyConfig(
                "p2", 2, 16, 4, 8, 2, 4);

        Optional<PolicyScore> best = ranker.best(
                classification, List.of(c1, c2));

        assertTrue(best.isPresent());
        assertEquals("p2", best.get().policyId());
    }

    @Test
    void shouldReturnEmptyForEmptyCandidates() {
        List<PolicyScore> ranked = ranker.rank(classification, List.of());
        assertTrue(ranked.isEmpty());

        Optional<PolicyScore> best = ranker.best(classification, List.of());
        assertTrue(best.isEmpty());
    }

    @Test
    void shouldHandleSingleCandidate() {
        ThresholdPolicyConfig config = ThresholdPolicyConfig.defaultAdaptive();
        List<PolicyScore> ranked = ranker.rank(classification, List.of(config));
        assertEquals(1, ranked.size());

        Optional<PolicyScore> best = ranker.best(classification, List.of(config));
        assertTrue(best.isPresent());
    }
}
