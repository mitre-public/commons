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

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Function;

public class CollectionUtils {

    private CollectionUtils() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    /**
     * Performs a binary search within a list that was pre-sorted by a feature of the list items.
     * For example, perhaps the input items are "OrderInvoice" objects have been sorted by their
     * orderDate, cost, or username.
     *
     * @param preSortedList   A list of items that are pre-sorted by some feature
     * @param sortValueGetter Extracts the "sort feature" from an instance of T. For example, when
     *                        items in the list are sorted by username this function might call the
     *                        "getUsername()" method.
     * @param searchKey       An instance of R that will be used to perform the binary search. This
     *                        might be a simple String or Instant object. It will NOT be an instance
     *                        of the complex class T.
     * @param <T>             The class of the "items in the list"
     * @param <R>             The class of the "sort feature"
     *
     * @return If the list contains an item whose "time()" method returns the searchTime it will
     * return the index of that "matching item".  Otherwise, this method will return (-(insertion
     * point) -1).  Where the insertion point is defined as the point at which an item with the
     * "searchTime" would be inserted into the list. See Collections.binarySearch for a nearly
     * identical use-case.
     */
    public static <T, R extends Comparable<? super R>> int binarySearch(
        List<? extends T> preSortedList,
        Function<T, R> sortValueGetter,
        R searchKey
    ) {

        //The implementation of this binarySearch is essentially copied (with small tweaks) from:
        //  java.util.Collections.binarySearch(List<? extends T> list, T key, Comparator<? super T> c)

        if (preSortedList instanceof RandomAccess) {
            return indexedBinarySearch(preSortedList, sortValueGetter, searchKey);
        } else {
            return iteratorBinarySearch(preSortedList, sortValueGetter, searchKey);
        }
    }


    //This method is ESSENTIALLY copied from java.util.Collections.indexedBinarySearch()
    private static <T, R extends Comparable<? super R>> int indexedBinarySearch(
        List<? extends T> preSortedList,
        Function<T, R> sortValueGetter,
        R searchKey
    ) {
        int low = 0;
        int high = preSortedList.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = preSortedList.get(mid);
            int cmp = sortValueGetter.apply(midVal).compareTo(searchKey);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }


    //This method is ESSENTIALLY copied from java.util.Collections.iteratorBinarySearch()
    private static <T, R extends Comparable<? super R>> int iteratorBinarySearch(
        List<? extends T> preSortedList,
        Function<T, R> sortValueGetter,
        R searchKey
    ) {
        int low = 0;
        int high = preSortedList.size() - 1;
        ListIterator<? extends T> i = preSortedList.listIterator();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = get(i, mid);
            int cmp = sortValueGetter.apply(midVal).compareTo(searchKey);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }


    //This method is copied EXACTLY from java.util.Collections.get() (where it's a generic private static method)
    private static <T> T get(ListIterator<? extends T> i, int index) {
        T obj = null;
        int pos = i.nextIndex();
        if (pos <= index) {
            do {
                obj = i.next();
            } while (pos++ < index);
        } else {
            do {
                obj = i.previous();
            } while (--pos > index);
        }
        return obj;
    }

    public static <A, B> Iterator<Pair<A, B>> zip(Iterable<A> srcA, Iterable<B> srcB) {
        requireNonNull(srcA);
        requireNonNull(srcB);
        return new PairIterator<>(srcA.iterator(), srcB.iterator());
    }

    public static <A, B> Iterator<Pair<A, B>> zip(Iterator<A> srcA, Iterator<B> srcB) {
        requireNonNull(srcA);
        requireNonNull(srcB);
        return new PairIterator<>(srcA, srcB);
    }

    public static class PairIterator<A, B> implements Iterator<Pair<A, B>> {

        final Iterator<A> iterA;

        final Iterator<B> iterB;

        private Pair<A, B> next;

        public PairIterator(Iterator<A> iterA, Iterator<B> iterB) {
            this.iterA = requireNonNull(iterA);
            this.iterB = requireNonNull(iterB);
            updateNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Pair<A, B> next() {

            if (next == null) {
                throw new NoSuchElementException();
            }

            Pair<A, B> returnMe = next;

            updateNext();

            return returnMe;
        }

        private void updateNext() {
            this.next = (iterA.hasNext() && iterB.hasNext())
                ? Pair.of(iterA.next(), iterB.next())
                : null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("A PairIterator cannot remove items.");
        }
    }

    public static <A, B, C> Iterator<Triple<A, B, C>> zip(Iterable<A> srcA, Iterable<B> srcB, Iterable<C> srcC) {
        requireNonNull(srcA);
        requireNonNull(srcB);
        requireNonNull(srcC);
        return new TripleIterator<>(srcA.iterator(), srcB.iterator(), srcC.iterator());
    }

    public static <A, B, C> Iterator<Triple<A, B, C>> zip(Iterator<A> srcA, Iterator<B> srcB, Iterator<C> srcC) {
        requireNonNull(srcA);
        requireNonNull(srcB);
        requireNonNull(srcC);
        return new TripleIterator<>(srcA, srcB, srcC);
    }

    public static class TripleIterator<A, B, C> implements Iterator<Triple<A, B, C>> {

        final Iterator<A> iterA;

        final Iterator<B> iterB;

        final Iterator<C> iterC;

        private Triple<A, B, C> next;

        public TripleIterator(Iterator<A> iterA, Iterator<B> iterB, Iterator<C> iterC) {
            this.iterA = requireNonNull(iterA);
            this.iterB = requireNonNull(iterB);
            this.iterC = requireNonNull(iterC);
            updateNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Triple<A, B, C> next() {

            if (next == null) {
                throw new NoSuchElementException();
            }

            Triple<A, B, C> returnMe = next;

            updateNext();

            return returnMe;
        }

        private void updateNext() {
            this.next = (iterA.hasNext() && iterB.hasNext() && iterC.hasNext())
                ? Triple.of(iterA.next(), iterB.next(), iterC.next())
                : null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("A TripleIterator cannot remove items.");
        }
    }

}
