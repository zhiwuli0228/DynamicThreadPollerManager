package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of thread pool pressure metrics at a point in time.
 */
public final class PressureSnapshot {

    private final Instant timestamp;
    private final int activeThreads;
    private final int poolSize;
    private final int queueSize;
    private final long completedTaskCount;
    private final double cpuUtilization;

    public PressureSnapshot(Instant timestamp, int activeThreads, int queueSize, double cpuUtilization) {
        this(timestamp, activeThreads, 0, queueSize, 0L, cpuUtilization);
    }

    public PressureSnapshot(Instant timestamp, int activeThreads, int poolSize,
                            int queueSize, long completedTaskCount, double cpuUtilization) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.activeThreads = activeThreads;
        this.poolSize = poolSize;
        this.queueSize = queueSize;
        this.completedTaskCount = completedTaskCount;
        this.cpuUtilization = cpuUtilization;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public int activeThreads() {
        return activeThreads;
    }

    public int poolSize() {
        return poolSize;
    }

    public int queueSize() {
        return queueSize;
    }

    public long completedTaskCount() {
        return completedTaskCount;
    }

    public double cpuUtilization() {
        return cpuUtilization;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp.toString());
        map.put("activeThreads", activeThreads);
        map.put("poolSize", poolSize);
        map.put("queueSize", queueSize);
        map.put("completedTaskCount", completedTaskCount);
        map.put("cpuUtilization", cpuUtilization);
        return map;
    }

    public static PressureSnapshot fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        Object timestampObj = map.get("timestamp");
        if (!(timestampObj instanceof String ts)) {
            throw new IllegalArgumentException(
                    "map must contain String 'timestamp', got "
                            + (timestampObj == null ? "null" : timestampObj.getClass().getSimpleName()));
        }
        Instant timestamp = Instant.parse(ts);
        int activeThreads = requireNumber(map, "activeThreads").intValue();
        int poolSize = requireNumber(map, "poolSize").intValue();
        int queueSize = requireNumber(map, "queueSize").intValue();
        long completedTaskCount = requireNumber(map, "completedTaskCount").longValue();
        double cpuUtilization = requireNumber(map, "cpuUtilization").doubleValue();
        return new PressureSnapshot(timestamp, activeThreads, poolSize,
                queueSize, completedTaskCount, cpuUtilization);
    }

    private static Number requireNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number num)) {
            throw new IllegalArgumentException(
                    "map must contain Number '" + key + "', got "
                            + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        return num;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof PressureSnapshot that
                && timestamp.equals(that.timestamp)
                && activeThreads == that.activeThreads
                && poolSize == that.poolSize
                && queueSize == that.queueSize
                && completedTaskCount == that.completedTaskCount
                && Double.compare(that.cpuUtilization, cpuUtilization) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization);
    }

    @Override
    public String toString() {
        return "PressureSnapshot{timestamp=%s, activeThreads=%d, poolSize=%d, queueSize=%d, completedTaskCount=%d, cpuUtilization=%.2f}"
                .formatted(timestamp, activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization);
    }
}
