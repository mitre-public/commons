package org.mitre.caasd.commons.math.locationfit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.time.Instant.EPOCH;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.mitre.caasd.commons.LatLong.clampLongitude;
import static org.mitre.caasd.commons.Spherical.mod;
import static org.mitre.caasd.commons.math.CurveFitters.weightedFit;

import java.time.Duration;
import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import org.mitre.caasd.commons.Acceleration;
import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.Spherical;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * LatLongFitter is an internal helper for LocalPolyInterpolator.
 * <p>
 * This class hides the corrections needed to handle discontinuous longitude values that cross the
 * international date line (e.g. when longitude value wrap around from 180.0 to -180)
 */
class LatLongFitter {
    // NOT PUBLIC because the API is unrefined (using Double as times is janky)

    private final int POLYNOMIAL_DEGREE = 2;

    /** A "well-fit polynomial" that approximates latitude vs. time. */
    private final PolynomialFunction latFunc;

    /** A "well-fit polynomial" that approximates longitude vs. time. */
    private final PolynomialFunction lonFunc;

    /** True if the longitude data crossed the international date line. */
    private boolean crossesDateLine;

    /**
     * Create a LatLongFitter for computing the best fit for KineticValues at time = 0
     *
     * @param weights        The weights of each LatLong sample in the least squares fits
     * @param timesAsEpochMs The time of each LatLong sample (these are normalized around t = 0)
     * @param locations      The raw LatLong data
     */
    LatLongFitter(List<Double> weights, List<Double> timesAsEpochMs, List<LatLong> locations) {
        requireNonNull(weights);
        requireNonNull(timesAsEpochMs);
        requireNonNull(locations);
        checkArgument(timesAsEpochMs.size() == locations.size(), "list sizes must be the same");

        List<Double> lats = locations.stream().map(loc -> loc.latitude()).collect(toList());
        this.latFunc = weightedFit(POLYNOMIAL_DEGREE, weights, timesAsEpochMs, lats);

        List<Double> rawLongs = locations.stream().map(loc -> loc.longitude()).collect(toList());

        this.crossesDateLine = crossesDateLine(rawLongs);

        List<Double> fittableLongs = crossesDateLine ? moddedLongs(rawLongs) : rawLongs;

        this.lonFunc = weightedFit(POLYNOMIAL_DEGREE, weights, timesAsEpochMs, fittableLongs);
    }

    /** @return True if these Longitudes spans the international date line. */
    static boolean crossesDateLine(List<Double> longs) {
        // slightly faster implementations may exist
        DoubleSummaryStatistics stats = longs.stream().mapToDouble(x -> x).summaryStatistics();

        return stats.getMax() - stats.getMin() > 350;
    }

    /**
     * Coerce real Longitude values (e.g. -180 to +180) into a range from 0 to 360.  This change
     * will remove a discontinuity in longitude values if they span the international date line
     * (e.g. go from 179.99 to -179.99).  But! This change adds a discontinuity in longitude values
     * if they span the Prime Meridian (e.g. go from -0.01 to 0.01).
     *
     * @return A list of "stand in" longitude values that range from 0 to 360 (NOT -180 to 180)
     */
    static List<Double> moddedLongs(List<Double> rawLongitudes) {
        // REMOVES discontinuity if longitude values span +180 to -180 ...
        // from: 179.8, 179.9, 180.0, -179.9, -179.8
        // to:   179.8, 179.9, 180.0, 180.1, 180.2

        // ADDS discontinuity if longitude values span -0 to +0
        // from: -0.2, -0.1, 0.0, 0.1, 0.2
        // to:   359.8, 359.9, 0.0, 0.1, 0.2
        return rawLongitudes.stream().map(lon -> mod(lon, 360)).collect(toList());
    }

    /**
     * "Unmod" a longitude value that was coerced from the normal (-180 to 180) range to the (0 to
     * 360) range. Here we "reverse" an international data line correction.
     */
    static Double unMod(double coercedLongitude) {
        checkArgument(0 <= coercedLongitude && coercedLongitude <= 360.0);

        return coercedLongitude > 180 ? coercedLongitude - 360 : coercedLongitude;
    }

