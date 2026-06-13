package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordingSessionTest {

    @Test
    void shouldStartSessionAsActive() {
        RecordingSession session = new RecordingSession(
                "sess-001", "run-001", 2, 4, 10, 60, "SECONDS", "PLATFORM");

        assertEquals(SessionStatus.ACTIVE, session.status());
        assertEquals(0, session.snapshotCount());
        assertEquals("sess-001", session.sessionId());
        assertEquals("run-001", session.runId());
        assertEquals(2, session.corePoolSize());
        assertEquals(4, session.maximumPoolSize());
        assertEquals(10, session.queueCapacity());
        assertEquals(60L, session.keepAliveTime());
        assertEquals("SECONDS", session.keepAliveTimeUnit());
        assertEquals("PLATFORM", session.threadMode());
        assertNotNull(session.startedAt());
    }

    @Test
    void shouldIncrementSnapshotCount() {
        RecordingSession session = new RecordingSession(
                "sess-001", "run-001", 2, 4, 10, 60, "SECONDS", "PLATFORM");

        session.incrementSnapshotCount();
        session.incrementSnapshotCount();
        session.incrementSnapshotCount();

        assertEquals(3, session.snapshotCount());
    }

    @Test
    void shouldCloseSessionAndReturnMetadata() {
        RecordingSession session = new RecordingSession(
                "sess-001", "run-001", 2, 4, 10, 60, "SECONDS", "PLATFORM");
        session.incrementSnapshotCount();
        session.incrementSnapshotCount();

        RecordingSessionMetadata metadata = session.close();

        assertEquals(SessionStatus.CLOSED, session.status());
        assertEquals(SessionStatus.CLOSED, metadata.status());
        assertEquals("sess-001", metadata.sessionId());
        assertEquals("run-001", metadata.runId());
        assertEquals(2, metadata.corePoolSize());
        assertEquals(4, metadata.maximumPoolSize());
        assertEquals(10, metadata.queueCapacity());
        assertEquals(60L, metadata.keepAliveTime());
        assertEquals("SECONDS", metadata.keepAliveTimeUnit());
        assertEquals("PLATFORM", metadata.threadMode());
        assertEquals(2, metadata.snapshotCount());
        assertNotNull(metadata.startedAt());
        assertNotNull(metadata.closedAt());
    }

    @Test
    void shouldThrowOnDoubleClose() {
        RecordingSession session = new RecordingSession(
                "sess-001", "run-001", 2, 4, 10, 60, "SECONDS", "PLATFORM");
        session.close();

        assertThrows(IllegalStateException.class, session::close);
    }

    @Test
    void shouldThrowOnIncrementAfterClose() {
        RecordingSession session = new RecordingSession(
                "sess-001", "run-001", 2, 4, 10, 60, "SECONDS", "PLATFORM");
        session.close();

        assertThrows(IllegalStateException.class, session::incrementSnapshotCount);
    }

    @Test
    void shouldStoreThreadMode() {
        RecordingSession session = new RecordingSession(
                "sess-001", "run-001", 2, 4, 10, 60, "SECONDS", "VIRTUAL");

        assertEquals("VIRTUAL", session.threadMode());

        RecordingSessionMetadata metadata = session.close();
        assertEquals("VIRTUAL", metadata.threadMode());
    }
}
