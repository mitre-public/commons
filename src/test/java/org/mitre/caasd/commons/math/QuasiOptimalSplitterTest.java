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
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.math3.util.FastMath.sin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mitre.caasd.commons.Pair;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class QuasiOptimalSplitterTest {

    @Disabled
    @Test
    public void showThatRuntimeDegradesOnLargeInput() {
        /* Confirm you can at least get an answer on larger inputs (slowly). */

        List<Double> x = xData();
        List<Double> y = sinX(x);

        QuasiOptimalSplitter splitter = new QuasiOptimalSplitter(0.25);
        int[] splits = splitter.computeSplitsFor(x, y);

        for (int i = 0; i < splits.length; i++) {
            System.out.println("Found Split at: " + splits[i] + " " + sin(splits[i] / 10.0));
        }
    }

    // gives 0 to 999.9 in increments of 0.1
    private List<Double> xData() {

        int n = 10_000;

        ArrayList<Double> x = newArrayList();
        for (double i = 0; i < n; i++) {
            x.add(i / 10.0);
        }
        return x;
    }

    // gives sin wave...
    private List<Double> sinX(List<Double> xData) {
        return xData.stream().map(x -> sin(x)).collect(toCollection(ArrayList::new));
    }

    @Disabled // not a rigorous test, just a demo
    @Test
    public void realSampleDataIsSplitWell() throws IOException {

        // @todo -- This is more demo than test, add a couple assertions (say avg error is "small")

        Pair<List<Double>, List<Double>> allData =
                DataSplitterTest.loadTestXYData("org/mitre/caasd/commons/math/altitudes1.txt");

        XyDataset data = new XyDataset(allData.first(), allData.second());

        XyDataset[] splits = data.split(new QuasiOptimalSplitter(50 * 50));

        for (XyDataset split : splits) {
            int n = split.xData().size();
            System.out.println(
                    "From: (" + split.xData().get(0) + "," + split.yData().get(0) + ")" + " to: ("
                            + split.xData().get(n - 1) + ", " + split.yData().get(n - 1) + ")");
            System.out.println("  length: " + split.length());
            System.out.println("  slope: " + split.slopeEstimate());
            System.out.println("  avgError: " + sqrt(split.avgSquaredErrorEstimate()));
        }
    }
}
