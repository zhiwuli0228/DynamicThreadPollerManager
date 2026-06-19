package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.time.Instant;

/**
 * Captures an {@link ObservedSnapshot} for a given run. Implementations must
 * remain deterministic with respect to their inputs so callers (and tests)
 * can supply a controlled timestamp and observation without depending on
 * wall-clock timing races.
 */
public interface PressureSampler {

    ObservedSnapshot sample(String runId, RuntimeObservation observation, Instant at);
}
