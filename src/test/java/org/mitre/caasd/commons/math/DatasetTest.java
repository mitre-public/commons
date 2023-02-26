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
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DatasetTest {

    @Test
    public void testConstructor() {
        ArrayList<Double> xValues = newArrayList(1.0, 2.0, 3.0, 4.0);
        ArrayList<Double> yValues = newArrayList(1.0, 1.0, 1.0, 1.0);

        XyDataset data = new XyDataset(xValues, yValues);
        assertThat(data.xData(), is(xValues));
        assertThat(data.yData(), is(yValues));

        assertThat(data.size(), is(4));
    }

    @Test
    public void testFitting() {

        ArrayList<Double> xValues = newArrayList(1.0, 2.0, 3.0, 4.0);
        ArrayList<Double> yValues = newArrayList(1.0, 1.0, 1.0, 1.0);

        XyDataset data = new XyDataset(xValues, yValues);

        assertThat(data.length(), is(3.0));
        assertThat(data.approximateFit().averageY(), is(1.0));
        assertThat(data.approximateFit().slope(), is(0.0));
    }

    @Test
    public void testTakeDerivative() {
        ArrayList<Double> xValues = newArrayList(1.0, 2.0, 3.0, 4.0);
        ArrayList<Double> yValues = newArrayList(1.0, 1.0, 1.0, 5.0);

        XyDataset data = new XyDataset(xValues, yValues);
        XyDataset derivative = data.takeDerivative();

        assertThat(derivative.xData(), is(xValues));

        List<Double> derivativeY = derivative.yData();

        assertThat(derivativeY.get(0), is(0.0));
        assertThat(derivativeY.get(1), is(0.0));
        assertThat(derivativeY.get(2), is(2.0));
        assertThat(derivativeY.get(3), is(4.0));
    }
}
