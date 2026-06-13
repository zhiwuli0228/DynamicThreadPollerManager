package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.acquisition.RunManifest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorConfigTest {

    @Test
    void shouldCreateWithValidParameters() {
        ManagedExecutorConfig config = new ManagedExecutorConfig(2, 4, 10, 60, TimeUnit.SECONDS);
        assertEquals(2, config.corePoolSize());
        assertEquals(4, config.maximumPoolSize());
        assertEquals(10, config.queueCapacity());
        assertEquals(60, config.keepAliveTime());
        assertEquals(TimeUnit.SECONDS, config.keepAliveTimeUnit());
    }

    @Test
    void shouldRejectZeroCorePoolSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutorConfig(0, 4, 10, 60, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectNegativeCorePoolSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutorConfig(-1, 4, 10, 60, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectMaxLessThanCore() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutorConfig(4, 2, 10, 60, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectNegativeQueueCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutorConfig(2, 4, -1, 60, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectNegativeKeepAliveTime() {
        assertThrows(IllegalArgumentException.class, () ->
                new ManagedExecutorConfig(2, 4, 10, -1, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectNullTimeUnit() {
        assertThrows(NullPointerException.class, () ->
                new ManagedExecutorConfig(2, 4, 10, 60, null));
    }

    @Test
    void defaultConfigShouldReturnStandardValues() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        assertEquals(2, config.corePoolSize());
        assertEquals(4, config.maximumPoolSize());
        assertEquals(10, config.queueCapacity());
        assertEquals(60, config.keepAliveTime());
        assertEquals(TimeUnit.SECONDS, config.keepAliveTimeUnit());
    }

    @Test
    void toManagedExecutorShouldCreateFunctionalExecutor() throws Exception {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        ManagedExecutor executor = config.toManagedExecutor();
        try {
            var future = executor.submit((Callable<Integer>) () -> 42);
            assertEquals(42, future.get());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void toPresetSummaryShouldMapFieldsCorrectly() {
        ManagedExecutorConfig config = new ManagedExecutorConfig(3, 6, 15, 30, TimeUnit.SECONDS);
        RunManifest.BaselinePresetSummary summary = config.toPresetSummary();

        assertEquals("managed-executor-v0.8.0", summary.policyId());
        assertEquals(3, summary.corePoolSize());
        assertEquals(6, summary.maximumPoolSize());
        assertEquals(15, summary.queueCapacity());
    }

    @Test
    void recordShouldSupportEquality() {
        var a = new ManagedExecutorConfig(2, 4, 10, 60, TimeUnit.SECONDS);
        var b = new ManagedExecutorConfig(2, 4, 10, 60, TimeUnit.SECONDS);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void recordShouldSupportToString() {
        var config = ManagedExecutorConfig.defaultConfig();
        String str = config.toString();
        assertTrue(str.contains("corePoolSize=2"));
        assertTrue(str.contains("queueCapacity=10"));
    }

    @Test
    void shouldDefaultConfigUsePlatform() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        assertEquals(ThreadMode.PLATFORM, config.threadMode());
    }

    @Test
    void shouldCreateConfigWithThreadMode() {
        ManagedExecutorConfig config = new ManagedExecutorConfig(
                2, 4, 10, 60, TimeUnit.SECONDS, ThreadMode.VIRTUAL);
        assertEquals(ThreadMode.VIRTUAL, config.threadMode());
    }

    @Test
    void shouldRejectNullThreadMode() {
        assertThrows(NullPointerException.class, () ->
                new ManagedExecutorConfig(2, 4, 10, 60, TimeUnit.SECONDS, null));
    }
}
