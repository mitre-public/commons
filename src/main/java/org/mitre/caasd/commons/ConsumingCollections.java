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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.function.Consumer;

/** This class provides Collections that implement Consumer. */
public class ConsumingCollections {

    private ConsumingCollections() {
        throw new IllegalStateException("Do Not Instatiate");
    }

    /** This ArrayList implement Consumer. */
    public static class ConsumingArrayList<T> extends ArrayList<T> implements Consumer<T> {

        @Override
        public void accept(T t) {
            add(t);
        }
    }

    public static <E> ConsumingArrayList<E> newConsumingArrayList() {
        return new ConsumingArrayList<>();
    }

    /** This LinkedList implement Consumer. */
    public static class ConsumingLinkedList<T> extends LinkedList<T> implements Consumer<T> {

        @Override
        public void accept(T t) {
            add(t);
        }
    }

    public static <E> ConsumingLinkedList<E> newConsumingLinkedList() {
        return new ConsumingLinkedList<>();
    }

    /** This HashSet implement Consumer. */
    public static class ConsumingHashSet<T> extends HashSet<T> implements Consumer<T> {

        @Override
        public void accept(T t) {
            add(t);
        }
    }

    public static <E> ConsumingHashSet<E> newConsumingHashSet() {
        return new ConsumingHashSet<>();
    }

    /** This TreeSet implement Consumer. */
    public static class ConsumingTreeSet<T> extends TreeSet<T> implements Consumer<T> {

        @Override
        public void accept(T t) {
            add(t);
        }
    }

    public static <E> ConsumingTreeSet<E> newConsumingTreeSet() {
        return new ConsumingTreeSet<>();
    }

    /** This PriorityQueue implement Consumer. */
    public static class ConsumingPriorityQueue<T> extends PriorityQueue<T> implements Consumer<T> {

        @Override
        public void accept(T t) {
            add(t);
        }
    }

    public static <E> ConsumingPriorityQueue<E> newConsumingPriorityQueue() {
        return new ConsumingPriorityQueue<>();
    }
}
