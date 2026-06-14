package com.zhiwu.dynamicthreadpollermanager.experiment.probe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemCpuProbeTest {

    @Test
    void shouldReturnNonNegativeProcessCpuLoad() {
        SystemCpuProbe probe = new SystemCpuProbe();
        double load = probe.sampleProcessCpuLoad();
        assertTrue(load >= 0.0, "process CPU load should be >= 0, was " + load);
    }

    @Test
    void shouldReturnNonNegativeSystemCpuLoad() {
        SystemCpuProbe probe = new SystemCpuProbe();
        double load = probe.sampleSystemCpuLoad();
        // system load average can be -1.0 on Windows or unavailable platforms
        assertTrue(load >= -1.0, "system load should be >= -1, was " + load);
    }

    @Test
    void shouldNotThrowOnRepeatedCalls() {
        SystemCpuProbe probe = new SystemCpuProbe();
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(probe::sampleProcessCpuLoad);
            assertDoesNotThrow(probe::sampleSystemCpuLoad);
        }
    }
}
