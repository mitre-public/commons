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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Random;

import org.mitre.caasd.commons.Pair;

/**
 * This class provides access to two implementations of a CenterPointSelector.
 * <p>
 * Of these two implementations the "maxOfRandomSamples()" is preferred because it generates fewer
 * spheres when used (i.e. spheres aren't wasted). It also requires relatively few distance
 * computations.
 */
public class CenterPointSelectors {

    /**
     * Provides a CenterPointSelector that immediately finds two sphere center points (because the
     * choice is purely random).
     *
     * @return A CenterPointSelector<K>
     */
    public static <K> CenterPointSelector<K> singleRandomSample(long randomSeed) {
        return new RandomCenterSelector<>(randomSeed);
    }

    /** This Selector picks 2 random Keys from a List of Keys provided. */
    private static class RandomCenterSelector<K> implements CenterPointSelector<K> {

        private static final long serialVersionUID = 1L;

        private final Random rng;

        private RandomCenterSelector(long seed) {
            this.rng = new Random(seed);
        }

        /**
         * @param keys   A List of Keys that needs to be split
         * @param metric The distance metric that measures distance between 2 keys
         *
         * @return Two keys that will be used as the centerPoints for a two new Spheres
         */
        @Override
        public Pair<K, K> selectNewCenterPoints(List<K> keys, DistanceMetric<K> metric) {
            checkArgument(keys.size() > 2);
            return selectRandomPairOfKeys(keys, rng);
        }
    }

    public static <K> CenterPointSelector<K> maxOfRandomSamples() {
        return new RandomizedMaxDistanceSelector<>();
    }

    /**
     * This Selector picks multiple random Keys Pairs from a List of Keys provided and returns the
     * pair with the largest distance between them. This pair of Keys should generate 2 child
     * spheres whose volumes overlap as little as possible.
     */
    private static class RandomizedMaxDistanceSelector<K> implements CenterPointSelector<K> {

        private static final long serialVersionUID = 1L;

        Random rng = new Random(17L);

        /**
         * @param keys   A List of Keys that needs to be split
         * @param metric The distance metric that measures distance between 2 keys
         *
         * @return Two keys that will be used as the centerPoints for a two new Spheres
         */
        @Override
        public Pair<K, K> selectNewCenterPoints(List<K> keys, DistanceMetric<K> metric) {

            int numPairsToDraw = (int) Math.sqrt(keys.size()); //sqrt strikes a good balance

            Pair<K, K> bestPair = selectRandomPairOfKeys(keys, rng);
            double biggestDistance = metric.distanceBtw(bestPair.first(), bestPair.second());
            numPairsToDraw--;

            for (int i = 0; i < numPairsToDraw; i++) {

                Pair<K, K> newPair = selectRandomPairOfKeys(keys, rng);
                double newDistance = metric.distanceBtw(newPair.first(), newPair.second());

                if (newDistance > biggestDistance) {
                    bestPair = newPair;
                    biggestDistance = newDistance;
                }
            }

            return bestPair;
        }
    }

    private static <KEY> Pair<KEY, KEY> selectRandomPairOfKeys(List<KEY> keys, Random rng) {

        //pick 2 random -- and unique -- index values
        int n = keys.size();
        int index1 = rng.nextInt(n);
        int index2 = rng.nextInt(n);
        while (index1 == index2) {
            index2 = rng.nextInt(n);
        }

        return Pair.of(keys.get(index1), keys.get(index2));
    }
}
