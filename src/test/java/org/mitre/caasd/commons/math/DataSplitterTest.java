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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.math.DataSplitter.checkInputData;
import static org.mitre.caasd.commons.math.DataSplitter.checkOrdering;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.fileutil.FileUtils;

public class DataSplitterTest {

    /**
     * Load some test XY data (altitude over time)
     *
     * @param filename
     *
     * @return Two lists that can easily be used via the DataSplitter interface
     * @throws IOException (if the target data can't be found)
     */
    public static Pair<List<Double>, List<Double>> loadTestXYData(String filename) throws IOException {
        File file = FileUtils.getResourceFile(filename);
        List<String> lines = Files.readAllLines(file.toPath());
        List<Double> xValues = newArrayList();
        List<Double> yValues = newArrayList();
        for (String line : lines) {
            String[] values = line.split("\t");
            double x = Double.parseDouble(values[0]);
            double y = Double.parseDouble(values[1]);
            xValues.add(x);
            yValues.add(y);
        }
        return Pair.of(xValues, yValues);
    }

    @Test
    public void checkInputRejectsNullInputs_x() {

        assertThrows(NullPointerException.class,
            () -> checkInputData(null, newArrayList(2.0, 3.0))
        );
    }

    @Test
    public void checkInputRejectsNullInputs_y() {
        assertThrows(NullPointerException.class,
            () -> checkInputData(newArrayList(2.0, 3.0), null)
        );
    }

    @Test
    public void checkInputRejectsInputWithDifferentSizes() {
        assertThrows(IllegalArgumentException.class,
            () -> checkInputData(
                newArrayList(2.0, 3.0),
                newArrayList(2.0, 3.0, 4.0)
            )
        );
    }

    @Test
    public void checkInputRejectsUnsortedXValues() {
        assertThrows(IllegalArgumentException.class,
            () -> checkInputData(
                newArrayList(2.0, 3.0, -10.0), //x values must be sorted
                newArrayList(2.0, 3.0, 4.0)
            )
        );
    }

    @Test
    public void checkInputDoesNothingWhenInputIsGood() {
        checkInputData(
            newArrayList(2.0, 3.0, 4.0),
            newArrayList(20.0, -10.0, 400.0)
        );
    }

    @Test
    public void checkOrderingDoesNothingWhenInputIsOrdered() {
        checkOrdering(newArrayList(1.0, 2.0, 3.0));
    }

    @Test
    public void checkOrderingRejectsUnsortedData() {
        assertThrows(IllegalArgumentException.class,
            () -> checkOrdering(newArrayList(10.0, 2.0, 11.0))
        );
    }

    @Test
    public void checkOrderingRejectsUnsortedData_duplicateValues() {
        //2 copies of the same value should fail
        assertThrows(IllegalArgumentException.class,
            () -> checkOrdering(newArrayList(1.0, 2.0, 3.0, 3.0))
        );
    }
}
