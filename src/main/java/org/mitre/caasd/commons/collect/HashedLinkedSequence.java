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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Iterators;

/**
 * A HashedLinkedSequence is a data structure that combines aspects of HashSet and LinkedList.
 * <p>
 * A HashedLinkedSequence can be used as a direct substitute for a HashSet. A HashedLinkedSequence
 * implements the Set interface and provides constant time access to any particular item in the
 * Set.
 * <p>
 * A HashedLinkedSequence is more powerful than a LinkedList because it supports methods like:
 * getElementBefore(v), getElementAfter(v), insertBefore(v1,v2), and insertAfter(v1,v2) in addition
 * to standard LinkedList methods like: getFirst(), getLast(), addFirst(), and addLast(). The
 * "reference-based" methods (e.g. getElementAfter(v)) all operate in constant time due to the
 * underlying Hash implementation.
 * <p>
 * A HashedLinkedSequence is less powerful than a LinkedList because it does not implement the List
 * interface. A HashedLinkedSequence does not provide access to its data via an integer index
 * value.
 * <p>
 * A HashedLinkedSequence is more powerful than a LinkedHashSet because a LinkedHashSet does not
 * provide a way to insert new data in between existing data points. A LinkedHashSet ONLY remembers
 * insert order, it doesn't allow you to manipulate the insert order easily.
 * <p>
 * Note: Despite the fact that HashedLinkedSequence implements Set the word "Set" is not include in
 * this classes name. The word "Set" is omitted to prevent confusion with java.util.LinkedHashSet.
 *
 * @Todo -- Add iterateForwardFrom(T element), iterateBackwardFrom(T), reverseIterator().
 */
public class HashedLinkedSequence<T> implements Collection<T>, Set<T> {

    private static final String DUPLICATE_ELEMENT_WARNING = "Cannot add the same element twice";

    private static final String ITEM_NOT_FOUND_WARNING = "Item not found";

    /**
     * Hash the linked nodes by their element so that we can easily find, and begin, an iteration
     * from an arbitrary element in the dataset.
     */
    private final HashMap<T, Node<T>> nodes;

    private Node<T> firstNode;

    private Node<T> lastNode;

    private int modCount; //used to detect concurrent modification

    public <T> HashedLinkedSequence() {
        this.nodes = newHashMap();
        this.firstNode = null;
        this.lastNode = null;
        this.modCount = 0;
    }

    /**
     * Create a new HashedLinkedSequence containing these elements. This is a shortcut for calling
     * the constructor and then immediately calling addAll()
     *
     * @param <E>      The type of elements
     * @param elements The elements themselves
     *
     * @return A new HashedLinkedSequence containing these elements.
     */
    public static <E> HashedLinkedSequence<E> newHashedLinkedSequence(E... elements) {
        HashedLinkedSequence<E> list = new HashedLinkedSequence<>();
        list.addAll(elements);
        return list;
    }

    /**
     * Create a new HashedLinkedList containing some pre-specified data. This is a shortcut for
     * calling the constructor and then immediately calling Iterators.addAll(this,
     * iterable.iterator())
     *
     * @param <E>      The type of elements
     * @param iterable A source of seed data
     *
     * @return A new HashedLinkedSequence containing this data.
     */
    public static <E> HashedLinkedSequence<E> newHashedLinkedSequence(Iterable<E> iterable) {
        checkNotNull(iterable);
        HashedLinkedSequence<E> list = new HashedLinkedSequence<>();
        Iterators.addAll(list, iterable.iterator());
        return list;
    }

    /**
     * This is tantamount to calling the constructor directly. This method is provided for
     * consistency with the other factory methods.
     *
     * @return A new HashedLinkedSequence containing no data.
     */
    public static <E> HashedLinkedSequence<E> newHashedLinkedSequence() {
        return new HashedLinkedSequence<>();
    }

    @Override
    public boolean add(T item) {
        addLast(item);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modified = false;
        for (T element : c) {
            modified |= this.add(element);
        }
        return modified;
    }

    public boolean addAll(T... array) {
        return Collections.addAll(this, array);
    }

    public void addFirst(T item) {
        checkNotNull(item);
        checkArgument(!nodes.containsKey(item), DUPLICATE_ELEMENT_WARNING);

        Node<T> oldFirst = firstNode;
        Node<T> newNode = new Node(null, item, oldFirst);
        this.nodes.put(item, newNode);
        this.firstNode = newNode;

        if (oldFirst == null) {
            lastNode = newNode;
        } else {
            oldFirst.previous = newNode;
        }
        modCount++;
    }

    public void addLast(T item) {
        checkNotNull(item);
        //adding the same element twice is illegal because it make "iterate from" operations unclear
        checkArgument(!nodes.containsKey(item), DUPLICATE_ELEMENT_WARNING);

        Node<T> oldLast = lastNode;
        Node<T> newNode = new Node(oldLast, item, null);
        this.nodes.put(item, newNode);
        this.lastNode = newNode;

        if (oldLast == null) {
            firstNode = newNode;
        } else {
            oldLast.next = newNode;
        }
        modCount++;
    }

