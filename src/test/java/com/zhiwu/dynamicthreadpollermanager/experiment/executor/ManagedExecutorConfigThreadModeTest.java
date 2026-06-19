package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorConfigThreadModeTest {

    @Test
    void shouldDefaultToPlatform() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        assertEquals(ThreadMode.PLATFORM, config.threadMode());
    }

    @Test
    void shouldCreateWithVirtualThreadMode() {
        ManagedExecutorConfig config = new ManagedExecutorConfig(
                2, 4, 10, 60, TimeUnit.SECONDS, ThreadMode.VIRTUAL);
        assertEquals(ThreadMode.VIRTUAL, config.threadMode());
    }

    @Test
    void shouldCreateVirtualThreadExecutor() throws Exception {
        ManagedExecutorConfig config = new ManagedExecutorConfig(
                2, 4, 10, 60, TimeUnit.SECONDS, ThreadMode.VIRTUAL);
        ManagedExecutor executor = config.toManagedExecutor();
        try {
            assertEquals(ThreadMode.VIRTUAL, executor.getThreadMode());
            assertTrue(executor.getCorePoolSize() >= 1);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldExecuteTaskOnVirtualThread() throws Exception {
        ManagedExecutorConfig config = new ManagedExecutorConfig(
                2, 4, 10, 60, TimeUnit.SECONDS, ThreadMode.VIRTUAL);
        ManagedExecutor executor = config.toManagedExecutor();
        try {
            Callable<Boolean> task = () -> Thread.currentThread().isVirtual();
            assertTrue(executor.submit(task).get(), "task should run on a virtual thread");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldRejectNullThreadMode() {
        assertThrows(NullPointerException.class, () ->
                new ManagedExecutorConfig(2, 4, 10, 60, TimeUnit.SECONDS, null));
    }

    @Test
    void shouldRejectVirtualThreadFactoryInPlatformConstructor() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutor(2, 4, 60,
                        TimeUnit.SECONDS, new LinkedBlockingQueue<>(10),
                        Thread.ofVirtual().factory(),
                        new java.util.concurrent.ThreadPoolExecutor.AbortPolicy()));
    }

    @Test
    void shouldDetectPlatformModeFromDirectConstructor() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        try {
            assertEquals(ThreadMode.PLATFORM, executor.getThreadMode());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void fiveArgConfigShouldDefaultToPlatform() {
        ManagedExecutorConfig config = new ManagedExecutorConfig(
                2, 4, 10, 60, TimeUnit.SECONDS);
        assertEquals(ThreadMode.PLATFORM, config.threadMode());
    }
}
