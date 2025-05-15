package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * An AltitudePath is a sequence of altitudes measured in feet. An AltitudePath can be combined with
 * a LatLong64Path (or LatLongPath) to produce a sequence of (Lat, Long, Altitude) locations.
 * <p>
 * AltitudePaths support "null altitudes" by using a constant int value that is close to, but not
 * exactly, Integer.MIN_VALUE (i.e., -2147482414)
 *
 * @param altitudesInFeet
 */
public record AltitudePath(int[] altitudesInFeet) {

    /**
     * This int represents having "NO ALTITUDE VALUE" at this sequence index. The constant value
     * used -2147482414.
     */
    public static final int NULL_ALTITUDE = Integer.MIN_VALUE + 1234;

    private static final Base64.Encoder BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public AltitudePath {
        requireNonNull(altitudesInFeet);
    }

    /**
     * Convert a List of Distances to an AltitudePath. Any null Distances in the list are converted
     * to the NULL_ALTITUDE constant.
     */
    public static AltitudePath from(List<Distance> altitudes) {
        requireNonNull(altitudes);
        int[] alts = altitudes.stream().mapToInt(dist -> asInt(dist)).toArray();
        return new AltitudePath(alts);
    }

    public static AltitudePath from(Distance... dist) {
        return from(List.of(dist));
    }

    /**
     * Create an AltitudePath that contains only null altitudes.  (This is useful for building
     * VehiclePaths from data that does not include altitude data).
     */
    public static AltitudePath ofNulls(int n) {
        int[] altitudes = new int[n];
        Arrays.fill(altitudes, NULL_ALTITUDE);
        return new AltitudePath(altitudes);
    }

    private static int asInt(Distance dist) {
        return nonNull(dist) ? (int) dist.inFeet() : NULL_ALTITUDE;
    }

    /**
     * Create a new AltitudePath from an array of bytes that looks like: {altitudeInFeet_0,
     * altitudeInFeet_1, altitudeInFeet_2, ...} (each altitude is encoded as one 4-byte int)
     */
    public static AltitudePath fromBytes(byte[] bytes) {
        requireNonNull(bytes);
        checkArgument(bytes.length % 4 == 0, "The byte[] must have a multiple of 4 bytes");

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int[] altData = new int[bytes.length / 4];
        for (int i = 0; i < altData.length; i++) {
            altData[i] = buffer.getInt();
        }

        return new AltitudePath(altData);
    }

    /**
     * Create a new AltitudePath object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a AltitudePath's
     *                       byte[]
     *
     * @return A new AltitudePath object.
     */
    public static AltitudePath fromBase64Str(String base64Encoding) {
        return AltitudePath.fromBytes(Base64.getUrlDecoder().decode(base64Encoding));
    }

    public int size() {
        return altitudesInFeet.length;
    }

    public Distance get(int i) {
        return (altitudesInFeet[i] == NULL_ALTITUDE) ? null : Distance.ofFeet(altitudesInFeet[i]);
    }

    /** @return This AltitudePath as a byte[] containing 4 bytes per int in the path */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size() * 4);
        for (int i = 0; i < altitudesInFeet.length; i++) {
            buffer.putInt(altitudesInFeet[i]);
        }
        return buffer.array();
    }

    /** @return The Base64 file and url safe encoding of this AltitudePath's byte[] . */
    public String toBase64() {
        return BASE_64_ENCODER.encodeToString(toBytes());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(altitudesInFeet);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AltitudePath other = (AltitudePath) obj;
        if (!Arrays.equals(altitudesInFeet, other.altitudesInFeet)) return false;
        return true;
    }

    /**
     * Compute the "total distance" between the altitudes in these two paths.  (Null altitudes are
     * treated as if Altitude = 0 for that index)
     * <p>
     * The distance computed here is the sum of the distances between "altitudes pairs" taken from
     * the two paths (e.g. the distance btw the 1st altitude from both paths PLUS the distance btw
     * the 2nd altitude from both paths PLUS the distance btw the 3rd altitude from both paths
     * ...).
     * <p>
     * The "distanceBtw" between identical paths will be 0. The "distanceBtw" between nearly
     * identical paths will be small.  The "distanceBtw" between two very different paths will be
     * large.
     * <p>
     * The computation requires both Paths to have the same size. This is an important requirement
     * for making a DistanceMetric using this method.
     *
     * @param p1 A path
     * @param p2 Another path
     *
     * @return The sum of the pair-wise distance measurements
     */
    public static Distance distanceBtw(AltitudePath p1, AltitudePath p2) {
        requireNonNull(p1);
        requireNonNull(p2);
        checkArgument(p1.size() == p2.size(), "Paths must have same size");

        return distanceBtw(p1, p2, p1.size());
    }

    /**
     * Compute the "total distance" between the first n altitudes of these two paths.  (Null
     * altitudes are treated as if Altitude = 0 for that index)
     * <p>
     * The distance computed here is the sum of the distances between "altitude pairs" taken from
     * the two paths (e.g. the distance btw the 1st altitude from both paths PLUS the distance btw
     * the 2nd altitude from both paths PLUS the distance btw the 3rd altitude from both paths
     * ...).
     * <p>
     * The "distanceBtw" between two identical paths will be 0. The "distanceBtw" between two nearly
     * identical paths will be small. The "distanceBtw" between two very different paths will be
     * large.
     * <p>
     * This The computation requires both Paths to have the same size.  This is an important
     * requirement for making a DistanceMetric using this method.
     *
     * @param p1 A path
     * @param p2 Another path
     * @param n  The number of points considered in the "path distance" computation
     *
     * @return The sum of the pair-wise distance measurements
     */
    public static Distance distanceBtw(AltitudePath p1, AltitudePath p2, int n) {
        requireNonNull(p1);
        requireNonNull(p2);
        checkArgument(n >= 0);
        checkArgument(p1.size() >= n, "Path1 does not have the required length");
        checkArgument(p2.size() >= n, "Path2 does not have the required length");

        double distanceSum = 0;
        for (int i = 0; i < n; i += 1) {
            // if either altitude is missing use 0 as the "stand in" altitude value
            int mine = p1.altitudesInFeet[i] == NULL_ALTITUDE ? 0 : p1.altitudesInFeet[i];
            int his = p2.altitudesInFeet[i] == NULL_ALTITUDE ? 0 : p2.altitudesInFeet[i];
            distanceSum += Math.abs(mine - his);
        }

        return Distance.ofFeet(distanceSum);
    }
}
