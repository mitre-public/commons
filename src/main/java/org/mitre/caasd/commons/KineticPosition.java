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
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;
import static org.mitre.caasd.commons.Speed.Unit.FEET_PER_MINUTE;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * KineticPosition gives the 4d-location (time, latitude, longitude, altitude) of an object as well
 * as higher order features like speed, climbRate, course, and turnRate
 * <p>
 * The class is designed to (A) have a useful Java API and (B) have a compact serialized form that
 * will be easy to work with when working with other languages that cannot use the Java API
 */
public class KineticPosition implements HasTime, HasPosition {

    /** KineticPosition are encoded as Base64 without padding (get the Encoder exactly once). */
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder().withoutPadding();

    private final long epochTime;

    //latitude kept independently for serialized form
    private final double latitude;

    //longitude kept independently for serialized form
    private final double longitude;

    private final double altitudeInFeet;

    private final double climbRateInFtPerMin;

    private final double courseInDegrees;

    private final double turnRateInDegreesPerSecond;

    private final double speedInKnots;

    private final double accelerationInKnotsPerSec;


    /**
     * A KineticPosition is generally created programmatically by fitting Latitude-vs-time,
     * Longitude-vs-time, and Altitude-vs-time functions to a time series of FourDPosition
     * measurements.
     *
     * @param time      A moment in time
     * @param location  A LatLong location
     * @param altitude  An altitude measurement
     * @param climbRate The derivative of altitude-vs-time
     * @param course    The course (e.g. direction of travel)
     * @param turnRate  The derivative of course-vs-time
     * @param speed     The speed
     * @param accel     The acceleration in Knots Per Second
     */
    public KineticPosition(Instant time, LatLong location, Distance altitude, Speed climbRate,
                           Course course, double turnRate, Speed speed, Acceleration accel) {
        requireNonNull(time);
        requireNonNull(location);
        requireNonNull(altitude);
        requireNonNull(speed);
        this.epochTime = time.toEpochMilli();
        this.latitude = location.latitude();
        this.longitude = location.longitude();
        this.altitudeInFeet = altitude.inFeet();
        this.climbRateInFtPerMin = climbRate.inFeetPerMinutes();
        this.courseInDegrees = course.inDegrees();
        this.speedInKnots = speed.inKnots();
        this.accelerationInKnotsPerSec = accel.speedDeltaPerSecond().inKnots();
        this.turnRateInDegreesPerSecond = turnRate;
    }

    /**
     * Directly build a KineticPosition from 72 bytes (useful when working with serialization
     * layers).
     *
     * @param seventyTwoBytes The bytes used to encode a KineticPosition
     */
    public KineticPosition(byte[] seventyTwoBytes) {
        requireNonNull(seventyTwoBytes);
        checkArgument(seventyTwoBytes.length == 72, "Must use exactly 72 bytes");
        ByteBuffer buffer = ByteBuffer.wrap(seventyTwoBytes);

        this.epochTime = buffer.getLong();
        this.latitude = buffer.getDouble();
        this.longitude = buffer.getDouble();
        this.altitudeInFeet = buffer.getDouble();
        this.climbRateInFtPerMin = buffer.getDouble();
        this.courseInDegrees = buffer.getDouble();
        this.turnRateInDegreesPerSecond = buffer.getDouble();
        this.speedInKnots = buffer.getDouble();
        this.accelerationInKnotsPerSec = buffer.getDouble();

        checkLatitude(latitude);
        checkLongitude(longitude);
    }

    @Override
    public Instant time() {
        return Instant.ofEpochMilli(epochTime);
    }

    @Override
    public LatLong latLong() {
        return LatLong.of(latitude, longitude);
    }

    public Distance altitude() {
        return Distance.ofFeet(altitudeInFeet);
    }

    public Speed climbRate() {
        return Speed.of(climbRateInFtPerMin, FEET_PER_MINUTE);
    }

    public Speed speed() {
        return Speed.of(speedInKnots, KNOTS);
    }

    public Acceleration acceleration() {
        return Acceleration.of(Speed.ofKnots(accelerationInKnotsPerSec));
    }

    public Course course() {
        return Course.ofDegrees(courseInDegrees);
    }

    /**
     * @return The turn rate measured in degrees per second.  Positive value indicate clockwise
     *     rotation.  Negative values indicate counter-clockwise rotation
     */
    public double turnRate() {
        return turnRateInDegreesPerSecond;
    }

    /** @return The radius of the circle that fits the current speed and turn rate. */
    public Distance turnRadius() {
        return turnRadius(speed(), turnRate());
    }

