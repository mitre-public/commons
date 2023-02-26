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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Pair;

public class PiecewiseLinearSplitterTest {

    @Test
    public void datasetsWithUnequalSizesAreRejected() {
        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(5.0);

        assertThrows(IllegalArgumentException.class,
            () -> splitter.computeSplitsFor(
                newArrayList(1.0, 2.0, 3.0),
                newArrayList(1.0, 2.0)
            )
        );
    }

    @Test
    public void outOfOrderXdataIsRejected() {
        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(5.0);

        assertThrows(IllegalArgumentException.class,
            () -> splitter.computeSplitsFor(
                newArrayList(3.0, 2.0, 1.0),
                newArrayList(1.0, 2.0, 3.0)
            )
        );
    }

    @Test
    public void inOrderXdataIsAccepted() {
        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(5.0);

        assertDoesNotThrow(
            () -> splitter.computeSplitsFor(
                newArrayList(1.0, 2.0, 3.0),
                newArrayList(1.0, 2.0, 3.0)
            )
        );
    }

    @Test
    public void splitTwoLinesCorrectly() {

        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(1.0);

        //2 saw teeth pattern
        Double[] xData = new Double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        Double[] yData = new Double[]{2.0, 4.0, 6.0, 8.0, 10.0, 1.0, 2.0, 3.0, 4.0, 5.0};

        List<Double> xDataList = newArrayList(xData);
        List<Double> yDataList = newArrayList(yData);

        //1 saw tooth pattern
        int[] splits = splitter.computeSplitsFor(xDataList, yDataList);

        assertThat(splits[0], is(0));
        assertThat(splits[1], is(5));
        assertThat(splits[2], is(10));

        assertThat(xDataList.subList(splits[0], splits[1]), contains(1.0, 2.0, 3.0, 4.0, 5.0));
        assertThat(yDataList.subList(splits[0], splits[1]), contains(2.0, 4.0, 6.0, 8.0, 10.0));

        assertThat(xDataList.subList(splits[1], splits[2]), contains(6.0, 7.0, 8.0, 9.0, 10.0));
        assertThat(yDataList.subList(splits[1], splits[2]), contains(1.0, 2.0, 3.0, 4.0, 5.0));
    }

    @Test
    public void straightLinesAreNotSplit() {

        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(1.0);

        Double[] xData = new Double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        Double[] yData = new Double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};

        List<Double> xDataList = newArrayList(xData);
        List<Double> yDataList = newArrayList(yData);

        int[] splits = splitter.computeSplitsFor(xDataList, yDataList);

        assertThat(splits[0], is(0));
        assertThat(splits[1], is(10));
        assertThat(splits.length, is(2));
    }

    @Test
    public void canSplitDatasetWith4Entries() {

        Double[] xData = new Double[]{1.0, 2.0, 3.0, 4.0};
        Double[] yData = new Double[]{0.0, -10.0, 90.0, 80.0};

        List<Double> xDataList = newArrayList(xData);
        List<Double> yDataList = newArrayList(yData);

        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(5.0);

        int[] splits = splitter.computeSplitsFor(xDataList, yDataList);

        assertThat(splits[0], is(0));
        assertThat(splits[1], is(2));
        assertThat(splits[2], is(4));
        assertThat(splits.length, is(3));

        List<Double> leftSplit = xDataList.subList(splits[0], splits[1]);
        List<Double> rightSplit = xDataList.subList(splits[1], splits[2]);

        assertThat(leftSplit, contains(1.0, 2.0));
        assertThat(rightSplit, contains(3.0, 4.0));
    }

    @Disabled
    @Test
    public void demoPiecewiseLinearSplitting_example1() throws IOException {
        /*
         * This "tes" is ignored because it does not VERIFY anything. It leaves it for the reader to
         * determine if the input and output are "good"
         */
        Pair<List<Double>, List<Double>> allData = DataSplitterTest.loadTestXYData(
            "org/mitre/caasd/commons/math/altitudes1.txt"
        );
        List<Double> xData = allData.first();
        List<Double> yData = allData.second();

        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(500);

        int[] splits = splitter.computeSplitsFor(xData, yData);

        for (int i = 0; i < splits.length; i++) {
            System.out.println("split: " + i + " = " + splits[i]);
        }

        for (int i = 0; i < splits.length - 1; i++) {
            int startIndex = splits[i];
            int endIndex = splits[i + 1] - 1;

            System.out.println(
                "Split: " + i
                    + " from: (" + xData.get(startIndex) + "," + yData.get(startIndex) + ")"
                    + " to: ( " + xData.get(endIndex) + ", " + yData.get(endIndex) + ")"
            );
        }
    }

    @Disabled
    @Test
    public void demoPiecewiseLinearSplitting_example2() throws IOException {
        Pair<List<Double>, List<Double>> allData = DataSplitterTest.loadTestXYData(
            "org/mitre/caasd/commons/math/altitudes2.txt"
        );
        List<Double> xData = allData.first();
        List<Double> yData = allData.second();

        PiecewiseLinearSplitter splitter = new PiecewiseLinearSplitter(500);

        int[] splits = splitter.computeSplitsFor(xData, yData);

        for (int i = 0; i < splits.length; i++) {
            System.out.println("split: " + i + " = " + splits[i]);
        }

        for (int i = 0; i < splits.length - 1; i++) {
            int startIndex = splits[i];
            int endIndex = splits[i + 1] - 1;

            System.out.println(
                "Split: " + i
                    + " from: (" + xData.get(startIndex) + "," + yData.get(startIndex) + ")"
                    + " to: ( " + xData.get(endIndex) + ", " + yData.get(endIndex) + ")"
            );
        }
    }
}
