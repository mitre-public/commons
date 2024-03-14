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

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A NeighborIterator is an Iterator decorated to always return two consecutive elements in the
 * iteration. The goal is to simplify code that requires access to the "prior element" and the
 * "current element" at the same time.
 * <p>
 * For example, say you want to compute the difference between two consecutive elements in a list of
 * numbers. Doing this careful requires handling 4 separate cases. Case 1: you are at the front of
 * the sequence and you do not have a "prior" element to compute a different. Case 2: you are in the
 * middle of the sequence and there is a "current" and "prior" element so computing a difference is
 * possible. Case 3: you are at the end of the sequence and you cannot compute a difference because
 * you do not have a "next" element. Case 4: the sequence contained no data whatsoever.
 * <p>
 * It is annoying, and error-prone, to correctly keep track of this state manually. Loops built
 * using manual consecutive element tracking are difficult to read because the important code is
 * occluded with basic housekeeping code. A NeighborIterator improves code clarity, and reliability,
 * because the need for housekeeping is eliminated because the NeighborIterator always gives you
 * exactly two elements.
 * <p>
 * Note: Inputs with 0 elements and 1 element BOTH begin with "hasNext() = false".  Use the
 * "wasEmpty()" and "hadExactlyOneElement()" methods to differentiate between these cases. Be aware
 * that when the input iteration contains exactly one element you won't be able to access that
 * element using the typical calls to "next()" (because these calls always returns an IterPair with
 * two valid elements).  The method "getSoleElement()" is provide for this situation.
 * <p>
 * Note: This class is similar in spirit to Guava's PeekingIterator. That class provides easy access
 * to the next element via calls to "peek()".
 */
public class NeighborIterator<T> implements Iterator<IterPair<T>> {

    private final Iterator<T> iterator;

    private T prior;
    private T current;
    private int elementCount = 0;

    @SafeVarargs
    public NeighborIterator(T... array) {
        this(asList(array));
    }

    public NeighborIterator(Collection<T> collection) {
        this(collection.iterator());
    }

    public NeighborIterator(Iterator<T> iter) {
        this.iterator = requireNonNull(iter);
        this.prior = null;

        /*
         * Take the first iteration step to ensure the first call to "next()" returns
         * OrderedPair.of(element0, element1) and not OrderedPair.of(null, element0)
         */
        if (iter.hasNext()) {
            this.next();
        }
    }

    public static <T> NeighborIterator<T> newNeighborIterator(Iterator<T> iter) {
        return new NeighborIterator<>(iter);
    }

    public static <T> NeighborIterator<T> newNeighborIterator(Collection<T> collection) {
        return new NeighborIterator<>(collection);
    }

    @SafeVarargs
    public static <T> NeighborIterator<T> newNeighborIterator(T... array) {
        return new NeighborIterator<>(array);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public IterPair<T> next() {
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }

        prior = current;
        current = iterator.next();
        elementCount++;
        return new IterPair<>(prior, current);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean wasEmpty() {
        return !hasNext() && elementCount == 0;
    }

    public boolean hadExactlyOneElement() {
        return !hasNext() && elementCount == 1;
    }

    public T getSoleElement() {
        if (hadExactlyOneElement()) {
            return current;
        } else {
            throw new IllegalStateException("Iterator did not have exactly one element");
        }
    }
}
