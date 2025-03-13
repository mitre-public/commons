package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * This class provides a byte-efficient way to store many latitude & longitude pairs. This class is
 * NOT (currently) intended to duplicate the convenience of {@link LatLong} or {@link LatLongPath}.
 * It simply provides a convenient transition to a more compact form:
 * <p>
 * The core usage idiom of this class is:
 *
 * <pre>{@code
 *     // Load many LatLong objects ...
 *     LatLongPath myPath = LatLongPath.from(oneThousandLatLongs);
 *
 *     // WHEN SERIALIZING DATA REPLACE THESE:
 *     byte[] pathAsByte = myPath.toBytes(); //16_000 bytes!
 *     String pathAsString = myPath.toBase64(); // 21_334 characters!
 *
 *     //WITH THESE:
 *     LatLong64Path compressedPath = myPath.compress();
 *     byte[] path64AsByte = compressedPath.toBytes(); // 8_000 bytes!
 *     String path64AsString = compressedPath.toBase64(); // 10_667 characters!
 * }</pre>
 * <p>
 * LatLong64Path purposefully does not implement java.io.Serializable. Instead, this class provides
 * 2 byte-efficient encodings: as a byte[], and as a Base64 encoded String. If you absolutely
 * require a Serializable type replace references to LatLong64Path with one of these encodings.
 */
public class LatLong64Path implements Iterable<LatLong> {

    /** Get the Encoder exactly once. */
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** This array contains {latLong64_0, latLong64_1, latLong64_2, ...}. */
    private final long[] locationData;

    /** Build a LatLong64Path by iterating through these locations. */
    public LatLong64Path(Collection<LatLong> locations) {
        requireNonNull(locations);

        this.locationData = new long[locations.size()];
        Iterator<LatLong> iter = locations.iterator();

        int i = 0;
        while (iter.hasNext()) {
            LatLong loc = iter.next();
            locationData[i] = loc.compress().toPrimitiveLong();
            i++;
        }
    }

    /** Build a LatLongPath by iterating through these locations. */
    public LatLong64Path(LatLong... locations) {
        requireNonNull(locations);
        this.locationData = new long[locations.length];
        for (int i = 0; i < locations.length; i++) {
            locationData[i] = locations[i].compress().toPrimitiveLong();
        }
    }

    private LatLong64Path(long[] data) {
        requireNonNull(data);
        this.locationData = data;
    }

    /** Build a LatLong64Path by iterating through these locations. */
    public static LatLong64Path from(Collection<LatLong> locations) {
        return new LatLong64Path(locations);
    }

    /** Build a LatLong64Path by iterating through these locations. */
    public static LatLong64Path from(LatLong... locations) {
        return new LatLong64Path(locations);
    }

    /** Build a LatLong64Path by iterating through these locations. */
    public static LatLong64Path from(LatLongPath path) {
        return new LatLong64Path(path.toArray());
    }

    /**
     * Create a new LatLong64Path from an array of bytes that looks like: {latLong64_0, latLong64_1,
     * latLong64_2, ...} (each LatLong64 is encoded as one 8-byte long)
     */
    public static LatLong64Path fromBytes(byte[] bytes) {
        requireNonNull(bytes);
        checkArgument(bytes.length % 8 == 0, "The byte[] must have a multiple of 8 bytes");

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long[] latLongData = new long[bytes.length / 8];
        for (int i = 0; i < latLongData.length; i++) {
            latLongData[i] = buffer.getLong();
        }

        return new LatLong64Path(latLongData);
    }

    /**
     * Create a new LatLong64Path object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a LatLong64Path's
     *                       byte[]
     *
     * @return A new LatLong64Path object.
     */
    public static LatLong64Path fromBase64Str(String base64Encoding) {
        return LatLong64Path.fromBytes(Base64.getUrlDecoder().decode(base64Encoding));
    }

    /**
     * Returns a LatLong64Path that is a subset of this LatLong64Path.  This method has the same
     * semantics as {@code String.substring(int beginIndex, int endIndex)}
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex   the ending index, exclusive.
     *
     * @return The specified LatLong64Path.
     */
    public LatLong64Path subpath(int beginIndex, int endIndex) {
        checkArgument(beginIndex >= 0, "beginIndex cannot be negative");
        checkArgument(endIndex <= size(), "endIndex cannot be greater than size()");
        checkArgument(beginIndex <= endIndex, "endIndex must be >= beginIndex");

        int len = endIndex - beginIndex;
        long[] subset = new long[len];
        System.arraycopy(locationData, beginIndex, subset, 0, len);

        return new LatLong64Path(subset);
    }

    /** @return This LatLong64Path as a byte[] containing 8 bytes per LatLong64 in the path */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(8 * size());
        for (long locationAsLong : locationData) {
            buffer.putLong(locationAsLong);
        }
        return buffer.array();
    }

    /** @return The Base64 file and url safe encoding of this LatLong64Path's byte[] . */
    public String toBase64() {
        return BASE_64_ENCODER.encodeToString(toBytes());
    }

    /**
     * Convert this compressed LatLong64Path into a LatLongPath. This provides access to the various
     * convenience functions LatLongPath supports but this class does not.
     */
    public LatLongPath inflate() {
        return LatLongPath.from(toArray());
    }

    public Stream<LatLong> stream() {
        return toList().stream();
    }

    public ArrayList<LatLong> toList() {
        ArrayList<LatLong> list = new ArrayList<>(size());
        for (long locationDatum : locationData) {
            list.add(LatLong64.fromPrimitiveLong(locationDatum).inflate());
        }

        return list;
    }

    public LatLong[] toArray() {

        LatLong[] array = new LatLong[locationData.length];
        for (int i = 0; i < locationData.length; i++) {
            array[i] = LatLong64.fromPrimitiveLong(locationData[i]).inflate();
        }
        return array;
    }

    /** @return The i_th entry in this path (yields same result as this.asList().get(i)). */
    public LatLong64 get(int i) {
        checkArgument(0 <= i && i < locationData.length);
        return LatLong64.fromPrimitiveLong(locationData[i]);
    }

    /** The number of LatLong locations in this path. */
    public int size() {
        return locationData.length;
    }

    public boolean isEmpty() {
        return locationData.length == 0;
    }

    @Override
    public Iterator<LatLong> iterator() {
        return toList().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        LatLong64Path latLongs = (LatLong64Path) o;
        return Arrays.equals(locationData, latLongs.locationData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(locationData);
    }
}
