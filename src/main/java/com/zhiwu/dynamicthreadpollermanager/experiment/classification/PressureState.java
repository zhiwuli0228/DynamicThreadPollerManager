package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

/**
 * Six semantic pressure states in classification priority order.
 * Declaration order is the priority chain: REJECTION_ACTIVE is highest.
 */
public enum PressureState {

    REJECTION_ACTIVE("任务正被拒绝——最近窗口内检测到拒绝事件"),
    OVERLOAD("线程饱和，队列深度高——系统处于过载状态"),
    QUEUE_BUILDUP("队列持续增长中，线程尚未完全饱和——前兆状态"),
    RECOVERY("队列和线程均在下降——系统从高压力状态恢复中"),
    UNDER_UTILIZED("线程空闲，队列为空——资源利用不足"),
    NORMAL("平衡状态——线程工作中，队列在可管理范围内");

    private final String description;

    PressureState(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
