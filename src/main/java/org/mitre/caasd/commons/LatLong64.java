package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Base64;

import com.google.common.collect.ComparisonChain;

/**
 * LatLong64 is a lossy-compressed version of the Immutable {@link LatLong} class. The compression
 * used here saves 50% of the space while maintaining numeric equivalence for the first 7 decimal
 * places. This class uses two 32-bit ints to encode latitude and longitude values (as opposed to
 * directly storing the values in two 64-bit doubles). This space savings is relevant when you store
 * many latitude and longitude pairs as seen in {@link LatLong64Path}.
 * <p>
 * The goal of this class is to support a convenient transition to a more compact form for
 * {@link LatLongPath} data. It is NOT the goal of this class to provide a near-duplicate of
 * {@link LatLong}. Convenience functions like "distanceTo(LatLong)" and "courseTo(LatLong)" were
 * excluded from this class to strengthen this dichotomy.
 * <p>
 * Here is an example of the small accuracy concession made when converting a LatLong to a
 * LatLong64. "LatLong.of(20*PI, -10*PI)" stores the 2 double primitives: (62.83185307179586,
 * -31.41592653589793). Whereas "LatLong64.of(20*PI, -10*PI)" stores 2 ints that equate to the
 * values: (62.8318531, -31.4159265). Notice, these approximate values are perfect to the 7th
 * decimal place. Geo-Location data is difficult and expensive to measure beyond this level of
 * accuracy.
 * <p>
 * LatLong64 purposefully does not implement java.io.Serializable. Instead, this class provides 3
 * byte-efficient encodings: as a primitive long, as a byte[], and as a Base64 encoded String. If
 * you absolutely require a Serializable type replace references to LatLong64 with one of these
 * encodings.
 */
public class LatLong64 implements Comparable<LatLong64> {

    // The compression technique used here is inspired by how ASTERIX stores values in 32 bits.
    // ASTERIX's technique SUPPORTS MORE UNIQUE LAT_LONG VALUES, but these values are not "aligned
    // nicely" on the decimal grid. Therefore, NO encoded value perfectly matches a "nice input"
    // LatLong of say (1.234, -5.678). The ASTERIX technique ALWAYS INJECTS UGLY NUMERIC ERROR that
    // converts inputs like (1.234, -5.678) to something like (1.2339998314519, -5.678000410343).
    // See also:
    // https://mustache.mitre.org/projects/TTFS/repos/asterix/browse/parser/src/main/scala/org/mitre/caasd/asterix/common/package.scala#75-120

    // To prevent, the "aesthetics" issue of ASTERIX, but get equal space savings, we use the
    // technique often used to store currency amounts (e.g. store NUMBER_OF_CENTS as an int
    // instead of storing NUM_OF_DOLLARS as a double).

    // A Java int can hold values from -2,147,483,648 to 2,147,483,647 -- E.g. 4.29 Billion possible values.
    //
    // Thus, we can store 180.0 as 1_800_000_000 and -180.0 as -1_800_000_000.
    // This means we can EXACTLY store all possible Latitude or Longitude values when we limit
    // the inputs to 7 digits of accuracy. E.g. We can store double: 179.123_456_7XX_XXX as the
    // primitive int 1_791_234_567.

    private final int latitudeAsInt;
    private final int longitudeAsInt;

    static int encodeAsInt(double latOrLong) {
        double shifted = latOrLong * 10_000_000.0;
        // Do not simply cast, Round!.
        // This prevents input latOrLong values like "1.001" becoming "10_009_999" (e.g., 1.0009999)
        return (int) Math.round(shifted);
    }

    static double decodeInt(int latOrLongAsInt) {
        return ((double) latOrLongAsInt) / 10_000_000.0;
    }

    /** All public construction requires using LatLong's compress() method. */
    LatLong64(Double latitude, Double longitude) {
        // go through LatLong to get bounds checking
        this(LatLong.of(latitude, longitude));
    }

    /** All public construction requires using LatLong's compress() method. */
    LatLong64(LatLong location) {
        this(encodeAsInt(location.latitude()), encodeAsInt(location.longitude()));
    }

    private LatLong64(int encodedLatitude, int encodedLongitude) {
        // Ensures deserializing byte[] and long inputs are always checked
        checkArgument(-900000000 <= encodedLatitude && encodedLatitude <= 900000000);
        checkArgument(-1800000000 <= encodedLongitude && encodedLongitude <= 1800000000);
        this.latitudeAsInt = encodedLatitude;
        this.longitudeAsInt = encodedLongitude;
    }

