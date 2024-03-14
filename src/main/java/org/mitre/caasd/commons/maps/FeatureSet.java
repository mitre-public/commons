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

package org.mitre.caasd.commons.maps;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The static Builder is the reason for writing this wrapper.
 */
public class FeatureSet implements Iterable<MapFeature> {

    private final List<MapFeature> features;

    public FeatureSet(Collection<MapFeature> features) {
        this.features = newArrayList(features);
    }

    /** @return An empty FeatureList. */
    public static FeatureSet noMapFeatures() {
        return new FeatureSet(newArrayList());
    }

    @Override
    public Iterator<MapFeature> iterator() {
        return features.iterator();
    }
}
