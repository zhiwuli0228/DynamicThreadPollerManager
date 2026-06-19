package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.RejectionPolicySafetyGate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class RejectionPolicyEndToEndTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private QueueResizeSafetyGate queueResizeSafetyGate;
    private ExecutorRebuildStrategy rebuildStrategy;
    private QueueResizeAdjustmentAdapter queueResizeAdapter;
    private RejectionPolicySafetyGate policySafetyGate;
    private RejectionPolicyAdjustmentAdapter policyAdapter;
    private Supplier<Instant> clock;
    private CountDownLatch taskBlocker;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        clock = Instant::now;
        queueResizeSafetyGate = new QueueResizeSafetyGate();
        rebuildStrategy = new ExecutorRebuildStrategy(registry, clock);
        queueResizeAdapter = new QueueResizeAdjustmentAdapter(
                registry, queueResizeSafetyGate, rebuildStrategy);
        policySafetyGate = new RejectionPolicySafetyGate(
                queueResizeAdapter::isResizeInProgress);
        policyAdapter = new RejectionPolicyAdjustmentAdapter(registry, policySafetyGate);
        taskBlocker = new CountDownLatch(1);
    }

    @AfterEach
    void tearDown() {
        taskBlocker.countDown();
        for (String name : java.util.List.copyOf(registry.list())) {
            registry.get(name).ifPresent(executor -> {
                if (!executor.isTerminated()) {
                    executor.shutdownNow();
                    try {
                        executor.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            registry.remove(name);
        }
    }

    private void fillThreadAndQueue(ManagedExecutor executor) {
        executor.submit(() -> {
            try {
                taskBlocker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
    }

    // --- Scenario 1: AbortPolicy → CallerRunsPolicy ---

    @Test
    void switchAbortToCallerRunsAndVerifyOverloadBehavior() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.register("e2e-exec", executor);

        // Fill thread (1) + queue (2) = 3 tasks
        fillThreadAndQueue(executor);
        fillThreadAndQueue(executor);
        fillThreadAndQueue(executor);

        // AbortPolicy: overload throws
        assertThrows(RejectedExecutionException.class,
                () -> executor.submit(() -> {}));

        // Switch to CallerRunsPolicy
        RejectedExecutionHandler callerRuns = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(callerRuns, "switch to caller runs");
        PolicyReplacementResult result = policyAdapter.apply("e2e-exec", cmd);
        assertTrue(result.success(), "policy switch failed: " + result.failureCode());

        // Release blocking tasks
        taskBlocker.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Create fresh executor with CallerRunsPolicy for phase 2
        CountDownLatch blocker2 = new CountDownLatch(1);
        ManagedExecutor executor2 = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.remove("e2e-exec");
        registry.register("e2e-exec-2", executor2);

        // Fill with blocking tasks
        executor2.submit(() -> {
            try {
                blocker2.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        executor2.submit(() -> {
            try {
                blocker2.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        executor2.submit(() -> {
            try {
                blocker2.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        // Switch to CallerRunsPolicy on executor2
        policyAdapter.apply("e2e-exec-2", new RejectionPolicyCommand(callerRuns, "switch"));

        // CallerRunsPolicy: overload runs in caller thread (no exception)
        AtomicBoolean callerRan = new AtomicBoolean(false);
        assertDoesNotThrow(() -> executor2.submit(() -> callerRan.set(true)));

        blocker2.countDown();
        executor2.shutdown();
        executor2.awaitTermination(10, TimeUnit.SECONDS);
        registry.remove("e2e-exec-2");

        assertTrue(callerRan.get(), "CallerRunsPolicy should have executed the task in caller thread");
    }

    // --- Scenario 2: AbortPolicy → DiscardPolicy ---

    @Test
    void switchAbortToDiscardAndVerifySilentDiscard() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.register("e2e-exec", executor);

        // Fill thread + queue
        fillThreadAndQueue(executor);
        fillThreadAndQueue(executor);
        fillThreadAndQueue(executor);

        // AbortPolicy: overload throws
        assertThrows(RejectedExecutionException.class,
                () -> executor.submit(() -> {}));

        // Switch to DiscardPolicy
        RejectedExecutionHandler discard = new ThreadPoolExecutor.DiscardPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(discard, "switch to discard");
        PolicyReplacementResult result = policyAdapter.apply("e2e-exec", cmd);
        assertTrue(result.success());

        long taskCountBefore = executor.getTaskCount();

        // Overload with DiscardPolicy → silent discard, no exception
        assertDoesNotThrow(() -> executor.submit(() -> {}));
        Thread.sleep(100);

        assertEquals(taskCountBefore, executor.getTaskCount(),
                "DiscardPolicy should silently discard overload task without incrementing taskCount");

        taskBlocker.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    // --- Scenario 3: DiscardOldestPolicy eviction ---

    @Test
    void switchToDiscardOldestAndVerifyEviction() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.register("e2e-exec", executor);

        // Switch to DiscardOldestPolicy
        RejectedExecutionHandler discardOldest = new ThreadPoolExecutor.DiscardOldestPolicy();
        policyAdapter.apply("e2e-exec",
                new RejectionPolicyCommand(discardOldest, "switch to discard oldest"));

        AtomicBoolean taskAExecuted = new AtomicBoolean(false);
        AtomicBoolean taskBExecuted = new AtomicBoolean(false);
        AtomicBoolean taskCExecuted = new AtomicBoolean(false);

        // Fill the single thread with a blocking task
        executor.submit(() -> {
            try {
                taskBlocker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        // Fill queue with Task-A and Task-B
        executor.submit(() -> { taskAExecuted.set(true); return null; });
        executor.submit(() -> { taskBExecuted.set(true); return null; });

        // Task-C triggers DiscardOldestPolicy → Task-A evicted
        assertDoesNotThrow(() -> executor.submit(() -> { taskCExecuted.set(true); return null; }));

        // Release the blocking task, allowing queue to drain
        taskBlocker.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(taskCExecuted.get(), "Task-C should have been enqueued and executed");
        assertTrue(taskBExecuted.get(), "Task-B should have been executed");
        assertFalse(taskAExecuted.get(), "Task-A should have been evicted by DiscardOldestPolicy");
    }

    // --- Scenario 4: Safety gate DENY on shutdown executor ---

    @Test
    void safetyGateDenyOnShutdownExecutor() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.register("e2e-exec", executor);

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        RejectionPolicyCommand cmd = new RejectionPolicyCommand(
                new ThreadPoolExecutor.CallerRunsPolicy(), "switch");
        PolicyReplacementResult result = policyAdapter.apply("e2e-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
        assertNotNull(result.evidence());
        assertFalse(result.evidence().success());
    }

    // --- Scenario 5: Safety gate DENY on same policy type ---

    @Test
    void safetyGateDenyOnSamePolicyType() {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.register("e2e-exec", executor);

        // Default is AbortPolicy, target with another AbortPolicy
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(
                new ThreadPoolExecutor.AbortPolicy(), "no-op");
        PolicyReplacementResult result = policyAdapter.apply("e2e-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
    }

    // --- Scenario 6: Rebuild preserves rejection policy ---

    @Test
    void rebuildPreservesRejectionPolicy() {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5),
                java.util.concurrent.Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        registry.register("e2e-exec", executor);

        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                executor.getRejectionPolicy());

        QueueResizeCommand resizeCmd = new QueueResizeCommand(10, "expand", 10_000L);
        QueueResizeResult resizeResult = queueResizeAdapter.apply("e2e-exec", resizeCmd);

        assertTrue(resizeResult.success(), "resize failed: " + resizeResult.errorMessage());

        ManagedExecutor newExecutor = registry.get("e2e-exec").orElseThrow();
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                newExecutor.getRejectionPolicy(),
                "rejection policy should be preserved after rebuild");
    }

    // --- Scenario 7: Policy replacement evidence complete ---

    @Test
    void policyReplacementEvidenceComplete() {
        ManagedExecutor executor = new ManagedExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2));
        registry.register("e2e-exec", executor);

        RejectionPolicyCommand cmd = new RejectionPolicyCommand(
                new ThreadPoolExecutor.CallerRunsPolicy(), "switch to caller runs");
        PolicyReplacementResult result = policyAdapter.apply("e2e-exec", cmd);

        assertTrue(result.success());
        PolicyReplacementEvidence evidence = result.evidence();
        assertNotNull(evidence);

        assertTrue(evidence.beforePolicyClass().endsWith("AbortPolicy"),
                "beforePolicyClass should end with AbortPolicy: " + evidence.beforePolicyClass());
        assertTrue(evidence.afterPolicyClass().endsWith("CallerRunsPolicy"),
                "afterPolicyClass should end with CallerRunsPolicy: " + evidence.afterPolicyClass());
        assertTrue(evidence.success());
        assertNotNull(evidence.replacedAt());
        assertNotNull(evidence.executorState());
        assertEquals("switch to caller runs", evidence.reason());
    }

    // --- Scenario 8: Executor not found ---

    @Test
    void executorNotFoundReturnsFailure() {
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(
                new ThreadPoolExecutor.CallerRunsPolicy(), "switch");
        PolicyReplacementResult result = policyAdapter.apply("nonexistent", cmd);

        assertFalse(result.success());
        assertEquals("EXECUTOR_NOT_FOUND", result.failureCode());
        assertNull(result.evidence());
    }
}
