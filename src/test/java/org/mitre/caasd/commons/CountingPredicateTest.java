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

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class CountingPredicateTest {

    @Test
    public void fromCreatesWorkingPredicate() {

        Predicate<Integer> greaterThanZero = (Integer t) -> t > 0;

        CountingPredicate<Integer> counter = CountingPredicate.from(greaterThanZero);

        assertThat(counter.count(), is(0L));
        assertThat(counter.trueCount(), is(0L));
        assertThat(counter.falseCount(), is(0L));

        counter.test(5);

        assertThat(counter.count(), is(1L));
        assertThat(counter.trueCount(), is(1L));
        assertThat(counter.falseCount(), is(0L));

        counter.test(-5);

        assertThat(counter.count(), is(2L));
        assertThat(counter.trueCount(), is(1L));
        assertThat(counter.falseCount(), is(1L));

        counter.resetCounts();

        assertThat(counter.count(), is(0L));
        assertThat(counter.trueCount(), is(0L));
        assertThat(counter.falseCount(), is(0L));
    }
}
