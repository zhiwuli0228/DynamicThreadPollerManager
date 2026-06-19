package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PressureClassificationTest {

    private final NormalizedPressureMetrics metrics =
            NormalizedPressureMetrics.fromSnapshots(List.of(), 0L, 0, 0);

    @Test
    void shouldCreateWithValidValues() {
        PressureClassification c = new PressureClassification(
                PressureState.NORMAL, 0.8, List.of("evidence"),
                metrics, Instant.now());
        assertEquals(PressureState.NORMAL, c.state());
        assertEquals(0.8, c.confidence());
        assertEquals(1, c.evidence().size());
    }

    @Test
    void shouldRejectNullState() {
        assertThrows(NullPointerException.class,
                () -> new PressureClassification(
                        null, 0.8, List.of(), metrics, Instant.now()));
    }

    @Test
    void shouldRejectConfidenceBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new PressureClassification(
                        PressureState.NORMAL, -0.1, List.of(), metrics,
                        Instant.now()));
    }

    @Test
    void shouldRejectConfidenceAboveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new PressureClassification(
                        PressureState.NORMAL, 1.1, List.of(), metrics,
                        Instant.now()));
    }

    @Test
    void shouldAcceptBoundaryConfidence() {
        assertDoesNotThrow(() -> new PressureClassification(
                PressureState.NORMAL, 0.0, List.of(), metrics, Instant.now()));
        assertDoesNotThrow(() -> new PressureClassification(
                PressureState.NORMAL, 1.0, List.of(), metrics, Instant.now()));
    }

    @Test
    void shouldRejectNullMetrics() {
        assertThrows(NullPointerException.class,
                () -> new PressureClassification(
                        PressureState.NORMAL, 0.5, List.of(), null,
                        Instant.now()));
    }

    @Test
    void shouldRejectNullClassifiedAt() {
        assertThrows(NullPointerException.class,
                () -> new PressureClassification(
                        PressureState.NORMAL, 0.5, List.of(), metrics, null));
    }
}
