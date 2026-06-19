package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

public final class RuntimeSetting {

    public static final IntParameterBounds CORE_POOL_SIZE_BOUNDS =
            IntParameterBounds.of(1, Integer.MAX_VALUE);

    public static final IntParameterBounds MAX_POOL_SIZE_BOUNDS =
            IntParameterBounds.of(1, Integer.MAX_VALUE);

    public static final LongParameterBounds KEEP_ALIVE_TIME_BOUNDS =
            LongParameterBounds.of(0, Long.MAX_VALUE);

    private RuntimeSetting() {
    }
}
