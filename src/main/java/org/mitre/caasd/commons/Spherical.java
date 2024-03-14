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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;
import static org.apache.commons.math3.util.FastMath.acos;
import static org.apache.commons.math3.util.FastMath.asin;
import static org.apache.commons.math3.util.FastMath.atan;
import static org.apache.commons.math3.util.FastMath.atan2;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.apache.commons.math3.util.FastMath.sqrt;
import static org.apache.commons.math3.util.FastMath.toDegrees;

import com.google.common.collect.Range;

/**
 * A Collection of convenience methods for making transformations and computations with latitude /
 * longitude values on a sphere.
 * <p>
 * The methods in this class are backed by Apache FastMath. FastMath provides methods like cos, sin,
 * tan, asin, acos, atan. However, the implementations of these methods in FastMath do not rely on
 * native code as their counterparts in java.lang.Math do. Consequently, the FastMath version are
 * about 2.23x faster than their java.lang.Math counterparts (this was a carefully benchmarked
 * measurement).
 * <p>
 * The improved runtime comes at the cost of distance measurements (between two random LatLong
 * locations) that changed by at most 1.8E-12 Nautical Miles in absolute distance and 2.6E-16 in
 * relative error. The average change in distance was 9.1E-14. In other words, using FastMath is
 * 2.23x faster than using java.lang.Math and has near-zero effect on results.
 */
public class Spherical {

    public static final Double EARTH_RADIUS_NM = 3438.14021579022;

    public static final Double METERS_PER_NM = 1852.0;

    public static final Double METERS_PER_FOOT = 0.3048;

    public static Double oneQuarterCircumferenceOfEarthInNM() {
        return EARTH_RADIUS_NM * PI / 2;
    }

    /** @return The number of feet per nautical mile (6076.1154855643044619422572178478). */
    public static Double feetPerNM() {
        return METERS_PER_NM / METERS_PER_FOOT;
    }

    /**
     * Compute the great circle spherical distance between two locations.
     *
     * @param latDeg1 latitude in degrees (of point 1)
     * @param lonDeg1 longitude in degrees (of point 1)
     * @param latDeg2 latitude in degrees (of point 2)
     * @param lonDeg2 longitude in degrees (of point 2)
     *
     * @return The spherical distance between two locations
     */
    public static Double distanceInNM(Double latDeg1, Double lonDeg1, Double latDeg2, Double lonDeg2) {
        double lat1 = toRadians(latDeg1);
        double lon1 = toRadians(lonDeg1);
        double lat2 = toRadians(latDeg2);
        double lon2 = toRadians(lonDeg2);
        return EARTH_RADIUS_NM * ahaversine(haversine(lat2 - lat1) + cos(lat1) * cos(lat2) * haversine(lon2 - lon1));
    }

    /** Compute the great circle Distance between two locations. */
    public static Distance distanceBtw(LatLong one, LatLong two) {
        return Distance.ofNauticalMiles(distanceInNM(
                one.latitude(), one.longitude(),
                two.latitude(), two.longitude()));
    }

    /** Compute the great circle Distance between two object with a location. */
    public static Distance distanceBtw(HasPosition hp1, HasPosition hp2) {
        return distanceBtw(hp1.latLong(), hp2.latLong());
    }

    /**
     * Compute the course between two locations.
     *
     * @param startLat  latitude in degrees of starting location
     * @param startLong longitude in degrees of starting location
     * @param endLat    latitude in degrees of starting location
     * @param endLong   longitude in degrees of starting location
     *
     * @return The course between two points
     */
    public static Double courseInDegrees(Double startLat, Double startLong, Double endLat, Double endLong) {
        double lat1 = toRadians(startLat);
        double lon1 = toRadians(startLong);
        double lat2 = toRadians(endLat);
        double lon2 = toRadians(endLong);
        double y = sin(lon1 - lon2) * cos(lat2);
        double x = (cos(lat1) * sin(lat2)) - (sin(lat1) * cos(lat2) * cos(lon2 - lon1));
        double crs = (2.0 * PI) - mod(atan2(y, x), (2.0 * PI));
        return toDegrees(crs);
    }

