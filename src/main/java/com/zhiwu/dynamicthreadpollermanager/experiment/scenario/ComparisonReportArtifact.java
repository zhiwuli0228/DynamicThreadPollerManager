package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ComparisonReportArtifact(
        String comparisonId,
        String scenarioId,
        Instant createdAt,
        CommonExecutorPreset baselinePreset,
        ManagedExecutorConfig managedConfig,
        ComparisonResult result,
        String conclusion
) {
    public ComparisonReportArtifact {
        Objects.requireNonNull(comparisonId);
        Objects.requireNonNull(scenarioId);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(baselinePreset);
        Objects.requireNonNull(result);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("comparisonId", comparisonId);
        map.put("scenarioId", scenarioId);
        map.put("createdAt", createdAt.toString());
        map.put("baselinePreset", baselinePreset.toMap());
        map.put("managedConfig", managedConfigToMap(managedConfig));
        map.put("result", result.toMap());
        if (conclusion != null) {
            map.put("conclusion", conclusion);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ComparisonReportArtifact fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        CommonExecutorPreset preset = CommonExecutorPreset.fromMap(
                (Map<String, Object>) map.get("baselinePreset"));
        return new ComparisonReportArtifact(
                (String) map.get("comparisonId"),
                (String) map.get("scenarioId"),
                Instant.parse((String) map.get("createdAt")),
                preset,
                null, // ManagedExecutorConfig not reconstructed from map
                ComparisonResult.fromMap((Map<String, Object>) map.get("result")),
                (String) map.get("conclusion"));
    }

    static Map<String, Object> managedConfigToMap(ManagedExecutorConfig c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("corePoolSize", c.corePoolSize());
        map.put("maximumPoolSize", c.maximumPoolSize());
        map.put("queueCapacity", c.queueCapacity());
        map.put("keepAliveTime", c.keepAliveTime());
        map.put("keepAliveTimeUnit", c.keepAliveTimeUnit().name());
        map.put("threadMode", c.threadMode().name());
        return map;
    }
}
