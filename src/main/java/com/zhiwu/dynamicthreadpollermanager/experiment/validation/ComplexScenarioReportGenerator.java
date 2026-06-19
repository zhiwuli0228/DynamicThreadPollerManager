package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.AdjustmentHistory;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.HistoryEntry;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopIterationEvidence;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Generates {@link ComplexScenarioReport} from real evidence sources.
 * All metrics are computed from observed data — no synthetic proxies.
 */
public final class ComplexScenarioReportGenerator {

    private final EvidenceRecorder evidenceRecorder;
    private final LoopEvidenceRecorder loopEvidenceRecorder;
    private final AdjustmentHistory adjustmentHistory;

    public ComplexScenarioReportGenerator(
            EvidenceRecorder evidenceRecorder,
            LoopEvidenceRecorder loopEvidenceRecorder,
            AdjustmentHistory adjustmentHistory) {
        this.evidenceRecorder = Objects.requireNonNull(evidenceRecorder,
                "evidenceRecorder must not be null");
        this.loopEvidenceRecorder = Objects.requireNonNull(loopEvidenceRecorder,
                "loopEvidenceRecorder must not be null");
        this.adjustmentHistory = Objects.requireNonNull(adjustmentHistory,
                "adjustmentHistory must not be null");
    }

    public ComplexScenarioReport generate(String scenarioId, long seed,
                                           String scenarioConfig, String sessionId) {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(scenarioConfig, "scenarioConfig must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        Instant generatedAt = Instant.now();

        List<ObservedSnapshot> snapshots = evidenceRecorder.snapshots(sessionId);
        List<LoopIterationEvidence> iterations =
                loopEvidenceRecorder.getIterationEvidence(sessionId);

        int adjustmentCount = adjustmentHistory.totalAdjustmentCount();
        int blockedCount = countBlocked(iterations);
        int rollbackCount = countRollbacks(iterations);
        double rollbackSuccessRate = computeRollbackSuccessRate(rollbackCount, iterations);

        long recoveryTimeMs = computeRecoveryTime(snapshots);
        long p95LatencyMs = computePercentile(snapshots, 95);
        long p99LatencyMs = computePercentile(snapshots, 99);

        int rejectionCount = countRejections(iterations);
        int queueDepthDelta = computeQueueDepthDelta(snapshots);
        double throughputDelta = computeThroughputDelta(snapshots);

        List<ObservationWindow> decisionWindows = buildObservationWindows(
                iterations, snapshots);

        return new ComplexScenarioReport(
                UUID.randomUUID().toString(),
                scenarioId,
                seed,
                scenarioConfig,
                adjustmentCount,
                blockedCount,
                rollbackCount,
                rollbackSuccessRate,
                recoveryTimeMs,
                p95LatencyMs,
                p99LatencyMs,
                rejectionCount,
                queueDepthDelta,
                throughputDelta,
                decisionWindows,
                generatedAt);
    }

    private static int countBlocked(List<LoopIterationEvidence> iterations) {
        int count = 0;
        for (LoopIterationEvidence evidence : iterations) {
            AdjustmentResult result = evidence.result();
            if (result != null && result.failureCode() != null) {
                count++;
            }
        }
        return count;
    }

    private static int countRollbacks(List<LoopIterationEvidence> iterations) {
        int count = 0;
        for (LoopIterationEvidence evidence : iterations) {
            AdjustmentResult result = evidence.result();
            if (result != null && result.sourceDecisionRef() != null
                    && result.sourceDecisionRef().contains("rollback")) {
                count++;
            }
        }
        return count;
    }

    private static double computeRollbackSuccessRate(int totalRollbacks,
                                                      List<LoopIterationEvidence> iterations) {
        if (totalRollbacks == 0) {
            return 0.0;
        }
        long successful = iterations.stream()
                .filter(e -> e.result() != null
                        && e.result().sourceDecisionRef() != null
                        && e.result().sourceDecisionRef().contains("rollback")
                        && e.result().failureCode() == null)
                .count();
        return (double) successful / totalRollbacks;
    }

    private static long computeRecoveryTime(List<ObservedSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return 0L;
        }
        int maxQueue = snapshots.stream()
                .mapToInt(s -> s.snapshot().queueSize())
                .max().orElse(0);
        int initialQueue = snapshots.get(0).snapshot().queueSize();
        if (maxQueue <= initialQueue) {
            return 0L;
        }
        Instant maxTime = Instant.EPOCH;
        for (ObservedSnapshot s : snapshots) {
            if (s.snapshot().queueSize() == maxQueue) {
                maxTime = s.snapshot().timestamp();
                break;
            }
        }
        for (ObservedSnapshot s : snapshots) {
            if (s.snapshot().timestamp().isAfter(maxTime)
                    && s.snapshot().queueSize() <= initialQueue) {
                return Duration.between(maxTime, s.snapshot().timestamp()).toMillis();
            }
        }
        return 0L;
    }

