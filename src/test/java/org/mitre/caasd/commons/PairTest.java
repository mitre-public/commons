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

package org.mitre.caasd.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PairTest {

    @Test
    public void testInputOrder() {
        Pair<String, Double> p1 = Pair.of("String", 5.0);

        assertTrue(p1.first().equals("String"));
        assertTrue(p1.second().equals(5.0));

        Pair<Double, String> p2 = Pair.of(5.0, "String");

        assertTrue(p2.first().equals(5.0));
        assertTrue(p2.second().equals("String"));
    }

    @Test
    public void testEquals() {

        Pair<String, Double> p1 = Pair.of("String", 5.0);
        Pair<String, Double> p2 = Pair.of("String", 5.0);

        Pair<String, Double> p3 = Pair.of("String", 6.0);
        Pair<String, Double> p4 = Pair.of("Another String", 5.0);

        assertThat("Should be the same", p1.equals(p2), is(true));
        assertThat("Not the same, different in 2nd value", p1.equals(p3), is(false));
        assertThat("Not the same, different in 1st value", p1.equals(p4), is(false));
        assertTrue(p1.equals(p1));
        assertFalse(p1.equals(null));
        assertThat("A Pair object is not equal to a different class", p1.equals("Hello"), is(false));
    }

    @Test
    public void testHashcode() {

        Pair<String, Double> p1 = Pair.of("String", 5.0);
        Pair<String, Double> p2 = Pair.of("String", 5.0);

        Pair<String, Double> p3 = Pair.of("String", 6.0);
        Pair<String, Double> p4 = Pair.of("Another String", 5.0);

        assertThat("Should be the same", p1.hashCode(), is(p2.hashCode()));
        assertThat("Not the same, different in 2nd value", p1.hashCode(), is(not(p3.hashCode())));
        assertThat("Not the same, different in 1st value", p1.hashCode(), is(not(p4.hashCode())));
    }
}
