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

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static java.util.stream.Collectors.toList;
import static org.mitre.caasd.commons.math.CurveFitters.weightedFit;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.mitre.caasd.commons.*;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * A LocalPolyInterpolator converts a sequence of Positions into a KineticPosition that fits the
 * local trends in position data. Polynomials fitting the local position data are found using
 * Weighted Least Squares. These "best fit" polynomials are then used to generate KineticPositions.
 *
 * <p>Generally, a LocalPolyInterpolator is used repeatedly to manufacture many noise-reduced
 * KineticPositions.
 *
 * <p>A LocalPolyInterpolator can be configured to ignore altitude data. Ignoring altitude data
 * will increase computational speed (because less work is done) and allow the input Positions to
 * not have altitude data.
 *
 * <p>NOTE: The polynomial fitting process has a stricter requirement than just "N input samples".
 * More precisely, the polynomial fitting process requires "M input samples that together cover at
 * least N unique x values".  For example, we cannot find a unique best fit parabola for the data
 * (0,0), (1,0), (1,1), (1,2), (1,3), (1,4).  Yes, there are 6 input points.  But, 5 of these input
 * points share the same x value.  Consequently, there are only two unique x values: {0, 1}.  The
 * polynomial curve fitting process used herein has this limitation. Therefore, if the input
 * Positions reuse timestamps it will increase the number of data points that need to be in the
 * interpolation window.
 */
public class LocalPolyInterpolator implements PositionInterpolator {

    /** Computes Normally distributed weights for the data points we are fitting. */
    private final GaussianWindow weightedWindow;

    // Only generate synthetic points when this many points are inside the sampling window
    private final int requiredPoints;

    /**
     * When true, the "altitude fitting computation" is skipped AND the input PositionRecord do not
     * have their altitude queried (thus the value can be null)
     */
    private final boolean ignoreAltitude;

    /**
     * Create a LocalPolyInterpolator that reflects altitude data AND requires 3 or more
     * PositionRecords to generate KineticRecords.
     *
     * @param windowSize Defines what "local" mean when generating polynomials that fit the "local"
     *                   data. Position data inside this temporal window is included when generating
     *                   polynomials that summarize the Position data supplied when calling the
     *                   "interpolate" method.
     *
     *                   <p>Note: this windowSize should be thought of as a "diameter" not a
     *                   "radius".
     */
    public LocalPolyInterpolator(Duration windowSize) {
        this(windowSize, 3, false);
    }

    /**
     * Create a LocalPolyInterpolator that reflects altitude data AND requires N or more
     * PositionRecords to generate KineticRecords.
     *
     * @param windowSize     Defines what "local" mean when generating polynomials that fit the
     *                       "local" data. Position data inside this temporal window is included
     *                       when generating polynomials that summarize the Position data supplied
     *                       when calling the "interpolate" method
     *
     *                       <p>Note: this windowSize should be thought of as a "diameter" not a
     *                       "radius".
     * @param requiredPoints The number of PositionRecords that are required to generate
     *                       KineticRecords
     */
    public LocalPolyInterpolator(Duration windowSize, int requiredPoints) {
        this(windowSize, requiredPoints, false);
    }

    /**
     * Create a LocalPolyInterpolator that MAY or MAY NOT reflects altitude data AND requires N or
     * more PositionRecords to generate KineticRecords.  Note: ignoring altitude data will decrease
     * compute time.
     *
     * @param windowSize     Defines what "local" mean when generating polynomials that fit the
     *                       "local" data. Position data inside this temporal window is included
     *                       when generating polynomials that summarize the Position data supplied
     *                       when calling the "interpolate" method
     *
     *                       <p>Note: this windowSize should be thought of as a "diameter" not a
     *                       "radius".
     * @param requiredPoints The number of PositionRecords that are required to generate
     *                       KineticRecords
     * @param ignoreAltitude Do not interact with altitude data in anyway.  {@link KineticRecord}s
     *                       produced using the interpolate method will have their Altitude and
     *                       Climb rates set to zero.
     */
    public LocalPolyInterpolator(Duration windowSize, int requiredPoints, boolean ignoreAltitude) {
        this.weightedWindow = new GaussianWindow(windowSize);
        this.requiredPoints = requiredPoints;
        this.ignoreAltitude = ignoreAltitude;
    }

