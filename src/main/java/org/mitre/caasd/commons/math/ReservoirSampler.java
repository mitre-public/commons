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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Random;

/**
 * A ReservoirSampler maintains a random sample of exactly K items from a stream of N items (K is
 * known, N is unknown)
 */
public class ReservoirSampler<T> {

    private int numItemsSeen;

    List sample;

    int k;

    Random rng;

    public ReservoirSampler(int k, Random rng) {
        checkArgument(k >= 0, "k must be positive");
        checkNotNull(rng);
        this.numItemsSeen = 0;
        this.sample = newArrayList();
        this.k = k;
        this.rng = rng;
    }

    public void observe(T item) {

        numItemsSeen++;

        // keep every sample until we have at least k observations...
        if (sample.size() < k) {
            sample.add(item);
            return;
        }

        // the chance "this" item is in the sample is k / numItemsSeen
        int randomDraw = rng.nextInt(numItemsSeen);
        if (randomDraw < k) {
            sample.set(randomDraw, item);
        }
    }

    public List<T> currentSample() {
        // return a defensive copy of the sample.
        return newArrayList(sample);
    }

    /** @return The number of samples this ReservoirSampler is extracting. */
    public int k() {
        return this.k;
    }

    /** @return The total number of items this ReservoirSampler has seen in its lifetime. */
    public int numObservations() {
        return this.numItemsSeen;
    }
}
