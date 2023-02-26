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

import java.util.Objects;

import com.google.common.primitives.Doubles;

/**
 * SetSearchResult relay the output of a K-nearest neighbor search or a range search.
 *
 * @param <K> The key class
 */
public class SetSearchResult<K> implements Comparable<SetSearchResult<K>> {

    final K key;

    final double distance;

    SetSearchResult(K key, double distance) {
        this.key = key;
        this.distance = distance;
    }

    public K key() {
        return this.key;
    }

    public double distance() {
        return this.distance;
    }

    /**
     * Sort by distance. This is required for the PriorityQueue used to collect the Results always
     * has the Result with the k-th largest distance on top. This means the threshold for improving
     * the k-nearest neighbor result is readily accessible.
     */
    @Override
    public int compareTo(SetSearchResult<K> other) {
        return Doubles.compare(other.distance, this.distance);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.key);
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.distance) ^ (Double.doubleToLongBits(this.distance) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SetSearchResult<?> other = (SetSearchResult<?>) obj;
        if (Double.doubleToLongBits(this.distance) != Double.doubleToLongBits(other.distance)) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        return true;
    }
}
