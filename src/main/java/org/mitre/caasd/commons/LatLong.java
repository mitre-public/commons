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
import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.math3.util.FastMath.atan2;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.hypot;
import static org.apache.commons.math3.util.FastMath.sin;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.google.common.collect.ComparisonChain;

/**
 * A LatLong is an immutable Latitude Longitude pair. A LatLong object is always checked to ensure
 * it contains a valid value.
 */
public class LatLong implements Comparable<LatLong>, Serializable {

    private static final long serialVersionUID = 1L;

    private final double latitude;

    private final double longitude;

    private LatLong() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent "standard users" from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */

        this(0.0, 0.0);
    }

    /**
     * Create a new LatLong object.
     *
     * @param latitude  A non-null Latitude value from (-90 to 90)
     * @param longitude A non-null Longitude value from (-180 to 180)
     */
    public LatLong(Double latitude, Double longitude) {
        checkNotNull(latitude);
        checkNotNull(longitude);
        checkLatitude(latitude);
        checkLongitude(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Create a new LatLong object.
     *
     * @param latitude  A non-null Latitude value from (-90 to 90)
     * @param longitude A non-null Longitude value from (-180 to 180)
     *
     * @return A newly created LatLong object
     */
    public static LatLong of(Double latitude, Double longitude) {
        return new LatLong(latitude, longitude);
    }

    /**
     * Create a new LatLong object.
     *
     * @param exactly16Bytes The bytes defining two doubles: {latitude, longitude}
     *
     * @return A new LatLong object.
     */
    public static LatLong fromBytes(byte[] exactly16Bytes) {
        requireNonNull(exactly16Bytes);
        checkArgument(exactly16Bytes.length == 16, "Must use exactly 16 bytes");
        long bigBits = 0; // e.g. most significant bits
        long smallBits = 0; // e.g. least significant bits
        for (int i = 0; i < 8; i++) {
            bigBits = (bigBits << 8) | (exactly16Bytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            smallBits = (smallBits << 8) | (exactly16Bytes[i] & 0xff);
        }
        double longitude = Double.longBitsToDouble(bigBits);
        double latitude = Double.longBitsToDouble(smallBits);

        return LatLong.of(longitude, latitude);
    }

    /**
     * Create a new LatLong object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a LatLong's
     *                       byte[]
     *
     * @return A new LatLong object.
     */
    public static LatLong fromBase64Str(String base64Encoding) {
        return LatLong.fromBytes(Base64.getUrlDecoder().decode(base64Encoding));
    }

    /**
     * Throw an IllegalArguementException is the latitude value is illegal
     *
     * @param latitude A value from (-90 to 90)
     */
    public static void checkLatitude(double latitude) {
        /*
         * Note: do not use Preconditions.checkArgument, it is significantly slower because it
         * creates a unique error String every call (and that can be signifcant if this data check
         * occurs inside a tight loop)
         */
        if (!(-90.0 <= latitude && latitude <= 90)) {
            throw new IllegalArgumentException("Latitude is out of range: " + latitude);
        }
    }

    /**
     * Throw an IllegalArguementException is the longitude value is illegal
     *
     * @param longitude A value from (-180 to 180)
     */
    public static void checkLongitude(double longitude) {
        /*
         * Note: do not use Preconditions.checkArgument, it is significantly slower because it
         * creates a unique error String every call (and that can be signifcant if this data check
         * occurs inside a tight loop)
         */
        if (!(-180.0 <= longitude && longitude <= 180.0)) {
            throw new IllegalArgumentException("Longitude is out of range: " + longitude);
        }
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "(" + latitude + "," + longitude + ")";
    }

    /**
     * @return This LatLong as a byte[] of length 16 containing the 8-byte doubles latitude and
     * longitude.
     */
    public byte[] toBytes() {
        return ByteBuffer.allocate(16).putDouble(latitude).putDouble(longitude).array();
    }

    /** @return The Base64 file and url safe encoding of this LatLong's byte[] . */
    public String toBase64() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toBytes());
    }

    public Distance distanceTo(LatLong that) {
        return Distance.ofNauticalMiles(distanceInNM(that));
    }

    public double distanceInNM(LatLong that) {
        return Spherical.distanceInNM(this, that);
    }

    public double courseInDegrees(LatLong that) {
        return Spherical.courseInDegrees(latitude, longitude, that.latitude(), that.longitude());
    }

    public Course courseTo(LatLong that) {
        return Spherical.courseBtw(this, that);
    }

    /**
     * @param distance The maximum qualifying distance (inclusive)
     * @param location The "other"
     *
     * @return True if this LatLong is within the specified Distance to the provided location.
     */
    public boolean isWithin(Distance distance, LatLong location) {
        return this.distanceTo(location).isLessThanOrEqualTo(distance);
    }

    /**
     * Find a new LatLong by projecting out from this location in a specific direction and
     * distance.
     *
     * @param direction The direction of travel (in degrees)
     * @param distance  The distance traveled (in nautical miles)
     *
     * @return The destination
     */
    public LatLong projectOut(Double direction, Double distance) {
        return Spherical.projectOut(latitude, longitude, direction, distance);
    }

    /**
     * Find a new LatLong by projecting out from this location in a specific direction and
     * distance.
     *
     * @param direction The direction of travel
     * @param distance  The distance traveled
     *
     * @return The destination
     */
    public LatLong project(Course direction, Distance distance) {
        return Spherical.projectOut(this, direction, distance);
    }

    /**
     * Find a new LatLong by projecting out from this location in a specific direction, distance,
     * and curvature.
     *
     * @param direction The direction of travel
     * @param distance  The distance traveled
     * @param curvature The curvature of travel
     *
     * @return The destination
     */
    public LatLong projectOut(Double direction, Double distance, Double curvature) {
        return Spherical.projectOut(latitude, longitude, direction, distance, curvature);
    }

    public LatLong greatCircleOrigin(Double course) {
        return Spherical.greatCircleOrigin(latitude, longitude, course);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash
                + (int) (Double.doubleToLongBits(this.latitude) ^ (Double.doubleToLongBits(this.latitude) >>> 32));
        hash = 89 * hash
                + (int) (Double.doubleToLongBits(this.longitude) ^ (Double.doubleToLongBits(this.longitude) >>> 32));
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
        final LatLong other = (LatLong) obj;
        if (Double.doubleToLongBits(this.latitude) != Double.doubleToLongBits(other.latitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.longitude) != Double.doubleToLongBits(other.longitude)) {
            return false;
        }
        return true;
    }

    /**
     * ACCURATELY compute the average LatLong positions of these two locations. The underlying
     * computation performs several somewhat expensive trig operations. Consequently, you should
     * consider using the quick version if the distance between the two input points is small.
     *
     * @param one The first location
     * @param two The second location
     *
     * @return The average location
     */
    public static LatLong avgLatLong(LatLong one, LatLong two) {
        checkNotNull(one);
        checkNotNull(two);

        SphericalUnitVector vectorOne = new SphericalUnitVector(one);
        SphericalUnitVector vectorTwo = new SphericalUnitVector(two);

        double avgX = (vectorOne.x + vectorTwo.x) / 2.0;
        double avgY = (vectorOne.y + vectorTwo.y) / 2.0;
        double avgZ = (vectorOne.z + vectorTwo.z) / 2.0;

        double avgLong = atan2(avgY, avgX);
        double avgSqareRoot = hypot(avgX, avgY);
        double avgLat = atan2(avgZ, avgSqareRoot);

        return LatLong.of(toDegrees(avgLat), toDegrees(avgLong));
    }

    /**
     * QUICKLY compute the arithmetic average of LatLong positions of these two locations. This
     * computation does not reflect curvature of the earth but it does correct for the international
     * date line. The difference between the result computed by this method and the result computed
     * by avgLatLong grows as (1) the distance between the two input points grows and (2) the points
     * move further and further away from the equator.
     *
     * @param one The first location
     * @param two The second location
     *
     * @return The average location
     */
    public static LatLong quickAvgLatLong(LatLong one, LatLong two) {
        // latitude never wraps, so arithmatic average is fine
        double averageLat = (one.latitude + two.latitude) / 2.0;

        // be careful with longitude -- the international date line is a problem
        double averageLong = (abs(one.longitude - two.longitude) > 180.0)
                ? ((one.longitude + 180.0) + (two.longitude + 180.0)) / 2.0
                : (one.longitude + two.longitude) / 2.0;

        return LatLong.of(averageLat, averageLong);
    }

    /**
     * ACCURATELY compute the average LatLong positions of these locations. The underlying
     * computation performs several somewhat expensive trig operations when converting the LatLong
     * data to Spherical Unit Vectors.
     *
     * @param locations An array of LatLong locations
     *
     * @return The average location
     * @throws NoSuchElementException When locations is empty
     */
    public static LatLong avgLatLong(LatLong... locations) {
        requireNonNull(locations);

        if (locations.length == 0) {
            throw new NoSuchElementException("Average LatLong not defined when empty");
        }

        double x = 0;
        double y = 0;
        double z = 0;
        for (LatLong location : locations) {
            SphericalUnitVector vector = new SphericalUnitVector(location);
            x += vector.x;
            y += vector.y;
            z += vector.z;
        }
        x /= locations.length;
        y /= locations.length;
        z /= locations.length;

        double avgLong = atan2(y, x);
        double avgSqareRoot = hypot(x, y);
        double avgLat = atan2(z, avgSqareRoot);

        return LatLong.of(toDegrees(avgLat), toDegrees(avgLong));
    }

    /**
     * ACCURATELY compute the average LatLong positions of these locations. The underlying
     * computation performs several somewhat expensive trig operations when converting the LatLong
     * data to Spherical Unit Vectors.
     *
     * @param locations A collection of LatLong locations
     *
     * @return The average location
     * @throws NoSuchElementException When locations is empty
     */
    public static LatLong avgLatLong(Collection<LatLong> locations) {
        requireNonNull(locations);

        LatLong[] asArray = locations.toArray(new LatLong[0]);
        return avgLatLong(asArray);
    }

    /**
     * QUICKLY compute the ARITHMETIC average of these LatLong positions. This computation does not
     * reflect curvature of the earth, but it does correct for the international date line. The
     * difference between the result computed by this method and the result computed by
     * {@code avgLatLong()} grows as (1) the path distance grows and (2) the path locations move
     * further and further away from the equator.
     * <p>
     * This method is FASTER and LESS ACCURATE because it utilizes simple arithmetic instead of
     * accurate trigonometric functions.
     *
     * @param locations An array of locations
     *
     * @return The average location
     * @throws NoSuchElementException When locations is empty
     */
    public static LatLong quickAvgLatLong(LatLong... locations) {
        requireNonNull(locations);

        if (locations.length == 0) {
            throw new NoSuchElementException("The input array was empty");
        }

        if (locations.length == 1) {
            return locations[0];
        }

        // just take the simple average of latitude values....
        double avgLatitude =
                Stream.of(locations).mapToDouble(loc -> loc.latitude).average().getAsDouble();
        // longitude cannot be simply averaged due to discontinuity when -180 abuts 180
        // So, we are going to take several "weighted averages of TWO Longitude values"
        // We can correct for the international date line with every subsequent avg.
        double[] longitudes =
                Stream.of(locations).mapToDouble(loc -> loc.longitude()).toArray();

        // average the first two entries, then average in the 3rd entry, then the 4th...
        // increase the "weight" on the "curAverage" each time through the loop
        double curAvgLongitude = longitudes[0];
        for (int i = 1; i < longitudes.length; i++) {
            curAvgLongitude = avgLong(curAvgLongitude, i, longitudes[i], 1);
        }

        return LatLong.of(avgLatitude, curAvgLongitude);
    }

    /**
     * QUICKLY compute the ARITHMETIC average of these LatLong positions. This computation does not
     * reflect curvature of the earth, but it does correct for the international date line. The
     * difference between the result computed by this method and the result computed by
     * {@code avgLatLong()} grows as (1) the path distance grows and (2) the path locations move
     * further and further away from the equator.
     * <p>
     * This method is FASTER and LESS ACCURATE because it utilizes simple arithmetic instead of
     * accurate trigonometric functions.
     *
     * @param locations A collection of LatLong locations
     *
     * @return The average location
     * @throws NoSuchElementException When locations is empty
     */
    public static LatLong quickAvgLatLong(Collection<LatLong> locations) {
        requireNonNull(locations);
        LatLong[] asArray = locations.toArray(new LatLong[0]);
        return quickAvgLatLong(asArray);
    }

    /**
     * Naively compute the weighted average of two longitude values. Be careful, This method ignores
     * curvature of the earth.
     */
    private static double avgLong(double longitudeA, int weightA, double longitudeB, int weightB) {

        double w1 = (double) (weightA) / (double) (weightA + weightB);
        double w2 = (double) (weightB) / (double) (weightA + weightB);

        double averageLong = (abs(longitudeA - longitudeB) > 180.0)
                ? w1 * (longitudeA + 180.0) + w2 * (longitudeB + 180.0)
                : w1 * longitudeA + w2 * longitudeB;

        return averageLong;
    }

    private static final String COLLECTION_CANNOT_BE_NULL = "The collection of LatLong locations cannot be null";
    private static final String COLLECTION_CANNOT_BE_EMPTY = "The collection of LatLong locations cannot be empty";

    public static Double maxLatitude(Collection<LatLong> locations) {
        checkNotNull(locations, COLLECTION_CANNOT_BE_NULL);
        checkArgument(!locations.isEmpty(), COLLECTION_CANNOT_BE_EMPTY);

        return locations.stream().map(LatLong::latitude).reduce(-Double.MAX_VALUE, Math::max);
    }

    public static Double minLatitude(Collection<LatLong> locations) {
        checkNotNull(locations, COLLECTION_CANNOT_BE_NULL);
        checkArgument(!locations.isEmpty(), COLLECTION_CANNOT_BE_EMPTY);

        return locations.stream().map(LatLong::latitude).reduce(Double.MAX_VALUE, Math::min);
    }

    public static Double maxLongitude(Collection<LatLong> locations) {
        checkNotNull(locations, COLLECTION_CANNOT_BE_NULL);
        checkArgument(!locations.isEmpty(), COLLECTION_CANNOT_BE_EMPTY);

        return locations.stream().map(LatLong::longitude).reduce(-Double.MAX_VALUE, Math::max);
    }

    public static Double minLongitude(Collection<LatLong> locations) {
        checkNotNull(locations, COLLECTION_CANNOT_BE_NULL);
        checkArgument(!locations.isEmpty(), COLLECTION_CANNOT_BE_EMPTY);

        return locations.stream().map(LatLong::longitude).reduce(Double.MAX_VALUE, Math::min);
    }

    @Override
    public int compareTo(LatLong other) {
        return ComparisonChain.start()
                .compare(latitude, other.latitude)
                .compare(longitude, other.longitude)
                .result();
    }

    /**
     * Models a LatLong location as a 3 dimensional unit vector. This conversion can be useful for
     * eliminating some corner cases involved with certain LatLong operations (like find the average
     * LatLong across the international date line).
     */
    private static class SphericalUnitVector {

        final double x;
        final double y;
        final double z;

        private SphericalUnitVector(LatLong location) {
            double latInRadian = toRadians(location.latitude);
            double longInRadian = toRadians(location.longitude);
            this.x = cos(latInRadian) * cos(longInRadian);
            this.y = cos(latInRadian) * sin(longInRadian);
            this.z = sin(latInRadian);
        }
    }
}
