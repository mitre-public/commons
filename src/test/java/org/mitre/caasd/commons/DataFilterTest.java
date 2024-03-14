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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingLinkedList;

import java.util.Optional;
import java.util.function.Predicate;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingLinkedList;

import org.junit.jupiter.api.Test;

public class DataFilterTest {

    @Test
    public void testFilteringWorks() {
        DataFilter<Integer> filter = new DataFilter<>(isPositive());

        Optional<Integer> ouput1 = filter.clean(20);
        Optional<Integer> ouput2 = filter.clean(-10);

        assertTrue(ouput1.isPresent());
        assertEquals(20, (int) ouput1.get());
        assertFalse(ouput2.isPresent());
    }

    @Test
    public void testRemovedItemsAreForwarded() {

        ConsumingLinkedList<Integer> onRemoval = newConsumingLinkedList();

        DataFilter<Integer> filter = new DataFilter<>(isPositive(), onRemoval);

        filter.clean(20);
        filter.clean(-10);
        filter.clean(-5);
        filter.clean(18);

        assertThat(onRemoval, hasSize(2));
        assertThat(onRemoval.getFirst(), equalTo(-10));
        assertThat(onRemoval.getLast(), equalTo(-5));
    }

    static Predicate<Integer> isPositive() {
        return (Integer a) -> a > 0;
    }
}
