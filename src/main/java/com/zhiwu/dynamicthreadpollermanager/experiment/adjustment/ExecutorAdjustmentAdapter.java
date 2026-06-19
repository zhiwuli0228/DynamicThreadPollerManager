package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

/**
 * Runtime adapter contract. The adapter is the only place where
 * pool size mutations are applied. Safety gate evaluation must
 * happen before {@link #apply(ScaleAdjustmentCommand)}; the adapter
 * itself does not run gate logic. The adapter MUST NOT instantiate
 * or integrate a production Java standard library thread pool
 * implementation in the first bounded change.
 */
public interface ExecutorAdjustmentAdapter {

    /**
     * Return a snapshot of the controlled executor state. The
     * snapshot is for audit and gate input; it does not perform any
     * mutation.
     */
    ExecutorStateSnapshot currentState();

    /**
     * Apply the requested pool size adjustment. The adapter
     * returns a structured {@link AdjustmentResult}; it MUST NOT
     * throw an unclassified exception. Any runtime failure is
     * captured as a {@link AdjustmentStatus#FAILED} result.
     */
    AdjustmentResult apply(ScaleAdjustmentCommand command);
}
