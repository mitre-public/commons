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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class CompositeCleanerTest {

    // an example DataCleaner that ONLY works on Integers
    static class RemoveOddIntegers implements DataCleaner<Integer> {

        @Override
        public Optional<Integer> clean(Integer number) {
            return (number % 2 == 1) ? Optional.empty() : Optional.of(number);
        }
    }

    // an example DataCleaner that ONLY works on Integers
    static class RemoveNegativeIntegers implements DataCleaner<Integer> {

        @Override
        public Optional<Integer> clean(Integer number) {
            return (number < 0) ? Optional.empty() : Optional.of(number);
        }
    }

    // an example DataCleaner that ONLY works on Doubles
    static class TruncateDouble implements DataCleaner<Double> {

        @Override
        public Optional<Double> clean(Double data) {
            return Optional.of(Math.floor(data));
        }
    }

    // an example DataCleaner that works on all Numbers
    static class RemoveNegativeNumbers implements DataCleaner<Number> {

        @Override
        public Optional<Number> clean(Number number) {
            return (number.doubleValue() < 0) ? Optional.empty() : Optional.of(number);
        }
    }

    @Test
    public void testConstructor_1() {

        List<DataCleaner<? super Integer>> cleaningSteps = newArrayList();
        cleaningSteps.add(new RemoveOddIntegers());
        cleaningSteps.add(new RemoveNegativeNumbers());

        CompositeCleaner<Integer> cleaner = new CompositeCleaner<>(cleaningSteps);

        assertThat(cleaner.clean(12).get(), is(12));
        assertThat(cleaner.clean(11), is(Optional.empty()));
        assertThat(cleaner.clean(-2), is(Optional.empty()));
    }

    @Test
    public void testConstructor_2() {

        CompositeCleaner<Integer> cleaner = CompositeCleaner.of(new RemoveOddIntegers(), new RemoveNegativeNumbers());

        assertThat(cleaner.clean(12).get(), is(12));
        assertThat(cleaner.clean(11), is(Optional.empty()));
        assertThat(cleaner.clean(-2), is(Optional.empty()));
    }

    @Test
    public void testBulkClean_onlyIntegers() {

        // Note: both of these DataCleaners are DataCleaner<Integer>
        CompositeCleaner<Integer> cleaner = CompositeCleaner.of(new RemoveOddIntegers(), new RemoveNegativeIntegers());

        List<Integer> list = newArrayList(-2, -1, 0, 1, 2, 3, 4);

        List<Integer> result = cleaner.clean(list);

        assertThat(result.size(), is(3)); // "list should only contain 0, 2, 4
        assertTrue(result.contains(0));
        assertTrue(result.contains(2));
        assertTrue(result.contains(4));
    }

    @Test
    public void testBulkClean_compatibleGenericTypes() {

        // Note: One DataCleaner is a DataCleaner<Integer> and the other is a DataCleaner<Number>
        CompositeCleaner<Integer> cleaner = CompositeCleaner.of(new RemoveOddIntegers(), new RemoveNegativeNumbers());

        List<Integer> list = newArrayList(-2, -1, 0, 1, 2, 3, 4);

        List<Integer> result = cleaner.clean(list);

        assertThat(result.size(), is(3)); // list should only contain 0, 2, 4
        assertTrue(result.contains(0));
        assertTrue(result.contains(2));
        assertTrue(result.contains(4));
    }

    @Test
    public void testEmptyListShouldWork() {
        List<DataCleaner<Integer>> filters = newArrayList();
        CompositeCleaner<Integer> cleaner = new CompositeCleaner(filters);

        Optional<Integer> result = cleaner.clean(22);

        assertTrue(result.isPresent());
        assertThat(result.get(), is(22));
    }
}
