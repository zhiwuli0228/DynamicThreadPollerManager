package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RecordingSession;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RecordingSessionMetadata;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.SessionStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBackedEvidenceRecorderTest {

    private Path tempDir;
    private FileBackedEvidenceRecorder recorder;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("fber-test-");
        recorder = new FileBackedEvidenceRecorder(tempDir, "v0.11.0");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var files = Files.walk(tempDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    private ObservedSnapshot createSnapshot(String runId, int value) {
        PressureSnapshot ps = new PressureSnapshot(
                Instant.parse("2026-06-13T10:00:0" + (value % 10) + "Z"),
                value, value + 1, value + 2, value * 10L, 0.1 * value);
        RuntimeObservation obs = new RuntimeObservation(
                Instant.parse("2026-06-13T10:00:0" + (value % 10) + "Z"),
                MetricValue.present(value),
                MetricValue.present(value + 1),
                MetricValue.present(value + 2),
                MetricValue.present(value * 10L),
                MetricValue.absent(),
                MetricValue.present(60L),
                MetricValue.present(value + 1),
                MetricValue.present(value * 100L));
        return new ObservedSnapshot(runId, ps, obs);
    }

    @Test
    void shouldRecordAndRetrieveSingleSnapshot() {
        ObservedSnapshot snapshot = createSnapshot("run-001", 1);
        recorder.record(snapshot);

        List<ObservedSnapshot> retrieved = recorder.snapshots("run-001");
        assertEquals(1, retrieved.size());
        assertEquals(snapshot, retrieved.get(0));
    }

    @Test
    void shouldRecordMultipleSnapshotsAcrossRunIds() {
        recorder.record(createSnapshot("run-001", 1));
        recorder.record(createSnapshot("run-001", 2));
        recorder.record(createSnapshot("run-002", 3));

        assertEquals(2, recorder.snapshots("run-001").size());
        assertEquals(1, recorder.snapshots("run-002").size());
        assertEquals(Set.of("run-001", "run-002"), recorder.runIds());
    }

    @Test
    void shouldReturnEmptyListForUnknownRunId() {
        List<ObservedSnapshot> retrieved = recorder.snapshots("unknown-run");
        assertTrue(retrieved.isEmpty());
    }

    @Test
    void shouldReturnImmutableList() {
        recorder.record(createSnapshot("run-001", 1));
        List<ObservedSnapshot> retrieved = recorder.snapshots("run-001");

        assertThrows(UnsupportedOperationException.class, () -> retrieved.add(
                createSnapshot("run-001", 2)));
    }

    @Test
    void shouldReturnImmutableRunIds() {
        recorder.record(createSnapshot("run-001", 1));
        Set<String> ids = recorder.runIds();

        assertThrows(UnsupportedOperationException.class, () -> ids.add("new"));
    }

    @Test
    void shouldStartAndCloseSession() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        RecordingSession session = recorder.startSession("run-001", config);

        assertNotNull(session);
        assertEquals(SessionStatus.ACTIVE, session.status());
        assertEquals("run-001", session.runId());

        recorder.record(createSnapshot("run-001", 1));
        recorder.record(createSnapshot("run-001", 2));

        RecordingSessionMetadata metadata = recorder.closeSession("run-001");

        assertEquals(SessionStatus.CLOSED, metadata.status());
        assertEquals(2, metadata.snapshotCount());
        assertEquals("run-001", metadata.runId());
        assertEquals(config.corePoolSize(), metadata.corePoolSize());
        assertEquals(config.maximumPoolSize(), metadata.maximumPoolSize());
    }

    @Test
    void shouldWriteSessionMetadataFile() throws IOException {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        recorder.startSession("run-001", config);
        recorder.record(createSnapshot("run-001", 1));
        recorder.closeSession("run-001");

        Path metadataFile = AcquisitionReportPaths.sessionMetadataFile(tempDir, "run-001");
        assertTrue(Files.exists(metadataFile), "session metadata file should exist");

        String content = Files.readString(metadataFile);
        assertTrue(content.contains("\"sessionId\""), "should contain sessionId field");
        assertTrue(content.contains("\"run-001\""), "should contain runId");
        assertTrue(content.contains("\"CLOSED\""), "should contain CLOSED status");
    }

    @Test
    void shouldNotIncrementSessionCountWhenSessionClosed() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        recorder.startSession("run-001", config);
        recorder.record(createSnapshot("run-001", 1));
        RecordingSessionMetadata metadata = recorder.closeSession("run-001");
        assertEquals(1, metadata.snapshotCount());

        // record after close — should not throw and should not increment
        recorder.record(createSnapshot("run-001", 2));
        assertEquals(2, recorder.snapshots("run-001").size());
        // session count should still be 1 (not incremented after close)
        assertEquals(1, metadata.snapshotCount());
    }

    @Test
    void shouldThrowOnDuplicateActiveSession() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        recorder.startSession("run-001", config);

        assertThrows(IllegalStateException.class, () ->
                recorder.startSession("run-001", config));
    }

    @Test
    void shouldThrowOnCloseNonexistentSession() {
        assertThrows(IllegalStateException.class, () ->
                recorder.closeSession("nonexistent"));
    }

    @Test
    void shouldAllowNewSessionAfterClose() {
        ManagedExecutorConfig config = ManagedExecutorConfig.defaultConfig();
        recorder.startSession("run-001", config);
        recorder.closeSession("run-001");

        RecordingSession session2 = recorder.startSession("run-001", config);
        assertEquals(SessionStatus.ACTIVE, session2.status());
    }

    @Test
    void shouldWriteConcurrentlyWithoutDataLoss() throws Exception {
        int threadCount = 4;
        int snapshotsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        try (var executor = Executors.newFixedThreadPool(threadCount)) {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < snapshotsPerThread; i++) {
                        recorder.record(createSnapshot("concurrent-run",
                                threadId * 1000 + i));
                    }
                    doneLatch.countDown();
                });
            }
            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        }

        List<ObservedSnapshot> all = recorder.snapshots("concurrent-run");
        assertEquals(threadCount * snapshotsPerThread, all.size());
    }

    @Test
    void shouldCreateOutputDirectoryOnConstruction() {
        Path outputDir = tempDir.resolve("outputs/reports/v0.11.0");
        assertTrue(Files.isDirectory(outputDir), "output directory should exist");
    }

    @Test
    void shouldPersistEvidenceFile() throws IOException {
        recorder.record(createSnapshot("run-001", 1));
        recorder.record(createSnapshot("run-001", 2));

        Path evidenceFile = AcquisitionReportPaths.evidenceFile(tempDir, "run-001");
        assertTrue(Files.exists(evidenceFile), "evidence file should exist");

        List<String> lines = Files.readAllLines(evidenceFile);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"runId\""));
        assertTrue(lines.get(0).contains("\"snapshot\""));
        assertTrue(lines.get(0).contains("\"observation\""));
    }

    @Test
    void shouldThrowOnConstructionWithInvalidPath() {
        // Use a path where we can't create directories
        // On Windows, this is harder to test; use a file path
        Path filePath = tempDir.resolve("not-a-directory.txt");
        try {
            Files.writeString(filePath, "data");
        } catch (IOException e) {
            // skip test if we can't set up
            return;
        }
        assertThrows(UncheckedIOException.class, () ->
                new FileBackedEvidenceRecorder(filePath.resolve("sub"), "v0.11.0"));
    }

    @Test
    void shouldRequireNonNullSnapshot() {
        assertThrows(NullPointerException.class, () -> recorder.record(null));
    }

    @Test
    void shouldRequireNonNullRunId() {
        assertThrows(NullPointerException.class, () -> recorder.snapshots(null));
    }
}
