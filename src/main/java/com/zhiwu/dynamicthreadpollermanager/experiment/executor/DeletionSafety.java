package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

public interface DeletionSafety {

    void acquire(String executorName);

    void release(String executorName);

    int referenceCount(String executorName);

    boolean canRemove(String executorName, ExecutorRegistry registry);
}