    /**
     * @param speed                      An instantaneous speed
     * @param turnRateInDegreesPerSecond An instantaneous turn rate
     *
     * @return The radius of a circle that fits the provided speed * turn rate
     */
    public static Distance turnRadius(Speed speed, double turnRateInDegreesPerSecond) {
        requireNonNull(speed);
        if (turnRateInDegreesPerSecond == 0) {
            return Distance.ofNauticalMiles(Double.POSITIVE_INFINITY);
        }

        //Assume a circular travel path....

        //Find the time required to travel in a circle, i.e. while moving "straight" but gradually turning left or right
        double secToTurn360 = 360.0 / Math.abs(turnRateInDegreesPerSecond);
        long asMilliSec = (long) secToTurn360 * 1000;

        //Find distance traveled while "moving in a circle"
        Distance distTraveled = speed.times(Duration.ofMillis(asMilliSec));

        //convert circle circumference to a radius
        Distance radius = distTraveled.times(1.0 / (2.0 * Math.PI));

        return (turnRateInDegreesPerSecond > 0)
            ? radius //clockwise = positive radius
            : radius.times(-1); //counter-clockwise = negative radisu
    }

    /**
     * Express the data inside this KineticPosition as a byte[] 72 bytes that encodes {epochTime,
     * latitude, longitude, altitudeInFeet, climbRateInFtPerMin, courseInDegrees,
     * turnRateInDegreesPerSecond, speedInKnots, and accelerationInKnotsPerSec}.
     *
     * @return The bytes in this KineticPosition.
     */
    public byte[] toBytes() {
        return ByteBuffer.allocate(72) //72 bytes = 9 fields @ 8 bytes each
            .putLong(epochTime)
            .putDouble(latitude)
            .putDouble(longitude)
            .putDouble(altitudeInFeet)
            .putDouble(climbRateInFtPerMin)
            .putDouble(courseInDegrees)
            .putDouble(turnRateInDegreesPerSecond)
            .putDouble(speedInKnots)
            .putDouble(accelerationInKnotsPerSec)
            .array();
    }

    /**
     * @return The bytes() of this KineticPosition encoded in Base64. The encoding String will be 96
     *     characters long.
     */
    public String toBase64() {
        return BASE_64_ENCODER.encodeToString(toBytes());
    }

    /**
     * @param str Usually the bytes() of a KineticPosition that were previously encoded in Base64.
     *
     * @return A new KineticPosition by parsing the binary data represented within a Base64 String.
     */
    public static KineticPosition fromBase64(String str) {
        byte[] bytes = Base64.getDecoder().decode(str);
        return new KineticPosition(bytes);
    }

