package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PolicyReplacementResultTest {

    private PolicyReplacementEvidence successEvidence() {
        ExecutorStateSnapshot state = ExecutorStateSnapshot.builder(Instant.now())
                .corePoolSize(2)
                .maximumPoolSize(4)
                .queueCapacity(10)
                .build();
        return new PolicyReplacementEvidence(
                "AbortPolicy", "CallerRunsPolicy", state, Instant.now(),
                true, "switched");
    }

    private PolicyReplacementEvidence deniedEvidence() {
        ExecutorStateSnapshot state = ExecutorStateSnapshot.builder(Instant.now())
                .corePoolSize(2)
                .maximumPoolSize(4)
                .queueCapacity(10)
                .build();
        return new PolicyReplacementEvidence(
                "AbortPolicy", "AbortPolicy", state, Instant.now(),
                false, "executor is not in RUNNING state");
    }

    @Test
    void successFactorySetsCorrectFields() {
        PolicyReplacementEvidence evidence = successEvidence();
        PolicyReplacementResult result = PolicyReplacementResult.success(evidence);

        assertTrue(result.success());
        assertSame(evidence, result.evidence());
        assertNull(result.failureCode());
        assertNull(result.reason());
    }

    @Test
    void successFactoryRejectsNullEvidence() {
        assertThrows(NullPointerException.class,
                () -> PolicyReplacementResult.success(null));
    }

    @Test
    void deniedFactorySetsCorrectFields() {
        PolicyReplacementEvidence evidence = deniedEvidence();
        PolicyReplacementResult result = PolicyReplacementResult.denied(
                "SAFETY_GATE_DENIED", "executor is not in RUNNING state", evidence);

        assertFalse(result.success());
        assertSame(evidence, result.evidence());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
        assertEquals("executor is not in RUNNING state", result.reason());
    }

    @Test
    void deniedFactoryRejectsNullFailureCode() {
        assertThrows(NullPointerException.class,
                () -> PolicyReplacementResult.denied(null, "reason", deniedEvidence()));
    }

    @Test
    void deniedFactoryRejectsNullEvidence() {
        assertThrows(NullPointerException.class,
                () -> PolicyReplacementResult.denied("CODE", "reason", null));
    }

    @Test
    void failedWithoutEvidenceFactorySetsCorrectFields() {
        PolicyReplacementResult result = PolicyReplacementResult.failed(
                "POLICY_SET_FAILED", "setRejectionPolicy failed: error");

        assertFalse(result.success());
        assertNull(result.evidence());
        assertEquals("POLICY_SET_FAILED", result.failureCode());
        assertEquals("setRejectionPolicy failed: error", result.reason());
    }

    @Test
    void failedWithEvidenceFactorySetsCorrectFields() {
        PolicyReplacementEvidence evidence = deniedEvidence();
        PolicyReplacementResult result = PolicyReplacementResult.failed(
                "POLICY_SET_FAILED", "setRejectionPolicy failed: error", evidence);

        assertFalse(result.success());
        assertSame(evidence, result.evidence());
        assertEquals("POLICY_SET_FAILED", result.failureCode());
        assertEquals("setRejectionPolicy failed: error", result.reason());
    }

    @Test
    void failedWithoutEvidenceRejectsNullFailureCode() {
        assertThrows(NullPointerException.class,
                () -> PolicyReplacementResult.failed(null, "reason"));
    }

    @Test
    void failedWithEvidenceRejectsNullFailureCode() {
        assertThrows(NullPointerException.class,
                () -> PolicyReplacementResult.failed(null, "reason", deniedEvidence()));
    }

    @Test
    void executorNotFoundResult() {
        PolicyReplacementResult result = PolicyReplacementResult.failed(
                "EXECUTOR_NOT_FOUND", "no executor with id test-exec");

        assertFalse(result.success());
        assertNull(result.evidence());
        assertEquals("EXECUTOR_NOT_FOUND", result.failureCode());
    }
}
