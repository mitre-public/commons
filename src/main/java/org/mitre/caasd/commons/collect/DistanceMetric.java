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

package org.mitre.caasd.commons.collect;

import java.io.Serializable;

/**
 * The DistanceMetric should define a true Metric Space (in the strict algebraic sense) for KEY
 * objects. This means the following should be true:
 * <p>
 * (1) d(x,y) >= 0 (2) d(x,y) = d(y,x) (3) d(x,z) <= d(x,y) + d(y,z) (4) d(x , y ) = 0 if and only
 * if x = y (optional)
 */
@FunctionalInterface
public interface DistanceMetric<KEY> extends Serializable {

    /**
     * @param item1 The first of two items
     * @param item2 The second of two items
     *
     * @return The distance between the 2 objects in a Metric Space (this method must define a
     *     proper Metric Space in the strict algebraic sense).
     */
    public double distanceBtw(KEY item1, KEY item2);
}
