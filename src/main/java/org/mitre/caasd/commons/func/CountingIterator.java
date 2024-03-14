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

package org.mitre.caasd.commons.func;

import java.util.Iterator;

/**
 * A CountingIterator decorates the provided Iterator with a counting mechanic that is triggered on
 * every call to next.
 *
 * <p>A CountingIterator is designed to provide stats on how many items streamed through a Iterator
 * in its lifetime (assuming the root Iterator was wrapped throughout its lifetime)
 */
public class CountingIterator<T> implements Iterator<T> {

    int numCallsToNext = 0;

    private final Iterator<T> innerIter;

    public CountingIterator(Iterator<T> iter) {
        this.innerIter = iter;
    }

    public int numCallsNext() {
        return numCallsToNext;
    }

    @Override
    public boolean hasNext() {
        return innerIter.hasNext();
    }

    @Override
    public T next() {
        numCallsToNext++;
        return innerIter.next();
    }

    public Iterator<T> innerIterator() {
        return innerIter;
    }
}
