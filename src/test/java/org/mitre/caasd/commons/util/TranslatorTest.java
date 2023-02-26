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
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class TranslatorTest {

    Translator<String, Double> toNumber = new Translator<String, Double>() {
        @Override
        public Double to(String string) {
            return Double.parseDouble(string);
        }

        @Override
        public String from(Double item) {
            return item.toString();
        }
    };

    Translator<Double, Double> plus12 = new Translator<Double, Double>() {
        @Override
        public Double to(Double num) {
            return num + 12.0;
        }

        @Override
        public Double from(Double num) {
            return num - 12.0;
        }
    };

    @Test
    public void canComposeTwoTranslators() {
        Translator<String, Double> composition = toNumber.compose(plus12);

        double forward = composition.to("13.0");
        assertThat(forward, closeTo(25.0, 0.000001));

        String back = composition.from(11.0);
        assertThat(back, is("-1.0"));
    }
}
