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

import org.mitre.caasd.commons.Pair;

public class Parabolas {

    private Parabolas() {
        //do not instatiate
    }

    /**
     * Deduce the vertex of a parabola that goes through 3 known points.
     *
     * @return The vertex as an X-Y Point
     */
    public static Pair<Double, Double> parabolaVertex(
        double x1, double y1,
        double x2, double y2,
        double x3, double y3
    ) {

        double denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
        double a = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
        double b = (x3 * x3 * (y1 - y2) + x2 * x2 * (y3 - y1) + x1 * x1 * (y2 - y3)) / denom;
        double c = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

        double xOfVertex = -b / (2 * a);
        double yOfVertex = c - b * b / (4 * a);

        return Pair.of(xOfVertex, yOfVertex);
    }
}
