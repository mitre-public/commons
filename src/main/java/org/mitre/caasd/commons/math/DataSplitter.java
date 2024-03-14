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

import java.util.List;

/**
 * A DataSplitter partitions an XY dataset into multiple chunks based on some criterion.
 * <p>
 * One implementation could chop a dataset into multiple piece-wise linear segments. Another
 * implementation might split a dataset when two consecutive data points are too far apart.
 */
@FunctionalInterface
public interface DataSplitter {

    int[] computeSplitsFor(List<Double> xData, List<Double> yData);

    default int[] computeSplitsFor(XyDataset dataset) {
        return computeSplitsFor(dataset.xData(), dataset.yData());
    }

    default XyDataset[] split(XyDataset dataset) {
        return dataset.split(this);
    }

    /**
     * Reusable input check method for all DataSplitters. Confirms, (1) inputs are not null, (2)
     * inputs have same size, (3) the x data is strictly increasing.
     *
     * @param xData A strictly increasing sequence of x values
     * @param yData A y value for each x value
     */
    static void checkInputData(List<Double> xData, List<Double> yData) {
        checkNotNull(xData);
        checkNotNull(yData);
        checkArgument(xData.size() == yData.size(), "The xData and yData have different sizes");
        checkOrdering(xData);
    }

    /**
     * Throw an IllegalArgumentException if the xData is not strictly increasing.
     *
     * @param xData A sequence of strictly increasing values.
     */
    static void checkOrdering(List<Double> xData) {

        double last = Double.NEGATIVE_INFINITY;
        for (Double cur : xData) {
            if (cur <= last) {
                throw new IllegalArgumentException("The input is not sorted");
            }
            last = cur;
        }
    }
}
