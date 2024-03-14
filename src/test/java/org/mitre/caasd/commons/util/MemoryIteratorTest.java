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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.util.MemoryIterator.newMemoryIterator;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class MemoryIteratorTest {

    @Test
    public void hasNextWorks() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.hasNext(), is(true));
        memIter.next(); // 1
        assertThat(memIter.hasNext(), is(true));
        memIter.next(); // 2
        assertThat(memIter.hasNext(), is(true));
        memIter.next(); // 3
        assertThat(memIter.hasNext(), is(false));
    }

    @Test
    public void hasNextWorks_emptyList() {
        List<Integer> numbers = newArrayList();
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.hasNext(), is(false));
    }

    @Test
    public void hasNextWorks_nullEntry() {
        List<Integer> numbers = newArrayList(1, null, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.hasNext(), is(true));
        memIter.next(); // 1
        assertThat(memIter.hasNext(), is(true));
        assertNull(memIter.next()); // null
        assertThat(memIter.hasNext(), is(true));
        memIter.next(); // 3
        assertThat(memIter.hasNext(), is(false));
    }

    @Test
    public void hasPriorWorks() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.hasPrior(), is(false));
        memIter.next(); // 1
        assertThat(memIter.hasPrior(), is(true));
        memIter.next(); // 2
        assertThat(memIter.hasPrior(), is(true));
        memIter.next(); // 3
        assertThat(memIter.hasPrior(), is(true));
    }

    @Test
    public void hasPriorWorks_emptyList() {
        List<Integer> numbers = newArrayList();
        MemoryIterator<Integer> memIter = new MemoryIterator(numbers.iterator());

        assertThat(memIter.hasPrior(), is(false));
    }

    @Test
    public void hasPriorWorks_nullEntry() {
        List<Integer> numbers = newArrayList(1, null, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator(numbers.iterator());

        assertThat(memIter.hasPrior(), is(false));
        memIter.next(); // 1
        assertThat(memIter.hasPrior(), is(true));
        memIter.next(); // null
        assertThat(memIter.hasPrior(), is(true));
        memIter.next(); // 3
        assertThat(memIter.hasPrior(), is(true));
    }

    @Test
    public void nextGivesExpectedElement() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.next(), is(1));
        assertThat(memIter.next(), is(2));
        assertThat(memIter.next(), is(3));
    }

    @Test
    public void nextGivesExpectedElement_nullEntry() {
        List<Integer> numbers = newArrayList(1, null, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.next(), is(1));
        assertNull(memIter.next());
        assertThat(memIter.next(), is(3));
    }

    @Test
    public void noSuchElementExceptionIfNoNextElement() {
        List<Integer> numbers = newArrayList();
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThrows(NoSuchElementException.class, () -> memIter.next());
    }

    @Test
    public void priorGivesExpectedElement() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.next(), is(1));
        assertThat(memIter.prior(), is(1));

        assertThat(memIter.next(), is(2));
        assertThat(memIter.prior(), is(2));

        assertThat(memIter.next(), is(3));
        assertThat(memIter.prior(), is(3));
    }

    @Test
    public void priorGivesExpectedElement_nullEntry() {
        List<Integer> numbers = newArrayList(1, null, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.next(), is(1));
        assertThat(memIter.prior(), is(1));

        assertNull(memIter.next());
        assertNull(memIter.prior());

        assertThat(memIter.next(), is(3));
        assertThat(memIter.prior(), is(3));
    }

    @Test
    public void noSuchElementExceptionIfNoPriorElement() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThrows(NoSuchElementException.class, () -> memIter.prior());
    }

    @Test
    public void correctFirstMiddleLastStateAtConstruction() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.atFront(), is(true));
        assertThat(memIter.inMiddle(), is(false));
        assertThat(memIter.atEnd(), is(false));
    }

    @Test
    public void correctFirstMiddleLastStateAfterCallingNext() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        memIter.next();

        assertThat(memIter.atFront(), is(false));
        assertThat(memIter.inMiddle(), is(true));
        assertThat(memIter.atEnd(), is(false));
    }

    @Test
    public void correctFirstMiddleLastStateAfterReachingEnd() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        memIter.next();
        memIter.next();
        memIter.next();

        assertThat(memIter.atFront(), is(false));
        assertThat(memIter.inMiddle(), is(false));
        assertThat(memIter.atEnd(), is(true));
    }

    @Test
    public void isEmptyWorks_onEmptyDataset() {
        List<Integer> numbers = newArrayList();
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.isEmpty(), is(true));
    }

    @Test
    public void isEmptyWorks_whenThereIsData() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator<>(numbers.iterator());

        assertThat(memIter.isEmpty(), is(false));
    }

    @Test
    public void isEmptyWorks_whileIterating() {
        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = new MemoryIterator(numbers.iterator());

        assertThat(memIter.isEmpty(), is(false));
        memIter.next(); // 1
        assertThat(memIter.isEmpty(), is(false));
        memIter.next(); // 2
        assertThat(memIter.isEmpty(), is(false));
        memIter.next(); // 3
        assertThat(memIter.isEmpty(), is(false));
    }

    @Test
    public void removeShouldAlterTheSourceData_collection() {

        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = newMemoryIterator(numbers);

        memIter.next();
        memIter.remove();

        assertThat(numbers.get(0), is(2)); // we removed 1, so the new first element is 2
    }

    @Test
    public void removeShouldAlterTheSourceData_iterator() {

        List<Integer> numbers = newArrayList(1, 2, 3);
        MemoryIterator<Integer> memIter = newMemoryIterator(numbers.iterator());

        memIter.next();
        memIter.remove();

        assertThat(numbers.get(0), is(2)); // we removed 1, so the new first element is 2
    }

    @Test
    public void removeOnArrayIsNotSupported() {

        Integer[] numbers = new Integer[] {1, 2, 3};
        MemoryIterator<Integer> memIter = newMemoryIterator(numbers);

        memIter.next();

        assertThrows(UnsupportedOperationException.class, () -> memIter.remove()); // not supported
    }
}
