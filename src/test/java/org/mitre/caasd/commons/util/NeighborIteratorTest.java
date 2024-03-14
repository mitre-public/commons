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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class NeighborIteratorTest {

    @Test
    public void hasNextWorks() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThat(neighborIterator.hasNext(), is(true));
        neighborIterator.next(); // 1 & 2
        assertThat(neighborIterator.hasNext(), is(true));
        neighborIterator.next(); // 2 & 3
        assertThat(neighborIterator.hasNext(), is(false));
    }

    @Test
    public void hasNextWorks_emptyList() {
        List<Integer> numbers = newArrayList();
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThat(neighborIterator.hasNext(), is(false));
    }

    @Test
    public void isEmptyWorks_emptyList() {
        List<Integer> numbers = newArrayList();
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThat(neighborIterator.wasEmpty(), is(true));
    }

    @Test
    public void isEmptyWorks_listWithContent() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThat(neighborIterator.wasEmpty(), is(false));
    }

    @Test
    public void hasNextWorks_nullEntry() {
        List<Integer> numbers = newArrayList(1, null, 3);
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThat(neighborIterator.hasNext(), is(true));
        neighborIterator.next(); // 1 & null
        assertThat(neighborIterator.hasNext(), is(true));
        neighborIterator.next(); // null & 3
        assertThat(neighborIterator.hasNext(), is(false));
    }

    @Test
    public void nextGivesExpectedElement() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        IterPair<Integer> first = neighborIterator.next();
        assertThat(first.prior(), is(1));
        assertThat(first.current(), is(2));

        IterPair<Integer> second = neighborIterator.next();
        assertThat(second.prior(), is(2));
        assertThat(second.current(), is(3));

        assertThat(neighborIterator.hasNext(), is(false));
    }

    @Test
    public void nextGivesExpectedElement_nullEntry() {
        List<Integer> numbers = newArrayList(1, null, 3);
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        IterPair<Integer> first = neighborIterator.next();
        assertThat(first.prior(), is(1));
        assertThat(first.current(), nullValue());

        IterPair<Integer> second = neighborIterator.next();
        assertThat(second.prior(), nullValue());
        assertThat(second.current(), is(3));

        assertThat(neighborIterator.hasNext(), is(false));
    }

    @Test
    public void noSuchElementExceptionIfNoNextElement() {
        List<Integer> numbers = newArrayList();
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThrows(NoSuchElementException.class, () -> neighborIterator.next());
    }

    @Test
    public void canGetTheLoanElement() {
        List<Integer> numbers = newArrayList(1);
        NeighborIterator<Integer> neighborIterator = new NeighborIterator(numbers.iterator());

        assertThat(neighborIterator.hasNext(), is(false));
        assertThat(neighborIterator.hadExactlyOneElement(), is(true));
        assertThat(neighborIterator.getSoleElement(), is(1));
    }
}
