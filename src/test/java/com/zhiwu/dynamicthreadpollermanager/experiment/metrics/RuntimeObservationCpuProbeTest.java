package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.probe.SystemCpuProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeObservationCpuProbeTest {

    private ManagedExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.close();
        }
    }

    @Test
    void shouldStillWorkWithTwoArgSignature() {
        executor = ManagedExecutorConfig.defaultConfig().toManagedExecutor();
        RuntimeObservation obs = RuntimeObservation.fromExecutor(
                executor, Instant.now());
        assertNotNull(obs);
        // 2-arg signature remains backward-compatible: cpuUtilization is absent
        assertFalse(obs.cpuUtilization().isPresent());
    }

    @Test
    void shouldUseProbeFromThreeArgOverload() {
        executor = ManagedExecutorConfig.defaultConfig().toManagedExecutor();
        SystemCpuProbe probe = new SystemCpuProbe();
        RuntimeObservation obs = RuntimeObservation.fromExecutor(
                executor, Instant.now(), probe);
        assertNotNull(obs);
        assertNotNull(obs.cpuUtilization());
    }

    @Test
    void shouldSetCpuAbsentWhenProbeIsNull() {
        executor = ManagedExecutorConfig.defaultConfig().toManagedExecutor();
        RuntimeObservation obs = RuntimeObservation.fromExecutor(
                executor, Instant.now(), null);
        assertFalse(obs.cpuUtilization().isPresent());
    }
}
