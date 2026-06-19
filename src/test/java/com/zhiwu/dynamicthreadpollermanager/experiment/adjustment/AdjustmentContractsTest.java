package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-contract tests covering the immutability and validation rules of
 * {@link ScaleAdjustmentCommand}, {@link ExecutorStateSnapshot},
 * {@link AdjustmentResult}, {@link AdjustmentEvidence} and their
 * supporting enums. Each test pins a specific clause in the design and
 * spec documents.
 */
class AdjustmentContractsTest {

    private static final Instant FIXED_TS = Instant.parse("2026-06-05T10:00:00Z");

    @Test
    void scaleAdjustmentCommandShouldBuildDeterministicId() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                8,
                12,
                "scale up triggered",
                "policy-1:0",
                Instant::now);

        assertEquals("run-1:2026-06-05T10:00:00Z:8->12", command.commandId());
        assertEquals("run-1", command.runId());
        assertEquals(FIXED_TS, command.decisionTimestamp());
        assertEquals(8, command.currentPoolSize());
        assertEquals(12, command.targetPoolSize());
        assertEquals("scale up triggered", command.reason());
        assertEquals("policy-1:0", command.sourceDecisionRef());
    }

    @Test
    void scaleAdjustmentCommandShouldRejectNoOpTarget() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                8,
                8,
                "noop",
                "policy-1:0",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectBlankRunId() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "",
                FIXED_TS,
                8,
                12,
                "reason",
                "policy-1:0",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectBlankReason() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                8,
                12,
                "  ",
                "policy-1:0",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectBlankSourceDecisionRef() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                8,
                12,
                "reason",
                "",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectNullDecisionTimestamp() {
        assertThrows(NullPointerException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                null,
                8,
                12,
                "reason",
                "policy-1:0",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectNullClock() {
        assertThrows(NullPointerException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                8,
                12,
                "reason",
                "policy-1:0",
                null));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectNegativeCurrentPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                -1,
                12,
                "reason",
                "policy-1:0",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldRejectNegativeTargetPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "run-1",
                FIXED_TS,
                8,
                -1,
                "reason",
                "policy-1:0",
                Instant::now));
    }

    @Test
    void scaleAdjustmentCommandShouldExposeCommandId() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-2",
                FIXED_TS,
                4,
                6,
                "scale up",
                "policy-2:1",
                Instant::now);

        assertEquals("run-2:2026-06-05T10:00:00Z:4->6", command.commandId());
    }

    @Test
    void scaleAdjustmentCommandShouldReturnNoOpFactoryForNoOp() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.noOp("run-1", FIXED_TS, 8,
                "already at target", "policy-1:0", Instant::now);

        assertNotNull(command);
        assertTrue(command.isNoOp());
        assertEquals(8, command.targetPoolSize());
        assertEquals(8, command.currentPoolSize());
    }

    @Test
    void scaleAdjustmentCommandNoOpShouldRejectMismatch() {
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.noOp(
                "run-1", FIXED_TS, 8, "target differs", "policy-1:0", Instant::now, 9));
    }

    @Test
    void executorStateSnapshotShouldExposeRequiredFields() {
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8)
                .maximumPoolSize(16)
                .activeCount(4)
                .queueSize(2)
                .queueCapacity(32)
                .build();

        assertEquals(FIXED_TS, snapshot.observedAt());
        assertEquals(8, snapshot.corePoolSize());
        assertEquals(16, snapshot.maximumPoolSize());
        assertEquals(4, snapshot.activeCount());
        assertEquals(2, snapshot.queueSize());
        assertEquals(32, snapshot.queueCapacity());
    }

    @Test
    void executorStateSnapshotShouldAllowNullOptionalFields() {
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(4)
                .maximumPoolSize(4)
                .build();

        assertEquals(4, snapshot.corePoolSize());
        assertEquals(4, snapshot.maximumPoolSize());
        assertNull(snapshot.activeCount());
        assertNull(snapshot.queueSize());
        assertNull(snapshot.queueCapacity());
    }

    @Test
    void executorStateSnapshotShouldRejectNullObservedAt() {
        assertThrows(NullPointerException.class, () -> ExecutorStateSnapshot.builder(null));
    }

    @Test
    void executorStateSnapshotShouldRejectZeroPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(0)
                .maximumPoolSize(0)
                .build());
    }

    @Test
    void executorStateSnapshotShouldRejectMaxBelowCore() {
        assertThrows(IllegalArgumentException.class, () -> ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8)
                .maximumPoolSize(4)
                .build());
    }

    @Test
    void executorStateSnapshotShouldRejectNegativeQueueFields() {
        assertThrows(IllegalArgumentException.class, () -> ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(4)
                .maximumPoolSize(4)
                .queueSize(-1)
                .build());
        assertThrows(IllegalArgumentException.class, () -> ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(4)
                .maximumPoolSize(4)
                .queueCapacity(-1)
                .build());
    }

    @Test
    void adjustmentStatusShouldExposeRequiredStates() {
        AdjustmentStatus[] expected = {
                AdjustmentStatus.APPLIED,
                AdjustmentStatus.REJECTED,
                AdjustmentStatus.NO_OP,
                AdjustmentStatus.FAILED,
                AdjustmentStatus.DEFERRED
        };
        assertEquals(5, AdjustmentStatus.values().length);
        for (AdjustmentStatus status : expected) {
            assertNotNull(status);
        }
    }

    @Test
    void adjustmentFailureCodeShouldExposeRequiredCodes() {
        assertEquals(12, AdjustmentFailureCode.values().length);
        assertNotNull(AdjustmentFailureCode.NOT_READY);
        assertNotNull(AdjustmentFailureCode.RISK_NOT_ACCEPTED);
        assertNotNull(AdjustmentFailureCode.COOLDOWN_ACTIVE);
        assertNotNull(AdjustmentFailureCode.OPPOSITE_DIRECTION);
        assertNotNull(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED);
        assertNotNull(AdjustmentFailureCode.INVALID_COMMAND);
        assertNotNull(AdjustmentFailureCode.PROBE_FAILURE);
        assertNotNull(AdjustmentFailureCode.UNSUPPORTED);
        assertNotNull(AdjustmentFailureCode.EXECUTOR_NOT_FOUND);
        assertNotNull(AdjustmentFailureCode.COORDINATION_REJECTED);
        assertNotNull(AdjustmentFailureCode.COORDINATION_CAPPED);
        assertNotNull(AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE);
    }

    @Test
    void adjustmentResultShouldExposeAllFields() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot before = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        ExecutorStateSnapshot after = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(12).maximumPoolSize(16).build();

        AdjustmentResult result = new AdjustmentResult(
                command,
                AdjustmentStatus.APPLIED,
                before,
                12,
                12,
                after,
                "scale up applied",
                null,
                "policy-1:0",
                FIXED_TS);

        assertSame(command, result.command());
        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertSame(before, result.beforeState());
        assertEquals(12, result.requestedPoolSize());
        assertEquals(12, result.appliedPoolSize());
        assertSame(after, result.afterState());
        assertEquals("scale up applied", result.reason());
        assertNull(result.failureCode());
        assertEquals("policy-1:0", result.sourceDecisionRef());
        assertEquals(FIXED_TS, result.decisionTimestamp());
    }

    @Test
    void adjustmentResultShouldRejectNullStatus() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(NullPointerException.class, () -> new AdjustmentResult(
                command,
                null,
                snapshot,
                12,
                12,
                snapshot,
                "reason",
                null,
                "policy-1:0",
                FIXED_TS));
    }

    @Test
    void adjustmentResultShouldRequireReason() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(IllegalArgumentException.class, () -> new AdjustmentResult(
                command,
                AdjustmentStatus.REJECTED,
                snapshot,
                12,
                12,
                snapshot,
                "  ",
                AdjustmentFailureCode.NOT_READY,
                "policy-1:0",
                FIXED_TS));
    }

    @Test
    void adjustmentResultShouldRequireFailureCodeForFailed() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(IllegalArgumentException.class, () -> new AdjustmentResult(
                command,
                AdjustmentStatus.FAILED,
                snapshot,
                12,
                12,
                snapshot,
                "boom",
                null,
                "policy-1:0",
                FIXED_TS));
    }

    @Test
    void adjustmentResultShouldRequireFailureCodeForRejected() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(IllegalArgumentException.class, () -> new AdjustmentResult(
                command,
                AdjustmentStatus.REJECTED,
                snapshot,
                12,
                12,
                snapshot,
                "rejected",
                null,
                "policy-1:0",
                FIXED_TS));
    }

    @Test
    void adjustmentResultShouldExposeFailureCodeForFailed() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        AdjustmentResult result = new AdjustmentResult(
                command,
                AdjustmentStatus.FAILED,
                snapshot,
                12,
                12,
                snapshot,
                "probe failure",
                AdjustmentFailureCode.PROBE_FAILURE,
                "policy-1:0",
                FIXED_TS);
        assertEquals(AdjustmentFailureCode.PROBE_FAILURE, result.failureCode());
    }

    @Test
    void adjustmentEvidenceShouldFixTypeAndExposeFields() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot before = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        ExecutorStateSnapshot after = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(12).maximumPoolSize(16).build();
        Instant recorded = FIXED_TS.plusSeconds(1);

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                before,
                12,
                12,
                after,
                AdjustmentStatus.APPLIED,
                "applied",
                null,
                "policy-1:0",
                FIXED_TS,
                recorded);

        assertEquals("runtime_adjustment", evidence.evidenceType());
        assertEquals(command.commandId(), evidence.commandId());
        assertEquals("run-1", evidence.runId());
        assertEquals("policy-1:0", evidence.sourceDecisionRef());
        assertEquals(FIXED_TS, evidence.decisionTimestamp());
        assertEquals(recorded, evidence.recordedTimestamp());
        assertEquals(AdjustmentStatus.APPLIED, evidence.status());
        assertEquals(12, evidence.requestedPoolSize());
        assertEquals(12, evidence.appliedPoolSize());
        assertSame(before, evidence.beforeState());
        assertSame(after, evidence.afterState());
        assertEquals("applied", evidence.reason());
        assertNull(evidence.failureCode());
    }

    @Test
    void adjustmentEvidenceShouldPreserveBeforeStateOnRejection() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot before = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                before,
                12,
                Optional.of(8).orElse(8), // applied equals current (no change)
                before,
                AdjustmentStatus.REJECTED,
                "cooldown active",
                AdjustmentFailureCode.COOLDOWN_ACTIVE,
                "policy-1:0",
                FIXED_TS,
                FIXED_TS.plusSeconds(1));

        assertSame(before, evidence.beforeState());
        assertEquals(AdjustmentFailureCode.COOLDOWN_ACTIVE, evidence.failureCode());
        assertEquals(8, evidence.appliedPoolSize());
    }

    @Test
    void adjustmentEvidenceShouldRejectNullCommand() {
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(NullPointerException.class, () -> new AdjustmentEvidence(
                null,
                snapshot,
                12,
                12,
                snapshot,
                AdjustmentStatus.APPLIED,
                "applied",
                null,
                "policy-1:0",
                FIXED_TS,
                FIXED_TS));
    }

    @Test
    void adjustmentEvidenceShouldRejectBlankReason() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(IllegalArgumentException.class, () -> new AdjustmentEvidence(
                command,
                snapshot,
                12,
                12,
                snapshot,
                AdjustmentStatus.REJECTED,
                " ",
                AdjustmentFailureCode.NOT_READY,
                "policy-1:0",
                FIXED_TS,
                FIXED_TS));
    }

    @Test
    void adjustmentEvidenceShouldRequireFailureCodeForFailed() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(IllegalArgumentException.class, () -> new AdjustmentEvidence(
                command,
                snapshot,
                12,
                12,
                snapshot,
                AdjustmentStatus.FAILED,
                "boom",
                null,
                "policy-1:0",
                FIXED_TS,
                FIXED_TS));
    }

    @Test
    void adjustmentEvidenceShouldExposeAllStatusCategories() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        List<AdjustmentStatus> expected = List.of(
                AdjustmentStatus.APPLIED,
                AdjustmentStatus.REJECTED,
                AdjustmentStatus.NO_OP,
                AdjustmentStatus.FAILED,
                AdjustmentStatus.DEFERRED);
        for (AdjustmentStatus status : expected) {
            AdjustmentFailureCode code = switch (status) {
                case APPLIED, NO_OP -> null;
                case REJECTED -> AdjustmentFailureCode.INVALID_COMMAND;
                case FAILED -> AdjustmentFailureCode.PROBE_FAILURE;
                case DEFERRED -> AdjustmentFailureCode.UNSUPPORTED;
            };
            AdjustmentEvidence evidence = new AdjustmentEvidence(
                    command,
                    snapshot,
                    12,
                    12,
                    snapshot,
                    status,
                    "test",
                    code,
                    "policy-1:0",
                    FIXED_TS,
                    FIXED_TS);
            assertEquals(status, evidence.status());
            assertEquals(code, evidence.failureCode());
        }
    }

    @Test
    void adjustmentEvidenceShouldRejectNullRecordedTimestamp() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        assertThrows(NullPointerException.class, () -> new AdjustmentEvidence(
                command,
                snapshot,
                12,
                12,
                snapshot,
                AdjustmentStatus.APPLIED,
                "applied",
                null,
                "policy-1:0",
                FIXED_TS,
                null));
    }

    @Test
    void adjustmentEvidenceShouldNotExposeOfflineReplayMode() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", FIXED_TS, 8, 12, "scale up", "policy-1:0", Instant::now);
        ExecutorStateSnapshot snapshot = ExecutorStateSnapshot.builder(FIXED_TS)
                .corePoolSize(8).maximumPoolSize(16).build();
        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                snapshot,
                12,
                12,
                snapshot,
                AdjustmentStatus.APPLIED,
                "applied",
                null,
                "policy-1:0",
                FIXED_TS,
                FIXED_TS);
        assertFalse(evidence.evidenceType().contains("replay"));
        assertFalse(evidence.evidenceType().equals("offline_replay"));
    }
}
