package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CrossExecutorOscillationDetectorTest {

    private CrossExecutorOscillationDetector detector;
    private GroupCoordinationHistory history;
    private final Supplier<Instant> clock = Instant::now;

    @BeforeEach
    void setUp() {
        detector = new CrossExecutorOscillationDetector(6);
        history = new GroupCoordinationHistory();
    }

    @Test
    void emptyHistoryShouldNotOscillate() {
        ScaleAdjustmentCommand cmd = makeCommand(2, 5);
        assertFalse(detector.wouldCrossOscillate(cmd, "exec-A", history));
    }

    @Test
    void insufficientHistoryShouldNotOscillate() {
        recordEntry("exec-A", 2, 5);
        ScaleAdjustmentCommand cmd = makeCommand(2, 5);
        assertFalse(detector.wouldCrossOscillate(cmd, "exec-A", history));
    }

    @Test
    void scaleDownShouldNotOscillate() {
        recordEntry("exec-A", 5, 8);
        recordEntry("exec-B", 8, 3);
        ScaleAdjustmentCommand cmd = makeCommand(5, 3); // scale-down
        assertFalse(detector.wouldCrossOscillate(cmd, "exec-B", history));
    }

    @Test
    void shouldDetectLockstepPattern() {
        recordEntry("exec-A", 2, 5); // up
        recordEntry("exec-B", 5, 2); // down
        recordEntry("exec-A", 5, 8); // up
        recordEntry("exec-B", 8, 5); // down

        ScaleAdjustmentCommand pending = makeCommand(5, 8); // exec-A up again
        assertTrue(detector.wouldCrossOscillate(pending, "exec-A", history));
    }

    @Test
    void normalAlternatingShouldNotFlag() {
        recordEntry("exec-A", 2, 5);
        recordEntry("exec-A", 5, 8); // same executor, not cross-executor
        recordEntry("exec-A", 8, 11);

        ScaleAdjustmentCommand pending = makeCommand(2, 5);
        assertFalse(detector.wouldCrossOscillate(pending, "exec-A", history));
    }

    @Test
    void detectedPatternShouldReturnDescription() {
        recordEntry("exec-A", 2, 5);
        recordEntry("exec-B", 5, 2);
        recordEntry("exec-A", 5, 8);
        recordEntry("exec-B", 8, 5);

        var pattern = detector.detectedPattern(history);
        assertTrue(pattern.isPresent());
    }

    @Test
    void emptyHistoryShouldReturnEmptyPattern() {
        assertTrue(detector.detectedPattern(history).isEmpty());
    }

    private void recordEntry(String executorId, int current, int target) {
        ResourceBudget budget = new ResourceBudget(20, 0);
        ScaleAdjustmentCommand cmd = makeCommand(current, target);
        GroupCoordinationResult result = new GroupCoordinationResult(
                cmd, cmd, CoordinationOutcome.APPROVED_AS_IS,
                "test", List.of(), false, clock.get());
        history.record(new GroupCoordinationEntry(
                executorId, cmd, result, budget.snapshot(), budget.snapshot(), clock.get()));
    }

    private ScaleAdjustmentCommand makeCommand(int current, int target) {
        return ScaleAdjustmentCommand.create(
                "run-1", clock.get(), current, target,
                "test", "test-ref", clock);
    }
}
