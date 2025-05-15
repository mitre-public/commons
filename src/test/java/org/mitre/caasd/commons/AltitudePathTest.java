package org.mitre.caasd.commons;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.caasd.commons.AltitudePath.NULL_ALTITUDE;

import java.util.List;

import org.junit.jupiter.api.Test;

class AltitudePathTest {

    @Test
    void basicConstruction() {

        int[] altitudes = new int[] {1, 2, 3};
        List<Distance> distances = List.of(Distance.ofFeet(1), Distance.ofFeet(2), Distance.ofFeet(3));

        AltitudePath path1 = new AltitudePath(altitudes);
        AltitudePath path2 = AltitudePath.from(distances);

        assertThat(path1, is(path2));
    }

    @Test
    void pathWithNulls() {

        int[] altitudes = new int[] {1, NULL_ALTITUDE, 3};
        List<Distance> distances = newArrayList(Distance.ofFeet(1), null, Distance.ofFeet(3));

        AltitudePath path1 = new AltitudePath(altitudes);
        AltitudePath path2 = AltitudePath.from(distances);

        assertThat(path1, is(path2));
    }

    @Test
    void pathOfNulls() {

        AltitudePath path = AltitudePath.ofNulls(3);
        AltitudePath manual = new AltitudePath(new int[] {NULL_ALTITUDE, NULL_ALTITUDE, NULL_ALTITUDE});

        assertThat(path, is(manual));
    }

    @Test
    void distance_reflectsNull() {

        AltitudePath path1 = new AltitudePath(new int[] {1, 22, 3});

        // This null altitude should be treated as "zero" by distanceBtw
        AltitudePath path2 = new AltitudePath(new int[] {1, NULL_ALTITUDE, 3});

        assertThat(AltitudePath.distanceBtw(path1, path2), is(Distance.ofFeet(22)));
    }

    @Test
    void toBytesAndBackGivesSameResult() {

        AltitudePath path1 = new AltitudePath(new int[] {1, 2, 3});
        byte[] asBytes = path1.toBytes();
        AltitudePath path2 = AltitudePath.fromBytes(asBytes);

        assertThat(path1, is(path2));
    }

    @Test
    void toBase64AndBackGivesSameResult() {

        AltitudePath path1 = new AltitudePath(new int[] {1, 2, 3});
        String asBase64 = path1.toBase64();
        AltitudePath path2 = AltitudePath.fromBase64Str(asBase64);

        assertThat(path1, is(path2));
    }
}
