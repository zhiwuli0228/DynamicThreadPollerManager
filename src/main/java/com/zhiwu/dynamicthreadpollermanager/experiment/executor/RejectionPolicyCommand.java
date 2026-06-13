package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionHandler;

public record RejectionPolicyCommand(
        RejectedExecutionHandler targetPolicy,
        String reason) {

    public RejectionPolicyCommand {
        Objects.requireNonNull(targetPolicy, "targetPolicy must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    public static Optional<RejectionPolicyCommand> fromCurrent(
            RejectedExecutionHandler current,
            RejectedExecutionHandler target,
            String reason) {
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(target, "target must not be null");
        if (current.getClass() == target.getClass()) {
            return Optional.empty();
        }
        return Optional.of(new RejectionPolicyCommand(target, reason));
    }
}
