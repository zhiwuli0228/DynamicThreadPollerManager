package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class BaselineExecutorCatalog {

    private final Map<String, CommonExecutorPreset> presets;

    private BaselineExecutorCatalog(Map<String, CommonExecutorPreset> presets) {
        this.presets = Map.copyOf(presets);
    }

    public CommonExecutorPreset get(String presetId) {
        Objects.requireNonNull(presetId, "presetId must not be null");
        CommonExecutorPreset preset = presets.get(presetId);
        if (preset == null) {
            throw new NoSuchElementException("no preset for id: " + presetId);
        }
        return preset;
    }

    public Set<String> presetIds() {
        return presets.keySet();
    }

    public int size() {
        return presets.size();
    }

    public static final class Builder {
        private final Map<String, CommonExecutorPreset> presets = new LinkedHashMap<>();

        public Builder register(CommonExecutorPreset preset) {
            Objects.requireNonNull(preset, "preset must not be null");
            if (presets.containsKey(preset.presetId())) {
                throw new IllegalArgumentException("duplicate presetId: " + preset.presetId());
            }
            presets.put(preset.presetId(), preset);
            return this;
        }

        public BaselineExecutorCatalog build() {
            return new BaselineExecutorCatalog(new LinkedHashMap<>(presets));
        }
    }

    public static BaselineExecutorCatalog withDefaults() {
        return new Builder()
                .register(new CommonExecutorPreset(
                        "fixed-2", "FIXED_THREAD_POOL", 2, 2, -1,
                        "Fixed thread pool with 2 threads, unbounded queue"))
                .register(new CommonExecutorPreset(
                        "fixed-4", "FIXED_THREAD_POOL", 4, 4, -1,
                        "Fixed thread pool with 4 threads, unbounded queue"))
                .register(new CommonExecutorPreset(
                        "fixed-8", "FIXED_THREAD_POOL", 8, 8, -1,
                        "Fixed thread pool with 8 threads, unbounded queue"))
                .register(new CommonExecutorPreset(
                        "cached", "CACHED_THREAD_POOL", 0, Integer.MAX_VALUE, 0,
                        "Cached thread pool with SynchronousQueue, unbounded thread creation"))
                .register(new CommonExecutorPreset(
                        "single", "SINGLE_THREAD_EXECUTOR", 1, 1, -1,
                        "Single-thread executor with unbounded queue"))
                .register(new CommonExecutorPreset(
                        "fixed-2-bounded", "FIXED_THREAD_POOL", 2, 2, 10,
                        "Fixed thread pool with 2 threads and bounded queue (capacity=10)"))
                .build();
    }
}
