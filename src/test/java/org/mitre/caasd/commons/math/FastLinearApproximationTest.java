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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class FastLinearApproximationTest {

    @Test
    public void errorGivenSlopeReflectsBasicTrend() {

        // this is a straight line with slope 0.5
        Double[] xData = new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        Double[] yData = new Double[] {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};

        List<Double> xDataList = newArrayList(xData);
        List<Double> yDataList = newArrayList(yData);

        FastLinearApproximation fla = new FastLinearApproximation(xDataList, yDataList);

        // the slope of the line is positive, so proposing a postive slop should generate a smaller error the a negative
        // slope
        assertThat(fla.sumSquaredErrorGivenSlope(1) < fla.sumSquaredErrorGivenSlope(-1), is(true));

        // the slope of the line is exactly 0.5 -- this error should be smallest
        assertThat(fla.sumSquaredErrorGivenSlope(0.5) < fla.sumSquaredErrorGivenSlope(0.49), is(true));
        assertThat(fla.sumSquaredErrorGivenSlope(0.5) < fla.sumSquaredErrorGivenSlope(0.51), is(true));
        assertThat(fla.sumSquaredErrorGivenSlope(0.5), closeTo(0.0, 0.001));

        assertThat(fla.averageY(), is(2.75));
        assertThat(fla.slope(), closeTo(0.5, 0.001));
        assertThat(fla.totalSquaredError(), closeTo(0.0, 0.001));
    }

    @Test
    public void totalSquaredErrorComputedAsExpected() {

        // this is symmertric curve: straight from (1.0, 0.5) to (5.0, 2.5)...and then back down
        Double[] xData = new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        Double[] yData = new Double[] {0.5, 1.0, 1.5, 2.0, 2.5, 2.5, 2.0, 1.5, 1.0, 0.5};

        List<Double> xDataList = newArrayList(xData);
        List<Double> yDataList = newArrayList(yData);

        FastLinearApproximation fla = new FastLinearApproximation(xDataList, yDataList);

        assertThat(fla.averageY(), is(1.5));
        assertThat(fla.slope(), closeTo(0.0, 0.001));

        double expectedSquaredError = Math.pow(1.5 - 0.5, 2)
                + Math.pow(1.5 - 1.0, 2)
                + Math.pow(1.5 - 1.5, 2)
                + Math.pow(1.5 - 2.0, 2)
                + Math.pow(1.5 - 2.5, 2)
                + Math.pow(1.5 - 2.5, 2)
                + Math.pow(1.5 - 2.0, 2)
                + Math.pow(1.5 - 1.5, 2)
                + Math.pow(1.5 - 1.0, 2)
                + Math.pow(1.5 - 0.5, 2);

        assertThat(fla.totalSquaredError(), closeTo(expectedSquaredError, 0.001));
    }

    @Test
    public void badInputIsRejected_notSorted() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new FastLinearApproximation(newArrayList(2.0, 1.0, 3.0), newArrayList(1.0, 2.0, 3.0)));
    }

    @Test
    public void badInputIsRejected_sizeMismatch() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new FastLinearApproximation(newArrayList(1.0, 2.0, 3.0), newArrayList(1.0, 2.0, 3.0, 4.0)));
    }
}