    /**
     * Compute the course between two points.
     *
     * @param startLat  latitude in degrees (of point 1)
     * @param startLong longitude in degrees (of point 1)
     * @param endLat    latitude in degrees (of point 2)
     * @param endLong   longitude in degrees (of point 2)
     *
     * @return The course between two points as a Course object
     */
    public static Course courseBtw(Double startLat, Double startLong, Double endLat, Double endLong) {
        // call the other method, but return a Course object instead
        return Course.ofDegrees(courseInDegrees(startLat, startLong, endLat, endLong));
    }

    /**
     * Compute the course between two location.
     *
     * @param start The start location
     * @param end   The end location
     *
     * @return The course between two location as a Course object
     */
    public static Course courseBtw(LatLong start, LatLong end) {
        return courseBtw(
                start.latitude(), start.longitude(),
                end.latitude(), end.longitude());
    }

    /**
     * Compute the course between two objects with a location.
     *
     * @param start The start location
     * @param end   The end location
     *
     * @return The course between two objects
     */
    public static Course courseBtw(HasPosition start, HasPosition end) {
        return courseBtw(start.latLong(), end.latLong());
    }

    /**
     * Compute a new LatLong location from an starting location, a direction, and a distance.
     *
     * @param latDeg        latitude in degrees
     * @param lonDeg        longitude in degrees
     * @param headingDegree The heading in degrees
     * @param distNM        The distance in nautical miles
     *
     * @return The destination point
     * @throws IllegalArgumentException if either headingDegree or distNM is NaN
     */
    public static LatLong projectOut(Double latDeg, Double lonDeg, Double headingDegree, Double distNM) {
        checkArgument(!Double.isNaN(headingDegree));
        checkArgument(!Double.isNaN(distNM));
        double lat = toRadians(latDeg);
        double lon = toRadians(lonDeg);
        double crs = toRadians(distNM < 0.0 ? mod(headingDegree + 180.0, 360.0) : headingDegree);
        double dist = abs(distNM) / EARTH_RADIUS_NM;

        double latProj = asinReal((cos(dist) * sin(lat)) + (sin(dist) * cos(lat) * cos(crs)));
        double dlon = acosReal((cos(dist) - (sin(latProj) * sin(lat))) / (cos(latProj) * cos(lat)));
        double lonProj;
        if (EARTH_RADIUS_NM * abs(cos(latProj)) < 0.01) {
            lonProj = lon; // north pole
        } else if (abs(dlon) < (PI / 4.0)) {
            // near field
            lonProj = lon + asin(sin(dist) * sin(crs) / cos(latProj));
        } else {
            // far field
            lonProj = lon + (crs < PI ? 1.0 : -1.0) * dlon;
        }
        latProj = toDegrees(latProj);
        lonProj = mod(toDegrees(lonProj) + 180.0, 360.0) - 180.0;
        return new LatLong(latProj, lonProj);
    }

    /**
     * Compute a new LatLong location from an starting location, a direction, and a distance.
     *
     * @param start     The starting location
     * @param direction The direction of travel
     * @param distance  The distance traveled
     *
     * @return The destination location
     */
    public static LatLong projectOut(LatLong start, Course direction, Distance distance) {
        return Spherical.projectOut(
                start.latitude(), start.longitude(), direction.inDegrees(), distance.inNauticalMiles());
    }

    /**
     * Compute a new LatLong location from an starting location, a direction, a distance, and a
     * curvature.
     *
     * @param start     The starting location
     * @param direction The direction of travel
     * @param distance  The distance traveled
     * @param curvature The curvature of the travel path
     *
     * @return The destination location
     */
    public static LatLong projectOut(LatLong start, Course direction, Distance distance, double curvature) {
        return projectOut(
                start.latitude(), start.longitude(), direction.inDegrees(), distance.inNauticalMiles(), curvature);
    }

