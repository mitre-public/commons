package org.mitre.caasd.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class LatLong64PathTest {

    @Test
    void demonstrate_core_usage_pattern() {

        List<LatLong> manyLocations = randomLatLongs(10_000);
        LatLongPath path = LatLongPath.from(manyLocations);
        LatLong64Path compressedPath = path.compress();

        // compression "doesn't change" the actual data
        assertPathsAreBasicallyTheSame(path, compressedPath);

        byte[] uncompressedBytes = path.toBytes();
        byte[] compressedBytes = compressedPath.toBytes();

        // BUT!, you spend 1/2 as many bytes
        assertThat(uncompressedBytes.length, is(compressedBytes.length * 2));
    }

    static List<LatLong> randomLatLongs(int n) {
        Random rng = new Random(17L);

        List<LatLong> latLongs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {

            double lat = rng.nextDouble() * 90.0;
            double lng = rng.nextDouble() * 180.0;
            if (rng.nextBoolean()) {
                lat *= -1.0;
            }
            if (rng.nextBoolean()) {
                lng *= -1.0;
            }
            latLongs.add(LatLong.of(lat, lng));
        }

        return latLongs;
    }

    @Test
    void basicConstructor() {

        LatLong64Path path = LatLong64Path.from(LatLong.of(0.0, 0.1), LatLong.of(1.0, 1.1));

        assertThat(path.get(0).inflate(), is(LatLong.of(0.0, 0.1)));
        assertThat(path.get(1).inflate(), is(LatLong.of(1.0, 1.1)));
    }

    @Test
    public void toAndFromBytes() {

        LatLong64Path path = LatLong64Path.from(LatLong.of(0.0, 0.1), LatLong.of(1.0, 1.1), LatLong.of(2.0, 2.1));
        LatLong64Path path2 = LatLong64Path.fromBytes(path.toBytes());

        assertThat(path.size(), is(path2.size()));
        for (int i = 0; i < path.size(); i++) {
            assertThat(path.get(i), is(path2.get(i)));
        }

        assertThat(path.equals(path2), is(true));
    }

    @Test
    void toBytes_yields_8_bytes_per_location() {

        LatLong64Path path = LatLong64Path.from(LatLong.of(0.0, 0.1), LatLong.of(1.0, 1.1), LatLong.of(2.0, 2.1));

        byte[] asBytes = path.toBytes();

        assertThat(asBytes.length, is(3 * 8));
    }

    @Test
    public void toAndFromBase64() {

        LatLong64Path path = LatLong64Path.from(LatLong.of(0.0, 0.1), LatLong.of(1.0, 1.1), LatLong.of(2.0, 2.1));
        LatLong64Path path2 = LatLong64Path.fromBase64Str(path.toBase64());

        assertThat(path.size(), is(path2.size()));
        for (int i = 0; i < path.size(); i++) {
            assertThat(path.get(i), is(path2.get(i)));
        }

        assertThat(path.equals(path2), is(true));
    }

    @Test
    public void supportEmptyPaths() {

        LatLong64Path path = LatLong64Path.from();

        assertThat(path.size(), is(0));
        assertThat(path.isEmpty(), is(true));
        assertThat(path.toArray(), is(new LatLong64Path[] {}));
        assertThat(path.toList(), hasSize(0));

        byte[] bytes = path.toBytes();
        LatLong64Path path2 = LatLong64Path.fromBytes(bytes);

        assertThat(path, is(path2));
    }

    @Test
    public void testSubpath() {

        LatLong a = LatLong.of(0.0, 0.1);
        LatLong b = LatLong.of(1.0, 1.1);
        LatLong c = LatLong.of(2.0, 2.1);

        LatLong64Path fullPath = LatLong64Path.from(a, b, c);

        // full "copy subset" gives unique object with same data
        LatLong64Path abc = fullPath.subpath(0, 3);
        assertThat(fullPath.equals(abc), is(true));
        assertThat(fullPath == abc, is(false));

        assertThat(fullPath.subpath(0, 0), is(LatLong64Path.from()));
        assertThat(fullPath.subpath(0, 1), is(LatLong64Path.from(a)));
        assertThat(fullPath.subpath(0, 2), is(LatLong64Path.from(a, b)));
        assertThat(fullPath.subpath(0, 3), is(LatLong64Path.from(a, b, c)));

        assertThat(fullPath.subpath(1, 1), is(LatLong64Path.from()));
        assertThat(fullPath.subpath(1, 2), is(LatLong64Path.from(b)));
        assertThat(fullPath.subpath(1, 3), is(LatLong64Path.from(b, c)));

        assertThat(fullPath.subpath(2, 3), is(LatLong64Path.from(c)));

        assertThrows(IllegalArgumentException.class, () -> fullPath.subpath(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> fullPath.subpath(3, 1));
        assertThrows(IllegalArgumentException.class, () -> fullPath.subpath(0, 4));
    }

    @Test
    void compression_cuts_bytes_in_half() {

        LatLongPath path = LatLongPath.from(
                LatLong.of(0.0, 0.1),
                LatLong.of(1.0, 1.1),
                LatLong.of(2.0, 2.1),
                LatLong.of(3.0, 3.1),
                LatLong.of(4.0, 4.1),
                LatLong.of(5.0, 5.1));

        byte[] rawBytes = path.toBytes();
        byte[] compressedBytes = path.compress().toBytes();

        assertThat(rawBytes.length, is(compressedBytes.length * 2));
    }

    @Test
    void compression_path_is_basically_the_same() {

        LatLongPath path = LatLongPath.from(
                LatLong.of(0.0, 0.1),
                LatLong.of(1.0, 1.1),
                LatLong.of(2.0, 2.1),
                LatLong.of(3.0, 3.1),
                LatLong.of(4.0, 4.1),
                LatLong.of(5.0, 5.1));

        LatLong64Path compressedPath = path.compress();

        assertPathsAreBasicallyTheSame(path, compressedPath);
    }

    @Test
    void inflatedPath_is_basically_the_same() {

        LatLongPath path = LatLongPath.from(randomLatLongs(100));

        LatLong64Path compressedPath = path.compress();
        LatLongPath rebuilt = compressedPath.inflate();

        assertPathsAreBasicallyTheSame(path, compressedPath);
        assertPathsAreBasicallyTheSame(path, rebuilt);
        assertPathsAreBasicallyTheSame(rebuilt, compressedPath);
    }

    static void assertPathsAreBasicallyTheSame(LatLongPath rawPath, LatLong64Path compressedPath) {

        assertThat(rawPath.size(), is(compressedPath.size()));

        for (int i = 0; i < rawPath.size(); i++) {
            LatLong raw_i = rawPath.get(i);
            LatLong64 compressed_i = compressedPath.get(i);
            assertThat(raw_i.latitude(), closeTo(compressed_i.latitude(), 0.000_000_1));
            assertThat(raw_i.longitude(), closeTo(compressed_i.longitude(), 0.000_000_1));
        }
    }

    static void assertPathsAreBasicallyTheSame(LatLongPath path1, LatLongPath path2) {

        assertThat(path1.size(), is(path2.size()));

        for (int i = 0; i < path1.size(); i++) {
            LatLong path1_i = path1.get(i);
            LatLong path2_i = path2.get(i);
            assertThat(path1_i.latitude(), closeTo(path2_i.latitude(), 0.000_000_1));
            assertThat(path1_i.longitude(), closeTo(path2_i.longitude(), 0.000_000_1));
        }
    }
}
