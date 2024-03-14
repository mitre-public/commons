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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class CombinedPredicateTest {

    @Test
    public void simpleTestPredicatesWorkAsExpected() {

        Predicate<Integer> greaterThan20 = new GreaterThan20();

        assertThat(greaterThan20.test(19), is(false));
        assertThat(greaterThan20.test(20), is(false));
        assertThat(greaterThan20.test(21), is(true));

        Predicate<Integer> lessThan100 = new LessThan100();

        assertThat(lessThan100.test(99), is(true));
        assertThat(lessThan100.test(100), is(false));
        assertThat(lessThan100.test(101), is(false));
    }

    @Test
    public void combinedPredicateGivesCorrectAnswer() {

        GreaterThan20 greaterThan20 = new GreaterThan20();
        LessThan100 lessThan100 = new LessThan100();

        Predicate<Integer> combined = new CombinedPredicate<>(greaterThan20, lessThan100);

        // want 20 < x < 100
        assertThat(combined.test(19), is(false));
        assertThat(combined.test(20), is(false));
        assertThat(combined.test(21), is(true));
        assertThat(combined.test(50), is(true));
        assertThat(combined.test(99), is(true));
        assertThat(combined.test(100), is(false));
        assertThat(combined.test(101), is(false));
    }

    @Test
    public void combinedPredicateShortCircuitsWhenPossible() {

        GreaterThan20 greaterThan20 = new GreaterThan20();
        LessThan100 lessThan100 = new LessThan100();

        Predicate<Integer> combined = new CombinedPredicate<>(greaterThan20, lessThan100);

        assertThat(combined.test(0), is(false));

        // testing 0 SHOULD NOT require accessing the 2nd predicate because the 1st predicate fails
        assertThat(greaterThan20.counter, is(1));
        assertThat(lessThan100.counter, is(0));

        assertThat(combined.test(50), is(true));

        // testing 50 SHOULD require accessing the 2nd predicate because the 1st predicate passes
        assertThat(greaterThan20.counter, is(2));
        assertThat(lessThan100.counter, is(1));
    }

    @Test
    public void components_providesOriginalPredicates() {

        GreaterThan20 greaterThan20 = new GreaterThan20();
        LessThan100 lessThan100 = new LessThan100();

        CombinedPredicate<Integer> combined = new CombinedPredicate<>(greaterThan20, lessThan100);

        List<Predicate<Integer>> components = combined.components();

        assertThat(components.get(0), is(greaterThan20));
        assertThat(components.get(1), is(lessThan100));
    }

    static class GreaterThan20 implements Predicate<Integer> {

        int counter = 0;

        @Override
        public boolean test(Integer t) {
            counter++;
            return t > 20;
        }
    }

    static class LessThan100 implements Predicate<Integer> {

        int counter = 0;

        @Override
        public boolean test(Integer t) {
            counter++;
            return t < 100;
        }
    }
}
