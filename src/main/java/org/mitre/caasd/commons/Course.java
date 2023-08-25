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
import static java.lang.Math.PI;
import static org.mitre.caasd.commons.Course.Unit.DEGREES;
import static org.mitre.caasd.commons.Course.Unit.RADIANS;
import java.util.Objects;

import org.apache.commons.math3.util.FastMath;

/**
 * This class is intended to make working with Courses less error prone because (1) all Course
 * objects are immutable and (2) the unit is always required and always accounted for.
 *
 * <p>Course is similar in spirit to the LatLong, Distance, and Speed classes. These classes (along
 * with java.time.Instant and java.time.Duration) are particularly powerful when used to clarify
 * method signatures. For example "doSomthing(LatLong, Speed, Distance, Course)" is easier to
 * understand than "doSomthing(Double, Double, Double, Double, Double)"
 *
 * <p>There is a difference between a "Course" and a "Heading". The Course of an aircraft is the
 * direction this aircraft is MOVING. The Heading of an aircraft is the direction the aircraft is
 * POINTING. This distinction is important when (a) an aircraft is impacted by the wind and (b) when
 * a helicopter flies in a direction is it's pointed.
 */
public class Course implements Comparable<Course> {

    public static final Course ZERO = new Course(0.0, DEGREES);

    public static final Course NORTH = Course.ofDegrees(0);
    public static final Course EAST = Course.ofDegrees(90);
    public static final Course SOUTH = Course.ofDegrees(180);
    public static final Course WEST = Course.ofDegrees(270);

    private final double angle;

    private final Unit unit;

    private Course() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent "standard users" from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */
        this(0.0, DEGREES);
    }

    /**
     * Create an Immutable Course object that combines an angle and unit into a single quantity (ie
     * 5 degrees or 0.12 radians).
     *
     * @param angle An angle (cannot be NaN)
     * @param unit  The Distance Unit (ie METERS, FEET, or NAUTICAL_MILES)
     */
    public Course(double angle, Unit unit) {
        this.angle = angle;
        checkArgument(!Double.isNaN(angle), "\"Not a Number\" is not supported");
        this.unit = checkNotNull(unit);
    }

    public enum Unit {
        DEGREES(1.0, "deg"),
        RADIANS(PI / 180.0, "rad");

        private final double unitsPerDegree;

        private final String suffix;

        Unit(double unitsPerMeter, String suffix) {
            this.unitsPerDegree = unitsPerMeter;
            this.suffix = suffix;
        }

        public double unitsPerDegree() {
            return unitsPerDegree;
        }

        public String abbreviation() {
            return suffix;
        }
    }

    public static Course of(double angle, Unit unit) {
        return new Course(angle, unit);
    }

    public static Course ofDegrees(double angle) {
        return of(angle, DEGREES);
    }

    public static Course ofRadians(double angle) {
        return of(angle, RADIANS);
    }

    public static Course angleBetween(Course one, Course two) {
        return Course.ofDegrees(
            Spherical.angleDifference(one.inDegrees(), two.inDegrees())
        );
    }

    /**
     * @return The unit this Course was originally defined with.
     */
    public Unit nativeUnit() {
        return this.unit;
    }

    /**
     * Convert this Course into the desired unit.
     *
     * @param desiredUnit A unit like Degrees or Radians
     *
     * @return This Course expressed in the desired unit.
     */
    public double in(Unit desiredUnit) {
        return (this.unit == desiredUnit)
            ? angle
            : angle * desiredUnit.unitsPerDegree / this.unit.unitsPerDegree;
    }

    public double inDegrees() {
        return in(DEGREES);
    }

    public double inRadians() {
        return in(RADIANS);
    }

    public Course negate() {
        return Course.of(-angle, unit);
    }

    public Course abs() {
        return Course.of(Math.abs(angle), unit);
    }

    public boolean isPositive() {
        return angle > 0;
    }

    public boolean isNegative() {
        return angle < 0;
    }

    public boolean isZero() {
        return angle == 0;
    }

    public Course times(double scalar) {
        return Course.of(angle * scalar, unit);
    }

    /**
     * @return Another Course defined using "this" object's unit.
     */
    public Course plus(Course otherCourse) {
        return Course.of(angle + otherCourse.in(unit), unit);
    }

    /**
     * @return Another Course defined using "this" object's unit
     */
    public Course minus(Course otherCourse) {
        return plus(otherCourse.times(-1.0));
    }

    public boolean isLessThan(Course otherCourse) {
        return this.angle < otherCourse.in(unit);
    }

    public boolean isLessThanOrEqualTo(Course otherCourse) {
        return this.angle <= otherCourse.in(unit);
    }

    public boolean isGreaterThan(Course otherCourse) {
        return this.angle > otherCourse.in(unit);
    }

    public boolean isGreaterThanOrEqualTo(Course otherCourse) {
        return this.angle >= otherCourse.in(unit);
    }

    /**
     * @param otherCourse A second course
     *
     * @return The ratio between these courses
     */
    public double dividedBy(Course otherCourse) {
        return this.angle / otherCourse.in(unit);
    }

    public double sin() {
        return FastMath.sin(this.inRadians());
    }

    public double cos() {
        return FastMath.cos(this.inRadians());
    }

    public double tan() {
        return FastMath.tan(this.inRadians());
    }

    @Override
    public int compareTo(Course other) {
        return Double.compare(angle, other.in(unit));
    }

    @Override
    public String toString() {
        //when reporting a Course in terms of radians use 5 decimals places (by default)
        if (unit == RADIANS) {
            return toString(5);
        }

        //when reporting a Course in terms of degrees do not use decimals (by default)
        if (unit == DEGREES) {
            return toString(0);
        }

        throw new AssertionError("unhandled unit");
    }

    /**
     * @param digitsAfterDecimalPlace The number of digits after the decimal place to use.
     *
     * @return A String like "90deg", or "1.23456rad"
     */
    public String toString(int digitsAfterDecimalPlace) {
        return String.format("%." + digitsAfterDecimalPlace + "f" + unit.suffix, angle);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (int) (Double.doubleToLongBits(this.angle) ^ (Double.doubleToLongBits(this.angle) >>> 32));
        hash = 73 * hash + Objects.hashCode(this.unit);
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
        final Course other = (Course) obj;
        if (Double.doubleToLongBits(this.angle) != Double.doubleToLongBits(other.angle)) {
            return false;
        }
        if (this.unit != other.unit) {
            return false;
        }
        return true;
    }
}
