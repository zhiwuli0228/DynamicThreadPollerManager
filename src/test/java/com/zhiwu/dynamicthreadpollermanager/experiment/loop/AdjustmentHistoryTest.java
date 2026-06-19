package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class AdjustmentHistoryTest {

    private AdjustmentHistory history;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        history = new AdjustmentHistory();
    }

    private HistoryEntry createEntry(PressureState before, PressureState after) {
        PressureClassification beforeClass = new PressureClassification(
                before, 0.8, java.util.List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(java.util.List.of(), 0L, 4, 5),
                now);
        PressureClassification afterClass = new PressureClassification(
                after, 0.8, java.util.List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(java.util.List.of(), 0L, 4, 5),
                now);
        ThresholdPolicyConfig config = ThresholdPolicyConfig.defaultAdaptive();
        PolicyDecision decision = new PolicyDecision("r1", config.policyId(), now,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 4, 6, "test");
        AdjustmentDecision adjDecision = new AdjustmentDecision(
                beforeClass,
                new PolicyScore(config.policyId(), 0.8, 0.8, 0.8, 0.8, 0.8, "test"),
                config, decision, "test rationale", now);
        ExecutorStateSnapshot beforeState = ExecutorStateSnapshot.builder(now)
                .corePoolSize(4).maximumPoolSize(8).poolSize(4)
                .activeCount(4).queueSize(0).build();
        ExecutorStateSnapshot afterState = ExecutorStateSnapshot.builder(now)
                .corePoolSize(6).maximumPoolSize(8).poolSize(6)
                .activeCount(6).queueSize(0).build();
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "r1", now, 4, 6, "test", "ref", java.time.Instant::now);
        AdjustmentResult result = new AdjustmentResult(
                command, AdjustmentStatus.APPLIED,
                beforeState, 6, 6, afterState,
                "applied", null, "ref", now);
        history.record(adjDecision, result, beforeClass, afterClass);
        return history.recent(1).get(0);
    }

    @Test
    void shouldRecordAndRetrieveEntries() {
        createEntry(PressureState.OVERLOAD, PressureState.RECOVERY);
        createEntry(PressureState.RECOVERY, PressureState.NORMAL);
        assertEquals(2, history.totalAdjustmentCount());
        assertEquals(2, history.recent(5).size());
    }

    @Test
    void shouldCountSuccessfulAdjustments() {
        createEntry(PressureState.OVERLOAD, PressureState.RECOVERY);     // improvement
        createEntry(PressureState.QUEUE_BUILDUP, PressureState.NORMAL);   // improvement
        createEntry(PressureState.NORMAL, PressureState.QUEUE_BUILDUP);   // degradation
        assertEquals(2, history.successfulAdjustmentCount());
    }

    @Test
    void shouldCountNormalToNormalAsSuccess() {
        createEntry(PressureState.NORMAL, PressureState.NORMAL);
        assertEquals(1, history.successfulAdjustmentCount());
    }

    @Test
    void shouldNotCountDegradationAsSuccess() {
        createEntry(PressureState.NORMAL, PressureState.QUEUE_BUILDUP);
        assertEquals(0, history.successfulAdjustmentCount());
    }

    @Test
    void shouldReturnEmptyInitially() {
        assertTrue(history.isEmpty());
        assertEquals(0, history.totalAdjustmentCount());
    }

    @Test
    void shouldFilterByTime() {
        createEntry(PressureState.NORMAL, PressureState.NORMAL);
        var results = history.since(now.minusSeconds(3600));
        assertEquals(1, results.size());
        var empty = history.since(now.plusSeconds(3600));
        assertEquals(0, empty.size());
    }

    @Test
    void shouldClearAllEntries() {
        createEntry(PressureState.NORMAL, PressureState.NORMAL);
        history.clear();
        assertTrue(history.isEmpty());
        assertEquals(0, history.totalAdjustmentCount());
    }

    @Test
    void shouldBeThreadSafe() throws Exception {
        int threads = 4;
        int perThread = 25;
        var latch = new CountDownLatch(threads);
        var executor = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    createEntry(PressureState.NORMAL, PressureState.NORMAL);
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();
        assertEquals(threads * perThread, history.totalAdjustmentCount());
    }
}
