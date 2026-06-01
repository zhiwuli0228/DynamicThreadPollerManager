package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultEvidenceSummaryBuilderTest {

    private final EvidenceSummaryBuilder builder = new DefaultEvidenceSummaryBuilder();

    @Test
    void shouldSummarizePopulatedStream() {
        Instant t1 = Instant.parse("2026-06-02T10:00:00Z");
        Instant t2 = Instant.parse("2026-06-02T10:00:05Z");
        Instant t3 = Instant.parse("2026-06-02T10:00:10Z");
        List<ObservedSnapshot> snapshots = List.of(
                observed("run-1", t1, 1),
                observed("run-1", t2, 2),
                observed("run-1", t3, 3)
        );

        EvidenceSummary summary = builder.summarize("run-1", snapshots);

        assertEquals("run-1", summary.runId());
        assertEquals(3, summary.sampleCount());
        assertEquals(t1, summary.firstTimestamp().orElseThrow());
        assertEquals(t3, summary.lastTimestamp().orElseThrow());
    }

    @Test
    void shouldSummarizeSingleSampleStream() {
        Instant t1 = Instant.parse("2026-06-02T10:00:00Z");
        List<ObservedSnapshot> snapshots = List.of(observed("run-1", t1, 1));

        EvidenceSummary summary = builder.summarize("run-1", snapshots);

        assertEquals(1, summary.sampleCount());
        assertEquals(t1, summary.firstTimestamp().orElseThrow());
        assertEquals(t1, summary.lastTimestamp().orElseThrow());
    }

    @Test
    void shouldSummarizeEmptyStreamWithoutFabricatingValues() {
        EvidenceSummary summary = builder.summarize("run-1", List.of());

        assertEquals("run-1", summary.runId());
        assertEquals(0, summary.sampleCount());
        assertTrue(summary.firstTimestamp().isEmpty());
        assertTrue(summary.lastTimestamp().isEmpty());
    }

    @Test
    void shouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> builder.summarize(null, List.of()));
        assertThrows(NullPointerException.class, () -> builder.summarize("run-1", null));
    }

    private static ObservedSnapshot observed(String runId, Instant timestamp, int activeThreads) {
        RuntimeObservation observation = new RuntimeObservation(
                timestamp,
                MetricValue.present(activeThreads),
                MetricValue.absent(),
                MetricValue.absent()
        );
        PressureSnapshot snapshot = new PressureSnapshot(timestamp, activeThreads, 0, 0.0);
        return new ObservedSnapshot(runId, snapshot, observation);
    }
}
