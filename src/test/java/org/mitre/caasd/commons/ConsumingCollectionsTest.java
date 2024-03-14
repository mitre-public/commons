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
import static org.hamcrest.Matchers.*;
import static org.mitre.caasd.commons.ConsumingCollections.*;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class ConsumingCollectionsTest {

    @Test
    public void testConsumingArrayList() {

        ConsumingArrayList<Integer> consumer = ConsumingCollections.newConsumingArrayList();

        assertThat(consumer, is(empty()));

        consumer.accept(5);

        assertThat(consumer, hasSize(1));
        assertThat(consumer.get(0), equalTo(5));

        consumer.accept(12);

        assertThat(consumer, hasSize(2));
        assertThat(consumer.get(0), equalTo(5));
        assertThat(consumer.get(1), equalTo(12));

        consumer.clear();

        assertThat(consumer, empty());
    }

    @Test
    public void testConsumingHashSet() {
        ConsumingHashSet<Integer> consumer = newConsumingHashSet();

        assertThat(consumer, is(empty()));

        consumer.accept(5);

        assertThat(consumer, hasSize(1));
        assertThat(consumer, hasItem(5));

        consumer.accept(12);

        assertThat(consumer, hasSize(2));
        assertThat(consumer, hasItem(5));
        assertThat(consumer, hasItem(12));

        consumer.clear();

        assertThat(consumer, empty());
    }

    @Test
    public void linkedListAggregatorWorks() {

        ConsumingLinkedList<Integer> consumer = newConsumingLinkedList();

        consumer.accept(12);

        assertThat(consumer, instanceOf(LinkedList.class));
        assertThat(consumer, hasSize(1));
        assertThat(consumer.getFirst(), equalTo(12));
    }

    @Test
    public void treeSetAggregatorWorks() {

        ConsumingTreeSet<Integer> consumer = newConsumingTreeSet();

        consumer.accept(13);
        consumer.accept(12);
        consumer.accept(15);

        assertThat(consumer, instanceOf(TreeSet.class));
        assertThat(consumer, hasSize(3));
        assertThat(consumer.first(), equalTo(12));
        assertThat(consumer.last(), equalTo(15));
    }

    @Test
    public void priorityQueueAggregatorWorks() {

        ConsumingPriorityQueue<Integer> consumer = newConsumingPriorityQueue();

        consumer.accept(12);
        consumer.accept(12);
        consumer.accept(15);

        assertThat(consumer, instanceOf(PriorityQueue.class));
        assertThat(consumer, hasSize(3));
        assertThat(consumer.poll(), equalTo(12));
        assertThat(consumer.poll(), equalTo(12));
        assertThat(consumer.poll(), equalTo(15));
        assertThat(consumer, empty());
    }
}
