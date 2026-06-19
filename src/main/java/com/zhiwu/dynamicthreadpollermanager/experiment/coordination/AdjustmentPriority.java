package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

/**
 * Priority level for resource allocation precedence among executors
 * in a coordinated group. Higher levels can preempt lower levels
 * when the shared resource budget is exhausted.
 */
public enum AdjustmentPriority {
    CRITICAL(4),
    HIGH(3),
    NORMAL(2),
    LOW(1);

    private final int level;

    AdjustmentPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean canPreempt(AdjustmentPriority other) {
        return this.level > other.level;
    }
}
