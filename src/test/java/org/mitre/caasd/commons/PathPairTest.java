package org.mitre.caasd.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

class PathPairTest {

    @Test
    void distanceChoosesCorrectArrangement() {

        // Want 2 wildly different VehiclePaths.
        // The pack those SAME to VehiclePaths into 2 PathPairs but arranged different (i.e. ab and ba)

        // LatLong paths are DIFFERENT!
        LatLong64Path lat_path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        LatLong64Path lat_path_2 = LatLong64Path.from(LatLong.of(2.0, 1.0), LatLong.of(4.0, 3.0));

        // Alt paths are DIFFERENT!
        AltitudePath vert_path_1 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));
        AltitudePath vert_path_2 = AltitudePath.from(Distance.ofFeet(20000), Distance.ofFeet(3000));

        // Thus VehiclePaths are DIFFERENT
        VehiclePath vp_1 = new VehiclePath(lat_path_1, vert_path_1);
        VehiclePath vp_2 = new VehiclePath(lat_path_2, vert_path_2);

        PathPair a = new PathPair(vp_1, vp_2);
        PathPair b = new PathPair(vp_2, vp_1);

        // Paths are wildly different ... but the different pairs are good
        assertThat(VehiclePath.distanceBtw(vp_1, vp_2), greaterThan(1053393.0));

        assertThat(PathPair.distanceBtw(a, b), is(0.0));
        assertThat(PathPair.distanceBtw(b, a), is(0.0));
    }

    @Test
    void distanceReflectsAltitude() {

        // These are the same
        LatLong64Path lat_path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        LatLong64Path lat_path_2 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));

        // Alt paths are slightly different
        AltitudePath vert_path_1 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));
        AltitudePath vert_path_2 = AltitudePath.from(Distance.ofFeet(200), Distance.ofFeet(400));

        // Thus VehiclePaths are DIFFERENT
        VehiclePath vp_1 = new VehiclePath(lat_path_1, vert_path_1);
        VehiclePath vp_2 = new VehiclePath(lat_path_2, vert_path_2);

        PathPair a = new PathPair(vp_1, vp_2);
        PathPair b = new PathPair(vp_2, vp_1);

        // Paths are offset by 300 foot
        assertThat(VehiclePath.distanceBtw(vp_1, vp_2), is(300.0));

        assertThat(PathPair.distanceBtw(a, b), is(0.0));
        assertThat(PathPair.distanceBtw(b, a), is(0.0));
    }

    @Test
    void toAndFromBytes() {

        // Data to make 2 VehiclePath, i.e. 1 PathPair
        LatLong64Path lat_path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        LatLong64Path lat_path_2 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath vert_path_1 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));
        AltitudePath vert_path_2 = AltitudePath.from(Distance.ofFeet(200), Distance.ofFeet(400));
        VehiclePath vp_1 = new VehiclePath(lat_path_1, vert_path_1);
        VehiclePath vp_2 = new VehiclePath(lat_path_2, vert_path_2);

        PathPair path = new PathPair(vp_1, vp_2);
        PathPair path2 = PathPair.fromBytes(path.toBytes());

        assertThat(path, (CoreMatchers.is(path2)));
    }

    @Test
    void toBase64AndBack() {

        // Data to make 2 VehiclePath, i.e. 1 PathPair
        LatLong64Path lat_path_1 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        LatLong64Path lat_path_2 = LatLong64Path.from(LatLong.of(1.0, 2.0), LatLong.of(3.0, 4.0));
        AltitudePath vert_path_1 = AltitudePath.from(Distance.ofFeet(100), Distance.ofFeet(200));
        AltitudePath vert_path_2 = AltitudePath.from(Distance.ofFeet(200), Distance.ofFeet(400));
        VehiclePath vp_1 = new VehiclePath(lat_path_1, vert_path_1);
        VehiclePath vp_2 = new VehiclePath(lat_path_2, vert_path_2);

        PathPair path = new PathPair(vp_1, vp_2);
        PathPair path2 = PathPair.fromBase64Str(path.toBase64());

        assertThat(path, (CoreMatchers.is(path2)));
    }
}
