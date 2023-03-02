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

import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.caasd.commons.math.DataSplitter.checkInputData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A XyDataset is a convenience class that make DataSplitters easier to use. */
public class XyDataset {

    private final List<Double> xData;
    private final List<Double> yData;

    private FastLinearApproximation approx;

    /**
     * Create an XyDataset assuming: (1) inputs are not null, (2) inputs have same size, (3) the x
     * data is strictly increasing.
     *
     * @param xData A strictly increasing sequence of x values
     * @param yData A y value for each x value
     */
    public XyDataset(List<Double> xData, List<Double> yData) {
        checkInputData(xData, yData);
        this.xData = xData;
        this.yData = yData;
    }

    public List<Double> xData() {
        return xData;
    }

    public List<Double> yData() {
        return yData;
    }

    public int size() {
        return xData.size();
    }

    public FastLinearApproximation approximateFit() {
        if (approx == null) {
            this.approx = new FastLinearApproximation(xData, yData);
        }
        return approx;
    }

    public double slopeEstimate() {
        return approximateFit().slope();
    }

    public double avgSquaredErrorEstimate() {
        return approximateFit().avgSquaredError();
    }

    public double length() {
        return xData.get(xData.size() - 1) - xData.get(0);
    }

    /**
     * This is a convenience function that applies a DataSplitter to this data AND repackages the
     * "array of split index" results into a more useful array of new Datasets (which should
     * partition this dataset).
     *
     * @param splitter
     *
     * @return An array of new XyDatasets that partition this data.
     */
    public XyDataset[] split(DataSplitter splitter) {

        int[] splitIndices = splitter.computeSplitsFor(xData, yData);

        XyDataset[] splits = new XyDataset[splitIndices.length - 1];

        for (int i = 0; i < splitIndices.length - 1; i++) {
            splits[i] = new XyDataset(
                xData.subList(splitIndices[i], splitIndices[i + 1]),
                yData.subList(splitIndices[i], splitIndices[i + 1])
            );
        }
        return splits;
    }

    public ArrayList<XyPoint> asXyPointList() {
        ArrayList list = new ArrayList(xData.size());

        Iterator<Double> xIter = xData.iterator();
        Iterator<Double> yIter = yData.iterator();

        while (xIter.hasNext()) {
            list.add(new XyPoint(xIter.next(), yIter.next()));
        }
        return list;
    }

    public XyDataset takeDerivative() {
        ArrayList<Double> xCopy = newArrayList(xData);
        ArrayList<Double> yCopy = newArrayList();
        int n = xCopy.size();

        for (int i = 0; i < n; i++) {
            if (i == 0) {
                double slopeToNext = (yData.get(i + 1) - yData.get(i)) / (xData.get(i + 1) - xData.get(i));
                yCopy.add(slopeToNext);
            } else if (i == n - 1) {
                double slopeToPrior = (yData.get(i) - yData.get(i - 1)) / (xData.get(i) - xData.get(i - 1));
                yCopy.add(slopeToPrior);
            } else {
                double slopeToPrior = (yData.get(i) - yData.get(i - 1)) / (xData.get(i) - xData.get(i - 1));
                double slopeToNext = (yData.get(i + 1) - yData.get(i)) / (xData.get(i + 1) - xData.get(i));
                double slopeBetween = (yData.get(i + 1) - yData.get(i - 1)) / (xData.get(i + 1) - xData.get(i - 1));
                double avgSlope = (slopeToPrior + slopeToNext + slopeBetween) / 3.0;
                yCopy.add(avgSlope);
            }
        }
        return new XyDataset(xCopy, yCopy);
    }
}