    /**
     * Compute a new LatLong location from an starting location, a direction, a distance, and a
     * curvature.
     *
     * @param latDeg        latitude in degrees
     * @param lonDeg        longitude in degrees
     * @param headingDegree The heading in degrees
     * @param distNM        The distance in nautical miles
     * @param curvature     The curvature of the travel path
     *
     * @return The destination point
     */
    public static LatLong projectOut(
            Double latDeg, Double lonDeg, double headingDegree, double distNM, double curvature) {
        double rMax = EARTH_RADIUS_NM * Math.PI / 2.0;
        double radiusNM = Math.max(-rMax, Math.min(rMax, 1.0 / curvature));
        LatLong pair = Spherical.projectOut(latDeg, lonDeg, mod(headingDegree + 90.0, 360.0), radiusNM);
        double latCen = pair.latitude();
        double lonCen = pair.longitude();
        double crsDelta = Math.copySign(
                Math.toDegrees(distNM
                        / (EARTH_RADIUS_NM * Math.sin(Math.min(Math.PI / 2, Math.abs(radiusNM) / EARTH_RADIUS_NM)))),
                radiusNM);
        double crsCen = mod(courseInDegrees(latCen, lonCen, latDeg, lonDeg) + crsDelta, 360.0);
        return Spherical.projectOut(latCen, lonCen, crsCen, Math.abs(radiusNM));
    }

    /**
     * Compute the ‘center' of the great circle to the right of the course for that lat/long. For
     * example, a lat/long coordinate on the equator with a course heading due west will return the
     * north pole
     *
     * @param latitude  The latitude of a Point
     * @param longitude The longitude of a Point
     * @param course    The direction of travel (in degrees)
     *
     * @return The ‘center' of the great circle to the right of the course for that lat/long.
     */
    public static LatLong greatCircleOrigin(Double latitude, Double longitude, Double course) {
        return Spherical.projectOut(
                latitude, longitude, Spherical.mod(course + 90.0, 360.0), oneQuarterCircumferenceOfEarthInNM());
    }

    public static Double curvatureFromPointToPoint(
            Double latDeg1, Double lonDeg1, Double hdg1, Double latDeg2, Double lonDeg2) {
        double d = distanceInNM(latDeg1, lonDeg1, latDeg2, lonDeg2) / EARTH_RADIUS_NM;
        double dTheta = angleDifference(courseInDegrees(latDeg1, lonDeg1, latDeg2, lonDeg2), hdg1);
        double phi = toRadians(abs(dTheta));
        return 1.0 / Math.copySign(atan((1 - cos(d)) / (sin(d) * sin(phi))) * EARTH_RADIUS_NM, dTheta);
    }

    /**
     * Compute the length of an arc on the Earth's surface.
     *
     * @param radius  The radius (distance along the Earth's surface) of the arc
     * @param degrees The central angle of the arc
     *
     * @return The length of the arc in nautical miles
     */
    public static Double arcLength(Double radius, Double degrees) {
        return toRadians(degrees) * EARTH_RADIUS_NM * sin(radius / EARTH_RADIUS_NM);
    }

    private static double haversine(double x) {
        return (1.0 - cos(x)) / 2.0;
    }

    private static double ahaversine(double x) {
        return 2.0 * asin(sqrt(x));
    }

    public static double mod(double x, double y) {
        double z = x % y;
        if (z < 0) {
            z += y;
        }
        return z;
    }

    private static double asinReal(double x) {
        return asin(max(-1.0, min(1.0, x)));
    }

    private static double acosReal(double x) {
        return acos(max(-1.0, min(1.0, x)));
    }

    public static Double distanceInNM(LatLong pt1, LatLong pt2) {
        return distanceInNM(pt1.latitude(), pt1.longitude(), pt2.latitude(), pt2.longitude());
    }

    public static Double courseInDegrees(LatLong pt1, LatLong pt2) {
        return courseInDegrees(pt1.latitude(), pt1.longitude(), pt2.latitude(), pt2.longitude());
    }

    public static Double angleDifference(Double hdg, Double hdg0) {
        return angleDifference(hdg - hdg0);
    }

    public static Double angleDifference(Double dz) {
        if (dz > 180.0) {
            return dz - 360.0;
        } else if (dz < -180.0) {
            return dz + 360.0;
        } else {
            return dz;
        }
    }

