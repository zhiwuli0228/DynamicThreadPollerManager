package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;

import java.time.Instant;
import java.util.Objects;

/**
 * A single coordination record: command submitted, result produced,
 * and budget snapshots before and after.
 */
public record GroupCoordinationEntry(
        String executorId,
        ScaleAdjustmentCommand command,
        GroupCoordinationResult result,
        ResourceBudget budgetBefore,
        ResourceBudget budgetAfter,
        Instant timestamp) {

    public GroupCoordinationEntry {
        Objects.requireNonNull(executorId, "executorId must not be null");
        if (executorId.isBlank()) {
            throw new IllegalArgumentException("executorId must not be blank");
        }
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(budgetBefore, "budgetBefore must not be null");
        Objects.requireNonNull(budgetAfter, "budgetAfter must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
