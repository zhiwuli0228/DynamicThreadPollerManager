package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

/**
 * Scores a policy configuration against a pressure classification.
 * Implementations must guarantee compositeScore = weighted sum of dimension scores.
 */
public interface PolicyScorer {

    PolicyScore score(PressureClassification classification, ThresholdPolicyConfig config);
}
