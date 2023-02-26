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
import static com.google.common.collect.Sets.newTreeSet;
import static java.lang.Math.abs;
import static org.mitre.caasd.commons.math.DataSplitter.checkInputData;

import java.util.List;
import java.util.TreeSet;

import com.google.common.primitives.Ints;

/**
 * Use the Ramer–Douglas–Peucker algorithm to chop a dataset into approximately linear pieces.
 * <p>
 * This is a recursive algorithm <B>which assumes</B> the data forms an approximate straight line
 * from the first data point to the last data point. At each step in the recursion the algorithm
 * finds the point in the (working) dataset that is "least in line". If the error associated with
 * this data point is too big the algorithm chops the dataset in two. The same algorithm is then
 * applied to each of the "sub-datasets". Eventually, the initial dataset will be split into a few
 * subsequences in which each subsequence is roughly linear.
 */
public class PiecewiseLinearSplitter implements DataSplitter {

    //this index value means the dataset should not be split
    private static final int NO_MORE_SPLITS = -1;

    private final double maxFinalError;

    public PiecewiseLinearSplitter(double maxError) {
        this.maxFinalError = maxError;
    }

    /**
     * @param xData A strictly increase sequence of numbers
     * @param yData Exactly one y value for each piece of xData
     *
     * @return The index values for that can be used to split the input lists into "subLists" of
     *     approximately linear data.
     */
    @Override
    public int[] computeSplitsFor(List<Double> xData, List<Double> yData) {
        checkInputData(xData, yData);

        int n = xData.size();

        /*
         * Create an ordered sequence of index values where any two consecutive values represent the
         * beginning and end of a "subList" of linear data.
         */
        TreeSet<Integer> boundaryIndexValues = newTreeSet();
        boundaryIndexValues.add(0);
        boundaryIndexValues.add(n);

        recursivelySplitDataset(xData, yData, 0, n, boundaryIndexValues);

        return Ints.toArray(boundaryIndexValues);
    }

    /**
     * @param xData    The ENTIRE x dataset
     * @param yData    The ENTIRE y dataset
     * @param minIndex The minimum index this computation considers (inclusive)
     * @param maxIndex The maximum index this computation considers (exclusive)
     * @param chops    The set of all "data set chops" found so far (i.e a Set of list index
     *                 values)
     */
    private void recursivelySplitDataset(List<Double> xData, List<Double> yData, int minIndex, int maxIndex, TreeSet<Integer> chops) {

        int splitIndex = determineSplitFor(xData, yData, minIndex, maxIndex);

        //end the recursion
        if (splitIndex == NO_MORE_SPLITS) {
            return;
        }

        //save the chop, and recurse into the left and right side
        chops.add(splitIndex);
        recursivelySplitDataset(xData, yData, minIndex, splitIndex, chops);
        recursivelySplitDataset(xData, yData, splitIndex, maxIndex, chops);

    }

    /**
     * Find the index of the data point that least fits the assumption that the data forms a perfect
     * line from minIndex to maxIndex. This is the index for which "abs(true Y value -
     * predictedYValue)" is maximized.
     *
     * @param xData    The ENTIRE x dataset
     * @param yData    The ENTIRE y dataset
     * @param minIndex The minimum index this computation considers (inclusive)
     * @param maxIndex The maximum index this computation considers (exclusive)
     *
     * @return The index which represents the FIRST index of right side of the split -- OR --
     *     NO_MORE_SPLITS if the sublist between the two index values should not be split.
     */
    private int determineSplitFor(List<Double> xData, List<Double> yData, int minIndex, int maxIndex) {
        checkArgument(minIndex < maxIndex);

        //need at least 4 to split (2 values for left side and 2 values for right side)
        if (maxIndex - minIndex < 4) {
            return NO_MORE_SPLITS;
        }

        final double x0 = xData.get(minIndex);
        final double y0 = yData.get(minIndex);

        final double xN = xData.get(maxIndex - 1);
        final double yN = yData.get(maxIndex - 1);

        final double slope = (yN - y0) / (xN - x0);

        double maxError = Double.NEGATIVE_INFINITY;
        int indexOfMaxError = -1;

        //minIndex + 1 because the earliest "chop" has to leave at least 2 values for the left side (and chops have AFTER the index)
        for (int i = minIndex + 1; i < maxIndex - 2; i++) {
            double prediction = y0 + (xData.get(i) - x0) * slope;
            double actualY = yData.get(i);

            double error = abs(prediction - actualY);

            if (error > maxError) {
                indexOfMaxError = i;
                maxError = error;
            }
        }

        return maxError > maxFinalError
            ? indexOfMaxError + 1 //split AFTER the maxError
            : NO_MORE_SPLITS;
    }
}
