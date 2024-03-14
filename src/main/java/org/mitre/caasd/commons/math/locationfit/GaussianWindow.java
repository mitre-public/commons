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

package org.mitre.caasd.commons.math.locationfit;

import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;

import org.mitre.caasd.commons.TimeWindow;

/**
 * A GaussianWindow generates "normally distributed weights" for time-series data.
 *
 * <p>Use case: say you are given a time-series data set like "altitude vs time". You are then
 * asked to generate a synthetic data point that fits the pattern in the provided data. In this
 * situation you'd want to create a "local fit" for the raw data you have. The weights provided by a
 * GaussianWindow will allow you to generate "best fit curve" that only reflects the data around the
 * moment-in-time you are trying to fit.
 */
public class GaussianWindow {

    /** A window is 6 standard deviation. */
    private final Duration windowSize;

    /** One standard deviation (in milliseconds). */
    private final double sigmaInMilli;

    /**
     * @param windowSize The length of time between the -3 standard deviation and +3 standard
     *                   deviation
     */
    public GaussianWindow(Duration windowSize) {
        requireNonNull(windowSize);
        this.windowSize = windowSize;
        this.sigmaInMilli = (double) (windowSize.toMillis() / 6);
    }

    /**
     * @param centerOfWindow The moment in the window with highest weight (e.g. the center of the
     *                       normal distribution)
     * @param queryTime      Generate a guassianWeight for this moment
     *
     * @return A Gaussian weight for the query time (i.e. a weight that will form a normal
     *     distribution)
     */
    public Double computeGaussianWeight(Instant centerOfWindow, Instant queryTime) {
        long diff = abs(queryTime.toEpochMilli() - centerOfWindow.toEpochMilli());
        double zScore = ((double) diff / sigmaInMilli);

        return Math.exp(-(zScore * zScore) / 2.0);
    }

    /**
     * Generate a TimeWindow "centered around" a specific moment in time. This TimeWindow can be
     * used to filter time-series data so that ONLY the data points with non-zero weights are
     * considered in the computation. In other words, these TimeWindows help you speed up the
     * fitting process by reducing the size of a data set fed to a curve fitting algorithm.
     *
     * <p>NOTE: When this TimeWindow is used to filter data is will eliminate data beyond
     * plus/minus
     * 3 standard deviation (aka data points with a weight less than 0.011108996538242)
     *
     * @param center The midpoint of the newly generated TimeWindow (whose Duration matches this
     *               window's Duration)
     *
     * @return A new TimeWindow with the corresponding Duration and center
     */
    public TimeWindow windowCenteredAt(Instant center) {
        long halfWidth = windowSize.toMillis() / 2;

        return TimeWindow.of(center.minusMillis(halfWidth), center.plusMillis(halfWidth));
    }

    public Duration sigma() {
        return Duration.ofMillis((long) sigmaInMilli);
    }
}
