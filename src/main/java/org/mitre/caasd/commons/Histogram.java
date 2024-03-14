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

import static com.google.common.base.Preconditions.*;
import static com.google.common.math.DoubleMath.fuzzyEquals;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.google.common.primitives.Doubles;

/**
 * This Histogram class is built to support two main use cases. The first use case is to summerize a
 * dataset (i.e. a collection of numbers). The second use case to enable rigorously measuring the
 * "distance" between two datasets (that have both been summerized with a Histogram defined over the
 * same region).
 * <p>
 * This class has a Builder to enable literate coding.
 * <p>
 * For example,
 * <pre>{@code
 * //Create a Histogram from an arbitrary data type
 * List<MyDataType> listOfData = getMyData();
 * Function<MyDataType, Double> dataScoringFunction = getAScoringLambda();
 *
 * Histogram hist = Histogram.builder()
 *   .min(0.0)
 *   .max(100.0)
 *   .numColumn(20)
 *   .fromCollection(listOfData, dataScoringFunction)
 *   .build();
 * }</pre>
 */
public class Histogram {

    private final double min;
    private final double max;
    private final int numColumns;
    private final int[] counts;
    private final double[] percentages;

    /**
     * Create a Histogram from an array of numeric data.
     *
     * @param min        The "minimum" value. Data below the min is not lost, it is assigned to the
     *                   first column's count. This value is used in conjunction with the max value
     *                   to locate, and size, the columns.
     * @param max        The "maximum" value. Data above the max is not lost, it is assigned to the
     *                   last column's count. This value is used in conjunction with the min value
     *                   to locate, and size, the columns.
     * @param numColumns The number of columns this Histogram will use to summerize the data (must
     *                   be at least 1). Each column will
     * @param data       An array of numeric data.
     */
    Histogram(double min, double max, int numColumns, double[] data) {
        checkArgument(max > min);
        checkArgument(numColumns > 0);
        this.min = min;
        this.max = max;
        this.numColumns = numColumns;
        this.counts = createCountsArray(data);
        this.percentages = computePercentages(counts);
    }

    private int[] createCountsArray(double[] dataInput) {
        int[] columnCounts = new int[numColumns];

        for (double score : dataInput) {
            double fraction = (score - min) / (max - min);
            int bucket = (int) (numColumns * fraction);
            bucket = Math.min(numColumns - 1, bucket); // correct data that is above the max
            bucket = Math.max(0, bucket); // correct data that is below the min
            columnCounts[bucket]++;
        }

        return columnCounts;
    }

    private double[] computePercentages(int[] counts) {

        double totalCount = IntStream.of(counts).sum();

        double[] fractionsOfTotal = new double[numColumns];
        for (int i = 0; i < numColumns; i++) {
            fractionsOfTotal[i] = ((double) counts[i]) / totalCount;
        }

        return fractionsOfTotal;
    }

    /**
     * Compute the Earth Mover Distance between these two Histograms. For this comparison to be
     * valid both Histograms must have the same min, max, and number of columns.
     *
     * @param other Another Histogram
     *
     * @return The distance between these two Histograms.
     */
    public double earthMoverDistanceTo(Histogram other) {
        checkNotNull(other);
        checkArgument(fuzzyEquals(other.min, this.min, 0.000001));
        checkArgument(fuzzyEquals(other.max, this.max, 0.000001));
        checkArgument(other.numColumns == this.numColumns);

        return earthMoverDistanceBetween(this.percentages, other.percentages);
    }

    private static double earthMoverDistanceBetween(double[] percentages1, double[] percentages2) {
        double totalEarthMoved = 0;
        double currentExcessOrDearth = 0;
        int numColumns = percentages1.length;

        for (int i = 0; i < numColumns; i++) {
            currentExcessOrDearth += percentages1[i] - percentages2[i];
            totalEarthMoved += Math.abs(currentExcessOrDearth);
        }
        // use remaining excessOrDeath to check if the input arrays had equal sums
        checkArgument(
                fuzzyEquals(currentExcessOrDearth, 0.0, 0.0005),
                "FAILURE: Earth Mover Distance does not work when the input arrays have different sums, "
                        + "the difference was: " + currentExcessOrDearth);

        return totalEarthMoved;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public int numColumns() {
        return numColumns;
    }

    public double columnWidth() {
        return (max - min) / ((double) numColumns);
    }

    public String toString(int digitsAfterDecimalPlace) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numColumns; i++) {
            sb.append(describeColumn(i, digitsAfterDecimalPlace) + "\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(3);
    }

    /**
     * @param i                       The index of a column in this histogram
     * @param digitsAfterDecimalPlace How many significant figures are using when writing the floor
     *                                and ceiling of a histogram bin.
     *
     * @return A String like "[5.5-10.2] : 22" which contains the floor and ceiling of a particular
     *     column in the histogram as well as the count for that column.
     */
    public String describeColumn(int i, int digitsAfterDecimalPlace) {
        checkIndexBounds(i);

        String minSide = (i == 0)
                ? format("-[%." + (digitsAfterDecimalPlace) + "f", floorOfColumn(i))
                : format("[%." + (digitsAfterDecimalPlace) + "f", floorOfColumn(i));

        String maxSide = (i == numColumns - 1)
                ? format("%." + (digitsAfterDecimalPlace) + "f]+", ceilingOfColumn(i))
                : format("%." + (digitsAfterDecimalPlace) + "f]", ceilingOfColumn(i));

        String count = " : " + counts[i];

        return new StringBuilder()
                .append(minSide)
                .append("-")
                .append(maxSide)
                .append(count)
                .toString();
    }

    public double floorOfColumn(int i) {
        checkIndexBounds(i);
        return i * columnWidth() + min;
    }

    public double ceilingOfColumn(int i) {
        checkIndexBounds(i);
        return (i + 1) * columnWidth() + min;
    }

    private void checkIndexBounds(int i) {
        checkArgument(i >= 0);
        checkArgument(i < numColumns);
    }

    public int[] counts() {
        return counts;
    }

    public double[] percentages() {
        return percentages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Double minimum;
        private Double maximum;
        private Integer numColumns;
        private double[] data;

        public Builder min(double min) {
            checkState(isNull(minimum), "Minimum was already set");
            this.minimum = min;
            return this;
        }

        public Builder max(double max) {
            checkState(isNull(maximum), "Maximum was already set");
            this.maximum = max;
            return this;
        }

        public Builder numColumns(int numColumns) {
            checkState(isNull(this.numColumns), "Number of columns were already set");
            this.numColumns = numColumns;
            return this;
        }

        public Builder fromRawData(double[] data) {
            checkState(isNull(this.data), "Data import was already set");
            this.data = data;
            return this;
        }

        public <T> Builder fromCollection(Collection<T> data, Function<T, Double> dataScorer) {
            checkState(isNull(this.data), "Data import was already set");

            List<Double> scoredData =
                    data.stream().map(dataScorer).filter(Objects::nonNull).collect(toList());

            this.data = Doubles.toArray(scoredData);

            return this;
        }

        public Histogram build() {
            checkState(nonNull(minimum), "Building a Histogram requires setting the minimum");
            checkState(nonNull(maximum), "Building a Histogram requires setting the maximum");
            checkState(nonNull(numColumns), "Building a Histogram requires setting numColumns");
            checkState(nonNull(data), "Building a Histogram requires adding data");
            return new Histogram(minimum, maximum, numColumns, data);
        }
    }
}
