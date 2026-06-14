package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolicyScoreTest {

    @Test
    void shouldCreateWithValidValues() {
        PolicyScore score = new PolicyScore("test-policy", 0.8, 0.9, 0.7, 0.6, 0.5,
                "explanation");
        assertEquals("test-policy", score.policyId());
        assertEquals(0.8, score.compositeScore());
        assertEquals(0.9, score.responsivenessScore());
        assertEquals(0.7, score.safetyScore());
        assertEquals(0.6, score.stabilityScore());
        assertEquals(0.5, score.efficiencyScore());
        assertEquals("explanation", score.explanation());
    }

    @Test
    void shouldAcceptBoundaryScores() {
        assertDoesNotThrow(() -> new PolicyScore("p", 1.0, 0.0, 0.0, 0.0, 0.0, ""));
    }

    @Test
    void shouldRejectBlankPolicyId() {
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyScore("", 0.5, 0.5, 0.5, 0.5, 0.5, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyScore("  ", 0.5, 0.5, 0.5, 0.5, 0.5, "x"));
    }

    @Test
    void shouldRejectNullPolicyId() {
        assertThrows(NullPointerException.class,
                () -> new PolicyScore(null, 0.5, 0.5, 0.5, 0.5, 0.5, "x"));
    }

    @Test
    void shouldRejectNullExplanation() {
        assertThrows(NullPointerException.class,
                () -> new PolicyScore("p", 0.5, 0.5, 0.5, 0.5, 0.5, null));
    }

    @Test
    void shouldRejectScoreBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyScore("p", -0.1, 0.5, 0.5, 0.5, 0.5, "x"));
    }

    @Test
    void shouldRejectScoreAboveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new PolicyScore("p", 1.1, 0.5, 0.5, 0.5, 0.5, "x"));
    }
}
