package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ComparisonResult(
        String comparisonId,
        String scenarioId,
        String baselinePresetId,
        String managedConfigId,
        ScenarioRunOutcome baselineOutcome,
        ScenarioRunOutcome managedOutcome,
        NormalizedComparisonMetrics baselineMetrics,
        NormalizedComparisonMetrics managedMetrics,
        Map<String, MetricDelta> deltas,
        Instant createdAt
) {
    public ComparisonResult {
        Objects.requireNonNull(comparisonId);
        Objects.requireNonNull(scenarioId);
        Objects.requireNonNull(baselinePresetId);
        Objects.requireNonNull(managedConfigId);
        Objects.requireNonNull(baselineMetrics);
        Objects.requireNonNull(managedMetrics);
        Objects.requireNonNull(deltas);
        Objects.requireNonNull(createdAt);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("comparisonId", comparisonId);
        map.put("scenarioId", scenarioId);
        map.put("baselinePresetId", baselinePresetId);
        map.put("managedConfigId", managedConfigId);
        map.put("baselineMetrics", baselineMetrics.toMap());
        map.put("managedMetrics", managedMetrics.toMap());
        Map<String, Object> deltasMap = new LinkedHashMap<>();
        for (Map.Entry<String, MetricDelta> entry : deltas.entrySet()) {
            deltasMap.put(entry.getKey(), entry.getValue().toMap());
        }
        map.put("deltas", deltasMap);
        map.put("createdAt", createdAt.toString());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ComparisonResult fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        Map<String, Object> deltasMap = (Map<String, Object>) map.get("deltas");
        Map<String, MetricDelta> deltas = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : deltasMap.entrySet()) {
            deltas.put(entry.getKey(), MetricDelta.fromMap((Map<String, Object>) entry.getValue()));
        }
        return new ComparisonResult(
                (String) map.get("comparisonId"),
                (String) map.get("scenarioId"),
                (String) map.get("baselinePresetId"),
                (String) map.get("managedConfigId"),
                null, // ScenarioRunOutcome not reconstructed from map
                null,
                NormalizedComparisonMetrics.fromMap((Map<String, Object>) map.get("baselineMetrics")),
                NormalizedComparisonMetrics.fromMap((Map<String, Object>) map.get("managedMetrics")),
                deltas,
                Instant.parse((String) map.get("createdAt")));
    }
}
