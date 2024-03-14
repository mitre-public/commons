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

package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Double.compare;
import static org.mitre.caasd.commons.Distance.Unit.*;
import static org.mitre.caasd.commons.Speed.Unit.*;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * This Speed class is intended to make working with Speeds less error prone because (1) all Speed
 * objects are immutable and (2) the unit is always required and always accounted for.
 * <p>
 * This class is extremely similar in spirit and design to java.time.Duration, java.time.Instant,
 * and the Distance class (in this package).
 */
public class Speed implements Serializable, Comparable<Speed> {

    public static final Speed ZERO = Speed.of(0.0, KNOTS);
    public static final Speed ZERO_FEET_PER_MIN = Speed.of(0.0, FEET_PER_MINUTE);

    private static final double SEC_PER_HOUR = 60.0 * 60.0;

    private final Distance.Unit distanceUnit;

    private final double amountPerSecond;

    private Speed() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent "standard users" from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */

        this(NAUTICAL_MILES, 0.0);
    }

    public Speed(Distance dist, Duration timeDelta) {
        checkNotNull(dist);
        checkNotNull(timeDelta);
        checkArgument(!timeDelta.isNegative());
        checkArgument(!timeDelta.isZero());
        this.distanceUnit = dist.nativeUnit();
        double msElapsed = timeDelta.toMillis();
        this.amountPerSecond = dist.in(distanceUnit) / (msElapsed / 1_000.0);
    }

    private Speed(Distance dist, double msElapsed) {
        this(
                dist.nativeUnit(),
                dist.in(dist.nativeUnit()) / (msElapsed / 1_000.0) // compute distance units PerSecond
                );
    }

    private Speed(Distance.Unit distUnit, double amountPerSecond) {
        this.distanceUnit = distUnit;
        this.amountPerSecond = amountPerSecond;
    }

    @Override
    public int compareTo(Speed speed) {
        return compare(this.inMetersPerSecond(), speed.inMetersPerSecond());
    }

    public enum Unit {
        KNOTS(NAUTICAL_MILES, SEC_PER_HOUR, "kn"),
        METERS_PER_SECOND(METERS, 1.0, "mps"),
        FEET_PER_SECOND(FEET, 1.0, "fps"),
        FEET_PER_MINUTE(FEET, 60.0, "fpm"),
        MILES_PER_HOUR(MILES, SEC_PER_HOUR, "mph"),
        KILOMETERS_PER_HOUR(KILOMETERS, SEC_PER_HOUR, "kph");

        private final Distance.Unit distUnit;

        final double secondsPerTimeUnit;

        final String abbreviation;

        Unit(Distance.Unit distUnit, double secPerTimeUnit, String suffix) {
            this.distUnit = distUnit;
            this.secondsPerTimeUnit = secPerTimeUnit;
            this.abbreviation = suffix;
        }

        public Distance.Unit distanceUnit() {
            return distUnit;
        }

        public double secondsPerTimeUnit() {
            return secondsPerTimeUnit;
        }

        public String abbreviation() {
            return abbreviation;
        }
    }

    public static Speed ofMilesPerHour(double amount) {
        return of(amount, MILES_PER_HOUR);
    }

    public static Speed ofKilometersPerHour(double amount) {
        return of(amount, KILOMETERS_PER_HOUR);
    }

    public static Speed ofMetersPerSecond(double amount) {
        return of(amount, METERS_PER_SECOND);
    }

    public static Speed ofFeetPerSecond(double amount) {
        return of(amount, FEET_PER_SECOND);
    }

    public static Speed ofFeetPerMinute(double amount) {
        return of(amount, FEET_PER_MINUTE);
    }

    public static Speed ofKnots(double amount) {
        return of(amount, KNOTS);
    }

    public static Speed of(double amount, Unit unit) {
        return new Speed(Distance.of(amount, unit.distUnit), unit.secondsPerTimeUnit * 1000.0);
    }

    /**
     * Compute the speed required to travel between these two position and times
     *
     * @param pos1  The first LatLong position
     * @param time1 The Instant you are at the first position
     * @param pos2  A second LatLong position
     * @param time2 The Instant you are at the second position
     *
     * @return The Speed you would have to travel to go from the first position to the second
     *     position given the time that elapsed between the two Instants.
     */
    public static Speed between(LatLong pos1, Instant time1, LatLong pos2, Instant time2) {
        Distance distance = pos1.distanceTo(pos2);
        Duration timeDelta = Duration.between(time1, time2).abs();

        return new Speed(distance, timeDelta);
    }

    public double inKnots() {
        return in(KNOTS);
    }

    public double inMetersPerSecond() {
        return in(METERS_PER_SECOND);
    }

    public double inFeetPerSecond() {
        return in(FEET_PER_SECOND);
    }

    public double inFeetPerMinutes() {
        return in(FEET_PER_MINUTE);
    }

    public double inKilometersPerHour() {
        return in(KILOMETERS_PER_HOUR);
    }

    public double inMilesPerHour() {
        return in(MILES_PER_HOUR);
    }

    public boolean isLessThan(Speed otherSpd) {
        return this.inMetersPerSecond() < otherSpd.inMetersPerSecond();
    }

    public boolean isLessThanOrEqualTo(Speed otherSpd) {
        return this.inMetersPerSecond() <= otherSpd.inMetersPerSecond();
    }

    public boolean isGreaterThan(Speed otherSpd) {
        return this.inMetersPerSecond() > otherSpd.inMetersPerSecond();
    }

    public boolean isGreaterThanOrEqualTo(Speed otherSpd) {
        return this.inMetersPerSecond() >= otherSpd.inMetersPerSecond();
    }

    public double in(Unit unit) {
        // e.g. find factor needed to convert Feet to Kilometers
        double conversionFactor = unit.distUnit.unitsPerMeter() / this.distanceUnit.unitsPerMeter();

        return this.amountPerSecond * unit.secondsPerTimeUnit * conversionFactor;
    }

    /**
     * @return A String that contains 3 digits after the decimal place that always uses the default
     *     "Speed Unit" for this Speed's underlying distance. For example, a Speed of 12.01 Nautical
     *     miles in 15 minutes becomes "48.040kn" and 32 feet in 30 seconds becomes "64.000fpm"
     */
    @Override
    public String toString() {
        return toString(3);
    }

    /** @return A String that contains n digits after the decimal place (e.g. "42.0mph"). */
    public String toString(int digitsAfterDecimalPlace) {
        return toString(digitsAfterDecimalPlace, speedUnitFor(this.distanceUnit));
    }

    /**
     * Force to "toString" output to be expressed in a particular Speed unit. This method can help
     * ensure that all "text output" uses a consistent formatting schema.
     *
     * @param digitsAfterDecimalPlace The number of digits after the decimal
     * @param unit                    Express the Speed in this unit (e.g. KNOTS, METERS_PER_SECOND,
     *                                etc.)
     *
     * @return A String that contains n digits after the decimal place (e.g. "42.00mps")
     */
    public String toString(int digitsAfterDecimalPlace, Unit unit) {
        return String.format("%." + digitsAfterDecimalPlace + "f" + unit.abbreviation, in(unit));
    }

    /**
     * The "best" unit to write a Speed usually depends on how the internal "distance" value is
     * defined. For example, if a Speed is defined using a distance denominated in "Nautical miles"
     * then we'd typically want to report that speed in KNOTS and not, say, Feet per minute. This
     * method picks the mostly likely Speed Unit given the distance unit a speed is defined with.
     *
     * @param distanceUnit e.g. NAUTICAL_MILES, METERS, KILOMETERS, MILES, or FEET.
     *
     * @return A speed unit like KNOTS, METERS_PER_SECOND, KILOMETERS_PER_HOUR, MILES_PER_HOUR, or
     *     FEET_PER_MINUTE.
     */
    public static Speed.Unit speedUnitFor(Distance.Unit distanceUnit) {

        switch (distanceUnit) {
            case NAUTICAL_MILES:
                return KNOTS;
            case FEET:
                return FEET_PER_MINUTE;
            case KILOMETERS:
                return KILOMETERS_PER_HOUR;
            case METERS:
                return METERS_PER_SECOND;
            case MILES:
                return MILES_PER_HOUR;
            default:
                throw new IllegalArgumentException("unknown unit: " + distanceUnit);
        }
    }

    /**
     * Create a Speed object by parsing a String. This method works with both Speed.toString
     * methods.
     *
     * @param parseMe A String like "5.0 kph", "5.0kph", or "-52kph".
     *
     * @return A correctly parsed Speed object
     * @throws NullPointerException     On null input
     * @throws IllegalArgumentException When a unit cannot be determined
     * @throws NumberFormatException    When a quantity can't be extracted
     */
    public static Speed fromString(String parseMe) {
        checkNotNull(parseMe);

        parseMe = parseMe.trim();
        Unit speedUnit = unitFromString(parseMe);

        if (speedUnit == null) {
            throw new IllegalArgumentException("Could not parse Speed Unit from: " + parseMe);
        }

        // store "23.0" instead of "23.0 mph"
        String parseMeWithoutUnitsSuffix = parseMe.substring(0, parseMe.length() - speedUnit.abbreviation.length());

        double amount = Double.parseDouble(parseMeWithoutUnitsSuffix);

        return Speed.of(amount, speedUnit);
    }

    /**
     * Examine the end of a String and determine what units it contains by matching the end of the
     * string with the abbreviation for a Speed.Unit.
     *
     * @param parseMe A String like "5.0 mph", "mph", "22mps", or "mps"
     *
     * @return The correct Speed.Unit or null if no match can be found.
     */
    public static Speed.Unit unitFromString(String parseMe) {
        // given a String like: "23 fpm" or "23"

        for (Unit unit : Speed.Unit.values()) {
            if (parseMe.endsWith(unit.abbreviation)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * @param amountOfTime An amount of time
     *
     * @return The distance traveled in this amountOfTime at this Speed
     */
    public Distance times(Duration amountOfTime) {
        checkArgument(!amountOfTime.isNegative());

        double amount = (double) (amountOfTime.toMillis() / 1000.0) * amountPerSecond;
        return Distance.of(amount, this.distanceUnit);
    }

    public Speed times(double scalar) {
        return new Speed(distanceUnit, amountPerSecond * scalar);
    }

    public Speed plus(Speed otherSpeed) {

        switch (this.distanceUnit) {
            case NAUTICAL_MILES:
                return Speed.of(this.inKnots() + otherSpeed.inKnots(), KNOTS);
            case METERS:
                return Speed.of(this.inMetersPerSecond() + otherSpeed.inMetersPerSecond(), METERS_PER_SECOND);
            case FEET:
                return Speed.of(this.inFeetPerSecond() + otherSpeed.inFeetPerSecond(), FEET_PER_SECOND);
            case MILES:
                return Speed.of(this.inMilesPerHour() + otherSpeed.inMilesPerHour(), MILES_PER_HOUR);
            case KILOMETERS:
                return Speed.of(this.inKilometersPerHour() + otherSpeed.inKilometersPerHour(), KILOMETERS_PER_HOUR);
            default:
                throw new IllegalStateException("Unkown distanceUnit: " + this.distanceUnit);
        }
    }

    public Speed minus(Speed otherSpeed) {
        return this.plus(otherSpeed.times(-1.0));
    }

    /** @return The absolute value of this speed. */
    public Speed abs() {
        return (this.isNegative()) ? this.times(-1.0) : this;
    }

    /**
     * @param dist A distance
     *
     * @return The time required to travel this distance at this Speed.
     */
    public Duration timeToTravel(Distance dist) {
        checkNotNull(dist);

        double amountInDesiredUnit = dist.in(distanceUnit);
        double timeInSec = amountInDesiredUnit / amountPerSecond;
        double timeInMs = (long) (timeInSec * 1000.0);
        return Duration.ofMillis((long) timeInMs);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.distanceUnit);
        hash = 67 * hash
                + (int) (Double.doubleToLongBits(this.amountPerSecond)
                        ^ (Double.doubleToLongBits(this.amountPerSecond) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Speed other = (Speed) obj;
        if (Double.doubleToLongBits(this.amountPerSecond) != Double.doubleToLongBits(other.amountPerSecond)) {
            return false;
        }
        if (this.distanceUnit != other.distanceUnit) {
            return false;
        }
        return true;
    }

    public boolean isPositive() {
        return amountPerSecond > 0;
    }

    public boolean isNegative() {
        return amountPerSecond < 0;
    }

    public boolean isZero() {
        return amountPerSecond == 0;
    }
}
