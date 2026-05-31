package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.util.Objects;

/**
 * Immutable description of a control policy used to govern thread pool scaling decisions.
 */
public final class ControlPolicy {

    private final String policyId;
    private final String policyType;
    private final String description;

    public ControlPolicy(String policyId, String policyType, String description) {
        this.policyId = Objects.requireNonNull(policyId, "policyId must not be null");
        this.policyType = Objects.requireNonNull(policyType, "policyType must not be null");
        this.description = description;
    }

    public String policyId() {
        return policyId;
    }

    public String policyType() {
        return policyType;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ControlPolicy that && Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyId);
    }

    @Override
    public String toString() {
        return "ControlPolicy{policyId='%s', policyType='%s', description='%s'}"
                .formatted(policyId, policyType, description);
    }
}