    public static LatLong64 fromLatLong(LatLong loc) {
        return new LatLong64(loc.latitude(), loc.longitude());
    }

    /**
     * Parse a LatLong64 from a 64-bit primitive long.
     *
     * @param latLong64Bits The "latitudeAsInt" is in the upper 32 bits and the "longitudeAsInt" is
     *                      in the lower 32 bits.
     *
     * @return A new created LatLong64 object
     */
    public static LatLong64 fromPrimitiveLong(long latLong64Bits) {

        int lngBits = (int) latLong64Bits;
        int latBits = (int) (latLong64Bits >> 32);

        return new LatLong64(latBits, lngBits);
    }

    /**
     * Create a new LatLong64 object.
     *
     * @param exactly8Bytes The bytes defining two ints: {latitudeAsInt, longitudeAsInt}
     *
     * @return A new LatLong64 object.
     */
    public static LatLong64 fromBytes(byte[] exactly8Bytes) {
        requireNonNull(exactly8Bytes);
        checkArgument(exactly8Bytes.length == 8, "Must use exactly 8 bytes");
        ByteBuffer buffer = ByteBuffer.wrap(exactly8Bytes);

        int latitudeAsInt = buffer.getInt();
        int longitudeAsInt = buffer.getInt();
        return new LatLong64(latitudeAsInt, longitudeAsInt);
    }

    /**
     * Create a new LatLong64 object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a LatLong64's
     *                       byte[]
     *
     * @return A new LatLong64 object.
     */
    public static LatLong64 fromBase64Str(String base64Encoding) {
        return LatLong64.fromBytes(Base64.getUrlDecoder().decode(base64Encoding));
    }

    /**
     * Convert this compressed LatLong64 into a LatLong. This provides access to the "distance and
     * direction" functions that LatLong supports but LatLong64 does not.
     */
    public LatLong inflate() {
        return LatLong.of(latitude(), longitude());
    }

    public double latitude() {
        return decodeInt(latitudeAsInt);
    }

    public double longitude() {
        return decodeInt(longitudeAsInt);
    }

    /**
     * @return This LatLong64 written with 7 digits after the decimal point. Six digits are shown
     *     because all additional digits are unreliable due to the lossy compression.
     */
    @Override
    public String toString() {
        return "(" + String.format("%.7f", latitude()) + "," + String.format("%.7f", longitude()) + ")";
    }

    /** @return This LatLong64 as a 64-bit long (built by bit packing 2 32-bit int values). */
    public long toPrimitiveLong() {
        return pack(latitudeAsInt, longitudeAsInt);
    }

    /** Combine the bits from two 32-bit int primitives into a single 64 bit long. */
    static long pack(int upperInt, int lowerInt) {

        long upperBits = ((long) upperInt) << 32;
        long lowerBits = lowerInt & 0xffffffffL;
        // See also: "Integer.toUnsignedLong(int)" -- Using bitwise & to handle negative integers

        return upperBits | lowerBits;
    }

    /**
     * @return This LatLong64 as a byte[] of length 8. The array can be interpreted as a single
     *     8-byte long OR 2 4-byte ints that contain the "int encoded" latitude and longitude
     *     values. These 2 encodings are equivalent.
     */
    public byte[] toBytes() {
        return ByteBuffer.allocate(8).putLong(toPrimitiveLong()).array();
        // SAME AS
        // ByteBuffer.allocate(8).putInt(latitudeAsInt).putInt(longitudeAsInt).array();
    }

    /**
     * @return An 11 character Base64 file and url safe encoding of this LatLong64's byte[] (e.g.,
     *     "KUJSEZn8uzs", "-NWDIbs8BTQ", or "aHpvnvpRj8Y")
     */
    public String toBase64() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LatLong64 latLong64 = (LatLong64) o;

        if (latitudeAsInt != latLong64.latitudeAsInt) return false;
        return longitudeAsInt == latLong64.longitudeAsInt;
    }

    @Override
    public int hashCode() {
        int result = latitudeAsInt;
        result = 31 * result + longitudeAsInt;
        return result;
    }

    @Override
    public int compareTo(LatLong64 other) {
        return ComparisonChain.start()
                .compare(latitudeAsInt, other.latitudeAsInt)
                .compare(longitudeAsInt, other.longitudeAsInt)
                .result();
    }
}
