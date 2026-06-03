package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

/**
 * Threshold-based {@link PolicyEvaluator}.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Scale up when active threads &ge; scaleUpActiveThreadsThreshold
 *       or queue size &ge; scaleUpQueueSizeThreshold.</li>
 *   <li>Otherwise scale down when active threads &le; scaleDownActiveThreadsThreshold
 *       and queue size equals 0.</li>
 *   <li>Otherwise hold at the current pool size.</li>
 * </ol>
 * If both scale-up and scale-down conditions appear true, scale-up wins
 * so high queue pressure cannot be hidden by a low active-thread count.
 *
 * <p>The decision timestamp is always the input's {@code evaluatedAt};
 * this evaluator never reads wall-clock time.
 */
public final class ThresholdPolicyEvaluator implements PolicyEvaluator {

    private final ControlGate gate;

    public ThresholdPolicyEvaluator() {
        this(new DefaultControlGate());
    }

    public ThresholdPolicyEvaluator(ControlGate gate) {
        this.gate = gate;
    }

    @Override
    public PolicyDecision evaluate(PolicyEvaluationInput input, ThresholdPolicyConfig config) {
        PressureSnapshot snapshot = input.snapshot();
        int currentPoolSize = snapshot.poolSize();
        int activeThreads = snapshot.activeThreads();
        int queueSize = snapshot.queueSize();

        if (shouldScaleUp(activeThreads, queueSize, config)) {
            String reason = scaleUpReason(activeThreads, queueSize, config);
            return gate.apply(input, config, PolicyAction.SCALE_UP,
                    currentPoolSize + config.scaleStep(), reason);
        }

        if (shouldScaleDown(activeThreads, queueSize, config)) {
            String reason = "Scale down triggered by low active threads " + activeThreads
                    + " and empty queue";
            return gate.apply(input, config, PolicyAction.SCALE_DOWN,
                    currentPoolSize - config.scaleStep(), reason);
        }

        String reason = "Normal pressure: active threads " + activeThreads
                + ", queue size " + queueSize;
        return gate.apply(input, config, PolicyAction.HOLD, currentPoolSize, reason);
    }

    private static boolean shouldScaleUp(int activeThreads, int queueSize, ThresholdPolicyConfig config) {
        return activeThreads >= config.scaleUpActiveThreadsThreshold()
                || queueSize >= config.scaleUpQueueSizeThreshold();
    }

    private static boolean shouldScaleDown(int activeThreads, int queueSize, ThresholdPolicyConfig config) {
        return activeThreads <= config.scaleDownActiveThreadsThreshold() && queueSize == 0;
    }

    private static String scaleUpReason(int activeThreads, int queueSize, ThresholdPolicyConfig config) {
        boolean activeTriggered = activeThreads >= config.scaleUpActiveThreadsThreshold();
        boolean queueTriggered = queueSize >= config.scaleUpQueueSizeThreshold();
        if (activeTriggered && queueTriggered) {
            return "Scale up triggered by active threads " + activeThreads
                    + " (threshold " + config.scaleUpActiveThreadsThreshold()
                    + ") and queue size " + queueSize
                    + " (threshold " + config.scaleUpQueueSizeThreshold() + ")";
        }
        if (activeTriggered) {
            return "Scale up triggered by active threads " + activeThreads
                    + " (threshold " + config.scaleUpActiveThreadsThreshold() + ")";
        }
        return "Scale up triggered by queue size " + queueSize
                + " (threshold " + config.scaleUpQueueSizeThreshold() + ")";
    }
}
