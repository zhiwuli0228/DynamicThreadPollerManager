package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class GroupCoordinationResultTest {

    private final Supplier<Instant> clock = Instant::now;

    @Test
    void approvedAsIsShouldBeApproved() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, CoordinationOutcome.APPROVED_AS_IS,
                "approved", List.of(), false, clock.get());
        assertTrue(result.isApproved());
        assertFalse(result.isRejected());
    }

    @Test
    void modifiedShouldBeApproved() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, CoordinationOutcome.MODIFIED,
                "preempted", List.of("other:3"), false, clock.get());
        assertTrue(result.isApproved());
        assertFalse(result.isRejected());
    }

    @Test
    void rejectedShouldNotBeApproved() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, CoordinationOutcome.REJECTED,
                "budget exhausted", List.of(), false, clock.get());
        assertFalse(result.isApproved());
        assertTrue(result.isRejected());
    }

    @Test
    void cappedShouldNotBeApproved() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        ScaleAdjustmentCommand capped = makeCommand(5, 6);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, capped, CoordinationOutcome.CAPPED,
                "capped", List.of(), false, clock.get());
        assertFalse(result.isApproved());
        assertFalse(result.isRejected());
    }

    @Test
    void shouldRejectBlankRationale() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        assertThrows(IllegalArgumentException.class, () ->
                new GroupCoordinationResult(cmd, cmd, CoordinationOutcome.APPROVED_AS_IS,
                        "  ", List.of(), false, clock.get()));
    }

    @Test
    void conflictsShouldBeImmutable() {
        ScaleAdjustmentCommand cmd = makeCommand(5, 8);
        List<String> conflicts = new java.util.ArrayList<>();
        conflicts.add("exec-A:3");
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, CoordinationOutcome.MODIFIED,
                "test", conflicts, false, clock.get());
        assertThrows(UnsupportedOperationException.class, () -> result.conflicts().add("exec-B:1"));
    }

    private ScaleAdjustmentCommand makeCommand(int current, int target) {
        return ScaleAdjustmentCommand.create(
                "run-1", clock.get(), current, target,
                "test reason", "test-ref", clock);
    }
}
