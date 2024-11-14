package org.mitre.caasd.commons;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.math3.util.FastMath;
import org.junit.jupiter.api.Test;

class RhumbTest {

    static double lat1 = 35.766666666666666;
    static double lon1 = -111.84166666666667;

    static double lat2 = 35.7;
    static double lon2 = -110.23333333333333;

    static LatLong point1 = new LatLong(lat1, lon1);
    static LatLong point2 = new LatLong(lat2, lon2);

    @Test
    void testProject() {
        double distRad = Rhumb.rhumbDistance(point1, point2);
        double az12Rad = Rhumb.rhumbAzimuth(point1, point2);
        LatLong testPt = Rhumb.rhumbEndPosition(point1, az12Rad, distRad);
        double rhumbLineDistanceNM = Spherical.distanceInNM(distRad);
        double sphericalEarthDistanceNM = Spherical.distanceInNM(point1, point2);

        assertAll(
                () -> assertEquals(35.7, testPt.latitude(), .000000001),
                () -> assertEquals(-110.23333333333333, testPt.longitude(), .000000001),
                () -> assertEquals(
                        sphericalEarthDistanceNM,
                        rhumbLineDistanceNM,
                        .01,
                        "not the same distance but pretty close at this distance"),
                () -> assertNotEquals(rhumbLineDistanceNM, sphericalEarthDistanceNM, "def not the same though"));
    }

    @Test
    void testDistance() {
        double dist = Rhumb.rhumbDistance(point1, point2);
        assertEquals(.02281, dist, .001);
    }

    @Test
    void testAzimuth() {
        double azimuth = Rhumb.rhumbAzimuth(point1, point2);
        double expected = FastMath.toRadians(92.91030319); // geo-lib online calculator
        assertEquals(expected, azimuth, .001, "Expected is from Geo-Lib which is WGS84 vs Spherical Here");
    }
}
