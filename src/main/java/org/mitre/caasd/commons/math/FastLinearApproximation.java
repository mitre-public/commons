/*
 *    Copyright 2022 The MITRE Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.mitre.caasd.commons.math;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.min;
import static java.lang.Math.abs;
import static org.mitre.caasd.commons.math.DataSplitter.checkInputData;

import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * A FastLinearApproximation <B>quickly</B> produces an "quasi-optimal" Linear Approximation for the
 * supplied data by exploiting a simplifying assumption that speeds up the line fitting process.
 * <p>
 * The simplifying assumption is that the best fit line passes through the point at (x_midpoint,
 * avg_Y). This assumption can be fine if the x data is relatively evenly spaced. On the other hand,
 * if the X data comes in erratic spurts this assumption is dangerous.
 */
public class FastLinearApproximation {

    List<Double> xData;
    List<Double> yData;
    private final int n;
    private final double halfWidth;
    private final double middleX; // the exact middle of the bin
    private final double avgY;
    private double bestSlope;
    private double error;

    public FastLinearApproximation(List<Double> xData, List<Double> yData) {
        checkInputData(xData, yData);
        checkArgument(!xData.isEmpty());
        this.xData = xData;
        this.yData = yData;

        this.n = xData.size();
        double width = xData.get(n - 1) - xData.get(0);
        this.halfWidth = width / 2.0;
        this.middleX = this.xData.get(0) + halfWidth;
        this.avgY = computeAverageY();

        computeSlopeAndError();
    }

    public FastLinearApproximation(XyDataset data) {
        this(data.xData(), data.yData());
    }

    private double computeAverageY() {
        // DoubleSummaryStatistics is better at handling numeric error than naively computing the average.
        DoubleSummaryStatistics dss = new DoubleSummaryStatistics();
        yData.stream().mapToDouble(x -> x).forEach(dss);
        return dss.getAverage();
    }

    private void computeSlopeAndError() {
        // use these values for the inital points in the binary search
        double lowSlope = avgY / halfWidth; // this slope makes the first Y value 0
        double lowSlopeError = sumSquaredErrorGivenSlope(lowSlope);

        double highSlope = -avgY / halfWidth; // this slope makes the last Y value 0
        double highSlopeError = sumSquaredErrorGivenSlope(highSlope);

        // while slope is changing
        while (abs(lowSlope - highSlope) > 0.00001) {

            if (lowSlopeError < highSlopeError) {
                // low slope is better -- reset highSlope, recompute its error
                highSlope = (lowSlope + highSlope) / 2.0;
                highSlopeError = sumSquaredErrorGivenSlope(highSlope);
            } else {
                // high slope is better -- reset lowSlope, recompute its error
                lowSlope = (lowSlope + highSlope) / 2.0;
                lowSlopeError = sumSquaredErrorGivenSlope(lowSlope);
            }
        }

        this.bestSlope = (lowSlopeError < highSlopeError) ? lowSlope : highSlope;
        this.error = min(lowSlopeError, highSlopeError);
    }

    /**
     * Compute the sum of all squared prediction errors when the interpolating line has the provided
     * slope and goes through (xMidpoint, avgY). This interpolation error can be used to perform a
     * binary search for the "best slope".
     *
     * @param assumedSlope The slope of a presumed interpolating line.
     *
     * @return The totalSumSquareError associated with the presumed interpolating line.
     */
    double sumSquaredErrorGivenSlope(double assumedSlope) {
        double totalSumSquareError = 0;
        for (int i = 0; i < n; i++) {
            /*
             * Predict a y value assuming the linear approximate goes directly through the middleX
             * point at exactly avgY
             */
            double predictedY = avgY + (xData.get(i) - middleX) * assumedSlope;
            double actualY = yData.get(i);

            double err = predictedY - actualY;
            totalSumSquareError += (err * err);
        }
        return totalSumSquareError;
    }

    public double slope() {
        return bestSlope;
    }

    public double averageY() {
        return avgY;
    }

    /** @return The sum of all (predictedY - actualY)^2 terms. */
    public double totalSquaredError() {
        return error;
    }

    /** @return The average of all (predictedY - actualY)^2 terms. */
    public double avgSquaredError() {
        return error / (double) n;
    }

    public double predictY(double xValue) {
        return (xValue - middleX) * bestSlope + avgY;
    }

    public double minX() {
        return xData.get(0);
    }

    public double midpointX() {
        return middleX;
    }

    public double maxX() {
        return xData.get(n - 1);
    }
}
