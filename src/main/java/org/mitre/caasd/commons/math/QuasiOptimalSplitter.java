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
import static org.mitre.caasd.commons.math.DataSplitter.checkInputData;

import java.util.List;
import java.util.TreeSet;

import com.google.common.primitives.Ints;

/**
 * A QuasiOptimalSplitter repeatedly splits a Dataset at the "best" place. At any given iteration of
 * this repeating process the "best" place to split a dataset is where the combined errors from the
 * applying a linear fit to the left and right sides of split is minimized. The algorithm terminates
 * when the error of each split is as small as the user requires.
 * <p>
 * This approach <B>does not</B> usually produce the best possible "splits" for a dataset. Adding 1
 * split N times does not generally produce the "best N splits" because this problem is not easily
 * solved using a greedy algorithm see (https://en.wikipedia.org/wiki/Greedy_algorithm) for the
 * pitfalls of a greedy approach.
 */
public class QuasiOptimalSplitter implements DataSplitter {

    // this index value means the dataset should not be split
    private static final int NO_MORE_SPLITS = -1;

    /**
     * The TARGET for the average squared error in each "chunk" of the dataset. This is a target an
     * not a guarantee because not all datasets can be chopped (for example 3 points that do not
     * form a line will not be split)
     */
    private final double targetAverageSquaredError;

    /**
     * Create a QuasiOptimalSplitter that continues splitting data until the average interpolation
     * error across EACH split is below this average
     *
     * @param targetAverageSquaredError The TARGET for the average squared error in each "chunk" of
     *                                  dataset that is split. This is a target an not a guarantee
     *                                  because not all datasets can be chopped (for example 3
     *                                  points that do not form a line cannot be split)
     */
    public QuasiOptimalSplitter(double targetAverageSquaredError) {
        this.targetAverageSquaredError = targetAverageSquaredError;
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

    private void recursivelySplitDataset(
            List<Double> xData, List<Double> yData, int minIndex, int maxIndex, TreeSet<Integer> chops) {

        int splitIndex = determineSplitFor(xData, yData, minIndex, maxIndex);

        // end the recursion
        if (splitIndex == NO_MORE_SPLITS) {
            return;
        }

        // save the chop, and recurse into the left and right side
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

        if (maxIndex - minIndex < 4) {
            return NO_MORE_SPLITS;
        }
        double minErrorSoFar = Double.POSITIVE_INFINITY;
        int indexOfMinError = -1;

        // minIndex + 1 because the earliest "chop" has to leave at least 2 values for the left side (and chops have
        // AFTER the index)
        for (int i = minIndex + 1; i < maxIndex - 1; i++) {
            FastLinearApproximation left =
                    new FastLinearApproximation(xData.subList(minIndex, i), yData.subList(minIndex, i));
            FastLinearApproximation right =
                    new FastLinearApproximation(xData.subList(i, maxIndex), yData.subList(i, maxIndex));

            double thisError = left.totalSquaredError() + right.totalSquaredError();
            if (thisError < minErrorSoFar) {
                indexOfMinError = i;
                minErrorSoFar = thisError;
            }
        }

        double n = maxIndex - minIndex;
        double averageError = minErrorSoFar / n;

        return averageError > targetAverageSquaredError
                ? indexOfMinError + 1 // split AFTER the best index
                : NO_MORE_SPLITS;
    }
}
