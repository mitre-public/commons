/*
 *    Copyright 2022 The MITRE Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.mitre.caasd.commons;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.PI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.LatLong.*;
import static org.mitre.caasd.commons.Spherical.EARTH_RADIUS_NM;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.mitre.caasd.commons.fileutil.FileUtils;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LatLongTest {

    @TempDir
    public File tempDir;

    @Test
    public void testCheckBounds_latTooBig() {
        assertThrows(IllegalArgumentException.class, () -> LatLong.of(90.1, 0.0));
    }

    @Test
    public void testCheckBounds_latTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> LatLong.of(-90.1, 0.0));
    }

    @Test
    public void testCheckBounds_longTooBig() {
        assertThrows(IllegalArgumentException.class, () -> LatLong.of(0.0, 180.1));
    }

    @Test
    public void testCheckBounds_longTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> LatLong.of(0.0, -180.1));
    }

    @Test
    void clampLatitude_spec() {
        assertThat(clampLatitude(Double.NEGATIVE_INFINITY), is(-90.0));
        assertThat(clampLatitude(-90.001), is(-90.0));
        assertThat(clampLatitude(-90.0), is(-90.0));
        assertThat(clampLatitude(-89.999), is(-89.999));

        assertThat(clampLatitude(Double.POSITIVE_INFINITY), is(90.0));
        assertThat(clampLatitude(90.001), is(90.0));
        assertThat(clampLatitude(90.00), is(90.0));
        assertThat(clampLatitude(89.999), is(89.999));
    }

    @Test
    void clampLongitude_spec() {
        assertThat(clampLongitude(Double.NEGATIVE_INFINITY), is(-180.0));
        assertThat(clampLongitude(-180.001), is(-180.0));
        assertThat(clampLongitude(-180.0), is(-180.0));
        assertThat(clampLongitude(-179.999), is(-179.999));

        assertThat(clampLongitude(Double.POSITIVE_INFINITY), is(180.0));
        assertThat(clampLongitude(180.01), is(180.0));
        assertThat(clampLongitude(180.00), is(180.0));
        assertThat(clampLongitude(179.999), is(179.999));
    }

    @Test
    public void testEqualsAndHashcode() {
        LatLong one = new LatLong(45.0, 45.0);
        LatLong two = new LatLong(45.0, 45.0);
        LatLong three = new LatLong(45.0, 46.0);
        LatLong four = new LatLong(46.0, 45.0);
        LatLong five = new LatLong(46.0, 46.0);

        String other = "I am not a LatLong Obj";
        String nullObject = null;

        assertEquals(one, one);

        assertEquals(one, two);
        assertEquals(two, one);

        assertNotEquals(one, three);
        assertNotEquals(three, one);

        assertNotEquals(one, four);
        assertNotEquals(four, one);

        assertNotEquals(one, five);
        assertNotEquals(five, one);

        assertNotEquals(one, other);
        assertNotEquals(other, one);

        assertNotEquals(one, nullObject);

        assertEquals(one.hashCode(), two.hashCode());
        assertNotEquals(one.hashCode(), three.hashCode());
    }

    @Test
    public void testBothConstructors() {
        LatLong a = new LatLong(15.21, 32.5);
        LatLong b = LatLong.of(15.21, 32.5);
        assertThat("These are not the same physcial object", a == b, is(false));
        assertThat("But these objects are equal", a.equals(b), is(true));
    }

    @Test
    public void testToString() {
        LatLong instance = new LatLong(15.0, 22.0);

        String toString = instance.toString();

        assertThat(toString.contains("15.0"), is(true));
        assertThat(toString.contains("22.0"), is(true));

        assertThat(
                "The Latitude value should come first", toString.indexOf("15.0") < toString.indexOf("22.0"), is(true));
    }

    @Test
    public void testToBytesAndBack() {

        LatLong instance = new LatLong(15.0, 22.0);

        byte[] asBytes = instance.toBytes();

        LatLong instanceRemake = LatLong.fromBytes(asBytes);

        assertThat(instance, is(instanceRemake));
        assertThat(instance.latitude(), is(instanceRemake.latitude()));
        assertThat(instance.longitude(), is(instanceRemake.longitude()));
    }

    @Test
    public void testBase64Encoding() {

        Random rng = new Random(17L);
        int N = 50;

        for (int i = 0; i < N; i++) {
            double lat = rng.nextDouble() * 10;
            double lon = rng.nextDouble() * 20;
            LatLong in = LatLong.of(lat, lon);
            String asBase64 = in.toBase64();
            LatLong out = LatLong.fromBase64Str(asBase64);

            assertThat(in, is(out));
            assertThat(in.latitude(), is(out.latitude()));
            assertThat(in.longitude(), is(out.longitude()));
        }
    }

    @Test
    public void testDistanceTo() {
        LatLong one = new LatLong(0.0, 0.0);
        LatLong two = new LatLong(1.0, 1.0);

        double EXPECTED_DIST_IN_KM = 157.2;

        Distance expectedDist = Distance.ofKiloMeters(EXPECTED_DIST_IN_KM);
        Distance actualDistance = one.distanceTo(two);

        double TOLERANCE = 0.1;

        assertEquals(expectedDist.inNauticalMiles(), actualDistance.inNauticalMiles(), TOLERANCE);
    }

    @Test
    public void testDistanceTo_Triangle() {
        LatLong point_a = new LatLong(0.0, 0.0);
        LatLong point_b = new LatLong(0.0, 1.0);
        LatLong point_c = new LatLong(1.0, 0.0);

        double leg_one = point_a.distanceTo(point_b).inMiles();
        double leg_two = point_a.distanceTo(point_c).inMiles();
        double hypotenuse = point_b.distanceTo(point_c).inMiles();

        // for right triangle on sphere, the square of the hypotenuse <= sum of squares of legs
        assertThat(hypotenuse * hypotenuse <= leg_one * leg_one + leg_two * leg_two, is(true));
    }

    @Test
    public void testDistanceInNM() {
        LatLong one = new LatLong(0.0, 0.0);
        LatLong two = new LatLong(1.0, 1.0);

        double EXPECTED_DIST_IN_KM = 157.2;
        double KM_PER_NM = 1.852;
        double expectedDistance = EXPECTED_DIST_IN_KM / KM_PER_NM;
        double actualDistance = one.distanceInNM(two);

        double TOLERANCE = 0.1;

        assertEquals(expectedDistance, actualDistance, TOLERANCE);
    }

    @Test
    public void testCourseInDegrees() {
        LatLong one = new LatLong(0.0, 0.0);
        LatLong two = new LatLong(1.0, 1.0);

        double TOLERANCE = 0.1;

        assertEquals(45.0, one.courseInDegrees(two), TOLERANCE);
    }

    @Test
    public void testProjectOut() {

        LatLong source = new LatLong(0.0, 0.0);

        double course = 45.0;

        double DIST_IN_KM = 157.2;
        double KM_PER_NM = 1.852;
        double distance = DIST_IN_KM / KM_PER_NM;

        LatLong actualDestination = source.projectOut(course, distance);
        LatLong expectedDestination = new LatLong(1.0, 1.0);

        double TOLERANCE = 0.01;

        assertEquals(actualDestination.latitude(), expectedDestination.latitude(), TOLERANCE);
        assertEquals(actualDestination.longitude(), expectedDestination.longitude(), TOLERANCE);
    }

    @Test
    public void testProjectionInDegrees_negativeDist() {

        LatLong source = new LatLong(0.0, 0.0);

        double course = 45.0;

        double DIST_IN_KM = 157.2;
        double KM_PER_NM = 1.852;
        double distance = DIST_IN_KM / KM_PER_NM;
        double negativeDistance = -distance;

        LatLong actualDestination = source.projectOut(course, negativeDistance);
        LatLong expectedDestination = new LatLong(-1.0, -1.0);

        double TOLERANCE = 0.01;

        assertEquals(actualDestination.latitude(), expectedDestination.latitude(), TOLERANCE);
        assertEquals(actualDestination.longitude(), expectedDestination.longitude(), TOLERANCE);
    }

    @Test
    public void testGreatCircleOrigin() {

        LatLong source = new LatLong(0.0, 0.0);
        Double course = 0.0; // traveling due north

        LatLong expected = new LatLong(0.0, 90.0); // a point on the equator 1/4 around the earth
        LatLong actual = source.greatCircleOrigin(course);

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected.latitude(), actual.latitude(), TOLERANCE);
        assertEquals(expected.longitude(), actual.longitude(), TOLERANCE);
    }

    @Test
    public void testMinMaxMethods() {

        LatLong v1 = LatLong.of(40.75, -73.9);
        LatLong v2 = LatLong.of(40.75, -74.1);
        LatLong v3 = LatLong.of(40.7, -74.1);
        LatLong v4 = LatLong.of(40.7, -73.9);

        List<LatLong> points = newArrayList(v1, v2, v3, v4);

        double TOLERANCE = 0.001;
        assertEquals(40.7, minLatitude(points), TOLERANCE);
        assertEquals(40.75, maxLatitude(points), TOLERANCE);
        assertEquals(-74.1, minLongitude(points), TOLERANCE);
        assertEquals(-73.9, maxLongitude(points), TOLERANCE);
    }

    @Test
    public void testMinLatitude_nullInput() {
        // input cannot be null
        assertThrows(NullPointerException.class, () -> minLongitude(null));
    }

    @Test
    public void testMinLatitude_emptyInput() {
        // input cannot be empty
        assertThrows(IllegalArgumentException.class, () -> minLatitude(newArrayList()));
    }

    @Test
    public void testMaxLatitude_nullInput() {
        // input cannot be null
        assertThrows(NullPointerException.class, () -> maxLatitude(null));
    }

    @Test
    public void testMaxLatitude_emptyInput() {
        // input cannot be empty
        assertThrows(IllegalArgumentException.class, () -> maxLatitude(newArrayList()));
    }

    @Test
    public void testMinLongitude_nullInput() {
        // input cannot be null
        assertThrows(NullPointerException.class, () -> minLatitude(null));
    }

    @Test
    public void testMinLongitude_emptyInput() {
        // input cannot be empty
        assertThrows(IllegalArgumentException.class, () -> minLatitude(newArrayList()));
    }

    @Test
    public void testMaxLongitude_nullInput() {
        // input cannot be null
        assertThrows(NullPointerException.class, () -> maxLongitude(null));
    }

    @Test
    public void testMaxLongitude_emptyInput() {
        // input cannot be empty
        assertThrows(IllegalArgumentException.class, () -> maxLongitude(newArrayList()));
    }

    @Test
    public void testAvgLatLong() {
        LatLong one = LatLong.of(0.0, 10.0);
        LatLong two = LatLong.of(10.0, 20.0);
        LatLong niaveAverage = LatLong.of(5.0, 15.0); // niave arthimatic average of LatLong
        LatLong correctAverage = avgLatLong(one, two);

        Distance oneToAvg = one.distanceTo(correctAverage);
        Distance avgToTwo = correctAverage.distanceTo(two);
        Distance realToNiave = correctAverage.distanceTo(niaveAverage);

        assertEquals(oneToAvg.inNauticalMiles(), avgToTwo.inNauticalMiles(), 0.00001);

        // the naive answer is off by over 2.5 NM !
        assertThat(realToNiave.isGreaterThan(Distance.ofNauticalMiles(2.5)), is(true));
    }

    @Test
    public void testAvgLatLong_poleToPole() {
        LatLong north = LatLong.of(89.0, 0.0); // near north pole
        LatLong south = LatLong.of(-89.5, 0.0); // near south pole

        LatLong avg = avgLatLong(north, south);

        Distance oneToTwo = north.distanceTo(south);

        Distance halfEarthCircumfrence = Distance.ofNauticalMiles(PI * EARTH_RADIUS_NM);

        // these values are similar...but halfEarthCircumfrence is slightly larger
        assertThat(oneToTwo.dividedBy(halfEarthCircumfrence) > .95, is(true));
        assertThat(halfEarthCircumfrence.dividedBy(oneToTwo) < 1.05, is(true));

        // the average is the same distance to both the northern and southern points
        assertEquals(
                avg.distanceTo(north).inNauticalMiles(), avg.distanceTo(south).inNauticalMiles(), 0.001);
    }

    @Test
    public void testAvgLatLong_acrossDateLine() {

        LatLong east = LatLong.of(0.0, -179.5); // just east of line
        LatLong west = LatLong.of(0.0, 179.0); // just west of line
        LatLong niaveAverage = LatLong.of(0.0, -0.25); // niave arthimatic average of LatLong
        LatLong correctAverage = avgLatLong(east, west);

        Distance distBtwPoints = east.distanceTo(west);

        // the distance between the east and west points is small (about 90.01 NM)
        assertThat(distBtwPoints.isLessThan(Distance.ofNauticalMiles(91.0)), is(true));
        assertThat(distBtwPoints.isGreaterThan(Distance.ofNauticalMiles(89.0)), is(true));

        // the distance between the average point and the east point is about 45.5 NM
        assertThat(correctAverage.distanceTo(east).isLessThan(Distance.ofNauticalMiles(45.5)), is(true));
        assertThat(correctAverage.distanceTo(east).isGreaterThan(Distance.ofNauticalMiles(44.5)), is(true));

        // the distance between the average point and the west point is about 45.5 NM
        assertThat(correctAverage.distanceTo(west).isLessThan(Distance.ofNauticalMiles(45.5)), is(true));
        assertThat(correctAverage.distanceTo(west).isGreaterThan(Distance.ofNauticalMiles(44.5)), is(true));

        // the distance from average to east = distance from average to west
        assertEquals(
                correctAverage.distanceTo(east).inNauticalMiles(),
                correctAverage.distanceTo(west).inNauticalMiles(),
                0.001);

        // the naive answer is literally on the otherside of the planet
        Distance error = correctAverage.distanceTo(niaveAverage);
        assertThat(error.isGreaterThan(Distance.ofNauticalMiles(10_801)), is(true));
    }

    @Test
    public void testAvgLatLong_array() {

        LatLong one = LatLong.of(0.0, -179.5); // just east of date-line
        LatLong two = LatLong.of(0.0, 179.0); // just west of date-line
        LatLong three = LatLong.of(1.0, 179.0); // due north of two

        LatLong realAverage = LatLong.avgLatLong(one, two, three);
        LatLong manualAverage = LatLong.of(0.33, 179.5);

        // the real average is NOT the manual average
        assertThat(realAverage.distanceTo(manualAverage).isGreaterThan(Distance.ofNauticalMiles(0)), is(true));

        // distance are small -- so error won't be too big assuming the internation date line doesn't hose the
        // computation
        assertThat(realAverage.latitude() > .33 && realAverage.latitude() < .34, is(true));
        assertThat(realAverage.longitude() > 179.5 && realAverage.longitude() < 179.51, is(true));
    }

    @Test
    public void testQuickAvgLatLong_1() {

        LatLong east = LatLong.of(0.0, -179.5); // just east of line
        LatLong west = LatLong.of(0.0, 179.0); // just west of line
        LatLong quickAverage = LatLong.quickAvgLatLong(east, west);
        LatLong expected = LatLong.of(0.0, 179.750);

        assertThat(quickAverage.distanceTo(expected).isLessThan(Distance.ofNauticalMiles(0.001)), is(true));
    }

    @Test
    public void testQuickAvgLatLong_2() {

        LatLong east = LatLong.of(-11.0, 12.5);
        LatLong west = LatLong.of(15.0, -42.0);
        LatLong quickAverage = LatLong.quickAvgLatLong(east, west);
        LatLong expected = LatLong.of(2.0, -14.75);

        assertThat(quickAverage.distanceTo(expected).isLessThan(Distance.ofNauticalMiles(0.001)), is(true));
    }

    @Test
    public void testAvgLatLong_similarResults_differentMethods() {

        LatLong one = LatLong.of(42.93675, -83.70776);
        LatLong two = LatLong.of(42.95037, -83.70570);

        // compute the solution two ways..
        LatLong quickAverage = LatLong.quickAvgLatLong(one, two);
        LatLong accurateAverage = LatLong.avgLatLong(one, two);

        // solutions a NOT the same...but they are damn near indistiquishable
        assertNotEquals(quickAverage, accurateAverage);
        assertThat(quickAverage.distanceTo(accurateAverage).isLessThan(Distance.ofNauticalMiles(0.00005)), is(true));
    }

    @Test
    public void quickAvgLatLong_simple() {

        // These points are 846.45952 Nautical Miles apart!
        // The "naive average location" will be WRONG
        LatLong one = LatLong.of(0.0, 10.0);
        LatLong two = LatLong.of(10.0, 20.0);

        ArrayList<LatLong> path = newArrayList(one, two);

        LatLong average = LatLong.avgLatLong(path); // accurately computed avg LatLong
        LatLong naiveAverage = LatLong.of(5.0, 15.0); // naive arithmetic average of LatLong

        assertThat(LatLong.quickAvgLatLong(path), is(naiveAverage));

        // the naive answer is off by over 2.5 NM!
        Distance realToNaive = average.distanceTo(naiveAverage);
        assertThat(realToNaive.isGreaterThan(Distance.ofNauticalMiles(2.5)), is(true));
    }

    @Test
    public void testQuickAvgLatLong_acrossDateLine() {

        LatLong east = LatLong.of(0.0, -179.5); // just east of line
        LatLong west = LatLong.of(0.0, 179.0); // just west of line

        ArrayList<LatLong> path = newArrayList(east, west);

        LatLong naiveAverage = LatLong.of(0.0, -0.25); // naive arithmetic average of LatLong
        LatLong correctAverage = LatLong.quickAvgLatLong(path); // (0.0,179.75)

        Distance distBtwPoints = east.distanceTo(west);

        // the distance between the east and west points is small (about 90.01 NM)
        assertThat(distBtwPoints.isLessThan(Distance.ofNauticalMiles(91.0)), is(true));
        assertThat(distBtwPoints.isGreaterThan(Distance.ofNauticalMiles(89.0)), is(true));

        // the distance between the average point and the east point is about 45.5 NM
        assertThat(correctAverage.distanceTo(east).isLessThan(Distance.ofNauticalMiles(45.5)), is(true));
        assertThat(correctAverage.distanceTo(east).isGreaterThan(Distance.ofNauticalMiles(44.5)), is(true));

        // the distance between the average point and the west point is about 45.5 NM
        assertThat(correctAverage.distanceTo(west).isLessThan(Distance.ofNauticalMiles(45.5)), is(true));
        assertThat(correctAverage.distanceTo(west).isGreaterThan(Distance.ofNauticalMiles(44.5)), is(true));

        // the distance from average to east = distance from average to west
        assertEquals(
                correctAverage.distanceTo(east).inNauticalMiles(),
                correctAverage.distanceTo(west).inNauticalMiles(),
                0.001);

        // the naive answer is literally on the other side of the planet
        Distance error = correctAverage.distanceTo(naiveAverage);
        assertThat(error.isGreaterThan(Distance.ofNauticalMiles(10_801)), is(true));
    }

    @Test
    public void testQuickAvgLatLong_acrossDateLine_2() {

        LatLong east = LatLong.of(0.0, -179.5); // just east of line  (aka "180 - .5")
        LatLong west_1 = LatLong.of(0.0, 179.0); // just west of line (aka "180 + 1")
        LatLong west_2 = LatLong.of(0.0, 179.0); // just west of line (aka "180 + 1")

        ArrayList<LatLong> path_1 = newArrayList(east, west_1);
        ArrayList<LatLong> path_2 = newArrayList(east, west_1, west_2);

        LatLong midPoint = LatLong.quickAvgLatLong(path_1); // should be midpoint
        LatLong twoThirdPoint = LatLong.quickAvgLatLong(path_2); // should be 2/3rds towards west

        assertThat(midPoint, is(LatLong.of(0.0, 179.75)));
        assertThat(twoThirdPoint, is(LatLong.of(0.0, 179.5)));

        Distance distBtwPoints = east.distanceTo(west_1);

        // the distance between the east and west points is small (about 90.01 NM)
        assertThat(distBtwPoints.isLessThan(Distance.ofNauticalMiles(90.1)), is(true));
        assertThat(distBtwPoints.isGreaterThan(Distance.ofNauticalMiles(90.0)), is(true));

        // the distance between the average point and the east point is about 45.5 NM
        assertEquals(midPoint.distanceTo(east).inNauticalMiles(), 45.005, 0.001);

        // the distance between the average point and the west point is about 45.005 NM
        assertEquals(midPoint.distanceTo(west_1).inNauticalMiles(), 45.005, 0.001);

        // the distance from average to east = distance from average to west
        assertEquals(
                midPoint.distanceTo(east).inNauticalMiles(),
                midPoint.distanceTo(west_1).inNauticalMiles(),
                0.001);

        // the distance between the 2/3rd point and the east point is about 60.006 NM
        assertEquals(twoThirdPoint.distanceTo(east).inNauticalMiles(), 60.006, 0.001);

        // the distance between the 2/3rd point and the west point is about 30.003 NM
        assertEquals(twoThirdPoint.distanceTo(west_1).inNauticalMiles(), 30.003, 0.001);

        // the distance from 2/3rd point to east is twice the distance from 2/3rd point to west
        assertEquals(
                twoThirdPoint.distanceTo(east).inNauticalMiles(),
                twoThirdPoint.distanceTo(west_1).inNauticalMiles() * 2,
                0.001);
    }

    @Test
    public void testAvgLatLong_similarResults_differentMethods_longerPaths() {

        // total path length = 254.52942NM
        ArrayList<LatLong> path =
                newArrayList(LatLong.of(0.0, 0.1), LatLong.of(1.0, 1.1), LatLong.of(2.0, 2.1), LatLong.of(3.0, 3.1));

        // compute the solution two ways ..
        LatLong quickAverage = LatLong.quickAvgLatLong(path);
        LatLong accurateAverage = LatLong.avgLatLong(path);

        // solutions a NOT the same...but they are very similar given the length of the path involved
        assertNotEquals(quickAverage, accurateAverage);

        Distance leg1 = path.get(0).distanceTo(path.get(1));
        Distance leg2 = path.get(1).distanceTo(path.get(2));
        Distance leg3 = path.get(2).distanceTo(path.get(3));
        Distance pathDist = leg1.plus(leg2).plus(leg3); // 254.52942NM

        Distance delta = quickAverage.distanceTo(accurateAverage); // 0.03832NM

        assertThat(delta.inNauticalMiles() / pathDist.inNauticalMiles(), lessThan(0.001));
    }

    @Test
    public void avgLatLong_empty_collection() {
        /*
         * No change in behavior between avgLatLong and quickAvgLatLong
         */

        ArrayList<LatLong> locations = newArrayList();

        assertThrows(NoSuchElementException.class, () -> LatLong.avgLatLong(locations));

        assertThrows(NoSuchElementException.class, () -> LatLong.quickAvgLatLong(locations));
    }

    @Test
    public void avgLatLong_empty_arrays() {
        /*
         * No change in behavior between avgLatLong and quickAvgLatLong
         */

        LatLong[] locations = new LatLong[0];

        assertThrows(NoSuchElementException.class, () -> LatLong.avgLatLong(locations));

        assertThrows(NoSuchElementException.class, () -> LatLong.quickAvgLatLong(locations));
    }

    @Test
    public void avgLatLong_null_collection() {
        /*
         * No change in behavior between avgLatLong and quickAvgLatLong
         */

        ArrayList<LatLong> locations = null;

        assertThrows(NullPointerException.class, () -> LatLong.avgLatLong(locations));

        assertThrows(NullPointerException.class, () -> LatLong.quickAvgLatLong(locations));
    }

    @Test
    public void avgLatLong_null_arrays() {
        /*
         * No change in behavior between avgLatLong and quickAvgLatLong
         */

        LatLong[] locations = null;

        assertThrows(NullPointerException.class, () -> LatLong.avgLatLong(locations));

        assertThrows(NullPointerException.class, () -> LatLong.quickAvgLatLong(locations));
    }

    @Test
    public void isWithin_samePoint() {

        LatLong one = LatLong.of(0.0, 0.0);
        LatLong two = LatLong.of(0.0, 0.0); // same point twice

        assertThat(one.isWithin(Distance.ofFeet(0), two), is(true));
        assertThat(one.isWithin(Distance.ofFeet(0.1), two), is(true));
        assertThat(one.isWithin(Distance.ofFeet(-1.0), two), is(false));
    }

    @Test
    public void isWithin_differentPoints() {

        LatLong one = new LatLong(0.0, 0.0);
        LatLong two = new LatLong(1.0, 1.0);

        Distance computedDistance = one.distanceTo(two);

        double EXPECTED_DIST_IN_KM = 157.2;

        Distance expectedDist = Distance.ofKiloMeters(EXPECTED_DIST_IN_KM);

        double TOLERANCE = 0.05;
        assertThat(computedDistance.minus(expectedDist).inKilometers(), Matchers.closeTo(0.0, TOLERANCE));
        assertThat(one.isWithin(computedDistance, two), is(true));
    }

    @Test
    void can_compress() {

        LatLong raw = new LatLong(83.225689134, -22.324187543);
        LatLong64 compressed = raw.compress();

        assertThat(raw.latitude(), closeTo(compressed.latitude(), 0.0000001));
        assertThat(raw.longitude(), closeTo(compressed.longitude(), 0.0000001));
    }

    @Test
    public void verifySerializability() {

        LatLong location = new LatLong(1.0, 5.0);

        String fileName = "latLongSerializationTest.ser";
        File targetFile = new File(tempDir, fileName);

        assertThat("The targetFile should not exists yet", targetFile.exists(), is(false));

        FileUtils.serialize(location, targetFile);

        assertThat("The targetFile should now exist", targetFile.exists(), is(true));

        Object obj = FileUtils.deserialize(targetFile);

        assertEquals(location, obj);
    }

    @Test
    public void canBuildUsingPrivateNoArgConstructor() {

        Class<LatLong> clazz = LatLong.class;
        Class<?>[] NO_ARG = new Class[] {};

        // We can build a LatLong object using a private no-arg constructor
        assertDoesNotThrow(() -> {
            Constructor<LatLong> structor = clazz.getDeclaredConstructor(NO_ARG);
            structor.setAccessible(true); // don't allow a private constructor to stop us!

            LatLong location = structor.newInstance((Object[]) NO_ARG);
        });
    }

    @Test
    public void isAvroCompatible() throws IOException {

        Schema schema = ReflectData.get().getSchema(LatLong.class);
        DataFileWriter<LatLong> dfw = new DataFileWriter<>(new ReflectDatumWriter<>(schema));

        File targetFile = new File(tempDir, "LatLong.avro");
        assertThat(targetFile.exists(), is(false));

        dfw.create(schema, targetFile);

        dfw.append(LatLong.of(0.0, 0.0));
        dfw.append(LatLong.of(5.0, -5.0));
        dfw.close();

        DataFileReader<LatLong> reader = new DataFileReader<>(targetFile, new ReflectDatumReader<>(schema));

        LatLong first = reader.next();
        LatLong second = reader.next();

        assertThat(first, is(LatLong.of(0.0, 0.0)));
        assertThat(second, is(LatLong.of(5.0, -5.0)));
        assertThat(reader.hasNext(), is(false));

        reader.close();
    }
}
