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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Spherical.*;

import java.awt.Color;
import java.io.File;

import org.mitre.caasd.commons.maps.MapBuilder;
import org.mitre.caasd.commons.maps.MapFeatures;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SphericalTest {

    @Test
    public void testOneQuarterCircumferenceOfEarthInNM() {

        double EARTH_RADIUS_IN_MILES = 3_959; // according to google
        double MILES_PER_NAUTICAL_MILE = 1.15078; // according to google
        double EARTH_RADIUS_IN_NM = EARTH_RADIUS_IN_MILES / MILES_PER_NAUTICAL_MILE;
        double CIRCUMFERENCE_OF_EARTH_IN_NM = 2.0 * Math.PI * EARTH_RADIUS_IN_NM;

        double expected = CIRCUMFERENCE_OF_EARTH_IN_NM / 4;

        double TOLERANCE = 5; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, Spherical.oneQuarterCircumferenceOfEarthInNM(), TOLERANCE);
    }

    @Test
    public void testFeetPerNM() {

        double FEET_PER_NM = 6076.12; // according to google
        double expected = FEET_PER_NM;

        double TOLERANCE = 1; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, Spherical.feetPerNM(), TOLERANCE);
    }

    @Test
    public void testDistanceInNM_4args() {
        /*
         * TESTING: Spherical.distanceInNM(latDeg1, lonDeg1, latDeg2, lonDeg2)
         */
        double latDeg1 = 0;
        double lonDeg1 = 0;
        double latDeg2 = 10.0;
        double lonDeg2 = 10.0;

        // according to http://www.movable-type.co.uk/scripts/latlong.html
        double EXPECTED_DIST_IN_KM = 1569;
        double KM_PER_NM = 1.852;
        double expected = EXPECTED_DIST_IN_KM / KM_PER_NM;

        double TOLERANCE = 1; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, Spherical.distanceInNM(latDeg1, lonDeg1, latDeg2, lonDeg2), TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_sameLatLongTwice() {

        LatLong locationA = LatLong.of(42.0, 21.0);
        LatLong locationB = LatLong.of(21.0, 42.0);

        double expected = 360.0;
        double TOLERANCE = 0.001;

        assertEquals(expected, courseInDegrees(locationA, locationA), TOLERANCE);
        assertEquals(expected, courseInDegrees(locationB, locationB), TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_4args_dueEast() {
        /*
         * TESTING: Spherical.courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2) -- due east
         */
        double latDeg1 = 0;
        double lonDeg1 = 0;
        double latDeg2 = 0.0;
        double lonDeg2 = 10.0;

        double expected = 90.0;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2), TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_4args_dueWest() {
        /*
         * TESTING: Spherical.courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2) -- due west
         */
        double latDeg1 = 0;
        double lonDeg1 = 10;
        double latDeg2 = 0.0;
        double lonDeg2 = 0.0;

        double expected = 270.0;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2), TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_4args_dueNorth() {
        /*
         * TESTING: Spherical.courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2) -- due north
         */
        double latDeg1 = 0;
        double lonDeg1 = 0;
        double latDeg2 = 10.0;
        double lonDeg2 = 0.0;

        double expected = 360.0;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2), TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_4args_dueSouth() {
        /*
         * TESTING: Spherical.courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2) -- due south
         */
        double latDeg1 = 10;
        double lonDeg1 = 0;
        double latDeg2 = 0.0;
        double lonDeg2 = 0.0;

        double expected = 180.0;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2), TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_4args_northEast() {
        /*
         * TESTING: Spherical.courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2) -- northEast
         */
        double latDeg1 = 0.0;
        double lonDeg1 = 0.0;
        double latDeg2 = 1.0;
        double lonDeg2 = 1.0;

        double expected = 45.0;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected, courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2), TOLERANCE);
    }

    @Test
    public void courseTo_hasPosition() {
        // start at 0.0, 0.0
        HasPosition hp1 = () -> LatLong.of(0.0, 0.0);

        // move toward 1.0, 1.0
        HasPosition hp2 = () -> LatLong.of(1.0, 1.0);

        // you travel at almost 45 degrees
        assertThat(Spherical.courseBtw(hp1, hp2).inDegrees(), closeTo(45.0, 0.005));
    }

    //	@Test
    //	public void testProjectionInDegrees_4args() {
    //		/*
    //		 * TESTING: Spherical.projectOut(latDeg1, lonDeg1, latDeg2, lonDeg2) -- northEast
    //		 */
    //		double latDeg = 0.0;
    //		double lonDeg = 0.0;
    //		double headingInDegrees = Spherical.courseInDegrees(0.0, 0.0, 10.0, 10.0);
    //		double distNM = Spherical.distanceInNM(0.0, 0.0, 10.0, 10.0);
    //
    //		LatLong expected = new LatLong(10.0, 10.0);
    //		LatLong actual = Spherical.projectOut(latDeg, lonDeg, headingInDegrees, distNM);
    //
    //		double TOLERANCE = 0.01;  //some rounding is ok, this test is just for macro errors
    //
    //		assertEquals(
    //			expected.latitude(),
    //			actual.latitude(),
    //			TOLERANCE
    //		);
    //		assertEquals(
    //			expected.longitude(),
    //			actual.longitude(),
    //			TOLERANCE
    //		);
    //	}

    @Test
    public void projectWorksCorrectly() {

        LatLong start = LatLong.of(0.0, 0.0);
        LatLong end = LatLong.of(10.0, 10.0); // expected output LatLong
        Course direction = Spherical.courseBtw(start, end);
        Distance distance = Spherical.distanceBtw(start, end);

        LatLong actual = Spherical.projectOut(start, direction, distance);
        LatLong expected = end;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected.latitude(), actual.latitude(), TOLERANCE);
        assertEquals(expected.longitude(), actual.longitude(), TOLERANCE);
    }

    @Test
    public void testGreatCircleOrigin() {

        Double latitude = 0.0;
        Double longitude = 0.0;
        Double course = 90.0; // traveling due east

        LatLong expected = new LatLong(-90.0, 0.0); // the south pole
        LatLong actual = Spherical.greatCircleOrigin(latitude, longitude, course);

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected.latitude(), actual.latitude(), TOLERANCE);
        // all longitudes work at a pole
    }

    @Test
    public void testGreatCircleOrigin2() {

        Double latitude = 0.0;
        Double longitude = 0.0;
        Double course = 270.0; // traveling due west

        LatLong expected = new LatLong(90.0, 0.0); // the north pole
        LatLong actual = Spherical.greatCircleOrigin(latitude, longitude, course);

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected.latitude(), actual.latitude(), TOLERANCE);
        // all longitudes work at a pole
        //		assertEquals(
        //			expected.longitude(),
        //			actual.longitude(),
        //			TOLERANCE
        //		);
    }

    @Test
    public void testGreatCircleOrigin3() {

        Double latitude = 0.0;
        Double longitude = 0.0;
        Double course = 0.0; // traveling due north

        LatLong expected = new LatLong(0.0, 90.0); // a point on the equator 1/4 around the earth
        LatLong actual = Spherical.greatCircleOrigin(latitude, longitude, course);

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected.latitude(), actual.latitude(), TOLERANCE);
        assertEquals(expected.longitude(), actual.longitude(), TOLERANCE);
    }

    @Test
    public void testGreatCircleOrigin4() {

        Double latitude = 0.0;
        Double longitude = 0.0;
        Double course = 180.0; // traveling due south

        LatLong expected = new LatLong(0.0, -90.0); // a point on the equator 1/4 around the earth
        LatLong actual = Spherical.greatCircleOrigin(latitude, longitude, course);

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(expected.latitude(), actual.latitude(), TOLERANCE);
        assertEquals(expected.longitude(), actual.longitude(), TOLERANCE);
    }

    @Test
    public void testDistanceInNM_LatLongPair_LatLongPair() {
        /*
         * TESTING: Spherical.distanceInNM(LatLongPair, LatLongPair)
         */
        double latDeg1 = 0;
        double lonDeg1 = 0;
        double latDeg2 = 10.0;
        double lonDeg2 = 10.0;

        // according to http://www.movable-type.co.uk/scripts/latlong.html
        double EXPECTED_DIST_IN_KM = 1569;
        double KM_PER_NM = 1.852;
        double expected = EXPECTED_DIST_IN_KM / KM_PER_NM;

        double TOLERANCE = 1; // some rounding is ok, this test is just for macro errors

        assertEquals(
                expected,
                Spherical.distanceInNM(new LatLong(latDeg1, lonDeg1), new LatLong(latDeg2, lonDeg2)),
                TOLERANCE);
    }

    @Test
    public void testCourseInDegrees_LatLongPair_LatLongPair() {
        /*
         * TESTING: Spherical.courseInDegrees(LatLongPair, LatLongPair) -- northEast
         */
        double latDeg1 = 0.0;
        double lonDeg1 = 0.0;
        double latDeg2 = 1.0;
        double lonDeg2 = 1.0;

        double expected = 45.0;

        double TOLERANCE = 0.01; // some rounding is ok, this test is just for macro errors

        assertEquals(
                expected, courseInDegrees(new LatLong(latDeg1, lonDeg1), new LatLong(latDeg2, lonDeg2)), TOLERANCE);
    }

    @Test
    public void testAngleDifference_Double() {
        double TOLERANCE = 0.0001;

        // test positive inputs
        assertEquals(5.0, Spherical.angleDifference(5.0), TOLERANCE);
        assertEquals(175.0, Spherical.angleDifference(175.0), TOLERANCE);
        assertEquals(-175.0, Spherical.angleDifference(185.0), TOLERANCE);
        assertEquals(-5.0, Spherical.angleDifference(355.0), TOLERANCE);

        // test negative inputs
        assertEquals(-5.0, Spherical.angleDifference(-5.0), TOLERANCE);
        assertEquals(-175.0, Spherical.angleDifference(-175.0), TOLERANCE);
        assertEquals(175.0, Spherical.angleDifference(-185.0), TOLERANCE);
    }

    @Test
    public void testAngleDifference_2args() {

        double TOLERANCE = 0.0001;

        assertEquals(10.0, Spherical.angleDifference(5.0, 355.0), TOLERANCE);
        assertEquals(-10.0, Spherical.angleDifference(355.0, 5.0), TOLERANCE);
    }

    @Test
    public void testCrossTrackDistance() {

        double TOLERANCE = 0.0001;

        HasPosition ls = () -> LatLong.of(0.0, 0.0);
        HasPosition le = () -> LatLong.of(0.0, 10.0);
        HasPosition p = () -> LatLong.of(1.0, 0.5);

        double CTD = crossTrackDistanceNM(ls, le, p);
        double ATD = alongTrackDistanceNM(ls, le, p);

        assertTrue(CTD < 0.0);
        assertTrue(ATD < (-1.0 * CTD));

        assertEquals(-60.00686673640662, CTD, TOLERANCE);
        assertEquals(30.00343415285915, ATD, TOLERANCE);

        p = () -> LatLong.of(1.0, -0.5);

        CTD = crossTrackDistanceNM(ls, le, p);
        ATD = alongTrackDistanceNM(ls, le, p, CTD);
        assertEquals(-30.00343415285915, ATD, TOLERANCE);

        p = () -> LatLong.of(1.0, 11.0);

        CTD = crossTrackDistanceNM(ls, le, p);
        ATD = alongTrackDistanceNM(ls, le, p, CTD);
        assertTrue(ATD > ls.distanceInNmTo(le));
    }

    @Test
    void testAlongTrackDistanceFloatingPointError() {
        // In the past there 3 points generated a NaN for the alongTrackDistance computation
        // These 3 point form a Triangle with sides:
        // start-end = 25.97489NM
        // start-point = 0.01393NM
        // end-point = 25.97490NM
        final HasPosition START = HasPosition.from(46.294875, -119.96004166666667);
        final HasPosition END = HasPosition.from(46.57024166666667, -120.44463611111111);
        final HasPosition POINT = HasPosition.from(46.29469627061987, -119.96025624188381);

        double atd_method1 = alongTrackDistanceNM(START, END, POINT);
        double atd_method2 = alongTrackDistanceNM(START, END, POINT, crossTrackDistanceNM(START, END, POINT));

        assertThat(atd_method1, is(not(Double.NaN)));
        assertThat(atd_method2, is(not(Double.NaN)));
        assertThat(atd_method1, is(atd_method2));
    }

    @Disabled
    @Test
    void testShowMapForAlongTrackDistanceFloatingPointError() {
        // In the past there 3 points generated a NaN for the alongTrackDistance computation
        // These 3 point form a Triangle with sides:
        // start-end = 25.97489NM
        // start-point = 0.01393NM
        // end-point = 25.97490NM
        final HasPosition START = HasPosition.from(46.294875, -119.96004166666667);
        final HasPosition END = HasPosition.from(46.57024166666667, -120.44463611111111);
        final HasPosition POINT = HasPosition.from(46.29469627061987, -119.96025624188381);

        MapBuilder.newMapBuilder()
                .center(LatLong.avgLatLong(START.latLong(), END.latLong()))
                .width(Distance.ofNauticalMiles(30))
                .mapBoxDarkMode()
                .addFeature(MapFeatures.line(START.latLong(), END.latLong(), Color.RED, 2.0f))
                .addFeature(MapFeatures.circle(POINT.latLong(), Color.GREEN, 15, 2.0f))
                .toFile(new File("alongTrackNumericError.png"));
    }

    @Test
    void testArcLengthSmallRadius() {
        double radius = 1.0;
        double angle = 90;

        assertEquals(Math.PI / 2, Spherical.arcLength(radius, angle), 0.01);
    }

    @Test
    void testArcLengthLargeRadius() {
        double radius = Spherical.oneQuarterCircumferenceOfEarthInNM();
        double angle = 90;

        assertEquals(Spherical.oneQuarterCircumferenceOfEarthInNM(), Spherical.arcLength(radius, angle), 0.01);
    }
}
