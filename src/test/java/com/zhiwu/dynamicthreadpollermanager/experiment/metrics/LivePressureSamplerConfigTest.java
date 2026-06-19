package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LivePressureSamplerConfigTest {

    @Test
    void shouldCreateWithValidValues() {
        LivePressureSamplerConfig config = new LivePressureSamplerConfig(500, true, "session-1");

        assertEquals(500L, config.pollIntervalMs());
        assertTrue(config.autoStart());
        assertEquals("session-1", config.sessionId());
    }

    @Test
    void shouldRejectPollIntervalBelow100ms() {
        assertThrows(IllegalArgumentException.class,
                () -> new LivePressureSamplerConfig(99, false, "session-1"));
        assertThrows(IllegalArgumentException.class,
                () -> new LivePressureSamplerConfig(0, false, "session-1"));
        assertThrows(IllegalArgumentException.class,
                () -> new LivePressureSamplerConfig(-1, false, "session-1"));
    }

    @Test
    void shouldAcceptMinimalPollInterval() {
        LivePressureSamplerConfig config = new LivePressureSamplerConfig(100, false, "session-1");
        assertEquals(100L, config.pollIntervalMs());
    }

    @Test
    void shouldRejectNullSessionId() {
        assertThrows(NullPointerException.class,
                () -> new LivePressureSamplerConfig(1000, false, null));
    }

    @Test
    void shouldCreateDefaults() {
        LivePressureSamplerConfig config = LivePressureSamplerConfig.defaults("default-session");

        assertEquals(1000L, config.pollIntervalMs());
        assertFalse(config.autoStart());
        assertEquals("default-session", config.sessionId());
    }
}
