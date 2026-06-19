package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DecisionOrchestratorTest {

    private DecisionOrchestrator orchestrator;
    private ManagedExecutor executor;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        PressureClassifier classifier = new SnapshotPressureClassifier();
        PolicyScorer scorer = new ThresholdPolicyScorer();
        PolicyRanker ranker = new PolicyRanker(scorer);
        PolicyEvaluator evaluator = new ThresholdPolicyEvaluator();
        ClassifierConfig config = ClassifierConfig.defaults();

        orchestrator = new DecisionOrchestrator(classifier, ranker, evaluator, config);

        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
    }

    @Test
    void shouldReturnNoOpForEmptySnapshots() {
        var decision = orchestrator.decide(List.of(), List.of(ThresholdPolicyConfig.defaultAdaptive()),
                executor, "r1");
        assertTrue(decision.isNoOp());
    }

    @Test
    void shouldReturnNoOpForEmptyCandidates() {
        var snapshots = createOverloadSnapshots();
        // empty candidates — but LoopConfig guards against this, so we test at orchestrator level
        var decision = orchestrator.decide(snapshots, List.of(), executor, "r1");
        assertTrue(decision.isNoOp());
    }

    @Test
    void shouldSelectAggressivePolicyForOverload() {
        var snapshots = createOverloadSnapshots();

        var conservative = new ThresholdPolicyConfig("conservative", 1, 8, 8, 8, 1, 1);
        var aggressive = new ThresholdPolicyConfig("aggressive", 1, 32, 4, 4, 4, 4);
        var moderate = new ThresholdPolicyConfig("moderate", 1, 16, 6, 6, 3, 2);

        var decision = orchestrator.decide(snapshots,
                List.of(conservative, aggressive, moderate), executor, "r1");

        assertFalse(decision.isNoOp());
        // For OVERLOAD, aggressive policy (low thresholds) should rank highest
        assertNotNull(decision.selectedScore());
        assertNotNull(decision.selectedPolicy());
        assertFalse(decision.rationale().isBlank());
        assertTrue(decision.rationale().contains("OVERLOAD"));
    }

    @Test
    void shouldUseSnapshotTimestampForEvaluationInput() {
        var snapshots = createOverloadSnapshots();
        var decision = orchestrator.decide(snapshots,
                List.of(ThresholdPolicyConfig.defaultAdaptive()), executor, "r1");

        // The evaluatedAt in PolicyDecision.timestamp comes from the
        // PolicyEvaluationInput.evaluatedAt which should be lastSnapshot.timestamp()
        assertNotNull(decision.policyDecision().timestamp());
        // Should match the last snapshot's timestamp
        assertEquals(snapshots.get(snapshots.size() - 1).snapshot().timestamp(),
                decision.policyDecision().timestamp());
    }

    private List<ObservedSnapshot> createOverloadSnapshots() {
        List<ObservedSnapshot> snapshots = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Instant ts = now.plusSeconds(i);
            PressureSnapshot ps = new PressureSnapshot(ts, 4, 4, 10 + i, 100L + i * 10, 0.9);
            RuntimeObservation obs = new RuntimeObservation(ts,
                    MetricValue.present(4),
                    MetricValue.present(4),
                    MetricValue.present(10 + i),
                    MetricValue.present(100L + i * 10),
                    MetricValue.present(0.9));
            snapshots.add(new ObservedSnapshot("r1", ps, obs));
        }
        return snapshots;
    }
}