    /**
     * @param positionData A time sorted list of location data tracking a single object.
     * @param sampleTime   The time at which a "KineticRecord" will be made.
     *
     * @return A KineticPosition whose 1st order (e.g., latitude, longitude, and altitude) and 2nd
     * order (e.g., speed, course, climbRate, and turnRate) numerically fits the provided Position
     * data.
     */
    @Override
    public Optional<KineticPosition> interpolate(List<Position> positionData, Instant sampleTime) {

        // Start with the TimeWindow that will have non-zero sample weights...
        final TimeWindow sampleWindow = weightedWindow.windowCenteredAt(sampleTime);

        // isolate points inside the sample window so polynomial fitting has less data to churn through
        List<Position> pointsInSampleWindow = positionData.stream()
                .filter(pt -> sampleWindow.contains(pt.time()))
                .collect(toList());

        long numUniqueTimes =
                pointsInSampleWindow.stream().map(pt -> pt.time()).distinct().count();

        // Not enough sample data to perform polynomial fitting
        //   Data point sharing the same timestamp don't count!
        if (numUniqueTimes < requiredPoints) {
            return Optional.empty();
        }

        // Determine the range of our sample data
        TimeWindow dataSupportedWindow = TimeWindow.of(
                pointsInSampleWindow.get(0).time(),
                pointsInSampleWindow.get(pointsInSampleWindow.size() - 1).time());

        // NEVER generate a KineticRecord when the source data does not "surround" the sample time
        if (!dataSupportedWindow.contains(sampleTime)) {
            return Optional.empty();
        }

        long sampleEpochTime = sampleTime.toEpochMilli();

        // Save data we'll need for polynomial fitting
        List<Double> timesAsEpochMs = newArrayList();
        List<Double> weights = newArrayList();
        List<Double> lats = newArrayList();
        List<Double> longs = newArrayList();
        // altitudes are extracted separately in case they are ignored
        List<Double> altitudes = extractAltitudes(pointsInSampleWindow);

        for (Position pt : pointsInSampleWindow) {
            /*
             * IMPORTANT! **MUST** normalize time values around 0. "Quadratic" fitting will not
             * benefit from quadratic term when the x-values are all HUGE numbers (e.g. uncorrected
             * epochMilli values).
             */
            timesAsEpochMs.add((double) (pt.timeAsEpochMs() - sampleEpochTime));
            weights.add(weightedWindow.computeGaussianWeight(sampleTime, pt.time()));
            lats.add(pt.latitude());
            longs.add(pt.longitude());
        }

        // These two polynomials allow us to deduce location, speed, course, and turnRate
        PolynomialFunction latFunc = weightedFit(2, weights, timesAsEpochMs, lats);
        PolynomialFunction lonFunc = weightedFit(2, weights, timesAsEpochMs, longs);

        // When fitting altitude...
        // (1) Use a line fit because a quadratic fit is too sensitive and volatile

        /*
         * When fitting altitude...
         *
         * (1) Use a line fit because a quadratic fit is too sensitive and volatile.  For example,
         * when an aircraft spends 60 seconds at 3500ft and then the next 60 seconds at 3600 ft we
         * may not want the altitude to QUICKLY jump from 3500 to 3600 because then the climb rate
         *  for one (or more) of the KineticRecords may indicate an extreme value that is not
         * warranted
         *
         * (2) We may want a "is climbing" type rule to use interpolation technique A when climbing,
         * and technique B when not climbing
         *
         * (3) The "isClimbing" state might be "has that altitude been strictly increasing for X
         * seconds and passed more then X ft.
         */
        PolynomialFunction altitudeFunction = (ignoreAltitude)
                ? new PolynomialFunction(new double[] {0.0, 0.0}) // Altitudes are always 0, so is climbrate
                : weightedFit(1, weights, timesAsEpochMs, altitudes);

        BatchDeductions deductions = new BatchDeductions(latFunc, lonFunc);

        KineticPosition kinetics = KineticPosition.builder()
                .time(sampleTime)
                .latLong(deductions.location)
                .altitude(deduceAltitude(altitudeFunction))
                .climbRate(deduceClimbRate(altitudeFunction))
                .speed(deductions.speed)
                .course(deductions.course)
                .acceleration(deductions.acceleration)
                .turnRate(deduceTurnRate(latFunc, lonFunc))
                .build();

        return Optional.of(kinetics);
    }

    // only extract data when we'll use it -- Allows PositionRecords to not implement altitude()
    private <T> List<Double> extractAltitudes(List<Position> pointsInSampleWindow) {
        return (ignoreAltitude)
                ? newArrayList()
                : pointsInSampleWindow.stream()
                        .map(pt -> pt.altitude().inFeet())
                        .collect(toList());
    }

    /**
     * @param altitudeFunction A polynomials which gives altitude (in feet) vs. time (in millis)
     * @return The approximated altitude at time zero
     */
    private Distance deduceAltitude(PolynomialFunction altitudeFunction) {
        double altitudeInFt = altitudeFunction.value(0);
        return Distance.ofFeet(altitudeInFt);
    }

