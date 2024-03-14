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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.primitives.Doubles.max;

import java.util.HashMap;

import org.mitre.caasd.commons.Pair;

import com.google.common.primitives.Ints;

/**
 * This class computes the Levenshtein distance (ie edit distance) between two Strings.
 */
public class EditDistance {

    /**
     * Compute a measure of similarity between two Strings that ranges between 0 and 1.
     *
     * @param one a String
     * @param two another String
     *
     * @return A similarity score between these two Strings (1.0 = identical string (not case
     *     sensitive), while 0.0 = maximally different). The computed score = editDistance /
     *     maxStringLength
     */
    public static double similarity(String one, String two) {
        double editDist = between(one, two);
        double length = max(one.length(), two.length());
        double difference = length - editDist;
        checkState(length >= editDist, "I think this should always be true");
        return difference / length;
    }

    public static int between(String one, String two) {
        EditDistance ed = new EditDistance(one, two);
        return ed.computeEditDistance();
    }

    private final String startString;
    private final String endString;
    private final HashMap<Pair<Integer, Integer>, Integer> knownValues;

    EditDistance(String startString, String endString) {
        this.startString = checkNotNull(startString).toLowerCase();
        this.endString = checkNotNull(endString).toLowerCase();
        this.knownValues = newHashMap();
        this.knownValues.put(Pair.of(0, 0), 0);
    }

    /*
     * Get (or cache) the edit distance between the first j letters of the start string and the
     * first j letters of the end String.
     */
    private int editDistance(int i, int j) {
        Pair<Integer, Integer> key = Pair.of(i, j);
        if (knownValues.containsKey(Pair.of(i, j))) {
            return knownValues.get(key);
        } else {
            int value = computeDistanceAt(i, j);
            knownValues.put(key, value);
            return value;
        }
    }

    /*
     * Compute the edit distance between the first j letters of the start string and the first j
     * letters of the end String.
     */
    private int computeDistanceAt(int i, int j) {
        if (i == 0) {
            return j;
        }
        if (j == 0) {
            return i;
        }
        int incrementCost = startString.charAt(i - 1) == endString.charAt(j - 1) ? 0 : 1;

        return Ints.min(
                editDistance(i - 1, j) + 1, editDistance(i, j - 1) + 1, editDistance(i - 1, j - 1) + incrementCost);
    }

    public int computeEditDistance() {
        return editDistance(startString.length(), endString.length());
    }
}
