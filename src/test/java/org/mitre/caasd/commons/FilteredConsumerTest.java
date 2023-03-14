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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.Functions.ALWAYS_FALSE;
import static org.mitre.caasd.commons.Functions.ALWAYS_TRUE;
import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class FilteredConsumerTest {

    //Counts calls to "test".
    static class CountingPredicate implements Predicate<String> {

        boolean returnValue;

        CountingPredicate(boolean returnValue) {
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
    public void inputsAreSubmittedToFilter() {

        CountingPredicate filter = new CountingPredicate(true);

        FilteredConsumer<String> filterConsumer = new FilteredConsumer<>(filter, NO_OP_CONSUMER);

        assertThat(filter.numCallsToTest, is(0)); //"Not called yet"

        filterConsumer.accept("hello");

        assertThat(filter.numCallsToTest, is(1)); // "Called exactly once now"
    }

    @Test
    public void testFailingFilterDoesntForwardToConsumer() {

        CountingConsumer<String> counter = new CountingConsumer<>(NO_OP_CONSUMER);

        FilteredConsumer<String> filteredConsumer = new FilteredConsumer<>(ALWAYS_FALSE, counter);

        assertThat("The consumer has not been called before any input", counter.numCallsToAccept(), is(0));
        filteredConsumer.accept("testString");
        assertThat("The consumer was not called after failing input", counter.numCallsToAccept(), is(0));
    }

    @Test
    public void testPassingFilterForwardsToConsumer() {

        CountingConsumer<String> testConsumer = new CountingConsumer<>(NO_OP_CONSUMER);

        FilteredConsumer<String> filteredConsumer = new FilteredConsumer<>(ALWAYS_TRUE, testConsumer);

        assertThat("The consumer has not been called before any input", testConsumer.numCallsToAccept(), is(0));
        filteredConsumer.accept("testString");
        assertThat("The consumer was called after passing input", testConsumer.numCallsToAccept(), is(1));
    }

    @Test
    public void whenTrueConsumerReceivesWhenTrue() {

        FilteredConsumer<Integer> fc = new FilteredConsumer<>(
            ALWAYS_TRUE,
            new CountingConsumer(NO_OP_CONSUMER),
            NO_OP_CONSUMER
        );

        fc.accept(12);

        assertThat(((CountingConsumer<Integer>) fc.whenTrue()).numCallsToAccept(), is(1));
    }

    @Test
    public void whenFalseConsumerReceivesWhenFalse() {

        FilteredConsumer<Integer> fc = new FilteredConsumer<>(
            ALWAYS_FALSE,
            NO_OP_CONSUMER,
            new CountingConsumer<>(NO_OP_CONSUMER)
        );

        fc.accept(12);

        assertThat(((CountingConsumer<Integer>) fc.whenFalse()).numCallsToAccept(), is(1));
    }

    @Test
    public void predicateIsRequired() {
        assertThrows(
            NullPointerException.class,
            () -> new FilteredConsumer<>(null, NO_OP_CONSUMER, NO_OP_CONSUMER)
        );
    }

    @Test
    public void whenTrueConsumerIsRequired() {
        assertThrows(
            NullPointerException.class,
            () -> new FilteredConsumer<>(ALWAYS_TRUE, null, NO_OP_CONSUMER)
        );
    }

    @Test
    public void whenFalseConsumerIsRequired() {
        assertThrows(
            NullPointerException.class, ()
            -> new FilteredConsumer<>(ALWAYS_TRUE, NO_OP_CONSUMER, null)
        );
    }
}