    /**
     * @param latFunc A polynomial which gives latitude vs time
     * @param lonFunc A polynomial which gives longitude vs time
     * @return The approximate Course at time Zero (i.e. the sampleTime used when fitting the
     * polynomials).
     *
     * <p>WARNING -- Course estimates are ESSENTIALLY NOISE when the input position data is NOT
     * MOVING. The latFunc and lonFunc have near zero slopes and the course estimation has nothing
     * to work off of.  It could be possible to compute the course using a much wider time window
     * aperture if the speed is less than some threshold value.
     *
     * <p>This is a low priority issue because (1) when the aircraft isn't moving the "course" is
     * not very valuable (2) it would be easy enough to manually correct a course when speed is
     * near-zero
     */
    private Course deduceCourse(PolynomialFunction latFunc, PolynomialFunction lonFunc) {

        final long TIME_STEP_IN_MS = 100;

        LatLong stepBack = LatLong.of(latFunc.value(-TIME_STEP_IN_MS), lonFunc.value(-TIME_STEP_IN_MS));
        LatLong stepForward = LatLong.of(latFunc.value(TIME_STEP_IN_MS), lonFunc.value(TIME_STEP_IN_MS));

        return Spherical.courseBtw(stepBack, stepForward);
    }

    /**
     * @param latFunc A polynomial which gives latitude vs time
     * @param lonFunc A polynomial which gives longitude vs time
     * @return The approximate turnRate in degree per second at time Zero (i.e. the sampleTime used
     * when fitting the polynomials)
     */
    private double deduceTurnRate(PolynomialFunction latFunc, PolynomialFunction lonFunc) {

        final long TIME_STEP = 500;

        LatLong stepBack = LatLong.of(latFunc.value(-TIME_STEP), lonFunc.value(-TIME_STEP)); // backward 1/2 second
        LatLong current = LatLong.of(latFunc.value(0), lonFunc.value(0));
        LatLong stepForward = LatLong.of(latFunc.value(TIME_STEP), lonFunc.value(TIME_STEP)); // forward 1/2 second

        Course priorToCurrent = Spherical.courseBtw(stepBack, current);
        Course currentToNext = Spherical.courseBtw(current, stepForward);

        Course delta = Course.angleBetween(currentToNext, priorToCurrent);

        return delta.inDegrees(); // change in course over 1 second
    }

    /**
     * @param altitudeFunction A polynomials which gives altitude (in feet) vs. time (in millis)
     * @return The ClimbRate at time zero
     */
    private Speed deduceClimbRate(PolynomialFunction altitudeFunction) {
        double climbRateInFtPerMilli = altitudeFunction.derivative().value(0);

        return Speed.of(climbRateInFtPerMilli, Speed.Unit.FEET_PER_MINUTE);
    }

    /**
     * Deduce a batch of "Kinetic Values" because they rely on shared intermediate values.
     */
    private static class BatchDeductions {

        private final LatLong location;
        private final Speed speed;
        private final Course course;
        private final Acceleration acceleration;

        /**
         * Use the provided "location polynomials" to numerical estimate a few kinetic values.
         *
         * @param latFunc A "well-fit polynomial" that approximates latitude vs. time
         * @param lonFunc A "well-fit polynomial" that approximates longitude vs. time
         */
        BatchDeductions(PolynomialFunction latFunc, PolynomialFunction lonFunc) {

            // deduce location @ Time=0 for polynomials
            this.location = LatLong.of(latFunc.value(0), lonFunc.value(0));

            // deduce speed & course @ Time=0
            final long TIME_STEP_IN_MS =
                    1_000; // If this timestep is too small then the acceleration approximate becomes trash (e.g. 50MS =
            // fail)
            LatLong priorLocation = LatLong.of(latFunc.value(-TIME_STEP_IN_MS), lonFunc.value(-TIME_STEP_IN_MS));
            LatLong futureLocation = LatLong.of(latFunc.value(TIME_STEP_IN_MS), lonFunc.value(TIME_STEP_IN_MS));
            Instant priorTime = EPOCH.minusMillis(TIME_STEP_IN_MS);
            Instant futureTime = EPOCH.plusMillis(TIME_STEP_IN_MS);

            this.speed = Speed.between(priorLocation, priorTime, futureLocation, futureTime);
            this.course = Spherical.courseBtw(priorLocation, futureLocation);

            // estimate acceleration...
            Speed priorSpeed = Speed.between(priorLocation, priorTime, location, EPOCH);
            Speed futureSpeed = Speed.between(location, EPOCH, futureLocation, futureTime);

            // (speed delta) / (time Delta) = derivative of speed over time = acceleration..
            Speed speedDelta = futureSpeed.minus(priorSpeed);
            this.acceleration = Acceleration.of(speedDelta, Duration.ofMillis(TIME_STEP_IN_MS));
        }
    }
}
