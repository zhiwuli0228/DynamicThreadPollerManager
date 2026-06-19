package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PressureStateMachineTest {

    private PressureStateMachine machine;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        machine = new PressureStateMachine();
    }

    @Test
    void shouldReturnLegalForNormalToQueueBuildup() {
        assertEquals(TransitionLegality.LEGAL,
                machine.isLegalTransition(PressureState.NORMAL, PressureState.QUEUE_BUILDUP));
    }

    @Test
    void shouldReturnLegalForAnyStateToNormal() {
        for (PressureState state : PressureState.values()) {
            assertEquals(TransitionLegality.LEGAL,
                    machine.isLegalTransition(state, PressureState.NORMAL),
                    state + " → NORMAL should be LEGAL");
        }
    }

    @Test
    void shouldReturnLegalForSameState() {
        for (PressureState state : PressureState.values()) {
            assertEquals(TransitionLegality.LEGAL,
                    machine.isLegalTransition(state, state),
                    state + " → " + state + " should be LEGAL");
        }
    }

    @Test
    void shouldReturnLegalForOverloadToNormalDueToAnyToNormalRule() {
        // any→NORMAL always LEGAL (takes precedence over ANOMALOUS table)
        assertEquals(TransitionLegality.LEGAL,
                machine.isLegalTransition(PressureState.OVERLOAD, PressureState.NORMAL));
    }

    @Test
    void shouldReturnAnomalousForUnderUtilizedToOverload() {
        assertEquals(TransitionLegality.ANOMALOUS,
                machine.isLegalTransition(PressureState.UNDER_UTILIZED, PressureState.OVERLOAD));
    }

    @Test
    void shouldReturnLegalForRejectionActiveToNormalDueToAnyToNormalRule() {
        // any→NORMAL always LEGAL (takes precedence over ANOMALOUS table)
        assertEquals(TransitionLegality.LEGAL,
                machine.isLegalTransition(PressureState.REJECTION_ACTIVE, PressureState.NORMAL));
    }

    @Test
    void shouldReturnIllegalForRecoveryToOverload() {
        assertEquals(TransitionLegality.ILLEGAL,
                machine.isLegalTransition(PressureState.RECOVERY, PressureState.OVERLOAD));
    }

    @Test
    void shouldTrackTransitionHistory() {
        machine.recordTransition(PressureState.NORMAL, PressureState.QUEUE_BUILDUP, now, "test");
        machine.recordTransition(PressureState.QUEUE_BUILDUP, PressureState.OVERLOAD, now, "test");

        assertEquals(2, machine.transitionCount());
        assertEquals(Optional.of(PressureState.OVERLOAD), machine.currentState());
    }

    @Test
    void shouldReturnRecentTransitions() {
        machine.recordTransition(PressureState.NORMAL, PressureState.QUEUE_BUILDUP, now, "t1");
        machine.recordTransition(PressureState.QUEUE_BUILDUP, PressureState.OVERLOAD, now, "t2");
        machine.recordTransition(PressureState.OVERLOAD, PressureState.RECOVERY, now, "t3");

        var recent = machine.recentTransitions(2);
        assertEquals(2, recent.size());
        assertEquals(PressureState.QUEUE_BUILDUP, recent.get(0).from());
        assertEquals(PressureState.OVERLOAD, recent.get(0).to());
        assertEquals(PressureState.OVERLOAD, recent.get(1).from());
        assertEquals(PressureState.RECOVERY, recent.get(1).to());
    }

    @Test
    void shouldRecordCorrectLegality() {
        machine.recordTransition(PressureState.OVERLOAD, PressureState.RECOVERY, now, "t");
        assertEquals(TransitionLegality.LEGAL,
                machine.recentTransitions(1).get(0).legality());
    }

    @Test
    void shouldResetClearsHistory() {
        machine.recordTransition(PressureState.NORMAL, PressureState.QUEUE_BUILDUP, now, "t");
        machine.reset();
        assertEquals(0, machine.transitionCount());
        assertEquals(Optional.empty(), machine.currentState());
    }
}
