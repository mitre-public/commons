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
import static java.util.Arrays.asList;
import static org.mitre.caasd.commons.Distance.Unit.*;

import java.io.Serializable;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Iterators;

/**
 * This Distance class is intended to make working with Distances less error prone because (1) all
 * Distance objects are immutable and (2) the unit is always required and always accounted for.
 * <p>
 * This class is extremely similar in spirit and design to java.time.Duration and
 * java.time.Instant.
 */
public class Distance implements Serializable, Comparable<Distance> {

    //There is currently a quirk when dist.equals(ZERO) will fail if the units are different
    public static final Distance ZERO = new Distance(0.0, NAUTICAL_MILES);
    public static final Distance ZERO_FEET = new Distance(0.0, FEET);

    private final double amount;

    private final Unit unit;

    private Distance() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent "standard users" from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */

        this(0.0, NAUTICAL_MILES);
    }

    /**
     * Create an Immutable Distance object that combines the amount and unit into a single quantity
     * (ie 5 meters, 7 feet, or 1.3 nautical miles).
     *
     * @param amount An amount (cannot be NaN)
     * @param unit   The Distance Unit (ie METERS, FEET, or NAUTICAL_MILES)
     */
    public Distance(double amount, Unit unit) {
        this.amount = amount;
        checkArgument(!Double.isNaN(amount), "Distance values of \"Not a Number\" are not supported");
        this.unit = checkNotNull(unit);
    }

    public enum Unit {
        KILOMETERS(0.001, "km"),
        METERS(1.0, "m"),
        FEET(1.0 / 0.3048, "ft"), //this is the exact ratio as per the definition of feet
        MILES(1.0 / (0.3048 * 5_280.0), "mi"), //5,280 ft per mile
        NAUTICAL_MILES(1.0 / 1852.0, "NM"); //this is the exact ratio as per the definition NM

        private final double unitsPerMeter;

        private final String abbreviation;

        Unit(double unitsPerMeter, String suffix) {
            this.unitsPerMeter = unitsPerMeter;
            this.abbreviation = suffix;
        }

        public double unitsPerMeter() {
            return unitsPerMeter;
        }

        public String abbreviation() {
            return abbreviation;
        }
    }

    public static Distance of(double amount, Unit unit) {
        return new Distance(amount, unit);
    }

    public static Distance ofMeters(double amount) {
        return of(amount, METERS);
    }

    public static Distance ofKiloMeters(double amount) {
        return of(amount, KILOMETERS);
    }

    public static Distance ofFeet(double amount) {
        return of(amount, FEET);
    }

    public static Distance ofMiles(double amount) {
        return of(amount, MILES);
    }

    public static Distance ofNauticalMiles(double amount) {
        return of(amount, NAUTICAL_MILES);
    }

    public static Distance between(LatLong one, LatLong two) {
        return one.distanceTo(two);
    }

    /**
     * @return The unit this Distance was originally defined with.
     */
    public Unit nativeUnit() {
        return this.unit;
    }

    /**
     * Convert this Distance into the desired unit.
     *
     * @param desiredUnit A unit like Meters, Feet, or Nautical Miles
     *
     * @return This distance convert to a different unit.
     */
    public double in(Unit desiredUnit) {
        return (this.unit == desiredUnit)
            ? amount
            : amount * desiredUnit.unitsPerMeter / this.unit.unitsPerMeter;
    }

    public double inMeters() {
        return in(METERS);
    }

    public double inKilometers() {
        return in(KILOMETERS);
    }

    public double inNauticalMiles() {
        return in(NAUTICAL_MILES);
    }

    public double inFeet() {
        return in(FEET);
    }

    public double inMiles() {
        return in(MILES);
    }

    public Distance negate() {
        return Distance.of(-amount, unit);
    }

    public Distance abs() {
        return Distance.of(Math.abs(amount), unit);
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public Distance times(double scalar) {
        return Distance.of(amount * scalar, unit);
    }

    public Distance plus(Distance otherDist) {
        return Distance.of(amount + otherDist.in(unit), unit);
    }

    public Distance minus(Distance otherDist) {
        return plus(otherDist.times(-1.0));
    }

    public boolean isLessThan(Distance otherDist) {
        return this.amount < otherDist.in(unit);
    }

    public boolean isLessThanOrEqualTo(Distance otherDist) {
        return this.amount <= otherDist.in(unit);
    }

    public boolean isGreaterThan(Distance otherDist) {
        return this.amount > otherDist.in(unit);
    }

    public boolean isGreaterThanOrEqualTo(Distance otherDist) {
        return this.amount >= otherDist.in(unit);
    }

    /**
     * @param otherDist A second distance
     *
     * @return The ratio between these distances
     */
    public double dividedBy(Distance otherDist) {
        return this.amount / otherDist.in(unit);
    }

    /** @return The Speed required to travel this distance in this amount of time. */
    public Speed dividedBy(Duration lengthOfTime) {
        return new Speed(this, lengthOfTime);
    }

    @Override
    public int compareTo(Distance o) {
        return Double.compare(this.inMeters(), o.inMeters());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.amount) ^ (Double.doubleToLongBits(this.amount) >>> 32));
        hash = 59 * hash + Objects.hashCode(this.unit);
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
        final Distance other = (Distance) obj;
        if (Double.doubleToLongBits(this.amount) != Double.doubleToLongBits(other.amount)) {
            return false;
        }
        if (this.unit != other.unit) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        //use more digits after the decimal place when the units are significantly larger
        if (unit == NAUTICAL_MILES || unit == KILOMETERS || unit == MILES) {
            return toString(5);
        } else {
            return toString(2);
        }
    }

    /**
     * @param digitsAfterDecimalPlace The number of digits after the decimal place to use.
     *
     * @return A String like "1.00m", or "5.143nm"
     */
    public String toString(int digitsAfterDecimalPlace) {
        return String.format("%." + digitsAfterDecimalPlace + "f" + unit.abbreviation, amount);
    }

    /**
     * Create a Distance object by parsing a String. This method works with both Distance.toString
     * methods.
     *
     * @param parseMe A String like "5.0 km", "5.0km", or "-522ft".
     *
     * @return A correctly parsed Distance object
     * @throws NullPointerException     On null input
     * @throws IllegalArgumentException When a unit cannot be determined
     * @throws NumberFormatException    When a quantity can't be extracted
     */
    public static Distance fromString(String parseMe) {
        checkNotNull(parseMe);

        parseMe = parseMe.trim();
        Unit unit = unitFromString(parseMe);

        if (unit == null) {
            throw new IllegalArgumentException("Could not parse Distance Unit from: " + parseMe);
        }

        //store "23.0" instead of "23.0 ft"
        String parseMeWithoutUnitsSuffix = parseMe.substring(0, parseMe.length() - unit.abbreviation.length());

        double amount = Double.parseDouble(parseMeWithoutUnitsSuffix);

        return Distance.of(amount, unit);
    }

    /**
     * Matches the end of a String with the abbreviation of one of the Distance.Unit types
     *
     * @param parseMe A String like "5.0 km", "km", "22ft", or "ft"
     *
     * @return The correct Distance.Unit or null if no match can be found.
     */
    public static Distance.Unit unitFromString(String parseMe) {

        for (Unit unit : Distance.Unit.values()) {
            if (parseMe.endsWith(unit.abbreviation)) {
                return unit;
            }
        }
        return null;
    }

    public static Distance mean(Distance... distances) {
        return mean(asList(distances));
    }

    public static Distance mean(Iterable<Distance> distances) {
        checkNotNull(distances);
        checkArgument(distances.iterator().hasNext(), "Collection of Distances cannot be empty");

        EnumMultiset<Distance.Unit> unitCounts = EnumMultiset.create(Distance.Unit.class);
        double sum = 0;
        for (Distance distance : distances) {
            sum += distance.inMeters();
            unitCounts.add(distance.unit);
        }
        double averageInMeters = sum / (double) unitCounts.size();

        Distance avg = Distance.ofMeters(averageInMeters);
        Distance.Unit unit = mostCommonUnit(unitCounts);
        return Distance.of(avg.in(unit), unit);
    }

    public static Distance sum(Distance... distances) {
        return sum(asList(distances));
    }

    public static Distance sum(Iterable<Distance> distances) {
        checkNotNull(distances);

        if (!distances.iterator().hasNext()) {
            return Distance.ofMeters(0);
        }

        EnumMultiset<Distance.Unit> unitCounts = EnumMultiset.create(Distance.Unit.class);
        double sumInMeters = 0;
        for (Distance distance : distances) {
            sumInMeters += distance.inMeters();
            unitCounts.add(distance.unit);
        }

        Distance avg = Distance.ofMeters(sumInMeters);
        Distance.Unit unit = mostCommonUnit(unitCounts);
        return Distance.of(avg.in(unit), unit);
    }

    public static Distance min(Distance... distances) {
        checkNotNull(distances);
        return min(Iterators.forArray(distances));
    }

    public static Distance min(Iterable<Distance> distances) {
        checkNotNull(distances);
        return min(distances.iterator());
    }

    public static Distance min(Iterator<Distance> distances) {
        checkNotNull(distances);

        if (!distances.hasNext()) {
            return null;
        }

        Distance currentMin = null;

        while (distances.hasNext()) {
            if (currentMin == null) {
                currentMin = distances.next();
                continue;
            }
            currentMin = min(currentMin, distances.next());
        }
        return currentMin;
    }

    public static Distance min(Distance one, Distance two) {
        checkNotNull(one);
        checkNotNull(two);
        return (one.isLessThanOrEqualTo(two))
            ? one
            : two;
    }

    public static Distance max(Distance... distances) {
        checkNotNull(distances);
        return max(Iterators.forArray(distances));
    }

    public static Distance max(Iterable<Distance> distances) {
        checkNotNull(distances);
        return max(distances.iterator());
    }

    public static Distance max(Iterator<Distance> distances) {
        checkNotNull(distances);

        if (!distances.hasNext()) {
            return null;
        }

        Distance currentMax = null;

        while (distances.hasNext()) {
            if (currentMax == null) {
                currentMax = distances.next();
                continue;
            }
            currentMax = max(currentMax, distances.next());
        }
        return currentMax;
    }

    public static Distance max(Distance one, Distance two) {
        checkNotNull(one);
        checkNotNull(two);
        return (one.isGreaterThanOrEqualTo(two))
            ? one
            : two;
    }

    private static Distance.Unit mostCommonUnit(EnumMultiset<Distance.Unit> unitCounts) {

        return unitCounts.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getCount(), e1.getCount()))
            .findFirst().get().getElement();
    }
}
