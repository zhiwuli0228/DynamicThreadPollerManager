package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CommonExecutorPreset(
        String presetId,
        String executorType,
        int corePoolSize,
        int maxPoolSize,
        int queueCapacity,
        String description
) {
    private static final Set<String> VALID_TYPES = Set.of(
            "FIXED_THREAD_POOL", "CACHED_THREAD_POOL", "SINGLE_THREAD_EXECUTOR");

    public CommonExecutorPreset {
        Objects.requireNonNull(presetId, "presetId must not be null");
        if (presetId.isBlank()) {
            throw new IllegalArgumentException("presetId must not be blank");
        }
        Objects.requireNonNull(executorType, "executorType must not be null");
        if (!VALID_TYPES.contains(executorType)) {
            throw new IllegalArgumentException(
                    "executorType must be one of " + VALID_TYPES + ", was " + executorType);
        }
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize must be >= 0, was " + corePoolSize);
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException(
                    "maxPoolSize must be >= corePoolSize, was " + maxPoolSize);
        }
        if (queueCapacity < -1) {
            throw new IllegalArgumentException("queueCapacity must be >= -1, was " + queueCapacity);
        }
    }

    public BaselineExecutorPreset toBaselinePreset() {
        int mappedQueue = queueCapacity == -1 ? Integer.MAX_VALUE : queueCapacity;
        int mappedCore = corePoolSize == 0 ? 1 : corePoolSize;
        return new BaselineExecutorPreset(presetId, mappedCore, maxPoolSize, mappedQueue);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("presetId", presetId);
        map.put("executorType", executorType);
        map.put("corePoolSize", corePoolSize);
        map.put("maxPoolSize", maxPoolSize);
        map.put("queueCapacity", queueCapacity);
        if (description != null) {
            map.put("description", description);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static CommonExecutorPreset fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        return new CommonExecutorPreset(
                (String) map.get("presetId"),
                (String) map.get("executorType"),
                ((Number) map.get("corePoolSize")).intValue(),
                ((Number) map.get("maxPoolSize")).intValue(),
                ((Number) map.get("queueCapacity")).intValue(),
                (String) map.get("description"));
    }
}
