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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.util.EditDistance.similarity;

import org.junit.jupiter.api.Test;

public class EditDistanceTest {

    @Test
    public void confirmEditDistBtwSingleCharStringAndZeroCharString() {
        assertEquals(EditDistance.between("", "a"), 1);
        assertEquals(EditDistance.between("a", ""), 1);
    }

    @Test
    public void confirmEditDistBtwTwoSingleCharStrings() {
        assertEquals(EditDistance.between("a", "b"), 1);
    }

    @Test
    public void confirmEditDistanceBtw_twoCharStrings() {
        assertEquals(EditDistance.between("aa", "bb"), 2);
        assertEquals(EditDistance.between("ab", "bb"), 1);
        assertEquals(EditDistance.between("ba", "bb"), 1);
        assertEquals(EditDistance.between("", "bb"), 2);
    }

    @Test
    public void testEditDistance_1() {
        assertEquals(EditDistance.between("happy", "hapUGGpy"), 3);
        assertEquals(EditDistance.between("hapUGGpy", "happy"), 3);
    }

    @Test
    public void testEditDistance_2() {
        assertEquals(EditDistance.between("", "abcdefghijklmnopqrstuvwxyz"), 26);
        assertEquals(EditDistance.between("abcdefghijklmnopqrstuvwxyz", ""), 26);
    }

    @Test
    public void similaryRangesFrom1to0() {
        double TOL = 0.0001;
        assertEquals(1.0, similarity("happy", "happy"), TOL);
        assertEquals(0.0, similarity("", "happy"), TOL);
    }

    @Test
    public void testSimilarity_2() {
        assertTrue(similarity("aa", "ab") >= similarity("aa", "bb"));
        assertEquals(similarity("aa", "cc"), similarity("aa", "bb"));
    }

    @Test
    public void testSimilarity_3() {
        double TOL = 0.0001;
        assertEquals(0.5, similarity("aa", "ab"), TOL);
        assertEquals(0.75, similarity("aaaa", "aaac"), TOL);
        assertEquals(0.25, similarity("a", "aaac"), TOL);
    }
}
