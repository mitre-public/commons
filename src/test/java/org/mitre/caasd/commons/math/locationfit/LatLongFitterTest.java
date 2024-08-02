package org.mitre.caasd.commons.math.locationfit;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mitre.caasd.commons.Spherical.mod;
import static org.mitre.caasd.commons.math.locationfit.LatLongFitter.unMod;

import java.util.ArrayList;
import java.util.List;

import org.mitre.caasd.commons.LatLong;

import org.junit.jupiter.api.Test;

class LatLongFitterTest {

    @Test
    void crossesDateLine() {
        // "crossesDateLine" properly detects when longitude data crosses the internation date line

        ArrayList<Double> crossesDateLine = newArrayList(179.8, 179.9, 180.0, -179.9, -179.8);
        ArrayList<Double> crossesPrimeMeridian = newArrayList(-0.2, -0.1, 0.0, 0.1, 0.2);

        assertThat(LatLongFitter.crossesDateLine(crossesDateLine), is(true));
        assertThat(LatLongFitter.crossesDateLine(crossesPrimeMeridian), is(false));
    }

    @Test
    void moddedLongs() {
        // "moddedLongs" can patch longitude data with a discontinuity

        // Longitude values that cross the international date line are FIXED (e.g. no discontinuity)
        ArrayList<Double> crossesDateLine = newArrayList(179.8, 179.9, 180.0, -179.9, -179.8);
        List<Double> patchedData = LatLongFitter.moddedLongs(crossesDateLine);

        assertThat(patchedData, contains(179.8, 179.9, 180.0, 180.1, 180.2));

        // But Longitude values that cross the Prime Meridian are BROKEN (e.g. new discontinuity)
        ArrayList<Double> crossesPrimeMeridian = newArrayList(-0.2, -0.1, 0.0, 0.1, 0.2);
        List<Double> brokenData = LatLongFitter.moddedLongs(crossesPrimeMeridian);

        assertThat(brokenData, contains(359.8, 359.9, 0.0, 0.1, 0.2)); // jumps from 359.9 to 0.1
    }

    @Test
    void unModdingWorks() {
        // "unmodding" patched longitude values returns the original input data

        // Longitude values that cross the international date line are FIXED (e.g. no discontinuity)
        ArrayList<Double> crossesDateLine = newArrayList(179.8, 179.9, 180.0, -179.9, -179.8);

        List<Double> patchedData = LatLongFitter.moddedLongs(crossesDateLine);
        assertThat(patchedData, contains(179.8, 179.9, 180.0, 180.1, 180.2));

        List<Double> reversedPath =
                patchedData.stream().map(coerced -> unMod(coerced)).collect(toList());

        // Back to original data with the discontinuity ...
        assertThat(reversedPath, contains(179.8, 179.9, 180.0, -179.9, -179.8));
    }

    @Test
    void unModdingWorks_2() {
        // verify that all longitudes that get coerced into 0-360 can be fixed back to -180 to 180
        double stepSize = 0.1;
        double longitude = -180.0 + stepSize;

        while (longitude <= 180.0) {

            double coerced = mod(longitude, 360);
            double fixed = unMod(coerced);

            assertThat(longitude, closeTo(fixed, 0.00001));

            longitude += stepSize;
        }
    }

    @Test
    void cannotUnMod_negative180() {
        // documents this edge case

        double longitude = -180.0;
        double coerced = mod(longitude, 360); // apply correction for international date line
        double fixed = unMod(coerced); // undo the correction

        // note the "fixed" value is 180, NOT -180, aka it changed...
        assertThat(fixed, is(180.0));
        assertThat(fixed, is(not(longitude)));

        // but it doesn't matter because it represents the same point on earth
        LatLong a = LatLong.of(1.0, longitude);
        LatLong b = LatLong.of(1.0, fixed);
        assertThat(a.distanceTo(b).inNauticalMiles(), is(0.0));
    }
}