    /**
     * Convert a distance, in nautical miles, to the corresponding amount of "great circle radians"
     *
     * @param nauticalMiles An amount of nautical miles
     *
     * @return An amount of "great circle radians"
     */
    public static double distanceInRadians(double nauticalMiles) {
        return (PI / (180 * 60)) * nauticalMiles;
    }

    /**
     * Convert an amount of "great circle radians" to the corresponding amount of nautical miles.
     *
     * @param radians An amount of "great circle radians"
     *
     * @return An amount of nautical miles
     */
    public static double distanceInNM(double radians) {
        return ((180 * 60) / PI) * radians;
    }

    /**
     * Computes the spherical cross track distance (in NM) between a line segment on the sphere with
     * start point, end point, and an arbitrary location.
     *
     * @param startPoint The beginning location
     * @param endPoint   The ending location
     * @param location   A 3rd location that is usually "near the path" from startPoint to endPoint
     *
     * @return The distance (in NM) from the "great circle center line" of travel.
     */
    public static double crossTrackDistanceNM(HasPosition startPoint, HasPosition endPoint, HasPosition location) {
        double dist = startPoint.distanceInRadians(location);
        double angle =
                toRadians(startPoint.courseInDegrees(location)) - toRadians(startPoint.courseInDegrees(endPoint));
        return distanceInNM(asin(sin(dist) * sin(angle)));
    }

    // Tolerances used in the "alongTrackDistanceNM" computation to avoid failing due to numeric error.
    private static final Double TOLERANCE = 1E-10;
    private static final Range<Double> NUMERICALLY_TOLERANT_RANGE = Range.closed(-1 - TOLERANCE, 1 + TOLERANCE);

    /**
     * Computes the distance along the track (in NM) from startPoint to endPoint and the point p
     * using a provided cross track distance instead of re-calculating it internally.
     *
     * <p>Note the along track distance will be negative if the point is prior to the start point
     * of the segment and greater than the length of the segment if the point is past the end of the
     * segment.
     *
     * <p>If CTD is invalid (i.e. it is not the correct cross track distance for the {startPoint,
     * endPoint, and p} then it may return NaN.
     *
     * @param startPoint The beginning point of a line segment
     * @param endPoint   The ending point of a line segment
     * @param p          An arbitrary 3rd point (usually close to the line segment)
     * @param CTD        The "cross track distance" of p (computed via the sister method
     *                   crossTrackDistanceNM)
     */
    public static double alongTrackDistanceNM(HasPosition startPoint, HasPosition endPoint, HasPosition p, double CTD) {
        double relAng = Spherical.angleDifference(startPoint.courseInDegrees(endPoint), startPoint.courseInDegrees(p));
        double sign = (abs(relAng) > 90.0 ? -1.0 : 1.0);
        double cosCTD = cos(distanceInRadians(CTD));
        double cosPTD = cos(startPoint.distanceInRadians(p));

        /*
         * In rare cases this ratio can exceed 1 due to numeric error.  For example, we've observed
         * cases (see unit tests) where ratio = 1.0000000000000002
         */
        double ratio = cosPTD / cosCTD;

        if (!NUMERICALLY_TOLERANT_RANGE.contains(ratio)) {
            throw new IllegalStateException("Cannot compute acos of: " + ratio);
        }

        // clamp ratio down from the "tolerant range" to the "valid input to acos range"
        ratio = Math.min(Math.max(ratio, -1.0), 1.0);
        // @todo: Replace line with `ratio = constrainToRange(ratio, -1.0, 1.0);` when Guava is shaded from exported
        // library

        return sign * distanceInNM(acos(ratio));
    }

    /**
     * Computes the distance along the track (in NM) between ls and le the point p computing the CTD
     * internally.
     */
    public static double alongTrackDistanceNM(HasPosition ls, HasPosition le, HasPosition p) {
        double CTD = crossTrackDistanceNM(ls, le, p);
        return alongTrackDistanceNM(ls, le, p, abs(CTD));
    }
}
