package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fixed set of three replay configurations used by
 * {@link OfflinePolicyReplayService} and
 * {@link ThresholdSensitivityAnalyzer}. The {@code default} entry is
 * sourced from {@link ThresholdPolicyConfig#defaultAdaptive()} so the
 * analysis layer always reuses the canonical baseline; the other two
 * entries are pinned to the values fixed by the design.
 *
 * <p>This set is intentionally small and read-only — adding configs
 * requires a version-design revision.
 */
public final class SensitivityConfigSet {

    public static final String DEFAULT_LABEL = "default";
    public static final String CONSERVATIVE_LABEL = "conservative";
    public static final String AGGRESSIVE_LABEL = "aggressive";

    public static final String DEFAULT_POLICY_ID = "default-adaptive";
    public static final String CONSERVATIVE_POLICY_ID = "conservative-adaptive";
    public static final String AGGRESSIVE_POLICY_ID = "aggressive-adaptive";

    private final Map<String, ThresholdPolicyConfig> configs;

    private SensitivityConfigSet(Map<String, ThresholdPolicyConfig> configs) {
        this.configs = Collections.unmodifiableMap(new LinkedHashMap<>(configs));
    }

    public static SensitivityConfigSet defaults() {
        Map<String, ThresholdPolicyConfig> map = new LinkedHashMap<>();
        map.put(DEFAULT_LABEL, ThresholdPolicyConfig.defaultAdaptive());
        map.put(CONSERVATIVE_LABEL, new ThresholdPolicyConfig(
                CONSERVATIVE_POLICY_ID,
                1,
                32,
                28,
                20,
                2,
                1
        ));
        map.put(AGGRESSIVE_LABEL, new ThresholdPolicyConfig(
                AGGRESSIVE_POLICY_ID,
                1,
                32,
                20,
                12,
                6,
                3
        ));
        return new SensitivityConfigSet(map);
    }

    public static SensitivityConfigSet of(Map<String, ThresholdPolicyConfig> configs) {
        Objects.requireNonNull(configs, "configs must not be null");
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs must not be empty");
        }
        Map<String, ThresholdPolicyConfig> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ThresholdPolicyConfig> entry : configs.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("config label must not be blank");
            }
            Objects.requireNonNull(entry.getValue(),
                    () -> "config for label " + entry.getKey() + " must not be null");
            copy.put(entry.getKey(), entry.getValue());
        }
        return new SensitivityConfigSet(copy);
    }

    public List<String> labels() {
        return List.copyOf(configs.keySet());
    }

    public List<ThresholdPolicyConfig> configs() {
        return List.copyOf(configs.values());
    }

    public boolean contains(String label) {
        return configs.containsKey(label);
    }

    public ThresholdPolicyConfig config(String label) {
        ThresholdPolicyConfig cfg = configs.get(label);
        if (cfg == null) {
            throw new IllegalArgumentException("no config registered for label " + label);
        }
        return cfg;
    }

    public int size() {
        return configs.size();
    }
}