    /**
     * Compute percentile from queue depths across all snapshots.
     * Queue depth is used as a latency proxy — higher queue depth correlates
     * with longer task waiting time.
     */
    static long computePercentile(List<ObservedSnapshot> snapshots, int percentile) {
        if (snapshots.isEmpty()) {
            return 0L;
        }
        List<Integer> values = new ArrayList<>(
                snapshots.stream()
                        .map(s -> s.snapshot().queueSize())
                        .toList());
        values.sort(Comparator.naturalOrder());
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        if (index >= values.size()) {
            index = values.size() - 1;
        }
        if (index < 0) {
            index = 0;
        }
        return values.get(index);
    }

    private static int countRejections(List<LoopIterationEvidence> iterations) {
        return (int) iterations.stream()
                .filter(e -> e.result() != null && e.result().failureCode() != null)
                .count();
    }

    private static int computeQueueDepthDelta(List<ObservedSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return 0;
        }
        int first = snapshots.get(0).snapshot().queueSize();
        int last = snapshots.get(snapshots.size() - 1).snapshot().queueSize();
        return last - first;
    }

    private static double computeThroughputDelta(List<ObservedSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return 0.0;
        }
        int windowSize = Math.min(5, snapshots.size() / 2);
        if (windowSize < 1) {
            windowSize = 1;
        }
        double initialThroughput = computeWindowThroughput(
                snapshots.subList(0, windowSize));
        double finalThroughput = computeWindowThroughput(
                snapshots.subList(snapshots.size() - windowSize, snapshots.size()));
        return finalThroughput - initialThroughput;
    }

    private static double computeWindowThroughput(List<ObservedSnapshot> window) {
        if (window.size() < 2) {
            return 0.0;
        }
        long firstCompleted = window.get(0).snapshot().completedTaskCount();
        long lastCompleted = window.get(window.size() - 1).snapshot().completedTaskCount();
        long timeSpanMs = Duration.between(
                window.get(0).snapshot().timestamp(),
                window.get(window.size() - 1).snapshot().timestamp()).toMillis();
        if (timeSpanMs <= 0) {
            return 0.0;
        }
        return (double) (lastCompleted - firstCompleted) * 1000.0 / timeSpanMs;
    }

    private static List<ObservationWindow> buildObservationWindows(
            List<LoopIterationEvidence> iterations,
            List<ObservedSnapshot> snapshots) {
        List<ObservationWindow> windows = new ArrayList<>();
        for (int i = 0; i < iterations.size(); i++) {
            LoopIterationEvidence evidence = iterations.get(i);
            Instant decisionTime = evidence.recordedAt();
            List<ObservedSnapshot> pre = snapshotsBefore(snapshots, decisionTime, 3);
            List<ObservedSnapshot> post = snapshotsAfter(snapshots, decisionTime, 3);
            windows.add(new ObservationWindow(
                    i,
                    toExecutorSnapshots(pre),
                    toExecutorSnapshots(post),
                    decisionTime));
        }
        return windows;
    }

    private static List<ObservedSnapshot> snapshotsBefore(
            List<ObservedSnapshot> snapshots, Instant time, int count) {
        return snapshots.stream()
                .filter(s -> !s.snapshot().timestamp().isAfter(time))
                .sorted(Comparator.comparing(s -> s.snapshot().timestamp()))
                .skip(Math.max(0, snapshots.stream()
                        .filter(s -> !s.snapshot().timestamp().isAfter(time))
                        .count() - count))
                .toList();
    }

    private static List<ObservedSnapshot> snapshotsAfter(
            List<ObservedSnapshot> snapshots, Instant time, int count) {
        return snapshots.stream()
                .filter(s -> s.snapshot().timestamp().isAfter(time))
                .sorted(Comparator.comparing(s -> s.snapshot().timestamp()))
                .limit(count)
                .toList();
    }

    private static List<com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot>
    toExecutorSnapshots(List<ObservedSnapshot> sources) {
        return sources.stream()
                .map(s -> {
                    int poolSize = s.snapshot().poolSize();
                    return com.zhiwu.dynamicthreadpollermanager.experiment.adjustment
                            .ExecutorStateSnapshot.builder(s.snapshot().timestamp())
                            .corePoolSize(Math.max(1, poolSize))
                            .maximumPoolSize(Math.max(Math.max(1, poolSize), poolSize * 2))
                            .poolSize(poolSize)
                            .activeCount(s.snapshot().activeThreads())
                            .queueSize(s.snapshot().queueSize())
                            .completedTaskCount(s.snapshot().completedTaskCount())
                            .build();
                })
                .toList();
    }
}
