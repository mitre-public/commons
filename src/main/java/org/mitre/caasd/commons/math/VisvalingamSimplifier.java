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

package org.mitre.caasd.commons.math;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.abs;
import static org.apache.commons.math3.util.FastMath.hypot;
import static org.mitre.caasd.commons.collect.HashedLinkedSequence.newHashedLinkedSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;

import org.mitre.caasd.commons.collect.HashedLinkedSequence;

import com.google.common.primitives.Doubles;

/**
 * A VisvalingamSimplifier removes "visually unimportant" data from a Dataset by applying
 * Visvalingam’s algorithm.
 * <p>
 * Visvalingam's algorithm repeatedly removes points from a 2-dimensional data set until each point
 * remaining has a "big visual impact" on the curve that is drawn through all the remain data
 * points. The "visual impact" of any particular point in the curve is the area of the triangle
 * drawn between the point itself as well as its left and right neighbors. A point that is near the
 * line drawn between its two neighbors has a small visual impact on the curve because removing this
 * point doesn't change the curve much. (Notice, this occurs when a triangle drawn between these 3
 * consecutive points in the curve has a near-zero height).
 * <p>
 * The Visvalingam algorithm repeatedly removes points in the data that are at the center of a "low
 * area" triangle.
 */
public class VisvalingamSimplifier {

    /**
     * Apply Visvalingam's Algorithm to iteratively remove points from a dataset until any
     * consecutive sequence of 3 data points plots a triangle with at least the specified area.
     *
     * @param dataset          A Dataset
     * @param removalThreshold The area of a "data point triangle" required to retain the center
     *                         point in the triangle.
     *
     * @return A simplified dataset. This dataset will always contain the first and last points from
     *     the input dataset.
     */
    public XyDataset simplify(XyDataset dataset, double removalThreshold) {
        checkNotNull(dataset);
        checkArgument(removalThreshold >= 0);

        ArrayList<XyPoint> points = dataset.asXyPointList();

        //step 1 -- pipe all the x-y  data to a serachable linked dataset
        HashedLinkedSequence<XyPoint> hashedSequence = newHashedLinkedSequence(points);

        //step 2 -- Create initial triangles
        PriorityQueue<Triangle> triangleQueue = initalizeTriangles(points);

        //step 3 -- repeatedly remove the centerPoint from triangle's with small areas
        while (!triangleQueue.isEmpty() && triangleQueue.peek().area < removalThreshold) {

            Triangle lowAreaTriangle = triangleQueue.poll();

            //do nothing if the lowAreaTriangle is invalid because one of its points has already been removed
            if (!hashedSequence.containsAll(lowAreaTriangle.points())) {
                continue;
            }

            XyPoint pointToRemove = lowAreaTriangle.center;
            XyPoint left = hashedSequence.getElementBefore(pointToRemove); //always non-null
            XyPoint right = hashedSequence.getElementAfter(pointToRemove); //always non-null

            XyPoint leftOfLeft = (left == hashedSequence.getFirst())
                ? null
                : hashedSequence.getElementBefore(left);

            XyPoint rightOfRight = (right == hashedSequence.getLast())
                ? null
                : hashedSequence.getElementAfter(right);

            hashedSequence.remove(lowAreaTriangle.center);

            //add the new leftside Triangle
            if (leftOfLeft != null) {
                triangleQueue.add(new Triangle(leftOfLeft, left, right));
            } else {
                //We just removed Point B from ABCDEFGHIJKL..., so there is no new leftside Triangle
            }

            //add the new rightside Triangle
            if (rightOfRight != null) {
                triangleQueue.add(new Triangle(left, right, rightOfRight));
            } else {
                //We just removed Point Y from ...NOPQRSTUVWXYZ, so there is no new rightside Triangle
            }
        }

        return XyPoint.asDataset(hashedSequence);
    }

    /* Create all initial "triangles" from ALL the XyPoint data. */
    private PriorityQueue<Triangle> initalizeTriangles(ArrayList<XyPoint> points) {
        PriorityQueue<Triangle> triangleQueue = new PriorityQueue<>();
        for (int i = 1; i < points.size() - 1; i++) {
            Triangle tri = new Triangle(
                points.get(i - 1),
                points.get(i),
                points.get(i + 1)
            );
            triangleQueue.add(tri);
        }
        return triangleQueue;
    }

    private static class Triangle implements Comparable<Triangle> {

        XyPoint left;
        XyPoint center;
        XyPoint right;

        double area;

        Triangle(XyPoint left, XyPoint center, XyPoint right) {
            this.left = checkNotNull(left);
            this.center = checkNotNull(center);
            this.right = checkNotNull(right);
            this.area = computeTriangleArea(left, center, right);
        }

        @Override
        public int compareTo(Triangle other) {
            return Doubles.compare(area, other.area);
        }

        public Collection<XyPoint> points() {
            return newArrayList(left, center, right);
        }

        @Override
        public String toString() {
            return left.toString() + " to " + center.toString() + " to " + right.toString() + "\n area: " + area;
        }
    }

    /**
     * @param left   The left most point
     * @param center A point that falls between the left and right points
     * @param right  The right most point
     *
     * @return The area of a triangle between these three points.
     */
    public static double computeTriangleArea(XyPoint left, XyPoint center, XyPoint right) {
        checkArgument(left.x < center.x);
        checkArgument(center.x < right.x);

        double width = right.x - left.x;
        double rise = right.y - left.y;
        double slope = rise / width;
        double predictedCenterY = slope * (center.x - left.x) + left.y;

        //spacing between a perfect straight line from left to right and the actual center xyPoint
        double error = abs(center.y - predictedCenterY);

        return .5 * error * width; // .5 because these are triangles
    }

    /**
     * @param left   The left most point
     * @param center A point that falls between the left and right points (along the x dimension)
     * @param right  The right most point
     *
     * @return The area of a triangle between these three points.
     */
    public static double computeTriangleArea(XyzPoint left, XyzPoint center, XyzPoint right) {
        checkArgument(left.x < center.x);
        checkArgument(center.x < right.x);

        double width = right.x - left.x;
        double yDelta = right.y - left.y;
        double ySlope = yDelta / width;

        double predictedCenterY = ySlope * (center.x - left.x) + left.y;

        double zDelta = right.z - left.z;
        double zSlope = zDelta / width;
        double predictedCenterZ = zSlope * (center.x - left.x) + left.z;

        //spacing between a perfect straight line from left to right and the actual center xyzPoint
        double error = hypot(center.y - predictedCenterY, center.z - predictedCenterZ);

        return .5 * error * width; // .5 because these are triangles
    }

}
