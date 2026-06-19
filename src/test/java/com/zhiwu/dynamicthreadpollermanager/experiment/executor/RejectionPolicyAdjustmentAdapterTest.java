package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.RejectionPolicySafetyGate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RejectionPolicyAdjustmentAdapterTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private RejectionPolicySafetyGate safetyGate;
    private RejectionPolicyAdjustmentAdapter adapter;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        safetyGate = new RejectionPolicySafetyGate(id -> false);
        adapter = new RejectionPolicyAdjustmentAdapter(registry, safetyGate);
    }

    @AfterEach
    void tearDown() {
        for (String name : List.copyOf(registry.list())) {
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

    @Test
    void successfulPolicyReplacement() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch to caller runs");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        assertNotNull(result.evidence());
        assertTrue(result.evidence().success());
        assertEquals("CallerRunsPolicy",
                shortClassName(result.evidence().afterPolicyClass()));
        assertEquals("switch to caller runs", result.evidence().reason());

        ManagedExecutor updated = registry.get("test-exec").orElseThrow();
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                updated.getRejectionPolicy());
    }

    @Test
    void switchToDiscardPolicy() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.DiscardPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch to discard");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        ManagedExecutor updated = registry.get("test-exec").orElseThrow();
        assertInstanceOf(ThreadPoolExecutor.DiscardPolicy.class,
                updated.getRejectionPolicy());
    }

    @Test
    void switchToDiscardOldestPolicy() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.DiscardOldestPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch to discard oldest");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        ManagedExecutor updated = registry.get("test-exec").orElseThrow();
        assertInstanceOf(ThreadPoolExecutor.DiscardOldestPolicy.class,
                updated.getRejectionPolicy());
    }

    @Test
    void executorNotFound() {
        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch");
        PolicyReplacementResult result = adapter.apply("nonexistent", cmd);

        assertFalse(result.success());
        assertEquals("EXECUTOR_NOT_FOUND", result.failureCode());
        assertNull(result.evidence());
    }

    @Test
    void safetyGateDeniedForNonRunning() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);
        executor.shutdown();

        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
        assertNotNull(result.evidence());
        assertFalse(result.evidence().success());
    }

    @Test
    void safetyGateDeniedForSamePolicy() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.AbortPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "no-op");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
    }

    @Test
    void safetyGateDeniedWhenResizeInProgress() {
        RejectionPolicySafetyGate gateWithResize = new RejectionPolicySafetyGate(id -> true);
        RejectionPolicyAdjustmentAdapter adapterWithResizeGate =
                new RejectionPolicyAdjustmentAdapter(registry, gateWithResize);

        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch");
        PolicyReplacementResult result = adapterWithResizeGate.apply("test-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
    }

    @Test
    void evidenceContainsBeforeAfterPolicyClasses() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.DiscardPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        PolicyReplacementEvidence evidence = result.evidence();
        assertNotNull(evidence.beforePolicyClass());
        assertNotNull(evidence.afterPolicyClass());
        assertNotNull(evidence.executorState());
        assertNotNull(evidence.replacedAt());
    }

    @Test
    void nullExecutorIdThrows() {
        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch");
        assertThrows(NullPointerException.class,
                () -> adapter.apply(null, cmd));
    }

    @Test
    void nullCommandThrows() {
        assertThrows(NullPointerException.class,
                () -> adapter.apply("test-exec", null));
    }

    @Test
    void policySetFailureReturnsFailedResult() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10)) {
            @Override
            public void setRejectionPolicy(RejectedExecutionHandler newPolicy) {
                Objects.requireNonNull(newPolicy, "rejectionPolicy must not be null");
                throw new RuntimeException("simulated TPE failure");
            }
        };
        registry.register("test-exec", executor);

        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(target, "switch");
        PolicyReplacementResult result = adapter.apply("test-exec", cmd);

        assertFalse(result.success());
        assertEquals("POLICY_SET_FAILED", result.failureCode());
        assertNotNull(result.evidence());
        assertFalse(result.evidence().success());
        assertTrue(result.evidence().reason().contains("simulated TPE failure"),
                "reason should contain the exception message: " + result.evidence().reason());
    }

    private static String shortClassName(String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }
}
