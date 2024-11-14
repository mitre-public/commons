package org.mitre.caasd.commons;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.math3.util.FastMath.*;

import org.apache.commons.math3.util.FastMath;

/**
 * A collection of methods useful in Rhumb Line calculations.
 *
 * In navigation, a rhumb line, or loxodrome is an arc crossing all meridians of longitude at the
 * same angle, that is, a path with constant azimuth (bearing as measured relative to true north).
 * Navigation on a fixed course (i.e., steering the vessel to follow a constant cardinal direction)
 * would result in a rhumb-line track.
 *
 * Here are some resources to learn more about Rhumb Lines:
 * - https://astronavigationdemystified.com/the-rhumb-line/
 * - https://en.wikipedia.org/wiki/Rhumb_line
 */
public final class Rhumb {

    private Rhumb() {}

    /**
     * This finds the distance along the rhumb line
     *
     * @param lat1 lat1 in radians
     * @param lon1 long1 in radians
     * @param lat2 lat2 in radians
     * @param lon2 long2 in radians
     *
     * @return the rhumb distance in radians
     */
    public static double rhumbDistance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) return 0.0;

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double dPhi = log(tan(lat2 / 2.0 + Math.PI / 4.0) / tan(lat1 / 2.0 + Math.PI / 4.0));
        double q = dLat / dPhi;
        if (Double.isNaN(dPhi) || Double.isNaN(q)) {
            q = FastMath.cos(lat1);
        }

        if (FastMath.abs(dLon) > Math.PI) {
            dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        }

        double distanceRadians = sqrt(dLat * dLat + q * q * dLon * dLon);

        return Double.isNaN(distanceRadians) ? 0.0 : distanceRadians;
    }

    /**
     * This finds the distance along the run path between two latlongs
     *
     * @param p1 point one
     * @param p2 point two
     *
     * @return the distance in radians
     */
    public static double rhumbDistance(LatLong p1, LatLong p2) {
        requireNonNull(p1);
        requireNonNull(p2);
        return rhumbDistance(
                toRadians(p1.latitude()), toRadians(p1.longitude()),
                toRadians(p2.latitude()), toRadians(p2.longitude()));
    }

    /**
     * This method finds a rhumb azimuth.
     *
     * @param lat1 in radians
     * @param lon1 in radians
     * @param lat2 in radians
     * @param lon2 in radians
     *
     * @return the azimuth in radians
     */
    public static double rhumbAzimuth(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) return 0;

        double dLon = lon2 - lon1;
        double dPhi = log(tan(lat2 / 2.0 + Math.PI / 4.0) / tan(lat1 / 2.0 + Math.PI / 4.0));
        // If lonChange over 180 take shorter rhumb across 180 meridian.
        if (FastMath.abs(dLon) > Math.PI) {
            dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        }
        double azimuthRadians = FastMath.atan2(dLon, dPhi);

        return Double.isNaN(azimuthRadians) ? 0 : azimuthRadians;
    }

    /**
     * This method finds a rhumb azimuth.
     *
     * @param p1 the commons latlong of the first point
     * @param p2 the commons latlong of hte second point
     * @return the azimuth in radians
     */
    public static double rhumbAzimuth(LatLong p1, LatLong p2) {
        requireNonNull(p1);
        requireNonNull(p2);

        return rhumbAzimuth(
                toRadians(p1.latitude()),
                toRadians(p1.longitude()),
                toRadians(p2.latitude()),
                toRadians(p2.longitude()));
    }

    /**
     * Finds a projection on a rhumb line from another point
     *
     * @param lat1 the latitude in radians
     * @param lon1 the longitude in radians
     * @param rhumbAzimuth the azimuth in radians
     * @param pathLength the length in radians
     *
     * @return the position of the projection
     */
    public static LatLong rhumbEndPosition(double lat1, double lon1, double rhumbAzimuth, double pathLength) {
        if (pathLength == 0) return LatLong.of(lat1, lon1);

        double lat2 = lat1 + pathLength * FastMath.cos(rhumbAzimuth);
        double dPhi = log(tan(lat2 / 2.0 + Math.PI / 4.0) / tan(lat1 / 2.0 + Math.PI / 4.0));
        double q = (lat2 - lat1) / dPhi;
        if (Double.isNaN(dPhi) || Double.isNaN(q) || Double.isInfinite(q)) {
            q = FastMath.cos(lat1);
        }
        double dLon = pathLength * FastMath.sin(rhumbAzimuth) / q;

        if (FastMath.abs(lat2) > Math.PI / 2.0) {
            lat2 = lat2 > 0 ? Math.PI - lat2 : -Math.PI - lat2;
        }
        double lon2 = (lon1 + dLon + Math.PI) % (2 * Math.PI) - Math.PI;

        if (Double.isNaN(lat2) || Double.isNaN(lon2)) return LatLong.of(lat1, lon1);

        return LatLong.of(normalizedDegreesLatitude(toDegrees(lat2)), normalizedDegreesLongitude(toDegrees(lon2)));
    }

    /**
     * Finds a projection on a rhumb line from another point
     *
     * @param p the commons object
     * @param rhumbAzimuth the azimuth in radians
     * @param pathLength the path length in radians.
     *
     * @return the position in commons objects.
     */
    public static LatLong rhumbEndPosition(LatLong p, double rhumbAzimuth, double pathLength) {
        requireNonNull(p);
        return rhumbEndPosition(toRadians(p.latitude()), toRadians(p.longitude()), rhumbAzimuth, pathLength);
    }

    private static double normalizedDegreesLatitude(double degrees) {
        double lat = degrees % 180;
        return lat > 90 ? 180 - lat : lat < -90 ? -180 - lat : lat;
    }

    private static double normalizedDegreesLongitude(double degrees) {
        double lon = degrees % 360;
        return lon > 180 ? lon - 360 : lon < -180 ? 360 + lon : lon;
    }
}
