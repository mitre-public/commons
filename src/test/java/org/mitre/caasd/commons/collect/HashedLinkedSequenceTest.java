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

package org.mitre.caasd.commons.collect;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mitre.caasd.commons.collect.HashedLinkedSequence.newHashedLinkedSequence;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class HashedLinkedSequenceTest {

    @Test
    public void testBasicUsage() {

        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);
        list.add(12);
        list.add(13);

        assertThat(list.size(), is(3));
        assertThat(list.getFirst(), is(5));
        assertThat(list.getLast(), is(13));

        assertThat(list.getElementAfter(5), is(12));
        assertThat(list.getElementBefore(13), is(12));

        assertThat(list.contains(5), is(true));
        assertThat(list.contains(12), is(true));
        assertThat(list.contains(13), is(true));
    }

    @Test
    public void firstItemIsFirstAndLast() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence();
        list.add(5);
        assertThat(list.getFirst(), is(5));
        assertThat(list.getLast(), is(5));
    }

    @Test
    public void testAddFirst_null() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();

        assertThrows(NullPointerException.class,
            () -> list.addFirst(null)
        );
    }

    @Test
    public void testAddFirst_duplicateItem() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //duplicate should fail
        assertThrows(IllegalArgumentException.class,
            () -> list.addFirst(5)
        );
    }

    @Test
    public void addFirstToEmptyListWorks() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence();
        list.addFirst(12);
        assertThat(list.getFirst(), is(12));
        assertThat(list.getLast(), is(12));
        assertThat(list.size(), is(1));
        assertThat(list.isEmpty(), is(false));
    }

    @Test
    public void testAddFirst_happyPath() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(12);
        list.addFirst(1);

        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(12));
        assertThat(list.size(), is(2));
        assertThat(list.getElementAfter(1), is(12));
        assertThat(list.getElementBefore(12), is(1));
    }

    @Test
    public void testGetElementAfter_itemNotInList() {

        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //12 is not in the list -- so this call is undefined
        assertThrows(IllegalArgumentException.class,
            () -> list.getElementAfter(12)
        );
    }

    @Test
    public void testGetElementAfter_atEndOfList() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //at the end of the list, nothing to get
        assertThrows(NoSuchElementException.class,
            () -> list.getElementAfter(5)
        );
    }

    @Test
    public void testGetElementAfter() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);
        list.add(22);
        assertThat(list.getElementAfter(5), is(22));
    }

    @Test
    public void testGetElementBefore_itemNotInList() {

        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //12 is not in the list -- so this call is undefined
        assertThrows(IllegalArgumentException.class,
            () -> list.getElementBefore(12)
        );
    }

    @Test
    public void testGetElementBefore_atFrontOfList() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //at the front of the list, nothing to get
        assertThrows(NoSuchElementException.class,
            () -> list.getElementBefore(5)
        );
    }

    @Test
    public void testGetElementBefore() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);
        list.add(22);
        assertThat(list.getElementBefore(22), is(5));
    }

    @Test
    public void testInsertAfter_referenceDoesNotExist() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();

        //12 does not exist
        assertThrows(IllegalArgumentException.class,
            () -> list.insertAfter(5, 12)
        );
    }

    @Test
    public void testInsertAfter_alreadyPresent() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //5 already exists
        assertThrows(IllegalArgumentException.class,
            () -> list.insertAfter(5, 5)
        );
    }

    @Test
    public void testInsertAfter_happyPath() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);
        list.insertAfter(12, 5);

        assertThat(list.getFirst(), is(5));
        assertThat(list.getLast(), is(12));
        assertThat(list.getElementAfter(5), is(12));
        assertThat(list.size(), is(2));

        assertThat(list.contains(12), is(true));
    }

    @Test
    public void insertAfter_betweenTwoItems() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 2);
        list.insertAfter(12, 1);

        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(2));
        assertThat(list.getElementAfter(1), is(12));
        assertThat(list.getElementBefore(2), is(12));
        assertThat(list.size(), is(3));

        assertThat(list.contains(12), is(true));
    }

    @Test
    public void insertAfterLastElementOccursProperly() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 2, 3);
        list.insertAfter(22, 3);
        assertThat(list.size(), is(4));
        assertThat(list.getLast(), is(22));
        assertThat(list.getElementAfter(3), is(22));
        assertThat(list.getElementBefore(22), is(3));
    }

    @Test
    public void testInsertBefore_referenceDoesNotExist() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();

        //12 does not exist
        assertThrows(IllegalArgumentException.class,
            () -> list.insertBefore(5, 12)
        );
    }

    @Test
    public void testInsertBefore_alreadyPresent() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(5);

        //5 already exists
        assertThrows(IllegalArgumentException.class,
            () -> list.insertBefore(5, 5)
        );
    }

    @Test
    public void insertBeforeFirstElementWorks() {

        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 2);
        list.insertBefore(22, 1); //should get 22, 1, 2

        assertThat(list.size(), is(3));
        assertThat(list.getFirst(), is(22));
        assertThat(list.getLast(), is(2));
        assertThat(list.getElementAfter(22), is(1));
        assertThat(list.getElementAfter(1), is(2));
        assertThat(list.getElementBefore(2), is(1));
        assertThat(list.getElementBefore(1), is(22));
    }

    @Test
    public void testInsertBefore_happyPath() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(5);
        list.insertBefore(12, 5); //put 12 in between 1 and 5

        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(5));
        assertThat(list.getElementAfter(1), is(12));
        assertThat(list.getElementBefore(5), is(12));

        assertThat(list.size(), is(3));

        assertThat(list.contains(12), is(true));
    }

    @Test
    public void testRemove_first() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(3);

        boolean result = list.remove(1);

        assertThat(result, is(true));
        assertThat(list.size(), is(2));
        assertThat(list.getFirst(), is(2));
        assertThat(list.getLast(), is(3));
        assertThat(list.getElementAfter(2), is(3));
        assertThat(list.getElementBefore(3), is(2));

        assertThat(list.contains(1), is(false));
    }

    @Test
    public void testRemove_last() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(3);

        boolean result = list.remove(3);

        assertThat(result, is(true));
        assertThat(list.size(), is(2));
        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(2));
        assertThat(list.getElementAfter(1), is(2));
        assertThat(list.getElementBefore(2), is(1));

        assertThat(list.contains(3), is(false));
    }

    @Test
    public void testRemove_middle() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(3);

        boolean result = list.remove(2);

        assertThat(result, is(true));
        assertThat(list.size(), is(2));
        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(3));
        assertThat(list.getElementAfter(1), is(3));
        assertThat(list.getElementBefore(3), is(1));

        assertThat(list.contains(2), is(false));
    }

    @Test
    public void removingLastItemLeavesEmptyList() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(22);
        assertThat(list.contains(22), is(true));
        assertThat(list.isEmpty(), is(false));

        list.remove(22);

        assertThat(list.contains(22), is(false));
        assertThat(list.isEmpty(), is(true));

        try {
            list.getFirst(); //should throw NoSuchElementException
            fail();
        } catch (NoSuchElementException ex) {
            //exception is expected
        }
        try {
            list.getLast(); //should throw NoSuchElementException
            fail();
        } catch (NoSuchElementException ex) {
            //exception is expected
        }
    }

    @Test
    public void testRemove_miss() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(3);

        boolean result = list.remove(55);

        assertThat(result, is(false));
        assertThat(list.size(), is(3));
        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(3));
    }

    @Test
    public void iteratorProvidesAWorkingIterator_emptyList() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        Iterator<Integer> iter = list.iterator();

        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void iteratorProvidesAWorkingIterator_whenDataIsThere() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(3);

        Iterator<Integer> iter = list.iterator();

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(1));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(2));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(3));

        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void iteratorThrowsNoSuchElementException() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        Iterator<Integer> iter = list.iterator();

        assertThat(iter.hasNext(), is(false));

        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void iteratorThrowsConcurrentModification_add() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.add(22);

        //breaks because the list was editted after the iterator was made
        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void iteratorThrowsConcurrentModification_addLast() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.addLast(22);

        //breaks because the list was editted after the iterator was made
        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void iteratorThrowsConcurrentModification_addFirst() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.addFirst(22);

        //breaks because the list was editted after the iterator was made
        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void iteratorThrowsConcurrentModification_remove() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.remove(1);

        //breaks because the list was editted after the iterator was made
        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void iteratorThrowsConcurrentModification_insertBefore() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.insertBefore(101, 1);

        //breaks because the list was editted after the iterator was made
        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void iteratorThrowsConcurrentModification_insertAfter() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.insertAfter(101, 1);

        //breaks because the list was editted after the iterator was made
        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void iteratorDoesNotFailsWhenRemoveDoesNothing() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);

        Iterator<Integer> iter = list.iterator();

        list.remove(22);

        //remove did nothing, no ConcurrentModificationException should be thrown
        assertDoesNotThrow(() -> iter.next());
    }

    @Test
    public void collectionsToArrayMethodWorks() {

        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(5);

        Object[] array = list.toArray();
        assertThat(array.length, is(3));
        assertThat((Integer) array[0], is(1));
        assertThat((Integer) array[1], is(2));
        assertThat((Integer) array[2], is(5));
    }

    @Test
    public void collectionTypeToArrayMethodWorks() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(5);
        Integer[] array = list.toArray(new Integer[]{});
        assertThat(array.length, is(3));
        assertThat(array[0], is(1));
        assertThat(array[1], is(2));
        assertThat(array[2], is(5));
    }

    @Test
    public void contains_happyPath() {

        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(5);

        assertThat(list.contains(0), is(false));
        assertThat(list.contains(1), is(true));
        assertThat(list.contains(2), is(true));
        assertThat(list.contains(3), is(false));
        assertThat(list.contains(4), is(false));
        assertThat(list.contains(5), is(true));
        assertThat(list.contains(6), is(false));
    }

    @Test
    public void contains_badInput() {
        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        list.add(1);
        list.add(2);
        list.add(5);

        assertThat(list.contains(null), is(false));
        assertThat(list.contains("hello"), is(false));
    }

    @Test
    public void addAll_addsData() {

        HashedLinkedSequence<Integer> list = new HashedLinkedSequence<>();
        boolean result = list.addAll(newArrayList(1, 2, 5));

        assertThat(list.contains(1), is(true));
        assertThat(list.contains(2), is(true));
        assertThat(list.contains(5), is(true));
        assertThat(list, hasSize(3));
        assertThat(result, is(true));
    }

    @Test
    public void arrayFactoryMethodWorks() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 6, 12, 22);

        assertThat(list.contains(1), is(true));
        assertThat(list.contains(6), is(true));
        assertThat(list.contains(12), is(true));
        assertThat(list.contains(22), is(true));
        assertThat(list, hasSize(4));
        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(22));
        assertThat(list.getElementAfter(1), is(6));
        assertThat(list.getElementAfter(6), is(12));
        assertThat(list.getElementAfter(12), is(22));
        assertThat(list.getElementBefore(22), is(12));
        assertThat(list.getElementBefore(12), is(6));
        assertThat(list.getElementBefore(6), is(1));
    }

    @Test
    public void iterableFactoryMethodWorks() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(
            newArrayList(1, 6, 12, 22) //this an iterable
        );

        assertThat(list.contains(1), is(true));
        assertThat(list.contains(6), is(true));
        assertThat(list.contains(12), is(true));
        assertThat(list.contains(22), is(true));
        assertThat(list, hasSize(4));
        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(22));
        assertThat(list.getElementAfter(1), is(6));
        assertThat(list.getElementAfter(6), is(12));
        assertThat(list.getElementAfter(12), is(22));
        assertThat(list.getElementBefore(22), is(12));
        assertThat(list.getElementBefore(12), is(6));
        assertThat(list.getElementBefore(6), is(1));
    }

    @Test
    public void clearRemovesAllData() {

        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 6, 12, 22);

        assertThat(list.contains(1), is(true));
        assertThat(list.contains(6), is(true));
        assertThat(list.contains(12), is(true));
        assertThat(list.contains(22), is(true));
        assertThat(list, hasSize(4));
        assertThat(list.isEmpty(), is(false));

        list.clear(); //apply clear

        assertThat(list.contains(1), is(false));
        assertThat(list.contains(6), is(false));
        assertThat(list.contains(12), is(false));
        assertThat(list.contains(22), is(false));
        assertThat(list, hasSize(0));
        assertThat(list.isEmpty(), is(true));

        try {
            list.getFirst();
            fail();
        } catch (NoSuchElementException ex) {
            //this exception is expected
        }

        try {
            list.getLast();
            fail();
        } catch (NoSuchElementException ex) {
            //this exception is expected
        }
    }

    @Test
    public void containsAll_worksAsExpected() {
        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 2, 3, 4);

        assertThat(list.containsAll(newArrayList(1, 2, 3, 4)), is(true));
        assertThat(list.containsAll(newArrayList(1, 2, 3)), is(true));
        assertThat(list.containsAll(newArrayList(1, 2)), is(true));
        assertThat(list.containsAll(newArrayList(1)), is(true));

        assertThat(list.containsAll(newArrayList(1, 2, 5)), is(false));
        assertThat(list.containsAll(newArrayList(5)), is(false));
        assertThat(list.containsAll(newArrayList("String")), is(false));
    }

    @Test
    public void removeAll_worksAsExpected() {

        HashedLinkedSequence<Integer> list = newHashedLinkedSequence(1, 2, 3, 4, 5, 6);

        boolean modified = list.removeAll(newArrayList("String1", "String2"));
        assertThat(modified, is(false));

        assertThat(list, hasSize(6));

        modified = list.removeAll(newArrayList(2, 4, 6, 22));
        assertThat(modified, is(true));

        assertThat(list.contains(1), is(true));
        assertThat(list.contains(2), is(false));
        assertThat(list.contains(3), is(true));
        assertThat(list.contains(4), is(false));
        assertThat(list.contains(5), is(true));
        assertThat(list.contains(6), is(false));

        assertThat(list.size(), is(3));
        assertThat(list.getFirst(), is(1));
        assertThat(list.getLast(), is(5));
        assertThat(list.getElementAfter(1), is(3));
        assertThat(list.getElementAfter(3), is(5));
        assertThat(list.getElementBefore(5), is(3));
        assertThat(list.getElementBefore(3), is(1));
    }

    @Test
    public void removeAllWorksWhenGivenBigCollectionToRemove() {

        HashedLinkedSequence<Integer> seq = newHashedLinkedSequence(1, 2, 3);

        boolean modified = seq.removeAll(newArrayList(2, 4, 6, 22, 35));

        assertThat(modified, is(true));
        assertThat(seq.contains(2), is(false));
        assertThat(seq.size(), is(2));
    }

    @Test
    public void retainAllWorksAsExpect() {

        HashedLinkedSequence<Integer> seq = newHashedLinkedSequence(1, 2, 3);

        boolean modified = seq.retainAll(newArrayList(2, 4, 6, 22, 35));

        assertThat(modified, is(true));
        assertThat(seq.contains(1), is(false));
        assertThat(seq.contains(2), is(true));
        assertThat(seq.contains(3), is(false));
        assertThat(seq.size(), is(1));
    }

    @Test
    public void removingFirstElementViaIteratorWorks() {
        HashedLinkedSequence<Integer> seq = newHashedLinkedSequence(1, 2, 3);
        Iterator<Integer> iter = seq.iterator();
        assertThat(seq, hasSize(3));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(1));

        iter.remove();

        assertThat(seq.getFirst(), is(2));
        assertThat(seq.getLast(), is(3));
        assertThat(seq.getElementAfter(2), is(3));
        assertThat(seq.getElementBefore(3), is(2));
        assertThat(seq.contains(1), is(false));
        assertThat(seq.size(), is(2));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(2));
    }

    @Test
    public void removingLastElementViaIteratorWorks() {
        HashedLinkedSequence<Integer> seq = newHashedLinkedSequence(1, 2, 3);
        Iterator<Integer> iter = seq.iterator();
        assertThat(seq, hasSize(3));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(1));
        assertThat(iter.next(), is(2));
        assertThat(iter.next(), is(3));

        iter.remove();

        assertThat(seq.getFirst(), is(1));
        assertThat(seq.getLast(), is(2));
        assertThat(seq.getElementAfter(1), is(2));
        assertThat(seq.getElementBefore(2), is(1));
        assertThat(seq.contains(3), is(false));
        assertThat(seq.size(), is(2));

        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void removingMiddleElementViaIteratorWorks() {
        HashedLinkedSequence<Integer> seq = newHashedLinkedSequence(1, 2, 3);
        Iterator<Integer> iter = seq.iterator();
        assertThat(seq, hasSize(3));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(1));
        assertThat(iter.next(), is(2));

        iter.remove();

        assertThat(seq.getFirst(), is(1));
        assertThat(seq.getLast(), is(3));
        assertThat(seq.getElementAfter(1), is(3));
        assertThat(seq.getElementBefore(3), is(1));
        assertThat(seq.contains(2), is(false));
        assertThat(seq.size(), is(2));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(3));
    }

    @Test
    public void removingOnlyElementViaIteratorWorks() {
        HashedLinkedSequence<Integer> seq = newHashedLinkedSequence(1);
        Iterator<Integer> iter = seq.iterator();
        assertThat(seq, hasSize(1));

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(1));

        iter.remove();

        assertThat(seq.contains(1), is(false));
        assertThat(seq.isEmpty(), is(true));

        assertThat(iter.hasNext(), is(false));
    }
}
