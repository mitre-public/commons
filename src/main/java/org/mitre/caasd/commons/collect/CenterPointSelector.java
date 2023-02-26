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
import java.util.List;

import org.mitre.caasd.commons.Pair;

/**
 * A CenterPointSelector selects two keys from a List of keys. The selected key are used as the
 * "Center Points" for the multi-dimensional spheres use in the MetricTree and MetricSet classes.
 *
 * @param <K> The Key class
 */
public interface CenterPointSelector<K> extends Serializable {

    /**
     * @param keys   A List of Keys that needs to be split
     * @param metric The distance metric that measures distance between 2 keys
     *
     * @return Two keys that will be used as the centerPoints for a two new Spheres
     */
    Pair<K, K> selectNewCenterPoints(List<K> keys, DistanceMetric<K> metric);
}
