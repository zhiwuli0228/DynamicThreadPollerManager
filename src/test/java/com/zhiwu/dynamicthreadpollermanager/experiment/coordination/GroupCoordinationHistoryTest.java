package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class GroupCoordinationHistoryTest {

    private GroupCoordinationHistory history;
    private final Supplier<Instant> clock = Instant::now;

    @BeforeEach
    void setUp() {
        history = new GroupCoordinationHistory();
    }

    @Test
    void shouldStartEmpty() {
        assertTrue(history.isEmpty());
        assertEquals(0, history.totalCoordinationCount());
    }

    @Test
    void shouldRecordAndRetrieveEntries() {
        recordEntry("exec-A", 5, 8, CoordinationOutcome.APPROVED_AS_IS);
        recordEntry("exec-B", 3, 6, CoordinationOutcome.REJECTED);

        assertEquals(2, history.totalCoordinationCount());
        assertFalse(history.isEmpty());
        assertEquals(2, history.recent(5).size());
    }

    @Test
    void recentShouldRespectCount() {
        for (int i = 0; i < 10; i++) {
            recordEntry("exec-" + i, 5, 8, CoordinationOutcome.APPROVED_AS_IS);
        }
        assertEquals(3, history.recent(3).size());
        assertEquals(10, history.recent(20).size());
    }

    @Test
    void byExecutorShouldFilter() {
        recordEntry("exec-A", 5, 8, CoordinationOutcome.APPROVED_AS_IS);
        recordEntry("exec-A", 8, 6, CoordinationOutcome.APPROVED_AS_IS);
        recordEntry("exec-B", 3, 7, CoordinationOutcome.REJECTED);

        List<GroupCoordinationEntry> aEntries = history.byExecutor("exec-A");
        assertEquals(2, aEntries.size());
        assertEquals(1, history.byExecutor("exec-B").size());
        assertEquals(0, history.byExecutor("exec-C").size());
    }

    @Test
    void rejectedCountShouldBeCorrect() {
        recordEntry("exec-A", 5, 8, CoordinationOutcome.APPROVED_AS_IS);
        recordEntry("exec-B", 3, 7, CoordinationOutcome.REJECTED);
        recordEntry("exec-C", 4, 6, CoordinationOutcome.REJECTED);

        assertEquals(2, history.rejectedCount());
    }

    @Test
    void modifiedCountShouldIncludeCapped() {
        recordEntry("exec-A", 5, 10, CoordinationOutcome.MODIFIED);
        recordEntry("exec-B", 3, 7, CoordinationOutcome.CAPPED);
        recordEntry("exec-C", 4, 6, CoordinationOutcome.APPROVED_AS_IS);

        assertEquals(2, history.modifiedCount());
    }

    @Test
    void preemptionCountShouldCountConflicts() {
        ResourceBudget budget = new ResourceBudget(20, 0);
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, CoordinationOutcome.MODIFIED,
                "test", List.of("exec-B:3"), false, clock.get());
        history.record(new GroupCoordinationEntry(
                "exec-A", cmd, result, budget.snapshot(), budget.snapshot(), clock.get()));

        assertEquals(1, history.preemptionCount());
    }

    @Test
    void clearShouldReset() {
        recordEntry("exec-A", 5, 8, CoordinationOutcome.APPROVED_AS_IS);
        recordEntry("exec-B", 3, 6, CoordinationOutcome.REJECTED);
        history.clear();
        assertTrue(history.isEmpty());
        assertEquals(0, history.totalCoordinationCount());
    }

    private void recordEntry(String executorId, int current, int target,
                             CoordinationOutcome outcome) {
        ResourceBudget budget = new ResourceBudget(20, 0);
        ScaleAdjustmentCommand cmd = makeCommand(current, target);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, outcome, "test", List.of(), false, clock.get());
        history.record(new GroupCoordinationEntry(
                executorId, cmd, result, budget.snapshot(), budget.snapshot(), clock.get()));
    }

    private ScaleAdjustmentCommand makeCommand(int current, int target) {
        return ScaleAdjustmentCommand.create(
                "run-1", clock.get(), current, target,
                "test reason", "test-ref", clock);
    }
}
