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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A MemoryIterator is an Iterator that has been augmented to make it easy to access two consecutive
 * elements in the iteration.
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
 * occluded with basic housekeeping code. A MemoryIterator improves code clarity, and reliability,
 * because the basic housekeeping is done for you.
 * <p>
 * Note: This class is similar in spirit to Guava's PeekingIterator. That class provides easy access
 * to the next element via calls to "peek()". Whereas this class provides easy access to the last
 * element via calls to "prior()". This class also adds the methods "atFront()", "inMiddle()",
 * "atEnd()", and "isEmpty()".
 * <p>
 * This class is Deprecated in favor of NeighborIterator
 */
@Deprecated
public class MemoryIterator<T> implements Iterator<T> {

    private final Iterator<T> iter;

    private T prior;
    private boolean hasPrior;

    public MemoryIterator(Collection<T> collection) {
        this(collection.iterator());
    }

    /** The MemoryIterator created by this constructor does not support the remove() method. */
    @SafeVarargs
    public MemoryIterator(T... array) {
        this(asList(array));
    }

    public MemoryIterator(Iterator<T> iter) {
        this.iter = checkNotNull(iter);
        this.prior = null;
        this.hasPrior = false;
    }

    public static <T> MemoryIterator<T> newMemoryIterator(Iterator<T> iter) {
        return new MemoryIterator<>(iter);
    }

    public static <T> MemoryIterator<T> newMemoryIterator(Collection<T> collection) {
        return new MemoryIterator<>(collection);
    }

    /** The MemoryIterator created by this factory method does not support the remove() method. */
    @SafeVarargs
    public static <T> MemoryIterator<T> newMemoryIterator(T... array) {
        return new MemoryIterator<>(array);
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public T next() {
        prior = iter.next();
        hasPrior = true;
        return prior;
    }

    @Override
    public void remove() {
        iter.remove();
    }

    /** @return True if the wrapped Iterator provided at least one element via the "next()" method. */
    public boolean hasPrior() {
        return hasPrior;
    }

    /**
     * Provides access to the most recent result from the "next()" method. This method DOES NOT move
     * the iterator backwards.
     *
     * @return The result of the last call to "next()".
     * @throws NoSuchElementException if "next()" was not called yet.
     */
    public T prior() {
        if (hasPrior) {
            return prior;
        } else {
            throw new NoSuchElementException("There is no prior element");
        }
    }

    /**
     * @return True when there are more elements available AND zero elements have been provided via
     *     calls to next(). In other words, return true if hasNext() is true and hasPrior() is
     *     false.
     */
    public boolean atFront() {
        return hasNext() && !hasPrior;
    }

    /**
     * @return True when there are more elements available AND at least one element was provided via
     *     a call to next(). In other words, return true if hasNext() is true and hasPrior() is
     *     true.
     */
    public boolean inMiddle() {
        return hasNext() && hasPrior;
    }

    /**
     * @return True when there are no more elements AND at least one element was provided via a call
     *     to next(). In other words, return true if hasNext() is false and hasPrior() is true.
     */
    public boolean atEnd() {
        return !hasNext() && hasPrior;
    }

    /**
     * @return If the Iterator provided at construction has no elements to provide. In other words,
     *     return true if hasNext() is false and hasPrior() is false.
     */
    public boolean isEmpty() {
        return !hasNext() && !hasPrior;
    }
}
