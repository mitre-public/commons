package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;
import static org.mitre.caasd.commons.util.Preconditions.checkNoNullElement;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.mitre.caasd.commons.util.IterPair;
import org.mitre.caasd.commons.util.NeighborIterator;

/**
 * A LatLongPath is an ordered sequence of LatLong locations.
 * <p>
 * A LatLongPath "feels like" a {@code List<LatLong>} but it's easier on memory and serialization
 * tools because the LatLong objects are eschewed in favor of one double[] containing all latitude
 * and longitude data.
 */
public class LatLongPath implements Iterable<LatLong> {

    /** Get the Encoder exactly once. */
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** This data contains {latitude_0, longitude_0, latitude_1, longitude_1, ...}. */
    private final double[] latLongData;

    /** Build a LatLongPath by iterating through these locations. */
    public LatLongPath(Collection<LatLong> locations) {
        requireNonNull(locations);

        this.latLongData = new double[locations.size() * 2];
        Iterator<LatLong> iter = locations.iterator();
        int i = 0;
        while (iter.hasNext()) {
            LatLong loc = iter.next();
            latLongData[2 * i] = loc.latitude();
            latLongData[2 * i + 1] = loc.longitude();
            i++;
        }
    }

    /** Build a LatLongPath by iterating through these locations. */
    public LatLongPath(LatLong... locations) {
        requireNonNull(locations);
        this.latLongData = new double[locations.length * 2];
        for (int i = 0; i < locations.length; i++) {
            LatLong loc = locations[i];
            latLongData[2 * i] = loc.latitude();
            latLongData[2 * i + 1] = loc.longitude();
        }
    }

    private LatLongPath(double[] data) {
        this.latLongData = data;
    }

    /** Build a LatLongPath by iterating through these locations. */
    public static LatLongPath from(Collection<LatLong> locations) {
        return new LatLongPath(locations);
    }

    /** Build a LatLongPath by iterating through these locations. */
    public static LatLongPath from(LatLong... locations) {
        return new LatLongPath(locations);
    }

    /**
     * Returns a LatLongPath that is a subset of this LatLongPath.  This method has the same
     * semantics as {@code String.substring(int beginIndex, int endIndex)}
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex   the ending index, exclusive.
     *
     * @return The specified LatLongPath.
     */
    public LatLongPath subpath(int beginIndex, int endIndex) {
        checkArgument(beginIndex >= 0, "beginIndex cannot be negative");
        checkArgument(endIndex <= size(), "endIndex cannot be greater than size()");
        checkArgument(beginIndex <= endIndex, "endIndex must be >= beginIndex");

        ArrayList<LatLong> data = new ArrayList<>();
        for (int i = beginIndex; i < endIndex; i++) {
            data.add(this.get(i));
        }
        return new LatLongPath(data);
    }

