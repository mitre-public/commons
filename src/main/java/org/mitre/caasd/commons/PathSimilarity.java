package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.Spherical.angleDifference;

/**
 * This class has methods that can be the "foundation" of {@code DistanceMetric}
 * implementations that help sort and search regularly sized LatLongPaths.
 * <p>
 * The "general idea" is to use a LocalPolyInterpolator to smooth and sample object position data at
 * regular time intervals. This process converts a datafeed with irregular timing to a datafeed that
 * has a "fixed frequency" and can be chopped into snippets and searched.
 */
@Deprecated
public class PathSimilarity {

    /**
     * Compute an accurate, but computationally taxing, "similarity score" between these two Paths.
     * <p>
     * The similarity computed here is the NEGATIVE sum of the distance between each "LatLong pair"
     * taken from the paths (e.g. the distance btw the 1st LatLong from both paths PLUS the distance
     * btw the 2nd LatLong from both paths PLUS the distance btw the 3rd LatLong from both paths
     * ...).
     * <p>
     * The similarity between two identical paths will be 0.  The similarity between two nearly
     * identical paths will be a small negative number.  The similarity between two very different
     * paths will be a large negative number.
     * <p>
     * The computation requires both Paths to have the same size.  This is an important requirement
     * for making a DistanceMetric using this method.
     *
     * @param p1 A path
     * @param p2 Another path
     *
     * @return A similarity score (a negative number unless the paths are identical). Maximizing a
     *     similarity score finds the Most similar paths.
     */
    public static double slowSimilarity(LatLongPath p1, LatLongPath p2) {
        // ACCURATE BUT SLOW

        requireNonNull(p1);
        requireNonNull(p2);
        checkArgument(p1.size() == p2.size(), "Paths must have same size");
        double distanceSum = 0;
        int n = p1.size();
        for (int i = 0; i < n; i += 1) {
            LatLong mine = p1.get(i);
            LatLong his = p2.get(i);
            distanceSum += mine.distanceInNM(his);
        }

        // return the
        return 0 - distanceSum;
    }

    /**
     * Compute an approximate, and easily computed, "similarity score" between these two Paths.
     * <p>
     * The similarity computed here is optimized for fast computation.  It is maximally accurate
     * when paths are similar. This similarity score does NOT use expensive floating point
     * trigonometric operations because we believe accuracy is highest we care the most (when paths
     * are similar) and accuracy is lowest when we care the least (when paths are obviously
     * dissimilar).
     * <p>
     * The similarity score computed here is the NEGATIVE ("integral of latitude differences" plus
     * the "integral of the longitude differences").
     * <p>
     * The similarity between two identical paths will be 0.  The similarity between two nearly
     * identical paths will be a small negative number.  The similarity between two very different
     * paths will be a large negative number.
     * <p>
     * The computation requires both Paths to have the same size.  This is an important requirement
     * for making a DistanceMetric using this method.
     *
     * @param p1 A path
     * @param p2 Another path
     *
     * @return A similarity score (a negative number unless the paths are identical). Maximizing a
     *     similarity score finds the Most similar paths.
     */
    public static double similarity(LatLongPath p1, LatLongPath p2) {
        // USUALLY GOOD ENOUGH FOR PICKING THE "MOST SIMILAR PATH" BECAUSE ACCURACY IMPROVES AS PATHS GET MORE AND MORE
        // SIMILAR

        requireNonNull(p1);
        requireNonNull(p2);
        checkArgument(p1.size() == p2.size());

        double sumOfLatitudeDeltas = latitudeDist(p1.latitudes(), p2.latitudes());
        double sumOfLongitudeDeltas = longitudeDist(p1.longitudes(), p2.longitudes());

        return -sumOfLatitudeDeltas - sumOfLongitudeDeltas;
    }

    /** Sum the "deltas" for each pair of latitude values. */
    static double latitudeDist(double[] lats1, double[] lats2) {

        double totalDelta = 0;

        for (int i = 0; i < lats1.length; i++) {
            totalDelta += latitudeDelta(lats1[i], lats2[i]);
        }

        return totalDelta;
    }

    /** Sum the "deltas" for each pair of longitude values. */
    static double longitudeDist(double[] longs1, double[] longs2) {

        double totalDelta = 0;
        int n = longs1.length;

        for (int i = 0; i < n; i++) {
            totalDelta += longitudeDelta(longs1[i], longs2[i]);
        }

        return totalDelta;
    }

    /** Computes the "delta in degrees" between two valid latitudes (always positive). */
    static double latitudeDelta(double latitude1, double latitude2) {
        return abs(latitude1 - latitude2);
    }

    /** Computes the "delta in degrees" between two valid longitudes (always positive). */
    static double longitudeDelta(double longitude1, double longitude2) {
        // be careful with longitude -- the international date line is a problem
        return abs(angleDifference(longitude1 - longitude2));
    }
}
