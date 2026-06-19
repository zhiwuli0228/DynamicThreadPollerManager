package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEvidenceRecorderTest {

    private final InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();

    @Test
    void shouldAppendSnapshotsForSameRunInInsertionOrder() {
        Instant t1 = Instant.parse("2026-06-02T10:00:00Z");
        Instant t2 = Instant.parse("2026-06-02T10:00:05Z");
        recorder.record(observed("run-1", t1, 1));
        recorder.record(observed("run-1", t2, 2));

        List<ObservedSnapshot> recorded = recorder.snapshots("run-1");

        assertEquals(2, recorded.size());
        assertEquals(t1, recorded.get(0).snapshot().timestamp());
        assertEquals(t2, recorded.get(1).snapshot().timestamp());
    }

    @Test
    void shouldKeepRunsIsolated() {
        recorder.record(observed("run-1", Instant.parse("2026-06-02T10:00:00Z"), 1));
        recorder.record(observed("run-2", Instant.parse("2026-06-02T10:00:01Z"), 1));
        recorder.record(observed("run-1", Instant.parse("2026-06-02T10:00:02Z"), 2));

        assertEquals(2, recorder.snapshots("run-1").size());
        assertEquals(1, recorder.snapshots("run-2").size());
        assertEquals(Set.of("run-1", "run-2"), recorder.runIds());
    }

    @Test
    void shouldReturnEmptyListForUnknownRun() {
        assertTrue(recorder.snapshots("unknown").isEmpty());
    }

    @Test
    void shouldExposeImmutableSnapshotList() {
        recorder.record(observed("run-1", Instant.parse("2026-06-02T10:00:00Z"), 1));
        List<ObservedSnapshot> recorded = recorder.snapshots("run-1");

        assertThrows(UnsupportedOperationException.class, () -> recorded.add(observed("run-1", Instant.now(), 0)));
    }

    @Test
    void shouldNotReflectLaterMutationsInPreviouslyReadSnapshot() {
        recorder.record(observed("run-1", Instant.parse("2026-06-02T10:00:00Z"), 1));
        List<ObservedSnapshot> firstRead = recorder.snapshots("run-1");
        recorder.record(observed("run-1", Instant.parse("2026-06-02T10:00:01Z"), 2));

        assertEquals(1, firstRead.size(), "earlier read must not see later appends");
        assertEquals(2, recorder.snapshots("run-1").size());
    }

    @Test
    void shouldRejectNullSnapshot() {
        assertThrows(NullPointerException.class, () -> recorder.record(null));
    }

    @Test
    void shouldRejectNullRunId() {
        assertThrows(NullPointerException.class, () -> recorder.snapshots(null));
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
