package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureState;

import java.time.Instant;
import java.util.*;

/**
 * Tracks pressure state transitions and validates their legality.
 * The classifier itself remains stateless; this machine adds history awareness.
 */
public final class PressureStateMachine {

    private final List<PressureStateTransition> transitions = new ArrayList<>();

    private static final Set<Map.Entry<PressureState, PressureState>> LEGAL_TRANSITIONS =
            Set.of(
                    entry(PressureState.NORMAL, PressureState.QUEUE_BUILDUP),
                    entry(PressureState.NORMAL, PressureState.UNDER_UTILIZED),
                    entry(PressureState.NORMAL, PressureState.OVERLOAD),
                    entry(PressureState.QUEUE_BUILDUP, PressureState.OVERLOAD),
                    entry(PressureState.QUEUE_BUILDUP, PressureState.NORMAL),
                    entry(PressureState.OVERLOAD, PressureState.RECOVERY),
                    entry(PressureState.OVERLOAD, PressureState.REJECTION_ACTIVE),
                    entry(PressureState.REJECTION_ACTIVE, PressureState.RECOVERY),
                    entry(PressureState.RECOVERY, PressureState.NORMAL),
                    entry(PressureState.RECOVERY, PressureState.UNDER_UTILIZED),
                    entry(PressureState.UNDER_UTILIZED, PressureState.NORMAL)
            );

    private static final Set<Map.Entry<PressureState, PressureState>> ANOMALOUS_TRANSITIONS =
            Set.of(
                    entry(PressureState.OVERLOAD, PressureState.NORMAL),
                    entry(PressureState.UNDER_UTILIZED, PressureState.OVERLOAD),
                    entry(PressureState.REJECTION_ACTIVE, PressureState.NORMAL)
            );

    private static final Set<Map.Entry<PressureState, PressureState>> ILLEGAL_TRANSITIONS =
            Set.of(
                    entry(PressureState.RECOVERY, PressureState.OVERLOAD)
            );

    public TransitionLegality isLegalTransition(PressureState from, PressureState to) {
        if (from == to) return TransitionLegality.LEGAL;
        if (to == PressureState.NORMAL) return TransitionLegality.LEGAL;
        var pair = entry(from, to);
        if (LEGAL_TRANSITIONS.contains(pair)) return TransitionLegality.LEGAL;
        if (ANOMALOUS_TRANSITIONS.contains(pair)) return TransitionLegality.ANOMALOUS;
        if (ILLEGAL_TRANSITIONS.contains(pair)) return TransitionLegality.ILLEGAL;
        return TransitionLegality.LEGAL;
    }

    public void recordTransition(
            PressureState from, PressureState to,
            Instant timestamp, String trigger) {
        TransitionLegality legality = isLegalTransition(from, to);
        transitions.add(new PressureStateTransition(from, to, timestamp, trigger, legality));
    }

    public Optional<PressureState> currentState() {
        return transitions.isEmpty()
                ? Optional.empty()
                : Optional.of(transitions.get(transitions.size() - 1).to());
    }

    public List<PressureStateTransition> recentTransitions(int count) {
        int from = Math.max(0, transitions.size() - count);
        return List.copyOf(transitions.subList(from, transitions.size()));
    }

    public int transitionCount() {
        return transitions.size();
    }

    public void reset() {
        transitions.clear();
    }

    private static Map.Entry<PressureState, PressureState> entry(
            PressureState from, PressureState to) {
        return Map.entry(from, to);
    }
}
