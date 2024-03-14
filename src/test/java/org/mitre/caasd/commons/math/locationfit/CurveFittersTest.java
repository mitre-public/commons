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

package org.mitre.caasd.commons.math.locationfit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.mitre.caasd.commons.math.CurveFitters;

import com.google.common.collect.Range;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.junit.jupiter.api.Test;

public class CurveFittersTest {

    @Test
    public void fitAndPredict_unWeighted() {

        //y = x^2 (plus some noise)
        List<Double> xData = newArrayList(0.0, 1.0, 2.0, 3.0);
        List<Double> yData = newArrayList(-.1, .9, 4.1, 9.1);

        double prediction1 = CurveFitters.fitAndPredict(2, xData, yData, .5);
        double prediction2 = CurveFitters.fitAndPredict(2, xData, yData, 1.5);
        double prediction3 = CurveFitters.fitAndPredict(2, xData, yData, 2.5);

        Range expected1 = Range.closed(.5 * .5 - 0.1, .5 * .5 + .1); // [.15, .35] i.e. around (.5)^2
        Range expected2 = Range.closed(1.5 * 1.5 - 0.1, 1.5 * 1.5 + .1); // [2.15, 2.35] i.e. around (1.5)^2
        Range expected3 = Range.closed(2.5 * 2.5 - 0.1, 2.5 * 2.5 + .1); // [6.15, 6.35] i.e. around (2.5)^2

        assertThat(expected1.contains(prediction1), is(true));
        assertThat(expected2.contains(prediction2), is(true));
        assertThat(expected3.contains(prediction3), is(true));
    }

    @Test
    public void fit_unWeighted() {

        //y = x^2 (plus some noise)
        List<Double> xData = newArrayList(0.0, 1.0, 2.0, 3.0);
        List<Double> yData = newArrayList(-.1, .9, 4.1, 9.1);

        //Perform the fit exactly once, all samples are weighted the same..
        PolynomialFunction func = CurveFitters.fit(2, xData, yData);

        double prediction1 = func.value(.5);
        double prediction2 = func.value(1.5);
        double prediction3 = func.value(2.5);

        Range expected1 = Range.closed(.5 * .5 - 0.1, .5 * .5 + .1); // [.15, .35] i.e. around (.5)^2
        Range expected2 = Range.closed(1.5 * 1.5 - 0.1, 1.5 * 1.5 + .1); // [2.15, 2.35] i.e. around (1.5)^2
        Range expected3 = Range.closed(2.5 * 2.5 - 0.1, 2.5 * 2.5 + .1); // [6.15, 6.35] i.e. around (2.5)^2

        assertThat(expected1.contains(prediction1), is(true));
        assertThat(expected2.contains(prediction2), is(true));
        assertThat(expected3.contains(prediction3), is(true));
    }

}
