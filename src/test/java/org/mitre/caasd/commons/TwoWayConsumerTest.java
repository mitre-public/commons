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

import org.junit.jupiter.api.Test;

public class TwoWayConsumerTest {

    @Test
    public void whenTrueConsumerRecievesWhenTrue() {

        CountingConsumer<Integer> counter = new CountingConsumer(NO_OP_CONSUMER);

        TwoWayConsumer<Integer> twc = new TwoWayConsumer(
            ALWAYS_TRUE,
            counter,
            NO_OP_CONSUMER
        );

        twc.accept(12);

        assertThat(counter.numCallsToAccept(), is(1));
    }

    @Test
    public void whenFalseConsumerRecievesWhenFalse() {

        CountingConsumer<Integer> counter = new CountingConsumer(NO_OP_CONSUMER);

        TwoWayConsumer<Integer> twc = new TwoWayConsumer(
            ALWAYS_FALSE,
            NO_OP_CONSUMER,
            counter
        );

        twc.accept(12);

        assertThat(counter.numCallsToAccept(), is(1));
    }

    @Test
    public void predicateIsRequired() {
        assertThrows(NullPointerException.class,
            () -> new TwoWayConsumer(null, NO_OP_CONSUMER, NO_OP_CONSUMER)
        );
    }

    @Test
    public void whenTrueConsumerIsRequired() {
        assertThrows(NullPointerException.class,
            () -> new TwoWayConsumer(ALWAYS_TRUE, null, NO_OP_CONSUMER)
        );
    }

    @Test
    public void whenFalseConsumerIsRequired() {
        assertThrows(NullPointerException.class, ()
            -> new TwoWayConsumer(ALWAYS_TRUE, NO_OP_CONSUMER, null)
        );
    }
}
