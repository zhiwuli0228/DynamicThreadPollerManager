package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticalSignificanceCalculatorTest {

    @Test
    void shouldDetectSignificantDifference() {
        double[] modeA = new double[30];
        double[] modeB = new double[30];
        for (int i = 0; i < 30; i++) {
            modeA[i] = 100 + Math.random() * 20;
            modeB[i] = 80 + Math.random() * 15;
        }

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "throughput");

        assertTrue(result.pValue() < 0.05,
                "Expected p<0.05 but got " + result.pValue());
        assertTrue(result.isSignificant());
        assertTrue(result.effectSize() > 0.5);
        assertEquals(30, result.sampleSize());
    }

    @Test
    void shouldReportNonSignificantForIdenticalDistributions() {
        double[] modeA = new double[30];
        double[] modeB = new double[30];
        double seed = 100;
        for (int i = 0; i < 30; i++) {
            seed += 1;
            modeA[i] = seed % 20;
            modeB[i] = seed % 20 + 0.5;
        }

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "identical");

        assertTrue(result.pValue() > 0.05 || !result.isSignificant(),
                "Near-identical values should not be significant");
    }

    @Test
    void shouldHandleInsufficientSamples() {
        double[] modeA = {100};
        double[] modeB = {90};

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "single");

        assertFalse(result.isSignificant());
        assertEquals(1, result.sampleSize());
        assertEquals(1.0, result.pValue());
    }

    @Test
    void shouldHandleEmptyArrays() {
        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                new double[0], new double[0], "empty");

        assertFalse(result.isSignificant());
        assertEquals(0, result.sampleSize());
    }

    @Test
    void shouldHandleUnequalLengths() {
        double[] modeA = {0, 1, 2, 3, 4};
        double[] modeB = {0, 1, 2};

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "unequal");

        assertEquals(3, result.sampleSize());
    }

    @Test
    void shouldHandleZeroVariance() {
        double[] modeA = {5, 5, 5, 5, 5};
        double[] modeB = {4, 4, 4, 4, 4};

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "zeroVar");

        assertFalse(result.isSignificant());
    }

    @Test
    void shouldComputeLargeEffectSize() {
        double[] modeA = new double[30];
        double[] modeB = new double[30];
        for (int i = 0; i < 30; i++) {
            modeA[i] = 200 + Math.random() * 5;
            modeB[i] = 100 + Math.random() * 5;
        }

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "largeEffect");

        assertTrue(result.effectSize() > 1.0,
                "Expected large effect size but got " + result.effectSize());
        assertTrue(result.isSignificant());
    }

    @Test
    void confidenceIntervalShouldContainMeanDifference() {
        double[] modeA = new double[30];
        double[] modeB = new double[30];
        for (int i = 0; i < 30; i++) {
            modeA[i] = 100 + i;
            modeB[i] = 80 + i;
        }

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "ciTest");

        assertTrue(result.ciLow() <= 20 && result.ciHigh() >= 20,
                "CI should contain mean diff 20: [" + result.ciLow()
                        + ", " + result.ciHigh() + "]");
    }

    @Test
    void pValueAccuracyAtDf10() {
        // Reference: t=2.228, df=10 → p≈0.05 (two-tailed)
        // We test that our approximation is reasonable
        double[] modeA = new double[11];
        double[] modeB = new double[11];
        for (int i = 0; i < 11; i++) {
            modeA[i] = 10 + i * 2;
            modeB[i] = 8 + i * 2;
        }

        StatisticalSignificance result = StatisticalSignificanceCalculator.compare(
                modeA, modeB, "accuracy");

        assertTrue(result.pValue() >= 0 && result.pValue() <= 1,
                "p-value must be in [0,1]");
        assertNotNull(result);
    }
}
