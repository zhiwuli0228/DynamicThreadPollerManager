package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotPressureClassifierTest {

    private SnapshotPressureClassifier classifier;
    private ClassifierConfig config;

    @BeforeEach
    void setUp() {
        classifier = new SnapshotPressureClassifier();
        config = ClassifierConfig.defaults();
    }

    private static ObservedSnapshot snapshot(
            int activeThreads, int poolSize, int queueSize, long completed) {
        Instant now = Instant.now();
        PressureSnapshot ps = new PressureSnapshot(
                now, activeThreads, poolSize, queueSize, completed, 0.0);
        RuntimeObservation obs = new RuntimeObservation(
                now,
                MetricValue.present(activeThreads),
                MetricValue.present(queueSize),
                MetricValue.absent());
        return new ObservedSnapshot("run-1", ps, obs);
    }

    @Test
    void shouldClassifyUnderUtilized() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L));

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 5000L);

        assertEquals(PressureState.UNDER_UTILIZED, c.state());
        assertTrue(c.confidence() > 0.7, "confidence was " + c.confidence());
    }

    @Test
    void shouldClassifyNormal() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(4, 8, 3, 10L),
                snapshot(4, 8, 3, 20L),
                snapshot(4, 8, 3, 30L),
                snapshot(4, 8, 3, 40L),
                snapshot(4, 8, 3, 50L));

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 5000L);

        assertEquals(PressureState.NORMAL, c.state());
    }

    @Test
    void shouldClassifyQueueBuildup() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(3, 8, 2, 10L),
                snapshot(3, 8, 4, 20L),
                snapshot(3, 8, 6, 30L),
                snapshot(3, 8, 8, 40L),
                snapshot(3, 8, 10, 50L));

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 5000L);

        assertEquals(PressureState.QUEUE_BUILDUP, c.state());
    }

    @Test
    void shouldClassifyOverload() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(7, 8, 12, 10L),
                snapshot(7, 8, 13, 20L),
                snapshot(8, 8, 14, 30L),
                snapshot(8, 8, 15, 40L),
                snapshot(8, 8, 15, 50L));

        PressureClassification c = classifier.classify(
                snapshots, new ClassifierConfig(5, 0.1, 10, 20), 0L, 5000L);

        assertEquals(PressureState.OVERLOAD, c.state());
    }

    @Test
    void shouldClassifyRejectionActive() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(4, 8, 5, 10L),
                snapshot(4, 8, 5, 20L),
                snapshot(4, 8, 5, 30L),
                snapshot(4, 8, 5, 40L),
                snapshot(4, 8, 5, 50L));

        PressureClassification c = classifier.classify(
                snapshots, config, 3L, 5000L);

        assertEquals(PressureState.REJECTION_ACTIVE, c.state());
        assertTrue(c.confidence() >= 0.9,
                "rejection confidence should be high, was " + c.confidence());
    }

    @Test
    void shouldClassifyRecovery() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(5, 8, 10, 10L),
                snapshot(4, 8, 8, 20L),
                snapshot(3, 8, 6, 30L),
                snapshot(2, 8, 4, 40L),
                snapshot(1, 8, 2, 50L));

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 5000L);

        assertEquals(PressureState.RECOVERY, c.state());
    }

    @Test
    void shouldReturnNormalForEmptySnapshots() {
        PressureClassification c = classifier.classify(
                List.of(), config, 0L, 0L);

        assertEquals(PressureState.NORMAL, c.state());
        assertEquals(0.0, c.confidence());
    }

    @Test
    void shouldReduceConfidenceForShortSequence() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L));

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 2000L);

        assertTrue(c.confidence() < 0.5,
                "short sequence confidence should be reduced, was "
                        + c.confidence());
    }

    @Test
    void shouldNotReduceConfidenceForFullSequence() {
        List<ObservedSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            snapshots.add(snapshot(0, 4, 0, 0L));
        }

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 10000L);

        assertTrue(c.confidence() > 0.7,
                "full sequence confidence should not be reduced, was "
                        + c.confidence());
    }

    @Test
    void shouldOverloadTakePriorityOverQueueBuildup() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(7, 8, 5, 10L),
                snapshot(8, 8, 7, 20L),
                snapshot(8, 8, 9, 30L),
                snapshot(8, 8, 11, 40L),
                snapshot(8, 8, 13, 50L));

        PressureClassification c = classifier.classify(
                snapshots, new ClassifierConfig(5, 0.1, 10, 20), 0L, 5000L);

        assertEquals(PressureState.OVERLOAD, c.state(),
                "OVERLOAD should take priority over QUEUE_BUILDUP when "
                + "utilization >= 0.8 and queue >= capacity*0.5");
    }

    @Test
    void shouldRejectionActiveTakePriorityOverAll() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(8, 8, 15, 10L),
                snapshot(8, 8, 15, 20L),
                snapshot(8, 8, 15, 30L),
                snapshot(8, 8, 15, 40L),
                snapshot(8, 8, 15, 50L));

        PressureClassification c = classifier.classify(
                snapshots, new ClassifierConfig(5, 0.1, 10, 20), 5L, 5000L);

        assertEquals(PressureState.REJECTION_ACTIVE, c.state());
    }

    @Test
    void shouldContainEvidenceInClassification() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L),
                snapshot(0, 4, 0, 0L));

        PressureClassification c = classifier.classify(
                snapshots, config, 0L, 5000L);

        assertNotNull(c.evidence());
        assertFalse(c.evidence().isEmpty());
        assertNotNull(c.classifiedAt());
        assertNotNull(c.metrics());
    }
}
