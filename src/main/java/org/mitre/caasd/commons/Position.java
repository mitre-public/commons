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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.*;
import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;

import java.time.Instant;
import java.util.Optional;

/**
 * Position gives the 3d or 4d location of an object. The time, latitude, and longitude dimensions
 * are strictly required but the altitude dimension is optional.
 * <p>
 * The goals of this class are to (A) have a useful Java API and (B) have a compact serialized form
 * that will be easy to work with when working with other languages that cannot use the Java API
 */
public class Position implements HasTime, HasPosition {

    private final long epochTime;

    // latitude kept independently for serialized form (i.e. not nested inside a LatLong instance)
    private final double latitude;

    // longitude kept independently for serialized form (i.e. not nested inside a LatLong instance)
    private final double longitude;

    private final double altitudeInFeet;

    private final boolean altitudeIsValid;

    /**
     * Creates a Position without an altitude component
     *
     * @param time The time
     * @param location The LatLong
     */
    public Position(Instant time, LatLong location) {
        this(time, location, null);
    }

    /**
     * Creates a Position with or without an altitude component
     *
     * @param time The time
     * @param location The LatLong
     * @param altitude An Optional (i.e. nullable) altitude
     */
    public Position(Instant time, LatLong location, Distance altitude) {
        this(
                time.toEpochMilli(),
                location.latitude(),
                location.longitude(),
                nonNull(altitude) ? altitude.inFeet() : null);
    }

    /**
     * Creates a Position without an altitude component
     *
     * @param epochTimeInMilli The epoch time in milliseconds
     * @param latitude The latitude
     * @param longitude The longitude
     */
    public Position(long epochTimeInMilli, double latitude, double longitude) {
        this(epochTimeInMilli, latitude, longitude, null);
    }

    /**
     * Creates a Position with or without an altitude component
     *
     * @param epochTimeInMilli The epoch time in milliseconds
     * @param latitude The latitude
     * @param longitude The longitude
     * @param altInFeet An Optional (i.e. nullable) altitude value (in feet)
     */
    public Position(long epochTimeInMilli, double latitude, double longitude, Double altInFeet) {
        /*
         * Note: Using the nullable "Double altInFeet" parameter is ugly. But it enables all
         * constructors to funnel to a single "master constructor".  If "double altInFeet" was used
         * instead the method signature would not support null altitudes.
         */
        checkLatitude(latitude);
        checkLongitude(longitude);
        this.epochTime = epochTimeInMilli;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeInFeet = nonNull(altInFeet) ? altInFeet : 0.0;
        this.altitudeIsValid = nonNull(altInFeet);
    }

    private Position() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent standard API users from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */
        this(0L, 0.0, 0.0, null);
    }

    @Override
    public Instant time() {
        return Instant.ofEpochMilli(epochTime);
    }

    @Override
    public LatLong latLong() {
        return LatLong.of(latitude, longitude);
    }

    /** @return The altitude of this Position (which may be null). */
    public Distance altitude() {
        /* For API simplicity do not return an "Optional<Distance>" */
        return (altitudeIsValid) ? Distance.ofFeet(altitudeInFeet) : null;
    }

    /** @return Equivalent to: {@code Optional.ofNullable(this.altitude());} */
    public Optional<Distance> altitudeAsOpt() {
        return Optional.ofNullable(altitude());
    }

    public boolean hasAltitude() {
        return altitudeIsValid;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (int) (this.epochTime ^ (this.epochTime >>> 32));
        hash = 23 * hash
                + (int) (Double.doubleToLongBits(this.latitude) ^ (Double.doubleToLongBits(this.latitude) >>> 32));
        hash = 23 * hash
                + (int) (Double.doubleToLongBits(this.longitude) ^ (Double.doubleToLongBits(this.longitude) >>> 32));
        hash = 23 * hash
                + (int) (Double.doubleToLongBits(this.altitudeInFeet)
                        ^ (Double.doubleToLongBits(this.altitudeInFeet) >>> 32));
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
        final Position other = (Position) obj;
        if (this.epochTime != other.epochTime) {
            return false;
        }
        if (Double.doubleToLongBits(this.latitude) != Double.doubleToLongBits(other.latitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.longitude) != Double.doubleToLongBits(other.longitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.altitudeInFeet) != Double.doubleToLongBits(other.altitudeInFeet)) {
            return false;
        }
        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Position seed) {
        return builder().time(seed.time()).latLong(seed.latLong()).altitude(seed.altitude());
    }

    public static class Builder {

        private Long epochTime;
        private Double latitude;
        private Double longitude;
        private Double altitudeInFeet;

        public Position build() {
            requireNonNull(epochTime);
            requireNonNull(latitude);
            requireNonNull(longitude);

            return new Position(epochTime, latitude, longitude, altitudeInFeet);
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
            checkState(isNull(latitude), "latitude was already set");
            checkState(isNull(longitude), "longitude was already set");
            return butLatLong(location);
        }

        /** Set the LatLong regardless of whether it was set previously. */
        public Builder butLatLong(LatLong location) {
            requireNonNull(location);
            return butLatLong(location.latitude(), location.longitude());
        }

        /** Set the LatLong while enforcing that it was not set previously. */
        public Builder latLong(double lat, double lon) {
            checkState(isNull(latitude), "latitude was already set");
            checkState(isNull(longitude), "longitude was already set");
            return butLatLong(lat, lon);
        }

        /** Set the LatLong regardless of whether it was set previously. */
        public Builder butLatLong(double latitude, double longitude) {
            checkLatitude(latitude);
            checkLongitude(longitude);
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        /** Set the altitude while enforcing that it was not set previously. */
        public Builder altitude(Distance altitude) {
            checkState(isNull(altitudeInFeet), "altitude was already set");
            return butAltitude(altitude);
        }

        /** Set the altitude regardless of whether it was set previously. */
        public Builder butAltitude(Distance altitude) {
            this.altitudeInFeet = isNull(altitude) ? null : altitude.inFeet();
            return this;
        }

        /** Set the altitude while enforcing that it was not set previously. */
        public Builder altitudeInFeet(double altitudeInFeet) {
            checkState(isNull(this.altitudeInFeet), "altitude was already set");
            return butAltitudeInFeet(altitudeInFeet);
        }

        /** Set the altitude regardless of whether it was set previously. */
        public Builder butAltitudeInFeet(double altitudeInFeet) {
            this.altitudeInFeet = altitudeInFeet;
            return this;
        }
    }
}
