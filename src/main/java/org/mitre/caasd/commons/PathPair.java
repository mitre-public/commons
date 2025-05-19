package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

/**
 * A PathPair combines two VehiclePaths that have the same size.
 * <p>
 * This record is for finding "similar Vehicle events". The idea is: "When important pair-wise
 * vehicle interactions occur extract the path those two vehicles traveled around the time of the
 * event. Then, keep a record of that "pair-wise interaction". This allows multiple important
 * events that have the "similar paths" to be found.
 *
 * @param path0
 * @param path1
 */
public record PathPair(VehiclePath path0, VehiclePath path1) {

    private static final Base64.Encoder BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public PathPair {
        requireNonNull(path0);
        requireNonNull(path1);
        checkArgument(path0.size() == path1.size());
    }

    /**
     * Create a new PathPair from an array of bytes.
     */
    public static PathPair fromBytes(byte[] bytes) {
        requireNonNull(bytes);

        int n = bytes.length / 2;

        byte[] frontHalf = Arrays.copyOfRange(bytes, 0, n);
        byte[] backHalf = Arrays.copyOfRange(bytes, n, 2 * n);

        VehiclePath path0 = VehiclePath.fromBytes(frontHalf);
        VehiclePath path1 = VehiclePath.fromBytes(backHalf);

        return new PathPair(path0, path1);
    }

    /**
     * Create a new PathPair object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a PathPair's
     *                       byte[]
     *
     * @return A new PathPair object.
     */
    public static PathPair fromBase64Str(String base64Encoding) {
        return PathPair.fromBytes(Base64.getUrlDecoder().decode(base64Encoding));
    }

    /** @return The number of "locations" in each path (which must be the same). */
    public int size() {
        return path0.size();
    }

    /** @return This PathPair as a byte[]. */
    public byte[] toBytes() {
        byte[] p0Bytes = path0().toBytes();
        byte[] p1Bytes = path1().toBytes();

        checkState(p0Bytes.length == p1Bytes.length, "byte count mismatch");

        ByteBuffer buffer = ByteBuffer.allocate(p0Bytes.length + p1Bytes.length);

        buffer.put(p0Bytes);
        buffer.put(p1Bytes);

        return buffer.array();
    }

    /** @return The Base64 file and url safe encoding of this PathPair's byte[] . */
    public String toBase64() {
        return BASE_64_ENCODER.encodeToString(toBytes());
    }

    /** Compute the distance between these two paths (use the full paths) */
    public static double distanceBtw(PathPair a, PathPair b) {
        requireNonNull(a);
        requireNonNull(b);
        checkArgument(a.size() == b.size(), "Paths must have same size");

        return distanceBtw(a, b, a.size());
    }

    /** Compute the distance between these two paths using just the first n points of the paths */
    public static double distanceBtw(PathPair a, PathPair b, int n) {
        requireNonNull(a);
        requireNonNull(b);
        checkArgument(n >= 0);
        checkArgument(a.size() >= n, "PathPair1 does not have the required length");
        checkArgument(b.size() >= n, "PathPair2 does not have the required length");

        // We don't know how to pair off the 4 vehicles in this "Path Pair" comparison

        // a0-to-b0 + a1-to-b1
        double opt1 = VehiclePath.distanceBtw(a.path0, b.path0, n) + VehiclePath.distanceBtw(a.path1, b.path1);

        // a0-to-b1 + a1-to-b0
        double opt2 = VehiclePath.distanceBtw(a.path0, b.path1, n) + VehiclePath.distanceBtw(a.path1, b.path0);

        return Math.min(opt1, opt2);
    }
}
