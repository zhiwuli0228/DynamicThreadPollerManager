package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Raw observation input supplied to the snapshot assembler. Each metric is
 * represented as a {@link MetricValue} so unavailable values can be carried
 * through to the recorded snapshot instead of being lost.
 */
public final class RuntimeObservation {

    private final Instant timestamp;
    private final MetricValue<Integer> activeThreads;
    private final MetricValue<Integer> poolSize;
    private final MetricValue<Integer> queueSize;
    private final MetricValue<Long> completedTaskCount;
    private final MetricValue<Double> cpuUtilization;
    private final MetricValue<Long> keepAliveTimeSeconds;
    private final MetricValue<Integer> largestPoolSize;
    private final MetricValue<Long> taskCount;

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> queueSize,
                              MetricValue<Double> cpuUtilization) {
        this(timestamp, activeThreads, MetricValue.absent(), queueSize,
                MetricValue.absent(), cpuUtilization);
    }

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> poolSize,
                              MetricValue<Integer> queueSize,
                              MetricValue<Long> completedTaskCount,
                              MetricValue<Double> cpuUtilization) {
        this(timestamp, activeThreads, poolSize, queueSize, completedTaskCount,
                cpuUtilization, MetricValue.absent(), MetricValue.absent(),
                MetricValue.absent());
    }

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> poolSize,
                              MetricValue<Integer> queueSize,
                              MetricValue<Long> completedTaskCount,
                              MetricValue<Double> cpuUtilization,
                              MetricValue<Long> keepAliveTimeSeconds,
                              MetricValue<Integer> largestPoolSize,
                              MetricValue<Long> taskCount) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.activeThreads = Objects.requireNonNull(activeThreads, "activeThreads must not be null");
        this.poolSize = Objects.requireNonNull(poolSize, "poolSize must not be null");
        this.queueSize = Objects.requireNonNull(queueSize, "queueSize must not be null");
        this.completedTaskCount = Objects.requireNonNull(completedTaskCount, "completedTaskCount must not be null");
        this.cpuUtilization = Objects.requireNonNull(cpuUtilization, "cpuUtilization must not be null");
        this.keepAliveTimeSeconds = Objects.requireNonNull(keepAliveTimeSeconds, "keepAliveTimeSeconds must not be null");
        this.largestPoolSize = Objects.requireNonNull(largestPoolSize, "largestPoolSize must not be null");
        this.taskCount = Objects.requireNonNull(taskCount, "taskCount must not be null");
    }

    public Instant timestamp() { return timestamp; }
    public MetricValue<Integer> activeThreads() { return activeThreads; }
    public MetricValue<Integer> poolSize() { return poolSize; }
    public MetricValue<Integer> queueSize() { return queueSize; }
    public MetricValue<Long> completedTaskCount() { return completedTaskCount; }
    public MetricValue<Double> cpuUtilization() { return cpuUtilization; }
    public MetricValue<Long> keepAliveTimeSeconds() { return keepAliveTimeSeconds; }
    public MetricValue<Integer> largestPoolSize() { return largestPoolSize; }
    public MetricValue<Long> taskCount() { return taskCount; }

    public RuntimeObservation withTimestamp(Instant newTimestamp) {
        return new RuntimeObservation(newTimestamp, activeThreads, poolSize, queueSize,
                completedTaskCount, cpuUtilization, keepAliveTimeSeconds,
                largestPoolSize, taskCount);
    }

    public static RuntimeObservation fromExecutor(ManagedExecutor executor, Instant timestamp) {
        return new RuntimeObservation(
                timestamp,
                MetricValue.present(executor.getActiveCount()),
                MetricValue.present(executor.getPoolSize()),
                MetricValue.present(executor.getQueueSize()),
                MetricValue.present(executor.getCompletedTaskCount()),
                MetricValue.absent(),
                MetricValue.present(executor.getKeepAliveTime(TimeUnit.SECONDS)),
                MetricValue.present(executor.getLargestPoolSize()),
                MetricValue.present(executor.getTaskCount()));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp.toString());
        map.put("activeThreads", metricValueToMap(activeThreads));
        map.put("poolSize", metricValueToMap(poolSize));
        map.put("queueSize", metricValueToMap(queueSize));
        map.put("completedTaskCount", metricValueToMap(completedTaskCount));
        map.put("cpuUtilization", metricValueToMap(cpuUtilization));
        map.put("keepAliveTimeSeconds", metricValueToMap(keepAliveTimeSeconds));
        map.put("largestPoolSize", metricValueToMap(largestPoolSize));
        map.put("taskCount", metricValueToMap(taskCount));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static RuntimeObservation fromMap(Map<String, Object> map) {
        Instant timestamp = Instant.parse((String) map.get("timestamp"));
        return new RuntimeObservation(
                timestamp,
                metricValueFromMap(map, "activeThreads", Integer.class),
                metricValueFromMap(map, "poolSize", Integer.class),
                metricValueFromMap(map, "queueSize", Integer.class),
                metricValueFromMap(map, "completedTaskCount", Long.class),
                metricValueFromMap(map, "cpuUtilization", Double.class),
                metricValueFromMap(map, "keepAliveTimeSeconds", Long.class),
                metricValueFromMap(map, "largestPoolSize", Integer.class),
                metricValueFromMap(map, "taskCount", Long.class));
    }

    private static Map<String, Object> metricValueToMap(MetricValue<?> mv) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (mv instanceof MetricValue.Present<?> p) {
            m.put("status", "PRESENT");
            m.put("value", p.value());
        } else {
            m.put("status", "ABSENT");
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static <T> MetricValue<T> metricValueFromMap(
            Map<String, Object> map, String key, Class<T> targetType) {
        Map<String, Object> mvMap = (Map<String, Object>) map.get(key);
        String status = (String) mvMap.get("status");
        if ("ABSENT".equals(status)) {
            return MetricValue.absent();
        }
        Object rawValue = mvMap.get("value");
        T typedValue;
        if (targetType == Long.class) {
            typedValue = targetType.cast(((Number) rawValue).longValue());
        } else if (targetType == Integer.class) {
            typedValue = targetType.cast(((Number) rawValue).intValue());
        } else if (targetType == Double.class) {
            typedValue = targetType.cast(((Number) rawValue).doubleValue());
        } else {
            typedValue = targetType.cast(rawValue);
        }
        return MetricValue.present(typedValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuntimeObservation that)) return false;
        return timestamp.equals(that.timestamp)
                && activeThreads.equals(that.activeThreads)
                && poolSize.equals(that.poolSize)
                && queueSize.equals(that.queueSize)
                && completedTaskCount.equals(that.completedTaskCount)
                && cpuUtilization.equals(that.cpuUtilization)
                && keepAliveTimeSeconds.equals(that.keepAliveTimeSeconds)
                && largestPoolSize.equals(that.largestPoolSize)
                && taskCount.equals(that.taskCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, activeThreads, poolSize, queueSize,
                completedTaskCount, cpuUtilization, keepAliveTimeSeconds,
                largestPoolSize, taskCount);
    }

    @Override
    public String toString() {
        return "RuntimeObservation{timestamp=%s, activeThreads=%s, poolSize=%s, queueSize=%s, completedTaskCount=%s, cpuUtilization=%s, keepAliveTimeSeconds=%s, largestPoolSize=%s, taskCount=%s}"
                .formatted(timestamp, activeThreads, poolSize, queueSize,
                        completedTaskCount, cpuUtilization, keepAliveTimeSeconds,
                        largestPoolSize, taskCount);
    }
}
