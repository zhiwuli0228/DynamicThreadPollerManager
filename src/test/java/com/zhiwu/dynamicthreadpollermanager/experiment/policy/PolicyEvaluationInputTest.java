package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyEvaluationInputTest {

    @Test
    void shouldExposeRunIdSnapshotAndEvaluatedAt() {
        Instant now = Instant.parse("2026-06-04T10:00:00Z");
        PressureSnapshot snapshot = new PressureSnapshot(now, 8, 16, 4, 100L, 0.5);
        PolicyEvaluationInput input = new PolicyEvaluationInput("run-1", snapshot, now);

        assertEquals("run-1", input.runId());
        assertSame(snapshot, input.snapshot());
        assertEquals(now, input.evaluatedAt());
    }

    @Test
    void shouldRejectBlankRunId() {
        PressureSnapshot snapshot = new PressureSnapshot(Instant.parse("2026-06-04T10:00:00Z"),
                1, 1, 0, 0L, 0.0);
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyEvaluationInput(null, snapshot, Instant.parse("2026-06-04T10:00:00Z")));
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyEvaluationInput("", snapshot, Instant.parse("2026-06-04T10:00:00Z")));
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyEvaluationInput("   ", snapshot, Instant.parse("2026-06-04T10:00:00Z")));
    }

    @Test
    void shouldRejectNullSnapshot() {
        assertThrows(NullPointerException.class,
                () -> new PolicyEvaluationInput("run-1", null, Instant.parse("2026-06-04T10:00:00Z")));
    }

    @Test
    void shouldRejectNullEvaluatedAt() {
        PressureSnapshot snapshot = new PressureSnapshot(Instant.parse("2026-06-04T10:00:00Z"),
                1, 1, 0, 0L, 0.0);
        assertThrows(NullPointerException.class,
                () -> new PolicyEvaluationInput("run-1", snapshot, null));
    }
}
