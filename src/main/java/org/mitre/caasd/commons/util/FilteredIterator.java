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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * A FilteredIterator combines an Iterator and a Predicate to create a new Iterator that only
 * provides items that (A) came from unprotected Iterator and (B) satisfy the Predicate's test.
 * <p>
 * Using a FilteredIterator makes certain code easier to read and write because you no longer have
 * to manually check against a predicate.
 * <p>
 * //@todo -- Move this class to Commons
 */
public class FilteredIterator<T> implements Iterator<T> {

    private final Iterator<T> innerIterator;

    private final Predicate<T> predicate;

    private T next;

    public FilteredIterator(Iterator<T> iter, Predicate<T> predicate) {
        checkNotNull(iter);
        checkNotNull(predicate);
        this.innerIterator = iter;
        this.predicate = predicate;
        this.next = findNext();
    }

    public static <E> Iterator<E> filter(Iterator<E> iter, Predicate<E> predicate) {
        return new FilteredIterator(iter, predicate);
    }

    private T findNext() {

        while (innerIterator.hasNext()) {
            T candidate = innerIterator.next();
            if (predicate.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public T next() {
        if (next == null) {
            throw new NoSuchElementException();
        }

        T returnMe = next;
        next = findNext();
        return returnMe;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }
}
