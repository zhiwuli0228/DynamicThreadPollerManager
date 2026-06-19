package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceSummary;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluationInput;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluator;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflinePolicyReplayServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T00:01:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T00:02:00Z");

    @Test
    void sensitivityConfigSetShouldExposeThreeFixedLabels() {
        SensitivityConfigSet set = SensitivityConfigSet.defaults();

        assertTrue(set.contains("default"));
        assertTrue(set.contains("conservative"));
        assertTrue(set.contains("aggressive"));
        assertEquals(3, set.configs().size());
    }

    @Test
    void sensitivityConfigSetDefaultShouldMatchPolicyDefaults() {
        SensitivityConfigSet set = SensitivityConfigSet.defaults();

        assertEquals(ThresholdPolicyConfig.defaultAdaptive(), set.config("default"));
    }

    @Test
    void sensitivityConfigSetConservativeShouldMatchFixedValues() {
        SensitivityConfigSet set = SensitivityConfigSet.defaults();

        ThresholdPolicyConfig cfg = set.config("conservative");
        assertEquals("conservative-adaptive", cfg.policyId());
        assertEquals(1, cfg.minPoolSize());
        assertEquals(32, cfg.maxPoolSize());
        assertEquals(28, cfg.scaleUpActiveThreadsThreshold());
        assertEquals(20, cfg.scaleUpQueueSizeThreshold());
        assertEquals(2, cfg.scaleDownActiveThreadsThreshold());
        assertEquals(1, cfg.scaleStep());
    }

    @Test
    void sensitivityConfigSetAggressiveShouldMatchFixedValues() {
        SensitivityConfigSet set = SensitivityConfigSet.defaults();

        ThresholdPolicyConfig cfg = set.config("aggressive");
        assertEquals("aggressive-adaptive", cfg.policyId());
        assertEquals(1, cfg.minPoolSize());
        assertEquals(32, cfg.maxPoolSize());
        assertEquals(20, cfg.scaleUpActiveThreadsThreshold());
        assertEquals(12, cfg.scaleUpQueueSizeThreshold());
        assertEquals(6, cfg.scaleDownActiveThreadsThreshold());
        assertEquals(3, cfg.scaleStep());
    }

    @Test
    void replayShouldProduceOneDecisionPerSnapshotPerConfig() {
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(new RecordingEvaluator());
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(
                        observed("run-1", T0, 4, 8, 0),
                        observed("run-1", T1, 4, 8, 0),
                        observed("run-1", T2, 4, 8, 0)
                ));

        List<ReplayDecisionEvidence> evidence = service.replay(input);

        assertEquals(9, evidence.size());
    }

    @Test
    void replayShouldSetDecisionTimestampEqualToSnapshotTimestamp() {
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(new RecordingEvaluator());
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", T0, 4, 8, 0)));

        List<ReplayDecisionEvidence> evidence = service.replay(input);

        for (ReplayDecisionEvidence ev : evidence) {
            assertEquals(ev.snapshotTimestamp(), ev.decisionTimestamp(),
                    () -> "decisionTimestamp must equal snapshotTimestamp for label " + ev.policyConfigLabel());
        }
    }

    @Test
    void replayShouldUseSnapshotTimestampAsPolicyEvaluationInput() {
        RecordingEvaluator evaluator = new RecordingEvaluator();
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(evaluator);
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", T0, 4, 8, 0)));

        service.replay(input);

        assertEquals(3, evaluator.calls.size());
        for (PolicyEvaluationInput call : evaluator.calls) {
            assertEquals(T0, call.evaluatedAt(),
                    () -> "PolicyEvaluationInput.evaluatedAt must equal snapshot timestamp");
        }
    }

    @Test
    void replayShouldExposeAllRequiredFieldsOnDecisionEvidence() {
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(new RecordingEvaluator());
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", T0, 4, 8, 0)));

        List<ReplayDecisionEvidence> evidence = service.replay(input);

        ReplayDecisionEvidence ev = evidence.get(0);
        assertEquals("run-1", ev.runId());
        assertEquals("scenario-1", ev.scenarioId());
        assertEquals(ScenarioProfile.STEADY, ev.scenarioProfile());
        assertNotNull(ev.policyConfigLabel());
        assertNotNull(ev.policyId());
        assertEquals(0, ev.snapshotIndex());
        assertEquals(T0, ev.snapshotTimestamp());
        assertEquals(T0, ev.decisionTimestamp());
        assertNotNull(ev.action());
        assertNotNull(ev.gateStatus());
        assertNotNull(ev.reason());
        assertEquals("offline_replay", ev.replayMode());
    }

    @Test
    void replayShouldInvokeAllThreeConfigs() {
        RecordingEvaluator evaluator = new RecordingEvaluator();
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(evaluator);
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", T0, 4, 8, 0)));

        service.replay(input);

        assertEquals(List.of("default", "conservative", "aggressive"),
                evaluator.configLabelsSeen);
    }

    @Test
    void replayShouldRejectNullEvaluator() {
        assertThrows(NullPointerException.class, () -> new OfflinePolicyReplayService(null));
    }

    @Test
    void replayShouldRejectNullInput() {
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(new RecordingEvaluator());
        assertThrows(NullPointerException.class, () -> service.replay(null));
    }

    @Test
    void replayShouldNotProduceWallClockTimeOnReplay() {
        // The replay service must not call Instant.now(); we verify by
        // feeding it a far-future timestamp and confirming decisionTimestamp
        // matches it (not the wall-clock at evaluation time).
        Instant farFuture = Instant.parse("2099-12-31T23:59:59Z");
        OfflinePolicyReplayService service = new OfflinePolicyReplayService(new RecordingEvaluator());
        ReplayRunInput input = input("run-1", ScenarioProfile.STEADY,
                List.of(observed("run-1", farFuture, 4, 8, 0)));

        List<ReplayDecisionEvidence> evidence = service.replay(input);

        for (ReplayDecisionEvidence ev : evidence) {
            assertEquals(farFuture, ev.decisionTimestamp());
        }
    }

    private static ReplayRunInput input(String runId,
                                        ScenarioProfile profile,
                                        List<ObservedSnapshot> snapshots) {
        EvidenceSummary summary = new EvidenceSummary(runId, snapshots.size(),
                Optional.of(T0), Optional.of(T2));
        return new ReplayRunInput(
                runId, "scenario-1", profile, "policy-1",
                snapshots, summary, snapshots.size(), 10);
    }

    private static ObservedSnapshot observed(String runId, Instant ts, int active, int pool, int queue) {
        RuntimeObservation observation = new RuntimeObservation(
                ts,
                MetricValue.present(active),
                MetricValue.present(pool),
                MetricValue.present(queue),
                MetricValue.absent(),
                MetricValue.present(0.5)
        );
        PressureSnapshot snapshot = new PressureSnapshot(ts, active, pool, queue, 0L, 0.5);
        return new ObservedSnapshot(runId, snapshot, observation);
    }

    /**
     * Test evaluator that records the (input, config) pairs and returns
     * a deterministic decision derived from them so replay evidence is
     * stable across runs.
     */
    private static final class RecordingEvaluator implements PolicyEvaluator {
        final List<PolicyEvaluationInput> calls = new ArrayList<>();
        final List<String> configLabelsSeen = new ArrayList<>();
        final List<String> configIdsSeen = new ArrayList<>();

        @Override
        public PolicyDecision evaluate(PolicyEvaluationInput input, ThresholdPolicyConfig config) {
            calls.add(input);
            configLabelsSeen.add(labelFor(config));
            configIdsSeen.add(config.policyId());
            int pool = input.snapshot().poolSize();
            int active = input.snapshot().activeThreads();
            int queue = input.snapshot().queueSize();
            PolicyAction action;
            int proposed;
            if (active >= config.scaleUpActiveThreadsThreshold() || queue >= config.scaleUpQueueSizeThreshold()) {
                action = PolicyAction.SCALE_UP;
                proposed = Math.min(pool + config.scaleStep(), config.maxPoolSize());
            } else if (active <= config.scaleDownActiveThreadsThreshold() && queue == 0) {
                action = PolicyAction.SCALE_DOWN;
                proposed = Math.max(pool - config.scaleStep(), config.minPoolSize());
            } else {
                action = PolicyAction.HOLD;
                proposed = pool;
            }
            GateStatus status = proposed == pool ? GateStatus.HOLD : GateStatus.ACCEPTED;
            return new PolicyDecision(input.runId(), config.policyId(), input.evaluatedAt(),
                    action, status, pool, proposed, "recording-evaluator");
        }

        private static String labelFor(ThresholdPolicyConfig config) {
            return switch (config.policyId()) {
                case "default-adaptive" -> "default";
                case "conservative-adaptive" -> "conservative";
                case "aggressive-adaptive" -> "aggressive";
                default -> config.policyId();
            };
        }
    }
}
