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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.caasd.commons.math.DataSplitterTest.loadTestXYData;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.mitre.caasd.commons.Pair;

import org.junit.jupiter.api.Test;

public class VisvalingamSplitterTest {

    @Test
    public void realSampleDataIsSplitWell() throws IOException {
        Pair<List<Double>, List<Double>> allData = loadTestXYData(
            "org/mitre/caasd/commons/math/altitudes1.txt"
        );

        XyDataset inputData = new XyDataset(allData.first(), allData.second());

        XyDataset[] outputDatasets = (new VisvalingamSplitter(300 * 20)).split(inputData); //300 feet of error over 20 seconds

        XyDataset directSimplification = (new VisvalingamSimplifier()).simplify(inputData, 300 * 20);

        int totalOutputSize = Stream.of(outputDatasets)
            .mapToInt(dataset -> dataset.size())
            .sum();

        assertThat(inputData.size(), is(totalOutputSize));

        //the first point in each output partition is found in the results from directly applying a VisvalingamSimplifier
        for (XyDataset outputDataset : outputDatasets) {
            Double firstX = outputDataset.xData().get(0);
            assertThat(directSimplification.xData().contains(firstX), is(true));
        }

        //There is one output partition for each point identied by the VisvalingamSimplifier
        assertThat(directSimplification.size(), is(outputDatasets.length + 1));
    }
}
