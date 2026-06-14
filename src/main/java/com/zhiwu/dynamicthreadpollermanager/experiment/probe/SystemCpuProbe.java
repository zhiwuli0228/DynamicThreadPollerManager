package com.zhiwu.dynamicthreadpollermanager.experiment.probe;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * CPU utilization data source via JDK {@link ManagementFactory}.
 * Zero external dependencies. Graceful degradation to 0.0 when unavailable.
 */
public final class SystemCpuProbe {

    private final OperatingSystemMXBean osBean;

    public SystemCpuProbe() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    /**
     * Process-level CPU utilization [0.0, 1.0].
     * Returns 0.0 if the Sun-management extension is unavailable
     * or the value has not been initialized yet.
     */
    public double sampleProcessCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            return load >= 0 ? load : 0.0;
        }
        return 0.0;
    }

    /**
     * System-level CPU load average (Unix-like).
     * Returns 0.0 if unavailable.
     */
    public double sampleSystemCpuLoad() {
        double load = osBean.getSystemLoadAverage();
        return load >= 0 ? load : 0.0;
    }
}