    public T getElementBefore(T existingItem) {
        Node<T> node = nodes.get(existingItem);
        checkArgument(node != null, ITEM_NOT_FOUND_WARNING);
        if (node.previous == null) {
            throw new NoSuchElementException();
        }
        return node.previous.item;
    }

    public T getElementAfter(T existingItem) {
        Node<T> node = nodes.get(existingItem);
        checkArgument(node != null, ITEM_NOT_FOUND_WARNING);
        if (node.next == null) {
            throw new NoSuchElementException();
        }
        return node.next.item;
    }

    public void insertAfter(T newItem, T existingItem) {
        Node<T> anchorNode = nodes.get(existingItem);
        checkArgument(anchorNode != null, ITEM_NOT_FOUND_WARNING);
        checkArgument(!nodes.containsKey(newItem), DUPLICATE_ELEMENT_WARNING);

        Node<T> newNode = new Node(anchorNode, newItem, anchorNode.next);
        anchorNode.next = newNode;
        if (newNode.next != null) {
            newNode.next.previous = newNode;
        }

        if (anchorNode == lastNode) {
            lastNode = newNode;
        }

        nodes.put(newItem, newNode);

        modCount++;
    }

    public void insertBefore(T newItem, T existingItem) {
        Node<T> anchorNode = nodes.get(existingItem);
        checkArgument(anchorNode != null, ITEM_NOT_FOUND_WARNING);
        checkArgument(!nodes.containsKey(newItem), DUPLICATE_ELEMENT_WARNING);

        Node<T> newNode = new Node(anchorNode.previous, newItem, anchorNode);
        anchorNode.previous = newNode;
        if (newNode.previous != null) {
            newNode.previous.next = newNode;
        }

        if (anchorNode == firstNode) {
            firstNode = newNode;
        }

        nodes.put(newItem, newNode);

        modCount++;
    }

    /**
     * Returns the first element in this list.
     *
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public T getFirst() {
        if (firstNode == null) {
            throw new NoSuchElementException();
        }
        return firstNode.item;
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public T getLast() {
        if (lastNode == null) {
            throw new NoSuchElementException();
        }
        return lastNode.item;
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.nodes.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter(firstNode);
    }

    @Override
    public Object[] toArray() {
        return Iterators.toArray(iterator(), Object.class);
    }

    @Override
    public <E> E[] toArray(E[] a) {
        //implementation copied from java.util.LinkedList:
        if (a.length < size()) {
            a = (E[]) Array.newInstance(a.getClass().getComponentType(), size());
        }
        int i = 0;
        Object[] result = a;
        for (Node<T> x = firstNode; x != null; x = x.next) {
            result[i++] = x.item;
        }

        if (a.length > size()) {
            a[size()] = null;
        }

        return a;
    }

    @Override
    public boolean remove(Object o) {
        T item = (T) o;
        Node<T> node = nodes.remove(item);

        if (node == null) {
            return false;
        } else {
            unlink(node);
            modCount++;
            return true;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return nodes.keySet().containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        //implementation copied from java.util.AbstractSet:
        Objects.requireNonNull(c);
        boolean modified = false;

        if (size() > c.size()) {
            for (Iterator<?> i = c.iterator(); i.hasNext(); ) {
                modified |= remove(i.next());
            }
        } else {
            for (Iterator<?> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        //implementation copied from java.util.AbstractCollection:
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        nodes.clear();
        firstNode = null;
        lastNode = null;
    }

    private T unlink(Node<T> x) {
        final T element = x.item;
        final Node<T> next = x.next;
        final Node<T> prev = x.previous;

        if (prev == null) {
            this.firstNode = next;
        } else {
            prev.next = next;
            x.previous = null;
        }

        if (next == null) {
            this.lastNode = prev;
        } else {
            next.previous = prev;
            x.next = null;
        }

        x.item = null;
        return element;
    }

    private static class Node<T> {

        T item;
        Node<T> next;
        Node<T> previous;

        Node(Node<T> previous, T element, Node<T> next) {
            this.item = element;
            this.next = next;
            this.previous = previous;
        }
    }

    private class Iter implements Iterator<T> {

        Node<T> currentNode;
        Node<T> lastNodeReturned = null;
        private int expectedModCount = modCount;

        Iter(Node<T> startingNode) {
            this.currentNode = startingNode;
        }

        @Override
        public boolean hasNext() {
            return currentNode != null;
        }

        @Override
        public T next() {
            checkForComodification();
            if (currentNode == null) {
                throw new NoSuchElementException();
            }
            T returnMe = currentNode.item;
            lastNodeReturned = currentNode; //save for possible removal
            currentNode = currentNode.next;
            return returnMe;
        }

        final void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void remove() {
            if (lastNodeReturned == null) {
                throw new IllegalStateException();
            }
            //call the parent HashedLinkedSequence's remove method (not the Iterator's remove)
            HashedLinkedSequence.this.remove(lastNodeReturned.item);
            /*
             * it would be just slightly faster to manually remove this node here (but it is more
             * robust to reuse the remove method which already addresses all the corner cases).
             */

            lastNodeReturned = null; //an iterator's remove() method only works once per "next()"

            expectedModCount++;
        }
    }
}
