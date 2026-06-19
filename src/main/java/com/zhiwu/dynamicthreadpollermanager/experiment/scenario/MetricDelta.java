package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MetricDelta(
        String metricName,
        double baselineValue,
        double managedValue,
        double absoluteDelta,
        double relativeDelta,
        String direction
) {
    public MetricDelta {
        Objects.requireNonNull(metricName, "metricName must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
    }

    public static MetricDelta compute(
            String metricName,
            double baselineValue,
            double managedValue,
            boolean higherIsBetter) {

        double absDelta = managedValue - baselineValue;
        double relDelta = baselineValue != 0.0
                ? (absDelta / baselineValue) * 100.0
                : 0.0;

        String dir;
        if (Math.abs(relDelta) < 1.0) {
            dir = "NEUTRAL";
        } else if (higherIsBetter) {
            dir = absDelta > 0 ? "IMPROVED" : "REGRESSED";
        } else {
            dir = absDelta < 0 ? "IMPROVED" : "REGRESSED";
        }

        return new MetricDelta(metricName, baselineValue, managedValue, absDelta, relDelta, dir);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("metricName", metricName);
        map.put("baselineValue", baselineValue);
        map.put("managedValue", managedValue);
        map.put("absoluteDelta", absoluteDelta);
        map.put("relativeDelta", relativeDelta);
        map.put("direction", direction);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static MetricDelta fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        return new MetricDelta(
                (String) map.get("metricName"),
                ((Number) map.get("baselineValue")).doubleValue(),
                ((Number) map.get("managedValue")).doubleValue(),
                ((Number) map.get("absoluteDelta")).doubleValue(),
                ((Number) map.get("relativeDelta")).doubleValue(),
                (String) map.get("direction"));
    }
}
