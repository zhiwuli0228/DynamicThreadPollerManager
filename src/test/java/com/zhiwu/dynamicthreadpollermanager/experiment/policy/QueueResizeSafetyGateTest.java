package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.QueueResizeCommand;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate.EvaluationResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate.GateResult;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class QueueResizeSafetyGateTest {

    private QueueResizeSafetyGate gate;
    private ManagedExecutor executor;

    @BeforeEach
    void setUp() {
        gate = new QueueResizeSafetyGate();
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (!executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void permitValidExpand() {
        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand to 20");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.PERMIT, result.result());
        assertTrue(result.permitted());
    }

    @Test
    void permitValidShrink() {
        QueueResizeCommand cmd = new QueueResizeCommand(5, "shrink to 5");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.PERMIT, result.result());
    }

    @Test
    void denyNonRunningExecutor() {
        executor.shutdown();
        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.DENY, result.result());
        assertFalse(result.permitted());
        assertTrue(result.reason().contains("RUNNING"));
    }

    @Test
    void denyTerminatedExecutor() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.DENY, result.result());
        assertTrue(result.reason().contains("RUNNING"));
    }

    @Test
    void denySameCapacity() {
        QueueResizeCommand cmd = new QueueResizeCommand(10, "no change");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.DENY, result.result());
        assertTrue(result.reason().contains("equals current capacity"));
    }

    @Test
    void denyShrinkWhenQueueDepthExceedsNewCapacity() {
        // Fill queue with blocking tasks so queue depth > target capacity
        java.util.concurrent.CountDownLatch blocker = new java.util.concurrent.CountDownLatch(1);
        // core=2, max=4, so 4 threads will be busy; next tasks go to queue
        for (int i = 0; i < 7; i++) {
            executor.submit(() -> {
                try { blocker.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        // Wait for tasks to be queued
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        int queueDepth = executor.getQueueSize();
        assertTrue(queueDepth > 0, "precondition: queue must have items");

        // Target capacity is less than current queue depth
        QueueResizeCommand cmd = new QueueResizeCommand(1, "shrink below depth");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.DENY, result.result());
        assertTrue(result.reason().contains("current queue depth"));

        blocker.countDown(); // unblock tasks for cleanup
    }

    @Test
    void permitShrinkWhenQueueDepthWithinNewCapacity() {
        // Queue is empty, shrink is fine
        QueueResizeCommand cmd = new QueueResizeCommand(3, "shrink to 3");
        EvaluationResult result = gate.evaluate(cmd, executor);
        assertEquals(GateResult.PERMIT, result.result());
    }
}
