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

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class FilteredConsumerTest {

    /* Counts calls to "accept" */
    static class TestConsumer implements Consumer<String> {

        int numCallsToAccept = 0;

        @Override
        public void accept(String t) {
            numCallsToAccept++;
        }
    }

    ;

    /* Always returns the same value. Counts calls to "test". */
    static class TestPredicate implements Predicate<String> {

        boolean returnValue;

        TestPredicate(boolean returnValue) {
            this.returnValue = returnValue;
        }

        int numCallsToTest = 0;

        @Override
        public boolean test(String t) {
            numCallsToTest++;
            return returnValue;
        }
    }

    @Test
    public void testItemsAreSubmittedToFilter() {

        TestConsumer testConsumer = new TestConsumer();
        TestPredicate filter = new TestPredicate(true);

        FilteredConsumer filterConsumer = new FilteredConsumer(filter, testConsumer);

        assertThat(filter.numCallsToTest, is(0)); //"Not called yet"

        filterConsumer.accept("hello");

        assertThat(filter.numCallsToTest, is(1)); // "Called exactly once now"
    }

    @Test
    public void testFailingFilterDoesntForwardToConsumer() {

        TestConsumer testConsumer = new TestConsumer();
        TestPredicate testFilter = new TestPredicate(false);

        FilteredConsumer filteredConsumer = new FilteredConsumer(testFilter, testConsumer);

        assertThat("The consumer has not been called before any input", testConsumer.numCallsToAccept, is(0));
        filteredConsumer.accept("testString");
        assertThat("The consumer was not called after failing input", testConsumer.numCallsToAccept, is(0));
    }

    @Test
    public void testPassingFilterForwardsToConsumer() {

        TestConsumer testConsumer = new TestConsumer();
        TestPredicate testFilter = new TestPredicate(true);

        FilteredConsumer filteredConsumer = new FilteredConsumer(testFilter, testConsumer);

        assertThat("The consumer has not been called before any input", testConsumer.numCallsToAccept, is(0));
        filteredConsumer.accept("testString");
        assertThat("The consumer was called after passing input", testConsumer.numCallsToAccept, is(1));
    }
}
