package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

/**
 * Combines a Vehicle's LatLong Path and its Altitude path into a single object
 */
public record VehiclePath(LatLong64Path latLongs, AltitudePath altitudes) {

    private static final Base64.Encoder BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public VehiclePath {
        requireNonNull(latLongs);
        requireNonNull(altitudes);
        checkArgument(latLongs.size() == altitudes.size());
    }

    public static VehiclePath withoutAltitudes(LatLong64Path latLongs) {
        requireNonNull(latLongs);
        return new VehiclePath(latLongs, AltitudePath.ofNulls(latLongs.size()));
    }

    /**
     * Create a new VehiclePath from an array of bytes that looks like: {LatLong64Path bytes ..., AltitudePath bytes ...}
     */
    public static VehiclePath fromBytes(byte[] bytes) {
        requireNonNull(bytes);
        checkArgument(bytes.length % 12 == 0, "The byte[] must have a multiple of 12 bytes");

        int n = bytes.length / 12;

        byte[] latLongBytes = Arrays.copyOfRange(bytes, 0, 8 * n);
        byte[] altBytes = Arrays.copyOfRange(bytes, 8 * n, 12 * n);

        return new VehiclePath(LatLong64Path.fromBytes(latLongBytes), AltitudePath.fromBytes(altBytes));
    }

    /**
     * Create a new VehiclePath object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a VehiclePath's
     *                       byte[]
     *
     * @return A new VehiclePath object.
     */
    public static VehiclePath fromBase64Str(String base64Encoding) {
        return VehiclePath.fromBytes(Base64.getUrlDecoder().decode(base64Encoding));
    }

    public int size() {
        return latLongs.size();
    }

    /** @return This VehiclePath as a byte[] containing 12 bytes per item in the path */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size() * 12);

        buffer.put(latLongs.toBytes());
        buffer.put(altitudes.toBytes());

        return buffer.array();
    }

    /** @return The Base64 file and url safe encoding of this VehiclePath's byte[] . */
    public String toBase64() {
        return BASE_64_ENCODER.encodeToString(toBytes());
    }

    /** Compute the distance between these two paths (use the full paths) */
    public static double distanceBtw(VehiclePath a, VehiclePath b) {
        requireNonNull(a);
        requireNonNull(b);
        checkArgument(a.size() == b.size(), "Paths must have same size");

        return distanceBtw(a, b, a.size());
    }

    /** Compute the distance between these two paths using just the first n points of the paths */
    public static double distanceBtw(VehiclePath path1, VehiclePath path2, int n) {
        requireNonNull(path1);
        requireNonNull(path2);
        checkArgument(n >= 0);
        checkArgument(path1.size() >= n, "Path1 does not have the required length");
        checkArgument(path2.size() >= n, "Path2 does not have the required length");

        // Dist in NM
        double ld = LatLong64Path.distanceBtw(path1.latLongs, path2.latLongs, n);
        Distance lateralDist = Distance.ofNauticalMiles(ld);

        Distance vertDist = AltitudePath.distanceBtw(path1.altitudes, path2.altitudes, n);

        return lateralDist.plus(vertDist).inFeet();
    }
}