    /**
     * Directly build a KineticPosition from 72 bytes (useful when working with serialization
     * layers).
     *
     * @param seventyTwoBytes The bytes used to encode a KineticPosition
     *
     * @return A new KineticPosition by parsing these 72 bytes.
     */
    public static KineticPosition fromBytes(byte[] seventyTwoBytes) {
        return new KineticPosition(seventyTwoBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        KineticPosition that = (KineticPosition) o;

        if (epochTime != that.epochTime) {
            return false;
        }
        if (Double.compare(that.latitude, latitude) != 0) {
            return false;
        }
        if (Double.compare(that.longitude, longitude) != 0) {
            return false;
        }
        if (Double.compare(that.altitudeInFeet, altitudeInFeet) != 0) {
            return false;
        }
        if (Double.compare(that.climbRateInFtPerMin, climbRateInFtPerMin) != 0) {
            return false;
        }
        if (Double.compare(that.courseInDegrees, courseInDegrees) != 0) {
            return false;
        }
        if (Double.compare(that.turnRateInDegreesPerSecond, turnRateInDegreesPerSecond) !=
            0) {
            return false;
        }
        if (Double.compare(that.speedInKnots, speedInKnots) != 0) {
            return false;
        }
        return Double.compare(that.accelerationInKnotsPerSec, accelerationInKnotsPerSec) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (epochTime ^ (epochTime >>> 32));
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(altitudeInFeet);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(climbRateInFtPerMin);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(courseInDegrees);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(turnRateInDegreesPerSecond);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(speedInKnots);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(accelerationInKnotsPerSec);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(KineticPosition seed) {
        return builder()
            .time(seed.time())
            .latLong(seed.latLong())
            .altitude(seed.altitude())
            .climbRate(seed.climbRate())
            .course(seed.course())
            .turnRate(seed.turnRate())
            .speed(seed.speed())
            .acceleration(seed.acceleration());
    }

    public static class Builder {

        private Long epochTime;
        private LatLong latLong;
        private Double altitudeInFeet;
        private Double climbRateInFtPerMin;
        private Double speedInKnots;
        private Double accelerationInKnotsPerSec;
        private Double courseInDegrees;
        private Double turnRateInDegreesPerSec;

        public KineticPosition build() {
            requireNonNull(epochTime, "epochTime was not set");
            requireNonNull(latLong, "latLong was not set");
            requireNonNull(altitudeInFeet, "altitude was not set");
            requireNonNull(climbRateInFtPerMin, "climbRate was not set");
            requireNonNull(speedInKnots, "speed was not set");
            requireNonNull(accelerationInKnotsPerSec, "acceleration was not set");
            requireNonNull(courseInDegrees, "course was not set");
            requireNonNull(turnRateInDegreesPerSec, "turn rate was not set");

            return new KineticPosition(
                Instant.ofEpochMilli(epochTime),
                latLong,
                Distance.ofFeet(altitudeInFeet),
                Speed.of(climbRateInFtPerMin, FEET_PER_MINUTE),
                Course.ofDegrees(courseInDegrees),
                turnRateInDegreesPerSec,
                Speed.of(speedInKnots, KNOTS),
                Acceleration.of(Speed.ofKnots(accelerationInKnotsPerSec))
            );
        }

        /** Set the time while enforcing that it was not set previously. */
        public Builder time(Instant time) {
            checkState(isNull(epochTime), "time was already set");
            return butTime(time);
        }

        /** Set the time regardless of whether it was set previously. */
        public Builder butTime(Instant time) {
            this.epochTime = time.toEpochMilli();
            return this;
        }

        /** Set the LatLong while enforcing that it was not set previously. */
        public Builder latLong(LatLong location) {
            checkState(isNull(latLong), "latLong was already set");
            return butLatLong(location);
        }

        /** Set the LatLong regardless of whether it was set previously. */
        public Builder butLatLong(LatLong location) {
            requireNonNull(location);
            return butLatLong(location.latitude(), location.longitude());
        }

        /** Set the LatLong while enforcing that it was not set previously. */
        public Builder latLong(double latitude, double longitude) {
            checkState(isNull(latLong), "latLong was already set");
            return butLatLong(latitude, longitude);
        }

        /** Set the LatLong regardless of whether it was set previously. */
        public Builder butLatLong(double latitude, double longitude) {
            checkLatitude(latitude);
            checkLongitude(longitude);
            this.latLong = LatLong.of(latitude, longitude);
            return this;
        }

        /** Set the altitude while enforcing that it was not set previously. */
        public Builder altitude(Distance altitude) {
            checkState(isNull(altitudeInFeet), "altitude was already set");
            return butAltitude(altitude);
        }

        /** Set the altitude regardless of whether it was set previously. */
        public Builder butAltitude(Distance altitude) {
            this.altitudeInFeet = altitude.inFeet();
            return this;
        }

        /** Set the speed while enforcing that it was not set previously. */
        public Builder speed(Speed speed) {
            checkState(isNull(speedInKnots), "speed was already set");
            return butSpeed(speed);
        }

        /** Set the speed regardless of whether it was set previously. */
        public Builder butSpeed(Speed speed) {
            this.speedInKnots = speed.inKnots();
            return this;
        }

        /** Set the acceleration while enforcing that it was not set previously. */
        public Builder acceleration(Acceleration accel) {
            checkState(isNull(this.accelerationInKnotsPerSec), "acceleration was already set");
            return butAcceleration(accel);
        }

        /** Set the acceleration regardless of whether it was set previously. */
        public Builder butAcceleration(Acceleration accel) {
            this.accelerationInKnotsPerSec = accel.speedDeltaPerSecond().inKnots();
            return this;
        }

        /** Set the climbRate while enforcing that it was not set previously. */
        public Builder climbRate(Speed climbRate) {
            checkState(isNull(climbRateInFtPerMin), "climbRate was already set");
            return butClimbRate(climbRate);
        }

        /** Set the climbRate regardless of whether it was set previously. */
        public Builder butClimbRate(Speed climbRate) {
            this.climbRateInFtPerMin = climbRate.inFeetPerMinutes();
            return this;
        }

        /** Set the course while enforcing that it was not set previously. */
        public Builder course(Course course) {
            checkState(isNull(courseInDegrees), "course was already set");
            return butCourse(course);
        }

        /** Set the course regardless of whether it was set previously. */
        public Builder butCourse(Course course) {
            this.courseInDegrees = course.inDegrees();
            return this;
        }

        /** Set the turnRate while enforcing that it was not set previously. */
        public Builder turnRate(double degreesPerSec) {
            checkState(isNull(turnRateInDegreesPerSec), "turnRate was already set");
            return butTurnRate(degreesPerSec);
        }

        /** Set the turnRate regardless of whether it was set previously. */
        public Builder butTurnRate(double degreesPerSec) {
            this.turnRateInDegreesPerSec = degreesPerSec;
            return this;
        }
    }
}
