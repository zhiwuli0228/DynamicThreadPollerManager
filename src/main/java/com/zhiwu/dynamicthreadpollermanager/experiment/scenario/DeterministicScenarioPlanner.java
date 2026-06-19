package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic {@link ScenarioPlanner}. Produces the same plan for
 * the same {@link ScenarioDefinition}, with no dependence on wall
 * clock or random sources. Profile rules:
 *
 * <ul>
 *   <li>{@code STEADY} — every step uses {@code baseWorkUnits}.</li>
 *   <li>{@code RAMP} — step {@code i} uses {@code baseWorkUnits + i}.</li>
 *   <li>{@code BURST} — steps at zero-based indexes {@code i % 3 == 0}
 *       use {@code baseWorkUnits * 3}; other steps use {@code baseWorkUnits}.</li>
 *   <li>{@code LONG_TAIL} — {@code baseWorkUnits + (seed % 3 == 0 ? baseWorkUnits * 5 : 0)}.</li>
 *   <li>{@code MIXED_CPU_IO} — even index: {@code workUnits = baseWorkUnits * 3},
 *       odd index: {@code workUnits = baseWorkUnits, plannedDelayMillis = baseWorkUnits * 2}.</li>
 *   <li>{@code DOWNSTREAM_BLOCKED} — {@code workUnits = baseWorkUnits},
 *       {@code plannedDelayMillis = baseWorkUnits * 10}.</li>
 * </ul>
 *
 * Step indexes are zero-based.
 */
public final class DeterministicScenarioPlanner implements ScenarioPlanner {

    @Override
    public ScenarioPlan plan(ScenarioDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        List<ScenarioStep> steps = new ArrayList<>(definition.stepCount());
        for (int i = 0; i < definition.stepCount(); i++) {
            int workUnits = workUnitsFor(definition.profile(), definition.baseWorkUnits(), definition.seed(), i);
            long delay = delayMillisFor(definition.profile(), definition.baseWorkUnits(), i);
            steps.add(new ScenarioStep(i, workUnits, delay));
        }
        return new ScenarioPlan(definition.scenarioId(), steps);
    }

    private static int workUnitsFor(ScenarioProfile profile, int baseWorkUnits, long seed, int index) {
        return switch (profile) {
            case STEADY -> baseWorkUnits;
            case RAMP -> baseWorkUnits + index;
            case BURST -> (index % 3 == 0) ? baseWorkUnits * 3 : baseWorkUnits;
            case LONG_TAIL -> baseWorkUnits + (seed % 3 == 0 ? baseWorkUnits * 5 : 0);
            case MIXED_CPU_IO -> (index % 2 == 0) ? baseWorkUnits * 3 : baseWorkUnits;
            case DOWNSTREAM_BLOCKED -> baseWorkUnits;
        };
    }

    private static long delayMillisFor(ScenarioProfile profile, int baseWorkUnits, int index) {
        return switch (profile) {
            case MIXED_CPU_IO -> (index % 2 == 1) ? (long) baseWorkUnits * 2 : 0L;
            case DOWNSTREAM_BLOCKED -> (long) baseWorkUnits * 10;
            default -> 0L;
        };
    }
}
