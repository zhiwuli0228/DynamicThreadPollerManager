package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SafetyGateConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");
    private static final Supplier<Instant> CLOCK = () -> T0;

    private static ReadinessAssessment ready() {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ExecutorStateSnapshot snapshot() {
        return ExecutorStateSnapshot.builder(T0)
                .corePoolSize(2).maximumPoolSize(4)
                .activeCount(1).poolSize(2)
                .queueSize(0).queueCapacity(10)
                .completedTaskCount(0)
                .keepAliveTimeSeconds(60)
                .largestPoolSize(2).taskCount(0)
                .build();
    }

    @Test
    void concurrentEvaluateAndRecordShouldNotExceedPerRunLimit() throws Exception {
        SafetyGateConfig config = new SafetyGateConfig(
                0,     // cooldownDecisionIntervals
                5,     // maxAdjustmentsPerRun
                false, // blockImmediateOppositeDirection
                true   // allowReadyWithRisk
        );
        DefaultRuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate(config);

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger allowedCount = new AtomicInteger(0);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 3; i++) {
                        int target = 3 + ((threadId + i) % 8);
                        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                                "run-1", T0, 2, target, "test", "ref-" + threadId + "-" + i, CLOCK);
                        SafetyGateDecision decision = gate.evaluate(cmd, snapshot(), ready());
                        if (decision.outcome() == SafetyGateDecision.Outcome.ALLOW) {
                            gate.recordApplied(decision);
                            allowedCount.incrementAndGet();
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();

        assertTrue(errors.isEmpty(),
                "Expected no errors but got: " + errors.stream().map(Throwable::getMessage).toList());
        int allowed = allowedCount.get();
        assertTrue(allowed >= 1 && allowed <= config.maxAdjustmentsPerRun(),
                "allowed " + allowed + " should be in [1, " + config.maxAdjustmentsPerRun() + "]");
    }

    @Test
    void concurrentEvaluateIsDeterministicUnderContention() throws Exception {
        SafetyGateConfig config = new SafetyGateConfig(0, 3, false, true);
        DefaultRuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate(config);

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger allowedCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                            "run-1", T0, 2, 5, "test", "ref-" + threadId, CLOCK);
                    SafetyGateDecision decision = gate.evaluate(cmd, snapshot(), ready());
                    if (decision.outcome() == SafetyGateDecision.Outcome.ALLOW) {
                        gate.recordApplied(decision);
                        allowedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();

        int allowed = allowedCount.get();
        int gateApplied = gate.appliedAdjustmentsForRun();
        assertTrue(allowed >= 1 && allowed <= 3 && gateApplied >= 1 && gateApplied <= 3,
                "max 3 allowed under contention, got allowed=" + allowed + " gate=" + gateApplied);
    }
}
