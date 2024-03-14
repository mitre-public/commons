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
import static java.lang.Math.sin;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mitre.caasd.commons.Time.theDuration;
import static org.mitre.caasd.commons.math.DataSplitterTest.loadTestXYData;
import static org.mitre.caasd.commons.math.VisvalingamSimplifier.computeTriangleArea;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.util.SingleUseTimer;

import org.junit.jupiter.api.Test;

public class VisvalingamSimplifierTest {

    @Test
    public void testComputeTriangleArea_pointsInALine() {
        double area = computeTriangleArea(new XyPoint(0.0, 0.0), new XyPoint(1.0, 1.0), new XyPoint(2.0, 2.0));
        assertThat(area, closeTo(0.0, 0.00001));
    }

    @Test
    public void testComputeTriangleArea_pointsInATriangle() {
        double area = computeTriangleArea(new XyPoint(0.0, 0.0), new XyPoint(1.0, 1.0), new XyPoint(2.0, 0.0));
        assertThat(area, closeTo(1.0, 0.00001));
    }

    @Test
    public void testComputeTriangleArea_pointsInATriangle_2() {
        double area = computeTriangleArea(new XyPoint(0.0, 0.0), new XyPoint(1.0, 10.0), new XyPoint(2.0, 0.0));
        assertThat(area, closeTo(10.0, 0.00001));
    }

    @Test
    public void testComputeTriangleArea_pointsInALine_2() {
        double area = computeTriangleArea(new XyPoint(0.0, 675.0), new XyPoint(4.0, 675.0), new XyPoint(9.0, 675.0));
        assertThat(area, closeTo(0.0, 0.00001));
    }

    @Test
    public void demoOnRealWorldExample_altitudeData() throws IOException {
        /* This is more of a demo than a test.  Use this test to compare input/output pairs. */

        Pair<List<Double>, List<Double>> allData = loadTestXYData(
                "org/mitre/caasd/commons/math/altitudes1.txt"
                //			"org/mitre/caasd/commons/math/altitudes2.txt"
                //			"org/mitre/caasd/commons/math/altitudes3.txt"
                //			"org/mitre/caasd/commons/math/altitudes4.txt"
                );

        XyDataset inputData = new XyDataset(allData.first(), allData.second());

        //		System.out.println("Input data:");
        //		printPoints(dataset.asXyPointList());

        VisvalingamSimplifier simplifer = new VisvalingamSimplifier();

        XyDataset output = simplifer.simplify(inputData, 300 * 20); // 300 ft * 20 seconds

        //		System.out.println("Output Data");
        //		printPoints(output.asXyPointList());

        assertThat(
                "Most of the data was removed during simplification",
                inputData.size(),
                greaterThan(output.size() * 10));
    }

    @Test
    public void canProcess100kPointsInLessThanOneSecond() {

        List<Double> x = incrementsOfOneThousanths(100_000);
        List<Double> y = sinX(x);
        XyDataset input = new XyDataset(x, y);

        Double THRESHOLD = 0.05;

        SingleUseTimer timer = new SingleUseTimer();
        timer.tic();

        XyDataset output = (new VisvalingamSimplifier()).simplify(input, THRESHOLD);

        timer.toc();

        assertThat(
                "This operation should be quick -- even though it had 100k points",
                theDuration(timer.elapsedTime()).isLessThan(Duration.ofSeconds(1)),
                is(true));

        assertThat(
                "The output curve should have fewer than 1% of the original points",
                output.size() * 100 < input.size(), is(true));

        XyPoint left = null;
        XyPoint center = null;
        XyPoint right = null;

        for (XyPoint xyPoint : output.asXyPointList()) {
            left = center;
            center = right;
            right = xyPoint;

            if (nonNull(left) && nonNull(center) && nonNull(right)) {
                assertThat(computeTriangleArea(left, center, right), greaterThan(THRESHOLD));
            }
        }

        // print data
        // printPoints(output.asXyPointList());
    }

    private void printPoints(Collection<XyPoint> points) {
        points.forEach(xypoint -> printPoint(xypoint));
    }

    private void printPoint(XyPoint point) {
        System.out.println(point.x + "\t" + point.y);
    }

    private List<Double> incrementsOfOneThousanths(int n) {

        ArrayList<Double> x = newArrayList();
        for (double i = 0; i < n; i++) {
            x.add(i * 0.001);
        }
        return x;
    }

    // gives sin wave...
    private List<Double> sinX(List<Double> xData) {
        return xData.stream().map(x -> sin(x)).collect(toCollection(ArrayList::new));
    }
}
