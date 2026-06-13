package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RecordingSession;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RecordingSessionMetadata;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.SessionStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * File-backed implementation of {@link EvidenceRecorder} that persists
 * {@link ObservedSnapshot} instances as JSON Lines (.jsonl) files.
 *
 * <p>Placed in {@code experiment.acquisition} (not {@code experiment.metrics})
 * to avoid a circular dependency: acquisition already depends on metrics
 * ({@code AcquisitionReportBridge} consumes {@code List<ObservedSnapshot>}).
 */
public final class FileBackedEvidenceRecorder implements EvidenceRecorder {

    private final Path outputDir;
    private final ConcurrentHashMap<String, List<ObservedSnapshot>> buffer;
    private final ConcurrentHashMap<String, RecordingSession> sessions;

    public FileBackedEvidenceRecorder(Path outputRoot, String versionTag) {
        AcquisitionReportPaths paths = AcquisitionReportPaths.forVersion(versionTag);
        this.outputDir = outputRoot.resolve(paths.outputDirectory());
        this.buffer = new ConcurrentHashMap<>();
        this.sessions = new ConcurrentHashMap<>();
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ----- EvidenceRecorder implementation -----

    @Override
    public void record(ObservedSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        String runId = snapshot.runId();

        buffer.computeIfAbsent(runId, k ->
                new CopyOnWriteArrayList<>()).add(snapshot);

        RecordingSession session = sessions.get(runId);
        if (session != null && session.status() == SessionStatus.ACTIVE) {
            session.incrementSnapshotCount();
        }

        try {
            String json = renderSnapshot(snapshot);
            Path evidenceFile = evidenceFilePath(runId);
            Files.writeString(evidenceFile, json + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to write evidence for runId=" + runId, e);
        }
    }

    @Override
    public List<ObservedSnapshot> snapshots(String runId) {
        Objects.requireNonNull(runId, "runId must not be null");
        List<ObservedSnapshot> recorded = buffer.get(runId);
        if (recorded == null) {
            return Collections.emptyList();
        }
        return List.copyOf(recorded);
    }

    @Override
    public Set<String> runIds() {
        return Set.copyOf(buffer.keySet());
    }

    // ----- Session management -----

    public RecordingSession startSession(String runId, ManagedExecutorConfig config) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(config, "config must not be null");
        RecordingSession existing = sessions.get(runId);
        if (existing != null && existing.status() == SessionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "active session already exists for runId=" + runId);
        }
        String sessionId = UUID.randomUUID().toString();
        RecordingSession session = new RecordingSession(sessionId, runId,
                config.corePoolSize(), config.maximumPoolSize(),
                config.queueCapacity(), config.keepAliveTime(),
                config.keepAliveTimeUnit().name());
        sessions.put(runId, session);
        return session;
    }

    public RecordingSessionMetadata closeSession(String runId) {
        Objects.requireNonNull(runId, "runId must not be null");
        RecordingSession session = sessions.get(runId);
        if (session == null) {
            throw new IllegalStateException("no session for runId=" + runId);
        }
        RecordingSessionMetadata metadata = session.close();
        flush(runId);
        writeSessionMetadata(metadata);
        return metadata;
    }

    public void flush(String runId) {
        // record() writes immediately; flush is a no-op reserved for future batching
    }

    // ----- Private helpers -----

    private String renderSnapshot(ObservedSnapshot snapshot) {
        Map<String, Object> map = snapshot.toMap();
        return AcquisitionJsonWriter.renderCompact(map);
    }

    private void writeSessionMetadata(RecordingSessionMetadata metadata) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("sessionId", metadata.sessionId());
        map.put("runId", metadata.runId());
        map.put("executorConfig", executorConfigToMap(metadata));
        map.put("startedAt", metadata.startedAt().toString());
        map.put("closedAt", metadata.closedAt().toString());
        map.put("snapshotCount", metadata.snapshotCount());
        map.put("status", metadata.status().name());
        String json = AcquisitionJsonWriter.render(map);
        try {
            Files.writeString(sessionMetadataPath(metadata.runId()), json);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to write session metadata for runId=" + metadata.runId(), e);
        }
    }

    private Map<String, Object> executorConfigToMap(RecordingSessionMetadata metadata) {
        Map<String, Object> map = AcquisitionJsonWriter.map();
        map.put("corePoolSize", metadata.corePoolSize());
        map.put("maximumPoolSize", metadata.maximumPoolSize());
        map.put("queueCapacity", metadata.queueCapacity());
        map.put("keepAliveTime", metadata.keepAliveTime());
        map.put("keepAliveTimeUnit", metadata.keepAliveTimeUnit());
        return map;
    }

    private Path evidenceFilePath(String runId) {
        return outputDir.resolve(AcquisitionReportPaths.evidenceFileName(runId));
    }

    private Path sessionMetadataPath(String runId) {
        return outputDir.resolve(AcquisitionReportPaths.sessionMetadataFileName(runId));
    }
}
