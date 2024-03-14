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

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public interface AutoCloseableIterator<T> extends AutoCloseable, Iterator<T> {

    /**
     * Wraps the provided class which is both an iterator and auto-closeable as an {@link
     * AutoCloseableIterator}.
     */
    static <T, C extends Iterator<T> & AutoCloseable> AutoCloseableIterator<T> wrap(C autoCloseableAndIterator) {
        requireNonNull(autoCloseableAndIterator);
        return new AutoCloseableIterator<T>() {
            @Override
            public void close() throws Exception {
                autoCloseableAndIterator.close();
            }

            @Override
            public boolean hasNext() {
                return autoCloseableAndIterator.hasNext();
            }

            @Override
            public T next() {
                return autoCloseableAndIterator.next();
            }

            @Override
            public void remove() {
                autoCloseableAndIterator.remove();
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                autoCloseableAndIterator.forEachRemaining(consumer);
            }
        };
    }

    /**
     * Wraps a {@link AutoCloseableIterator} as a new iterator with the supplied transform applied to
     * elements as they are returned.
     */
    static <U, T> AutoCloseableIterator<T> transforming(
            AutoCloseableIterator<U> autoCloseableIterator, Function<U, T> transform) {
        requireNonNull(autoCloseableIterator, "Supplied iterator cannot be null.");
        requireNonNull(transform, "Supplied transform cannot be null");

        return new AutoCloseableIterator<T>() {

            @Override
            public void close() throws Exception {
                autoCloseableIterator.close();
            }

            @Override
            public boolean hasNext() {
                return autoCloseableIterator.hasNext();
            }

            @Override
            public T next() {
                return transform.apply(autoCloseableIterator.next());
            }

            @Override
            public void remove() {
                autoCloseableIterator.remove();
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                autoCloseableIterator.forEachRemaining(element -> consumer.accept(transform.apply(element)));
            }
        };
    }
}
