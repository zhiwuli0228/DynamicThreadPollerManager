package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

/**
 * In-project paired t-test, Cohen's d effect size, and 95% confidence
 * intervals. Uses Abramowitz &amp; Stegun 26.2.17 for normal CDF
 * approximation. No external library dependencies.
 */
public final class StatisticalSignificanceCalculator {

    private StatisticalSignificanceCalculator() {
    }

    /**
     * Compute paired t-test significance comparing two matched-sample arrays.
     *
     * @param modeA      sample values for mode A (closed-loop)
     * @param modeB      sample values for mode B (baseline or static)
     * @param metricName name of the metric being compared
     * @return StatisticalSignificance with p-value, CI, effect size
     */
    public static StatisticalSignificance compare(
            double[] modeA, double[] modeB, String metricName) {

        int n = Math.min(modeA.length, modeB.length);

        if (n < 2) {
            return new StatisticalSignificance(
                    metricName, 1.0, 0, 0, 0, false, n);
        }

        // Paired differences: modeA - modeB
        double sumD = 0;
        double sumSqD = 0;
        for (int i = 0; i < n; i++) {
            double d = modeA[i] - modeB[i];
            sumD += d;
            sumSqD += d * d;
        }

        double meanD = sumD / n;
        double varD = (sumSqD - (sumD * sumD) / n) / (n - 1);

        if (varD <= 0) {
            return new StatisticalSignificance(
                    metricName, 1.0, meanD, meanD, 0, false, n);
        }

        double sdD = Math.sqrt(varD);
        double se = sdD / Math.sqrt(n);
        double t = meanD / se;
        int df = n - 1;

        // t-to-z transformation (design formula)
        double absT = Math.abs(t);
        double z = absT * (1.0 - 1.0 / (4.0 * df))
                / Math.sqrt(1.0 + (absT * absT) / (2.0 * df));

        double pValue = 2.0 * (1.0 - normalCdf(z));

        // 95% CI: meanD ± z_critical(0.025) * se
        double zCrit = 1.96;
        double ciLow = meanD - zCrit * se;
        double ciHigh = meanD + zCrit * se;

        // Cohen's d: meanDiff / pooledStdDev
        double pooledSd = pooledStdDev(modeA, modeB);
        double effectSize = pooledSd > 0 ? Math.abs(meanD) / pooledSd : 0;

        boolean isSignificant = pValue < 0.05;

        return new StatisticalSignificance(
                metricName, pValue, ciLow, ciHigh, effectSize, isSignificant, n);
    }

    // --- Abramowitz & Stegun 26.2.17 normal CDF ---

    private static double normalCdf(double x) {
        if (x < 0) return 1.0 - normalCdf(-x);

        double p = 0.2316419;
        double b1 = 0.319381530;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;

        double t = 1.0 / (1.0 + p * x);
        double phi = standardNormalDensity(x);
        double z = phi * (b1 * t + b2 * t * t + b3 * t * t * t
                + b4 * t * t * t * t + b5 * t * t * t * t * t);

        return 1.0 - z;
    }

    private static double standardNormalDensity(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2.0 * Math.PI);
    }

    private static double pooledStdDev(double[] a, double[] b) {
        double meanA = mean(a);
        double meanB = mean(b);
        double varA = variance(a, meanA);
        double varB = variance(b, meanB);
        return Math.sqrt((varA + varB) / 2.0);
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private static double variance(double[] values, double mean) {
        double sum = 0;
        for (double v : values) {
            double d = v - mean;
            sum += d * d;
        }
        return sum / (values.length - 1);
    }
}
