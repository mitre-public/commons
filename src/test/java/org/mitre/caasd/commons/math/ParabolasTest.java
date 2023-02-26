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
import static org.hamcrest.Matchers.closeTo;
import static org.mitre.caasd.commons.math.Parabolas.parabolaVertex;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Pair;

public class ParabolasTest {

    @Test
    public void calcParabolaVertexIsCorrect() {
        /*
         * The point:(0,1) (1,5) and (2,3) Form the parabola: y = −3x^2 + 7x + 1
         *
         * The vertex of the Parabola is at (1.1666666666666667, 5.083333333333333)
         *
         * All of this can be shown algebraically
         */

        Pair<Double, Double> vertex = parabolaVertex(0, 1, 1, 5, 2, 3);

        assertThat(vertex.first(), closeTo(7.0 / 6.0, 0.0001));
        assertThat(vertex.second(), closeTo(5.08333, 0.0001));
    }
}
