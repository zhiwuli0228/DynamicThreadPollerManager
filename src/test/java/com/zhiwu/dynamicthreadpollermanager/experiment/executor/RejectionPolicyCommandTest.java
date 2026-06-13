package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

class RejectionPolicyCommandTest {

    @Test
    void validCommandCreation() {
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        RejectionPolicyCommand cmd = new RejectionPolicyCommand(handler, "switch to caller runs");
        assertSame(handler, cmd.targetPolicy());
        assertEquals("switch to caller runs", cmd.reason());
    }

    @Test
    void nullTargetPolicyThrows() {
        assertThrows(NullPointerException.class,
                () -> new RejectionPolicyCommand(null, "reason"));
    }

    @Test
    void nullReasonThrows() {
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        assertThrows(IllegalArgumentException.class,
                () -> new RejectionPolicyCommand(handler, null));
    }

    @Test
    void blankReasonThrows() {
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        assertThrows(IllegalArgumentException.class,
                () -> new RejectionPolicyCommand(handler, "   "));
    }

    @Test
    void emptyReasonThrows() {
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        assertThrows(IllegalArgumentException.class,
                () -> new RejectionPolicyCommand(handler, ""));
    }

    @Test
    void fromCurrentDifferentPolicyReturnsCommand() {
        RejectedExecutionHandler current = new ThreadPoolExecutor.AbortPolicy();
        RejectedExecutionHandler target = new ThreadPoolExecutor.CallerRunsPolicy();
        Optional<RejectionPolicyCommand> result = RejectionPolicyCommand.fromCurrent(
                current, target, "switch policy");

        assertTrue(result.isPresent());
        assertSame(target, result.get().targetPolicy());
        assertEquals("switch policy", result.get().reason());
    }

    @Test
    void fromCurrentSamePolicyTypeReturnsEmpty() {
        RejectedExecutionHandler current = new ThreadPoolExecutor.AbortPolicy();
        RejectedExecutionHandler target = new ThreadPoolExecutor.AbortPolicy();
        Optional<RejectionPolicyCommand> result = RejectionPolicyCommand.fromCurrent(
                current, target, "no-op");

        assertTrue(result.isEmpty());
    }

    @Test
    void fromCurrentNullCurrentThrows() {
        RejectedExecutionHandler target = new ThreadPoolExecutor.AbortPolicy();
        assertThrows(NullPointerException.class,
                () -> RejectionPolicyCommand.fromCurrent(null, target, "reason"));
    }

    @Test
    void fromCurrentNullTargetThrows() {
        RejectedExecutionHandler current = new ThreadPoolExecutor.AbortPolicy();
        assertThrows(NullPointerException.class,
                () -> RejectionPolicyCommand.fromCurrent(current, null, "reason"));
    }

    @Test
    void allFourJdkPoliciesCanBeUsed() {
        RejectedExecutionHandler[] policies = {
                new ThreadPoolExecutor.AbortPolicy(),
                new ThreadPoolExecutor.CallerRunsPolicy(),
                new ThreadPoolExecutor.DiscardPolicy(),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        };
        for (RejectedExecutionHandler policy : policies) {
            RejectionPolicyCommand cmd = new RejectionPolicyCommand(
                    policy, "test " + policy.getClass().getSimpleName());
            assertSame(policy, cmd.targetPolicy());
        }
    }
}
