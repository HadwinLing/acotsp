package com.caffinc.acotsp.util;

/**
 * Provides utility Math functions which are better suited for ACO
 *
 * @author Sriram
 */
public class Math {
    // Approximate power function, Math.pow is quite slow and we don't need accuracy.
    // See:
    // http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
    // Important facts:
    // - >25 times faster
    // - Extreme cases can lead to error of 25% - but usually less.
    // - Does not harm results -- not surprising for a stochastic algorithm.

    /**
     * Approximate power function, optimized for speed, but not accurate.
     * See:
     * http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
     *
     * @param base     Base
     * @param exponent Exponent
     * @return approximate base^exponent
     */
    public static double pow(final double base, final double exponent) {
        final long tmp = Double.doubleToLongBits(base);
        final long tmp2 = (long) (exponent * (tmp - 4606921280493453312L)) + 4606921280493453312L;
        return Double.longBitsToDouble(tmp2);
    }
}