    /**
     * Use the PolynomialFunction to deduce a LatLong at a specific moment in time.
     *
     * @param timeInEpochMs 0 = "now", +1000 = 1sec into the future, -1000 = 1sec into the past
     */
    private LatLong fitLocationAt(Double timeInEpochMs) {

        double latitude = latFunc.value(timeInEpochMs);
        double longitude = lonFunc.value(timeInEpochMs);

        // when the raw data crossed the international date line we also "undo" the correction
        if (crossesDateLine) {
            longitude = unMod(longitude);
        } else {
            // We have a documented failure where all longitude values are "-179.99 ish"
            // Here, the raw data DID NOT cross the IDL but an interpolated longitude did!
            // 99.999% of the time this is a NO-OP, but we clamp to avoid "Bad Longitude Exceptions"
            longitude = clampLongitude(longitude);
        }

        return LatLong.of(latitude, longitude);
    }

    /**
     * Efficiently deduce a batch of "Kinetic Values".  Access the internal polynomials function as
     * few times as possible.
     * <p>
     * This method is intentionally not public, it should not leak into the public API.
     */
    KineticValues fitKineticsAtTimeZero() {

        // deduce location @ Time=0 for polynomials
        LatLong location = fitLocationAt(0.0);

        // deduce speed & course @ Time=0
        final long TIME_STEP_IN_MS = 1_000;
        // If this timestep is too small then the acceleration approximate becomes trash
        // (e.g. 50MS = fail)

        LatLong priorLocation = fitLocationAt((double) -TIME_STEP_IN_MS);
        LatLong futureLocation = fitLocationAt((double) TIME_STEP_IN_MS);
        Instant priorTime = EPOCH.minusMillis(TIME_STEP_IN_MS);
        Instant futureTime = EPOCH.plusMillis(TIME_STEP_IN_MS);

        Speed speed = Speed.between(priorLocation, priorTime, futureLocation, futureTime);
        Course course = Spherical.courseBtw(priorLocation, futureLocation);

        // estimate acceleration...
        Speed priorSpeed = Speed.between(priorLocation, priorTime, location, EPOCH);
        Speed futureSpeed = Speed.between(location, EPOCH, futureLocation, futureTime);

        // (speed delta) / (time Delta) = derivative of speed over time = acceleration..
        Speed speedDelta = futureSpeed.minus(priorSpeed);
        Acceleration acceleration = Acceleration.of(speedDelta, Duration.ofMillis(TIME_STEP_IN_MS));

        return new KineticValues(location, speed, course, acceleration);
    }

    /**
     * @return The approximate turnRate in degree per second at time Zero (i.e. the sampleTime used
     * when fitting the polynomials)
     */
    double deduceTurnRate() {

        final double TIME_STEP_IN_MS = 500;

        LatLong stepBack = fitLocationAt(-TIME_STEP_IN_MS); // backward 1/2 second
        LatLong current = fitLocationAt(0.0);
        LatLong stepForward = fitLocationAt(TIME_STEP_IN_MS); // forward 1/2 second

        Course priorToCurrent = Spherical.courseBtw(stepBack, current);
        Course currentToNext = Spherical.courseBtw(current, stepForward);

        Course delta = Course.angleBetween(currentToNext, priorToCurrent);

        return delta.inDegrees(); // change in course over 1 second
    }

    /** KineticValues provides instantaneous values for location, speed, course, and acceleration. */
    static class KineticValues {

        private final LatLong location;
        private final Speed speed;
        private final Course course;
        private final Acceleration acceleration;

        public KineticValues(LatLong location, Speed speed, Course course, Acceleration acceleration) {
            requireNonNull(location);
            requireNonNull(speed);
            requireNonNull(course);
            requireNonNull(acceleration);
            this.location = location;
            this.speed = speed;
            this.course = course;
            this.acceleration = acceleration;
        }

        public LatLong latLong() {
            return location;
        }

        public Speed speed() {
            return speed;
        }

        public Course course() {
            return course;
        }

        public Acceleration acceleration() {
            return acceleration;
        }
    }
}
