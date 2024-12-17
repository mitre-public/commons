package org.mitre.caasd.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.LatLong64.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LatLong64Test {

    @Test
    void encoding_LatLong_doubles_as_ints() {

        assertThat(encodeAsInt(0.0), is(0));
        assertThat(encodeAsInt(0.001), is(10_000));
        assertThat(encodeAsInt(1.001), is(10_010_000));
        assertThat(encodeAsInt(123.456789), is(1_234_567_890));
        assertThat(encodeAsInt(123.4567891), is(1_234_567_891));
        assertThat(encodeAsInt(180.0), is(1_800_000_000));

        assertThat(decodeInt(1_234_567_891), is(123.4567891));
        assertThat(decodeInt(1_234_567_890), is(123.456789));
        assertThat(decodeInt(10_010_000), is(1.001));
        assertThat(decodeInt(10_000), is(0.001));
        assertThat(decodeInt(0), is(0.0));

        assertThat(encodeAsInt(-0.0), is(0));
        assertThat(encodeAsInt(-0.001), is(-10_000));
        assertThat(encodeAsInt(-1.001), is(-10_010_000));
        assertThat(encodeAsInt(-123.456789), is(-1_234_567_890));
        assertThat(encodeAsInt(-123.4567891), is(-1_234_567_891));

        assertThat(decodeInt(-1_234_567_891), is(-123.4567891));
        assertThat(decodeInt(-1_234_567_890), is(-123.456789));
        assertThat(decodeInt(-10_010_000), is(-1.001));
        assertThat(decodeInt(-10_000), is(-0.001));
        assertThat(decodeInt(0), is(0.0));
    }

    @Test
    void lossy_but_very_accurate_approximation() {

        double lat = 20 * Math.PI; // 62.831853071795865
        double lng = -10 * Math.PI; // -31.415926535897932

        LatLong64 location = new LatLong64(lat, lng);

        // Not the Same!
        assertThat(location.latitude() == lat, is(false)); // 62.831853033430804
        assertThat(location.longitude() == lng, is(false)); // -31.415926474805886

        // But extremely Similar!
        double TOL = 1E-7;
        assertThat(location.latitude(), closeTo(lat, TOL));
        assertThat(location.longitude(), closeTo(lng, TOL));
    }

    @Test
    void will_reject_invalid_int_encodings() {

        int illegal_lat_hi = encodeAsInt(90.0000001);
        int illegal_lat_low = encodeAsInt(-90.0000001);

        System.out.println(illegal_lat_hi);
        System.out.println(illegal_lat_low);

        assertThrows(IllegalArgumentException.class, () -> LatLong64.fromPrimitiveLong(pack(illegal_lat_hi, 0)));

        assertThrows(IllegalArgumentException.class, () -> LatLong64.fromPrimitiveLong(pack(illegal_lat_low, 0)));

        int illegal_long_hi = encodeAsInt(180.0000001);
        int illegal_long_low = encodeAsInt(-180.0000001);

        assertThrows(IllegalArgumentException.class, () -> LatLong64.fromPrimitiveLong(pack(0, illegal_long_hi)));

        assertThrows(IllegalArgumentException.class, () -> LatLong64.fromPrimitiveLong(pack(0, illegal_long_low)));
    }

    @Test
    void randomized_lossy_accuracy_verification() {

        int NUM_TRIALS = 10_000;
        double TOLERANCE = 1E-7;
        Random rng = new Random(17L);

        for (int i = 0; i < NUM_TRIALS; i++) {
            LatLong loc = randomLatLong(rng);

            LatLong64 lossy_location = new LatLong64(loc.latitude(), loc.longitude());
            assertThat(lossy_location.latitude(), closeTo(loc.latitude(), TOLERANCE));
            assertThat(lossy_location.longitude(), closeTo(loc.longitude(), TOLERANCE));
        }
    }

    @Test
    void toBytes_and_back() {

        LatLong64 instance = new LatLong64(15.0, 22.0);

        byte[] asBytes = instance.toBytes();

        LatLong64 instanceRemake = LatLong64.fromBytes(asBytes);

        assertThat(instance, is(instanceRemake));
        assertThat(instance.latitude(), is(instanceRemake.latitude()));
        assertThat(instance.longitude(), is(instanceRemake.longitude()));
    }

    @Test
    void toBase64_and_back() {

        Random rng = new Random(17L);
        int N = 50;

        for (int i = 0; i < N; i++) {
            LatLong loc = randomLatLong(rng);
            double lat = loc.latitude();
            double lon = loc.longitude();
            LatLong64 in = loc.compress();
            String asBase64 = in.toBase64();
            LatLong64 out = LatLong64.fromBase64Str(asBase64);

            assertThat(in, is(out));
            assertThat(in.latitude(), is(out.latitude()));
            assertThat(in.longitude(), is(out.longitude()));
        }
    }

    @Test
    void fromBase64Str_example() {
        // provides and example of decoding a base64 String AND breaks if encoding strategy changes
        String base64Str = "DMu9QwSdrGw";
        LatLong64 location = LatLong64.fromBase64Str(base64Str);
        assertThat(location.toString(), is("(21.4678851,7.7442156)"));
    }

    @Test
    void toString_shows_7_digits() {
        LatLong64 instance = new LatLong64(76.123, -23.201);

        String expectedStr = "(76.1230000,-23.2010000)";

        assertThat(instance.toString(), is(expectedStr));
    }

    @Test
    void toPrimitiveLongAndBack() {
        Random rng = new Random(17L);
        int N = 50;

        for (int i = 0; i < N; i++) {
            LatLong random = randomLatLong(rng);
            LatLong64 instance = new LatLong64(random.latitude(), random.longitude());

            long asLong = instance.toPrimitiveLong();

            LatLong64 instance2 = LatLong64.fromPrimitiveLong(asLong);

            assertThat("Latitudes must match", instance.latitude(), is(instance2.latitude()));
            assertThat("Longitudes must match", instance.longitude(), is(instance2.longitude()));

            assertThat(instance, is(instance2));
        }
    }

    static LatLong randomLatLong(Random rng) {
        double lat = rng.nextDouble() * 90.0;
        double lng = rng.nextDouble() * 180.0;
        if (rng.nextBoolean()) {
            lat *= -1.0;
        }
        if (rng.nextBoolean()) {
            lng *= -1.0;
        }
        return LatLong.of(lat, lng);
    }
}
