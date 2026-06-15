package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureState;

import java.time.Instant;
import java.util.Objects;

/**
 * A single pressure state transition recorded by the state machine.
 */
public record PressureStateTransition(
        PressureState from,
        PressureState to,
        Instant timestamp,
        String trigger,
        TransitionLegality legality
) {
    public PressureStateTransition {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(legality, "legality must not be null");
    }
}
