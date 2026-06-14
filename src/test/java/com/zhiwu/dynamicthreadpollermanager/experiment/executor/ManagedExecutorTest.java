package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorTest {

    private ManagedExecutor executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null && !executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldConstructWithDefaults() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaximumPoolSize());
        assertEquals(60, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertEquals(10, executor.getQueueCapacity());
        assertNotNull(executor.getRejectionPolicy());
    }

    @Test
    void shouldRejectInvalidCorePoolSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutor(0, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10)));
    }

    @Test
    void shouldRejectMaxPoolSizeLessThanCore() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutor(4, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10)));
    }

    @Test
    void shouldRejectNegativeKeepAlive() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutor(2, 4, -1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10)));
    }

    @Test
    void setCorePoolSizeShouldReflectImmediately() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.setMaximumPoolSize(8);
        executor.setCorePoolSize(3);
        assertEquals(3, executor.getCorePoolSize());
    }

    @Test
    void setMaximumPoolSizeShouldReflectImmediately() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.setMaximumPoolSize(12);
        assertEquals(12, executor.getMaximumPoolSize());
    }

    @Test
    void setKeepAliveTimeShouldReflectImmediately() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.setKeepAliveTime(120, TimeUnit.SECONDS);
        assertEquals(120, executor.getKeepAliveTime(TimeUnit.SECONDS));
    }

    @Test
    void submitCallableShouldReturnResult() throws Exception {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        Callable<Integer> task = () -> 42;
        assertEquals(42, executor.submit(task).get());
    }

    @Test
    void submitRunnableShouldExecute() throws Exception {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        AtomicBoolean executed = new AtomicBoolean(false);
        executor.submit(() -> executed.set(true)).get();
        assertTrue(executed.get());
    }

    @Test
    void readOnlyStateShouldReflectThreadPoolExecutor() throws Exception {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        executor.submit(() -> { /* no-op */ }).get();

        // TPE state is approximate — allow brief settling time
        for (int i = 0; i < 20 && executor.getActiveCount() > 0; i++) {
            Thread.sleep(5);
        }
        assertEquals(0, executor.getActiveCount());
        assertTrue(executor.getPoolSize() >= 0);
        assertEquals(0, executor.getQueueSize());
        assertTrue(executor.getCompletedTaskCount() >= 1);
        assertTrue(executor.getLargestPoolSize() >= 0);
        assertTrue(executor.getTaskCount() >= 1);
    }

    @Test
    void shutdownShouldStopAcceptingTasks() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    void shutdownNowShouldStopImmediately() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.shutdownNow();
        assertTrue(executor.isStopped() || executor.isTerminated());
    }

    @Test
    void awaitTerminationShouldSucceedAfterShutdown() throws Exception {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
    }

    @Test
    void unwrapShouldReturnUnderlyingThreadPoolExecutor() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        assertNotNull(executor.unwrap());
        assertEquals(2, executor.unwrap().getCorePoolSize());
    }

    @Test
    void closeShouldDelegateToShutdown() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.close();
        assertTrue(executor.isShutdown());
    }

    @Test
    void setRejectionPolicyShouldReflectImmediately() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        assertInstanceOf(ThreadPoolExecutor.AbortPolicy.class,
                executor.getRejectionPolicy());

        executor.setRejectionPolicy(new ThreadPoolExecutor.CallerRunsPolicy());
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                executor.getRejectionPolicy());
    }

    @Test
    void setRejectionPolicyNullThrows() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        assertThrows(NullPointerException.class,
                () -> executor.setRejectionPolicy(null));
    }

    @Test
    void getRejectionPolicyDelegatesToTpe() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        assertInstanceOf(ThreadPoolExecutor.AbortPolicy.class,
                executor.getRejectionPolicy());
    }

    @Test
    void setRejectionPolicyPropagatesToUnderlyingTpe() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        executor.setRejectionPolicy(new ThreadPoolExecutor.DiscardPolicy());
        assertInstanceOf(ThreadPoolExecutor.DiscardPolicy.class,
                executor.getRejectionPolicy());
    }

    @Test
    void toSnapshotShouldContainAllFields() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        ExecutorStateSnapshot snapshot = executor.toSnapshot();

        assertEquals(2, snapshot.corePoolSize());
        assertEquals(4, snapshot.maximumPoolSize());
        assertNotNull(snapshot.activeCount());
        assertNotNull(snapshot.poolSize());
        assertNotNull(snapshot.queueSize());
        assertNotNull(snapshot.queueCapacity());
        assertEquals(10, snapshot.queueCapacity());
        assertNotNull(snapshot.completedTaskCount());
        assertNotNull(snapshot.keepAliveTimeSeconds());
        assertNotNull(snapshot.largestPoolSize());
        assertNotNull(snapshot.taskCount());
    }

    @Test
    void shouldReturnPlatformThreadModeByDefault() {
        executor = new ManagedExecutor(2, 4, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        assertEquals(ThreadMode.PLATFORM, executor.getThreadMode());
    }
}
