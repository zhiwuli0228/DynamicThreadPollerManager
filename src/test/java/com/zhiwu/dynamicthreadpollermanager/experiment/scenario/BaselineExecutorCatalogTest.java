package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class BaselineExecutorCatalogTest {

    @Test
    void withDefaultsShouldRegisterSixPresets() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        assertEquals(6, catalog.size());
    }

    @Test
    void withDefaultsShouldContainAllExpectedPresetIds() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        assertTrue(catalog.presetIds().contains("fixed-2"));
        assertTrue(catalog.presetIds().contains("fixed-4"));
        assertTrue(catalog.presetIds().contains("fixed-8"));
        assertTrue(catalog.presetIds().contains("cached"));
        assertTrue(catalog.presetIds().contains("single"));
        assertTrue(catalog.presetIds().contains("fixed-2-bounded"));
    }

    @Test
    void getFixed4ShouldReturnCorrectConfiguration() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        CommonExecutorPreset preset = catalog.get("fixed-4");
        assertEquals("fixed-4", preset.presetId());
        assertEquals("FIXED_THREAD_POOL", preset.executorType());
        assertEquals(4, preset.corePoolSize());
        assertEquals(4, preset.maxPoolSize());
        assertEquals(-1, preset.queueCapacity());
    }

    @Test
    void getCachedShouldReturnZeroCorePoolSize() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        CommonExecutorPreset preset = catalog.get("cached");
        assertEquals(0, preset.corePoolSize());
        assertEquals(Integer.MAX_VALUE, preset.maxPoolSize());
        assertEquals(0, preset.queueCapacity());
    }

    @Test
    void getSingleShouldReturnSingleThreadConfig() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        CommonExecutorPreset preset = catalog.get("single");
        assertEquals(1, preset.corePoolSize());
        assertEquals(1, preset.maxPoolSize());
    }

    @Test
    void getNonexistentShouldThrowNoSuchElementException() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        assertThrows(NoSuchElementException.class, () -> catalog.get("nonexistent"));
    }

    @Test
    void registerDuplicateShouldThrowIllegalArgumentException() {
        BaselineExecutorCatalog.Builder builder = new BaselineExecutorCatalog.Builder();
        builder.register(new CommonExecutorPreset(
                "test", "FIXED_THREAD_POOL", 1, 1, -1, null));
        assertThrows(IllegalArgumentException.class, () -> builder.register(new CommonExecutorPreset(
                "test", "FIXED_THREAD_POOL", 2, 2, -1, null)));
    }

    @Test
    void builderShouldBuildImmutableCatalog() {
        BaselineExecutorCatalog.Builder builder = new BaselineExecutorCatalog.Builder();
        builder.register(new CommonExecutorPreset(
                "test", "FIXED_THREAD_POOL", 1, 1, -1, null));
        BaselineExecutorCatalog catalog = builder.build();
        assertEquals(1, catalog.size());
        assertEquals("test", catalog.get("test").presetId());
    }

    @Test
    void presetIdsShouldReturnUnmodifiableSet() {
        BaselineExecutorCatalog catalog = BaselineExecutorCatalog.withDefaults();
        assertThrows(UnsupportedOperationException.class, () -> catalog.presetIds().add("new"));
    }
}
