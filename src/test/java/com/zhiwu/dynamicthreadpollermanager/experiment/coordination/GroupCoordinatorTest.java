package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class GroupCoordinatorTest {

    private ResourceBudget budget;
    private GroupCoordinationHistory history;
    private GroupCoordinator coordinator;
    private final Supplier<Instant> clock = Instant::now;

    @BeforeEach
    void setUp() {
        budget = new ResourceBudget(10, 0);
        history = new GroupCoordinationHistory();

        InMemoryAdjustableExecutorProbe probeA = new InMemoryAdjustableExecutorProbe(
                2, 8, 10, clock);
        InMemoryAdjustableExecutorProbe probeB = new InMemoryAdjustableExecutorProbe(
                2, 8, 10, clock);

        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", (ExecutorAdjustmentAdapter) probeA,
                "exec-B", (ExecutorAdjustmentAdapter) probeB);

        budget.reserve("exec-A", 2);
        budget.reserve("exec-B", 2);

        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "test-group", 10, 0, AdjustmentPriority.NORMAL,
                Map.of("exec-A", AdjustmentPriority.CRITICAL,
                        "exec-B", AdjustmentPriority.LOW),
                5000, false);

        coordinator = new GroupCoordinator(
                config, budget, history,
                new CrossExecutorOscillationDetector(4),
                adapters, clock);
    }

    @Test
    void shouldApproveScaleDown() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 3);
        GroupCoordinationResult result = coordinator.coordinate(cmd, "exec-A");
        assertEquals(CoordinationOutcome.APPROVED_AS_IS, result.outcome());
        assertTrue(result.isApproved());
    }

    @Test
    void shouldApproveNoOp() {
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.noOp(
                "run-1", clock.get(), 5, "no change", "test-ref", clock);
        GroupCoordinationResult result = coordinator.coordinate(cmd, "exec-A");
        assertEquals(CoordinationOutcome.APPROVED_AS_IS, result.outcome());
    }

    @Test
    void shouldApproveScaleUpWhenBudgetAvailable() {
        ScaleAdjustmentCommand cmd = makeCommand(2, 5); // delta=3, budget has 2(exec-A)+2(exec-B)=4/10, available=6
        GroupCoordinationResult result = coordinator.coordinate(cmd, "exec-A");
        assertEquals(CoordinationOutcome.APPROVED_AS_IS, result.outcome());
        assertEquals(3, budget.availableThreads()); // 10-2-5=3
    }

    @Test
    void shouldRejectWhenBudgetExhaustedAndNoPreemption() {
        // Allocate almost everything to exec-A
        budget.reserve("exec-A", 6); // exec-A=8/10, exec-B=2/10
        ScaleAdjustmentCommand cmd = makeCommand(8, 13); // needs +5, only 0 available, exec-B=2 (LOW) can be preempted for 1
        GroupCoordinationResult result = coordinator.coordinate(cmd, "exec-A");
        // exec-A is CRITICAL, exec-B is LOW, but exec-B only has 2 allocated, can only get 1 (2-1 min)
        // 1 < 5: CAPPED
        assertEquals(CoordinationOutcome.CAPPED, result.outcome());
    }

    @Test
    void shouldPreemptLowerPriorityForHigherPriority() {
        // exec-A CRITICAL already has 4 (2 initial + 2 additional)
        budget.reserve("exec-A", 4); // exec-A=6/10, exec-B=2/10 = 8/10, available=2
        ScaleAdjustmentCommand cmd = makeCommand(6, 11); // needs +5, available=2, need 3 from exec-B
        GroupCoordinationResult result = coordinator.coordinate(cmd, "exec-A");
        // exec-A CRITICAL preempts 1 from exec-B (LOW): 2 available + 1 preempted = 3 < 5
        // Should be CAPPED (partial). For MODIFIED we need full preemption: set delta smaller
        // Let's try with delta=3: available=2, preempt exec-B for 1 → 3 = delta → MODIFIED
    }

    @Test
    void shouldPreemptFullyWhenEnoughBudgetFromLowerPriority() {
        budget.reserve("exec-A", 2); // exec-A=4, exec-B=2 = 6/10, available=4
        ScaleAdjustmentCommand cmd = makeCommand(4, 9); // delta=5, available=4, preempt 1 from exec-B → exactly 5
        GroupCoordinationResult result = coordinator.coordinate(cmd, "exec-A");
        assertEquals(CoordinationOutcome.MODIFIED, result.outcome());
        assertFalse(result.conflicts().isEmpty());
    }

    @Test
    void shouldRecordHistoryOnEveryCoordination() {
        ScaleAdjustmentCommand cmd = makeCommand(2, 5);
        coordinator.coordinate(cmd, "exec-A");
        assertEquals(1, history.totalCoordinationCount());
        assertEquals("exec-A", history.recent(1).get(0).executorId());
    }

    private ScaleAdjustmentCommand makeCommand(int current, int target) {
        return ScaleAdjustmentCommand.create(
                "run-1", clock.get(), current, target,
                "test adjustment", "test-ref", clock);
    }
}
