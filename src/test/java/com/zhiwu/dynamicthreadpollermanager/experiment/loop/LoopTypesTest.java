package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoopTypesTest {

    @Test
    void loopStateShouldHaveFiveValues() {
        assertEquals(5, LoopState.values().length);
        assertNotNull(LoopState.valueOf("IDLE"));
        assertNotNull(LoopState.valueOf("RUNNING"));
        assertNotNull(LoopState.valueOf("PAUSED"));
        assertNotNull(LoopState.valueOf("STOPPED"));
        assertNotNull(LoopState.valueOf("EMERGENCY_STOPPED"));
    }

    @Test
    void loopConfigShouldRejectInvalidSamplingInterval() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        assertThrows(IllegalArgumentException.class, () ->
                new LoopConfig(99, 100, 20, 6, 2, 10, 2, List.of(policy)));
    }

    @Test
    void loopConfigShouldRejectEmptyCandidates() {
        assertThrows(IllegalArgumentException.class, () ->
                new LoopConfig(2000, 100, 20, 6, 2, 10, 2, List.of()));
    }

    @Test
    void loopConfigShouldRejectNegativeMaxIterations() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        assertThrows(IllegalArgumentException.class, () ->
                new LoopConfig(2000, -1, 20, 6, 2, 10, 2, List.of(policy)));
    }

    @Test
    void loopConfigShouldRejectSmallSnapshotWindow() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        assertThrows(IllegalArgumentException.class, () ->
                new LoopConfig(2000, 100, 1, 6, 2, 10, 2, List.of(policy)));
    }

    @Test
    void loopConfigDefaultsShouldWork() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        LoopConfig config = LoopConfig.defaults(policy);
        assertEquals(2000, config.samplingIntervalMs());
        assertEquals(100, config.maxIterations());
        assertEquals(20, config.snapshotWindowSize());
        assertEquals(6, config.oscillationWindowSize());
        assertEquals(2, config.oscillationPatternThreshold());
        assertEquals(10, config.feedbackCalibrationWindow());
        assertEquals(2, config.emergencyStopThreshold());
        assertEquals(1, config.candidatePolicies().size());
        assertEquals(policy, config.candidatePolicies().get(0));
    }

    @Test
    void loopConfigShouldDefensivelyCopyCandidates() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        var mutable = new java.util.ArrayList<>(List.of(policy));
        var config = new LoopConfig(2000, 100, 20, 6, 2, 10, 2, mutable);
        mutable.clear();
        assertEquals(1, config.candidatePolicies().size());
    }

    @Test
    void loopSessionStartedShouldHaveCorrectDefaults() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        LoopConfig config = LoopConfig.defaults(policy);
        LoopSession session = LoopSession.started(config);
        assertNotNull(session.sessionId());
        assertFalse(session.sessionId().isBlank());
        assertEquals(LoopState.RUNNING, session.finalState());
        assertEquals(0, session.adjustmentCount());
        assertEquals(0, session.iterationCount());
        assertTrue(session.endTime().isEmpty());
    }

    @Test
    void loopSessionEndedShouldSetFields() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        LoopConfig config = LoopConfig.defaults(policy);
        LoopSession started = LoopSession.started(config);
        LoopSession ended = started.ended(LoopState.STOPPED, 3, 10, "test summary");
        assertEquals(started.sessionId(), ended.sessionId());
        assertEquals(LoopState.STOPPED, ended.finalState());
        assertEquals(3, ended.adjustmentCount());
        assertEquals(10, ended.iterationCount());
        assertTrue(ended.endTime().isPresent());
        assertEquals("test summary", ended.summary());
    }

    @Test
    void loopSessionShouldRejectInvalidCounts() {
        var policy = ThresholdPolicyConfig.defaultAdaptive();
        LoopConfig config = LoopConfig.defaults(policy);
        assertThrows(IllegalArgumentException.class, () ->
                new LoopSession("s1", config, java.time.Instant.now(), java.util.Optional.empty(),
                        -1, 0, LoopState.RUNNING, "bad"));
        assertThrows(IllegalArgumentException.class, () ->
                new LoopSession("s1", config, java.time.Instant.now(), java.util.Optional.empty(),
                        0, -1, LoopState.RUNNING, "bad"));
        assertThrows(IllegalArgumentException.class, () ->
                new LoopSession("s1", config, java.time.Instant.now(), java.util.Optional.empty(),
                        5, 3, LoopState.RUNNING, "iter < adj"));
    }

    @Test
    void transitionLegalityShouldHaveThreeValues() {
        assertEquals(3, TransitionLegality.values().length);
    }
}