    /** @return This LatLongPath as a byte[] containing "16 * size()" bytes */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(16 * size());
        for (double latLongDatum : latLongData) {
            buffer.putDouble(latLongDatum);
        }
        return buffer.array();
    }

    /**
     * Create a new LatLongPath from an array of bytes that looks like: {latitude_0, longitude_0,
     * latitude_1, longitude_1, ...}.  These bytes are converted to doubles and validated to ensure
     * they represent valid latitude and longitude values.
     */
    public static LatLongPath fromBytes(byte[] bytes) {
        requireNonNull(bytes);
        checkArgument(bytes.length % 16 == 0, "The byte[] must have 16*X bytes");

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        double[] latLongData = new double[bytes.length / 8];
        for (int i = 0; i < latLongData.length; i++) {
            latLongData[i] = buffer.getDouble();
        }
        //verify the latLongData...
        for (int i = 0; i < latLongData.length; i += 2) {
            checkLatitude(latLongData[i]);
            checkLongitude(latLongData[i + 1]);
        }

        return new LatLongPath(latLongData);
    }

    /** @return The Base64 file and url safe encoding of this LatLongPath's byte[] . */
    public String toBase64() {
        return BASE_64_ENCODER.encodeToString(toBytes());
    }

    /**
     * Create a new LatLongPath object.
     *
     * @param base64Encoding The Base64 safe and URL safe (no padding) encoding of a LatLongPath's
     *                       byte[]
     *
     * @return A new LatLongPath object.
     */
    public static LatLongPath fromBase64Str(String base64Encoding) {
        return LatLongPath.fromBytes(
            Base64.getUrlDecoder().decode(base64Encoding)
        );
    }

    public Stream<LatLong> stream() {
        return toList().stream();
    }

    public ArrayList<LatLong> toList() {
        ArrayList<LatLong> list = new ArrayList<>(size());
        for (int i = 0; i < latLongData.length; i += 2) {
            list.add(LatLong.of(latLongData[i], latLongData[i + 1]));
        }
        return list;
    }

    public LatLong[] toArray() {

        LatLong[] array = new LatLong[latLongData.length / 2];
        for (int i = 0; i < latLongData.length; i += 2) {
            array[i / 2] = LatLong.of(latLongData[i], latLongData[i + 1]);
        }
        return array;
    }

    /**
     * @return a (2 x n) array where the top row contains latitudes and the bottom row contains
     *     longitudes.
     */
    public double[][] toMatrix() {
        return new double[][]{latitudes(), longitudes()};
    }

    /** @return An array filled with the latitudes from this LatLongPath. */
    public double[] latitudes() {
        double[] latitudes = new double[size()];
        for (int i = 0; i < latitudes.length; i++) {
            latitudes[i] = latLongData[2 * i];
        }
        return latitudes;
    }

    /** @return An array filled with the longitudes from this LatLongPath. */
    public double[] longitudes() {
        double[] latitudes = new double[size()];
        for (int i = 0; i < latitudes.length; i++) {
            latitudes[i] = latLongData[2 * i + 1];
        }
        return latitudes;
    }

    /** @return The i_th entry in this path (yields same result as this.asList().get(i)). */
    public LatLong get(int i) {
        checkArgument(0 <= i && i <= latLongData.length / 2);
        return LatLong.of(latLongData[2 * i], latLongData[2 * i + 1]);
    }

    /** The number of LatLong locations in this path. */
    public int size() {
        return latLongData.length / 2;
    }

    public boolean isEmpty() {
        return latLongData.length == 0;
    }

    /** @return The total Distance obtained by walking from get(0) to get(1) to get(2) ... etc. */
    public Distance pathDistance() {
        if (size() <= 1) {
            return Distance.ZERO;
        }

        NeighborIterator<LatLong> iter = new NeighborIterator<>(iterator());
        Distance sum = Distance.ZERO;
        while (iter.hasNext()) {
            IterPair<LatLong> curLeg = iter.next();
            Distance legDist = Distance.between(curLeg.prior(), curLeg.current());
            sum = sum.plus(legDist);
        }

        return sum;
    }

    @Override
    public Iterator<LatLong> iterator() {
        return toList().iterator();
    }

    /**
     * @return An Iterator that gives "consecutive LatLong pairs" (equivalent to new
     *     {@code NeighborIterator<>(iterator())}).
     */
    public Iterator<IterPair<LatLong>> legIterator() {
        return new NeighborIterator<>(iterator());
    }


    /** @return A new LatLongPath with these additional locations appended to this path. */
    public LatLongPath append(LatLong... locations) {
        requireNonNull(locations);
        if (locations.length == 0) {
            return this;
        }

        ArrayList<LatLong> list = toList();
        Collections.addAll(list, locations);

        return new LatLongPath(list);
    }

    /** @return A new LatLongPath with these additional locations appended to this path. */
    public LatLongPath append(Collection<LatLong> locations) {
        requireNonNull(locations);
        if (locations.size() == 0) {
            return this;
        }

        ArrayList<LatLong> list = toList();
        list.addAll(locations);
        return new LatLongPath(list);
    }

    /** @return A new LatLongPath with this path2's LatLong's appended to this path. */
    public LatLongPath append(LatLongPath path2) {
        requireNonNull(path2);
        if (path2.size() == 0) {
            return this;
        }

        ArrayList<LatLong> list = toList();
        list.addAll(path2.toList());
        return new LatLongPath(list);
    }

    public static LatLongPath join(LatLongPath... paths) {
        requireNonNull(paths);
        checkNoNullElement(paths);
        if (paths.length == 0) {
            return new LatLongPath();
        }

        ArrayList<LatLong> allLatLongs = new ArrayList<>();
        for (LatLongPath path : paths) {
            allLatLongs.addAll(path.toList());
        }
        return new LatLongPath(allLatLongs);
    }

    public static LatLongPath join(Collection<LatLongPath> paths) {
        requireNonNull(paths);
        checkNoNullElement(paths);
        if (paths.size() == 0) {
            return new LatLongPath();
        }

        ArrayList<LatLong> allLatLongs = new ArrayList<>();
        for (LatLongPath path : paths) {
            allLatLongs.addAll(path.toList());
        }

        return new LatLongPath(allLatLongs);
    }


    /**
     * ACCURATELY compute the average LatLong positions of these locations. The underlying
     * computation performs trigonometric operations, so this method call can become computationally
     * taxing when it is used to process large volumes of data.
     *
     * @return The average location
     */
    public LatLong avgLatLong() {
        return LatLong.avgLatLong(this.toArray());
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
     * @return The average location
     */
    public LatLong quickAvgLatLong() {
        if (this.size() == 1) {
            return toArray()[0];
        }

        //just take the simple average of latitude values....
        double avgLatitude = DoubleStream.of(this.latitudes()).average().getAsDouble();

        //longitude cannot be simply averaged due to discontinuity when -180 abuts 180
        // So, we are going to take several "weighted averages of TWO Longitude values"
        // We can correct for the international date line with every subsequent avg.
        double[] longitudes = this.longitudes();

        //average the first two entries, then average in the 3rd entry, then the 4th...
        //increase the "weight" on the "curAverage" each time through the loop
        double curAvgLongitude = longitudes[0];
        for (int i = 1; i < longitudes.length; i++) {
            curAvgLongitude = avgLong(curAvgLongitude, i, longitudes[i], 1);
        }

        return LatLong.of(avgLatitude, curAvgLongitude);
    }

    /**
     * Naively compute the weighted average of two longitude values. Be careful, This method ignores
     * curvature of the earth.
     */
    private double avgLong(double longitudeA, int weightA, double longitudeB, int weightB) {

        double w1 = (double) (weightA) / (double) (weightA + weightB);
        double w2 = (double) (weightB) / (double) (weightA + weightB);

        double averageLong = (abs(longitudeA - longitudeB) > 180.0)
            ? w1 * (longitudeA + 180.0) + w2 * (longitudeB + 180.0)
            : w1 * longitudeA + w2 * longitudeB;

        return averageLong;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LatLongPath latLongs = (LatLongPath) o;
        return Arrays.equals(latLongData, latLongs.latLongData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(latLongData);
    }
}
