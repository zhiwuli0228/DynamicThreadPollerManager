package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PolicyReplacementEvidenceTest {

    private ExecutorStateSnapshot snapshot() {
        return ExecutorStateSnapshot.builder(Instant.now())
                .corePoolSize(2)
                .maximumPoolSize(4)
                .queueCapacity(10)
                .build();
    }

    @Test
    void successEvidenceContainsCorrectFields() {
        ExecutorStateSnapshot state = snapshot();
        Instant now = Instant.now();

        PolicyReplacementEvidence evidence = new PolicyReplacementEvidence(
                "java.util.concurrent.ThreadPoolExecutor$AbortPolicy",
                "java.util.concurrent.ThreadPoolExecutor$CallerRunsPolicy",
                state,
                now,
                true,
                "switched to caller runs");

        assertEquals("java.util.concurrent.ThreadPoolExecutor$AbortPolicy",
                evidence.beforePolicyClass());
        assertEquals("java.util.concurrent.ThreadPoolExecutor$CallerRunsPolicy",
                evidence.afterPolicyClass());
        assertSame(state, evidence.executorState());
        assertSame(now, evidence.replacedAt());
        assertTrue(evidence.success());
        assertEquals("switched to caller runs", evidence.reason());
    }

    @Test
    void failureEvidenceContainsCorrectFields() {
        ExecutorStateSnapshot state = snapshot();
        Instant now = Instant.now();

        PolicyReplacementEvidence evidence = new PolicyReplacementEvidence(
                "java.util.concurrent.ThreadPoolExecutor$AbortPolicy",
                "java.util.concurrent.ThreadPoolExecutor$AbortPolicy",
                state,
                now,
                false,
                "executor is not in RUNNING state");

        assertFalse(evidence.success());
        assertEquals("executor is not in RUNNING state", evidence.reason());
        assertEquals("java.util.concurrent.ThreadPoolExecutor$AbortPolicy",
                evidence.beforePolicyClass());
        assertEquals("java.util.concurrent.ThreadPoolExecutor$AbortPolicy",
                evidence.afterPolicyClass());
    }

    @Test
    void nullFieldsAreAllowed() {
        PolicyReplacementEvidence evidence = new PolicyReplacementEvidence(
                null, null, null, null, false, null);

        assertNull(evidence.beforePolicyClass());
        assertNull(evidence.afterPolicyClass());
        assertNull(evidence.executorState());
        assertNull(evidence.replacedAt());
        assertFalse(evidence.success());
        assertNull(evidence.reason());
    }
}
