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

package org.mitre.caasd.commons.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import org.junit.jupiter.api.Test;

public class CompositeTranslatorTest {

    public Translator<Double, Double> timesTwo = new Translator<Double, Double>() {
        @Override
        public Double to(Double item) {
            return item * 2.0;
        }

        @Override
        public Double from(Double item) {
            return item / 2.0;
        }
    };

    public Translator<Double, Double> plusFive = new Translator<Double, Double>() {
        @Override
        public Double to(Double item) {
            return item + 5.0;
        }

        @Override
        public Double from(Double item) {
            return item - 5.0;
        }
    };

    @Test
    public void verifyInvertability() {

        Translator<Double, Double> translator = new CompositeTranslator(timesTwo, plusFive);

        double toThenFrom = translator.from(translator.to(10.0));

        assertThat(10.0, closeTo(toThenFrom, 0.0000005));

        double fromThenTo = translator.to(translator.from(-10.0));

        assertThat(-10.0, closeTo(fromThenTo, 0.0000005));
    }

    @Test
    public void verifyConversion() {

        Translator<Double, Double> translator = new CompositeTranslator(timesTwo, plusFive);

        double forwardOutput = translator.to(10.0);

        assertThat((10.0 * 2) + 5.0, closeTo(forwardOutput, 0.00005));

        double backwardOutput = translator.from(10.0);

        assertThat((10 - 5.0) / 2.0, closeTo(backwardOutput, 0.00005));
    }
}
