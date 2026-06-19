package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentFailureCode;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.InMemoryAdjustableExecutorProbe;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CoordinatedAdjustmentAdapterTest {

    private ResourceBudget budget;
    private GroupCoordinationHistory history;
    private GroupCoordinator coordinator;
    private InMemoryAdjustableExecutorProbe delegate;
    private CoordinatedAdjustmentAdapter adapter;
    private final Supplier<Instant> clock = Instant::now;

    @BeforeEach
    void setUp() {
        budget = new ResourceBudget(3, 0);
        history = new GroupCoordinationHistory();

        delegate = new InMemoryAdjustableExecutorProbe(2, 10, 10, clock);

        // exec-B is just needed so GroupCoordinator has a preemption target
        InMemoryAdjustableExecutorProbe probeB = new InMemoryAdjustableExecutorProbe(
                1, 10, 10, clock);

        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", (ExecutorAdjustmentAdapter) delegate,
                "exec-B", (ExecutorAdjustmentAdapter) probeB);

        budget.reserve("exec-A", 2);
        budget.reserve("exec-B", 1); // at minimum, cannot be preempted

        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "test-group", 3, 0, AdjustmentPriority.NORMAL,
                Map.of("exec-A", AdjustmentPriority.CRITICAL,
                        "exec-B", AdjustmentPriority.LOW),
                5000, false);

        coordinator = new GroupCoordinator(
                config, budget, history,
                new CrossExecutorOscillationDetector(4),
                adapters, clock);

        adapter = new CoordinatedAdjustmentAdapter(
                delegate, coordinator, "exec-A", clock);
    }

    @Test
    void shouldDelegateCurrentState() {
        ExecutorStateSnapshot state = adapter.currentState();
        assertEquals(2, state.corePoolSize());
        assertEquals(10, state.maximumPoolSize());
    }

    @Test
    void shouldReturnRejectedWhenCoordinatorRejects() {
        // Budget fully allocated (exec-A=2, exec-B=1, total=3/3)
        // exec-B is at minimum (1), cannot be preempted
        // exec-A wants 2→8 (delta=6), available=0, no preemption → REJECTED
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                "run-1", clock.get(), 2, 8,
                "scale up", "test-ref", clock);

        AdjustmentResult result = adapter.apply(cmd);

        assertEquals(AdjustmentStatus.REJECTED, result.status());
        assertEquals(AdjustmentFailureCode.COORDINATION_REJECTED, result.failureCode());
    }

    @Test
    void shouldApplyCappedCommand() {
        // Clear then re-setup: exec-A=2, exec-B allocates 2 so available= -1 would fail
        // Instead: increase budget, then allocate so exec-A has room but not enough
        budget = new ResourceBudget(10, 0);
        delegate = new InMemoryAdjustableExecutorProbe(2, 10, 10, clock);

        InMemoryAdjustableExecutorProbe probeB = new InMemoryAdjustableExecutorProbe(
                2, 10, 10, clock);

        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", (ExecutorAdjustmentAdapter) delegate,
                "exec-B", (ExecutorAdjustmentAdapter) probeB);

        budget.reserve("exec-A", 2);
        budget.reserve("exec-B", 7); // exec-B=7, available=1

        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "test-group", 10, 0, AdjustmentPriority.NORMAL,
                Map.of("exec-A", AdjustmentPriority.CRITICAL,
                        "exec-B", AdjustmentPriority.LOW),
                5000, false);

        coordinator = new GroupCoordinator(
                config, budget, history,
                new CrossExecutorOscillationDetector(4),
                adapters, clock);

        adapter = new CoordinatedAdjustmentAdapter(
                delegate, coordinator, "exec-A", clock);

        // exec-A wants 2→12 (delta=10), available=1, preempt max 6 from exec-B (7-1)
        // collected=6 < delta=10 → CAPPED to 2+6=8
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                "run-1", clock.get(), 2, 12,
                "big scale up", "test-ref", clock);

        AdjustmentResult result = adapter.apply(cmd);

        assertNotEquals(AdjustmentStatus.REJECTED, result.status());
        assertEquals(8, result.appliedPoolSize());
    }

    @Test
    void shouldApplyApprovedCommand() {
        // exec-A=2/3, available=0 (exec-B=1) → scale-down doesn't need budget
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                "run-1", clock.get(), 2, 1,
                "scale down", "test-ref", clock);

        AdjustmentResult result = adapter.apply(cmd);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(1, result.appliedPoolSize());
    }

    @Test
    void shouldReleaseBudgetOnScaleDown() {
        // Reset budget with more room
        budget = new ResourceBudget(10, 0);
        budget.reserve("exec-A", 5);
        budget.reserve("exec-B", 1);

        InMemoryAdjustableExecutorProbe probeB = new InMemoryAdjustableExecutorProbe(
                1, 10, 10, clock);

        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", (ExecutorAdjustmentAdapter) delegate,
                "exec-B", (ExecutorAdjustmentAdapter) probeB);

        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "test-group", 10, 0, AdjustmentPriority.NORMAL,
                Map.of("exec-A", AdjustmentPriority.CRITICAL,
                        "exec-B", AdjustmentPriority.LOW),
                5000, false);

        coordinator = new GroupCoordinator(
                config, budget, history,
                new CrossExecutorOscillationDetector(4),
                adapters, clock);

        delegate = new InMemoryAdjustableExecutorProbe(5, 10, 10, clock);
        adapter = new CoordinatedAdjustmentAdapter(
                delegate, coordinator, "exec-A", clock);

        int beforeAvailable = budget.availableThreads();
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                "run-1", clock.get(), 5, 3,
                "scale down", "test-ref", clock);

        AdjustmentResult result = adapter.apply(cmd);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(beforeAvailable + 2, budget.availableThreads());
    }

    @Test
    void shouldNotReleaseBudgetOnScaleUp() {
        budget = new ResourceBudget(10, 0);
        budget.reserve("exec-A", 2);
        budget.reserve("exec-B", 1);

        InMemoryAdjustableExecutorProbe probeB = new InMemoryAdjustableExecutorProbe(
                1, 10, 10, clock);

        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", (ExecutorAdjustmentAdapter) delegate,
                "exec-B", (ExecutorAdjustmentAdapter) probeB);

        ExecutorGroupConfig config = new ExecutorGroupConfig(
                "test-group", 10, 0, AdjustmentPriority.NORMAL,
                Map.of("exec-A", AdjustmentPriority.CRITICAL,
                        "exec-B", AdjustmentPriority.LOW),
                5000, false);

        coordinator = new GroupCoordinator(
                config, budget, history,
                new CrossExecutorOscillationDetector(4),
                adapters, clock);

        delegate = new InMemoryAdjustableExecutorProbe(2, 10, 10, clock);
        adapter = new CoordinatedAdjustmentAdapter(
                delegate, coordinator, "exec-A", clock);

        int beforeAvailable = budget.availableThreads();
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                "run-1", clock.get(), 2, 5,
                "scale up with budget", "test-ref", clock);

        AdjustmentResult result = adapter.apply(cmd);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(beforeAvailable - 3, budget.availableThreads());
    }
}
