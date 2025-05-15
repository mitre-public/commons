package org.mitre.caasd.commons;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class VehiclePathTest {

    @Test
    void basicConstruction() {

        LatLong64Path path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath path_2 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));

        assertDoesNotThrow(() -> new VehiclePath(path_1, path_2));
    }

    @Test
    void canMakeVehiclePath_withNullAltitude() {

        // We want to support full VehiclePaths even when an AltitudePath is empty

        LatLong64Path path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));

        VehiclePath vp = VehiclePath.withoutAltitudes(path_1);
    }

    @Test
    void rejectsDifferentSizePaths() {

        LatLong64Path path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath path_2 = AltitudePath.from(Distance.ofFeet(100));

        assertThrows(IllegalArgumentException.class, () -> new VehiclePath(path_1, path_2));
    }

    @Test
    void toAndFromBytes() {

        LatLong64Path path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath path_2 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));

        VehiclePath path = new VehiclePath(path_1, path_2);
        VehiclePath path2 = VehiclePath.fromBytes(path.toBytes());

        assertThat(path, (is(path2)));
    }

    @Test
    void toBase64AndBack() {

        LatLong64Path path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath path_2 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));

        VehiclePath path = new VehiclePath(path_1, path_2);
        VehiclePath path2 = VehiclePath.fromBase64Str(path.toBase64());

        assertThat(path, (is(path2)));
    }

    @Test
    void distanceMeasurement() {

        LatLong64Path lat_path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath vert_path_1 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));
        VehiclePath vp_1 = new VehiclePath(lat_path_1, vert_path_1);

        LatLong64Path lat_path_2 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath vert_path_2 = AltitudePath.from(Distance.ofFeet(200), Distance.ofFeet(300));
        VehiclePath vp_2 = new VehiclePath(lat_path_2, vert_path_2);

        // lateral path is the same, vertical path is offset by 100 ft across 2 points
        double dist = VehiclePath.distanceBtw(vp_1, vp_2);
        assertThat(dist, closeTo(200.0, 0.00001));
    }
}
