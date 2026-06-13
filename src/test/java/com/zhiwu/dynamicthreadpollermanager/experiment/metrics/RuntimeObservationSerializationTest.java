package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeObservationSerializationTest {

    @Test
    void shouldRoundTripFullObservation() {
        RuntimeObservation original = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:00Z"),
                MetricValue.present(3),
                MetricValue.present(4),
                MetricValue.present(5),
                MetricValue.present(100L),
                MetricValue.present(0.5),
                MetricValue.present(60L),
                MetricValue.present(4),
                MetricValue.present(1000L));

        Map<String, Object> map = original.toMap();
        RuntimeObservation restored = RuntimeObservation.fromMap(map);

        assertEquals(original, restored);
    }

    @Test
    void shouldRoundTripObservationWithAbsentMetrics() {
        RuntimeObservation original = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:00Z"),
                MetricValue.present(3),
                MetricValue.absent(),
                MetricValue.present(5),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent());

        Map<String, Object> map = original.toMap();
        RuntimeObservation restored = RuntimeObservation.fromMap(map);

        assertEquals(original, restored);
    }

    @Test
    void shouldPreservePresentStatus() {
        RuntimeObservation obs = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:00Z"),
                MetricValue.present(3),
                MetricValue.present(4),
                MetricValue.present(5),
                MetricValue.present(100L),
                MetricValue.present(0.5),
                MetricValue.present(60L),
                MetricValue.present(4),
                MetricValue.present(1000L));

        Map<String, Object> map = obs.toMap();

        @SuppressWarnings("unchecked")
        Map<String, Object> activeThreads = (Map<String, Object>) map.get("activeThreads");
        assertEquals("PRESENT", activeThreads.get("status"));
        assertEquals(3, activeThreads.get("value"));
    }

    @Test
    void shouldPreserveAbsentStatus() {
        RuntimeObservation obs = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:00Z"),
                MetricValue.present(3),
                MetricValue.absent(),
                MetricValue.present(5),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent());

        Map<String, Object> map = obs.toMap();

        @SuppressWarnings("unchecked")
        Map<String, Object> poolSize = (Map<String, Object>) map.get("poolSize");
        assertEquals("ABSENT", poolSize.get("status"));
    }

    @Test
    void shouldBuildFromRealExecutor() {
        ManagedExecutor executor = ManagedExecutorConfig.defaultConfig().toManagedExecutor();
        try {
            executor.submit(() -> { /* task to make activeCount > 0 */ });
            Thread.sleep(50);

            RuntimeObservation obs = RuntimeObservation.fromExecutor(
                    executor, Instant.parse("2026-06-13T10:00:00Z"));

            assertEquals(Instant.parse("2026-06-13T10:00:00Z"), obs.timestamp());
            assertTrue(obs.activeThreads().isPresent());
            assertTrue(obs.poolSize().isPresent());
            assertTrue(obs.queueSize().isPresent());
            assertTrue(obs.completedTaskCount().isPresent());
            assertTrue(obs.cpuUtilization().isAbsent());
            assertTrue(obs.keepAliveTimeSeconds().isPresent());
            assertTrue(obs.largestPoolSize().isPresent());
            assertTrue(obs.taskCount().isPresent());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldHandleNumberTypeConversionInMetricValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", "2026-06-13T10:00:00Z");

        Map<String, Object> intMetric = new LinkedHashMap<>();
        intMetric.put("status", "PRESENT");
        intMetric.put("value", 3);  // Integer → Integer
        map.put("activeThreads", intMetric);

        Map<String, Object> longMetric = new LinkedHashMap<>();
        longMetric.put("status", "PRESENT");
        longMetric.put("value", 100L);  // Long → Long
        map.put("completedTaskCount", longMetric);

        Map<String, Object> absentMetric = new LinkedHashMap<>();
        absentMetric.put("status", "ABSENT");
        map.put("poolSize", absentMetric);

        map.put("queueSize", absentMetric);
        map.put("cpuUtilization", absentMetric);
        map.put("keepAliveTimeSeconds", absentMetric);
        map.put("largestPoolSize", absentMetric);
        map.put("taskCount", absentMetric);

        RuntimeObservation obs = RuntimeObservation.fromMap(map);

        assertEquals(3, obs.activeThreads().asOptional().get());
        assertEquals(100L, obs.completedTaskCount().asOptional().get());
        assertTrue(obs.poolSize().isAbsent());
    }

    @AfterEach
    void cleanup() {
        // ManagedExecutor is shut down in each test that uses it
    }
}
