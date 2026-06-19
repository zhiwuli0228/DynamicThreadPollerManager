package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceBudgetTest {

    private ResourceBudget budget;

    @BeforeEach
    void setUp() {
        budget = new ResourceBudget(10, 0);
    }

    @Test
    void shouldStartWithFullAvailability() {
        assertEquals(10, budget.availableThreads());
        assertEquals(0, budget.totalAllocatedThreads());
    }

    @Test
    void reserveShouldReduceAvailability() {
        budget.reserve("exec-A", 3);
        assertEquals(7, budget.availableThreads());
        assertEquals(3, budget.allocatedThreads("exec-A"));
        assertEquals(3, budget.totalAllocatedThreads());
    }

    @Test
    void reserveShouldRejectOverAllocation() {
        budget.reserve("exec-A", 8);
        assertThrows(IllegalStateException.class, () -> budget.reserve("exec-B", 5));
    }

    @Test
    void releaseShouldIncreaseAvailability() {
        budget.reserve("exec-A", 6);
        budget.release("exec-A", 2);
        assertEquals(6, budget.availableThreads());
        assertEquals(4, budget.allocatedThreads("exec-A"));
    }

    @Test
    void releaseShouldRemoveEntryWhenZero() {
        budget.reserve("exec-A", 3);
        budget.release("exec-A", 3);
        assertEquals(10, budget.availableThreads());
        assertEquals(0, budget.allocatedThreads("exec-A"));
    }

    @Test
    void negativeReserveShouldWorkAsRelease() {
        budget.reserve("exec-A", 5);
        budget.reserve("exec-A", -3);
        assertEquals(8, budget.availableThreads());
        assertEquals(2, budget.allocatedThreads("exec-A"));
    }

    @Test
    void multipleExecutorsShouldTrackIndependently() {
        budget.reserve("exec-A", 3);
        budget.reserve("exec-B", 4);
        assertEquals(3, budget.allocatedThreads("exec-A"));
        assertEquals(4, budget.allocatedThreads("exec-B"));
        assertEquals(3, budget.availableThreads());
        assertEquals(7, budget.totalAllocatedThreads());
    }

    @Test
    void snapshotShouldPreserveState() {
        budget.reserve("exec-A", 3);
        budget.reserve("exec-B", 2);
        ResourceBudget snapshot = budget.snapshot();
        assertEquals(5, snapshot.totalAllocatedThreads());
        assertEquals(3, snapshot.allocatedThreads("exec-A"));

        // Modifying original should not affect snapshot
        budget.release("exec-A", 1);
        assertEquals(3, snapshot.allocatedThreads("exec-A"));
        assertEquals(2, budget.allocatedThreads("exec-A"));
    }

    @Test
    void getThreadAllocationsShouldReturnImmutableCopy() {
        budget.reserve("exec-A", 3);
        var allocations = budget.getThreadAllocations();
        assertThrows(UnsupportedOperationException.class, () -> allocations.put("exec-B", 1));
    }

    @Test
    void invariantsShouldHoldUnderSequentialOps() {
        for (int i = 0; i < 100; i++) {
            budget.reserve("exec-A", 1);
            budget.release("exec-A", 1);
        }
        assertEquals(10, budget.availableThreads());
        assertEquals(0, budget.totalAllocatedThreads());
        assertTrue(budget.availableThreads() >= 0);
        assertTrue(budget.totalAllocatedThreads() <= 10);
    }

    @Test
    void shouldRejectNegativeAmountRelease() {
        assertThrows(IllegalArgumentException.class, () -> budget.release("exec-A", -1));
    }
}
