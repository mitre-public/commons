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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.Objects.requireNonNull;

import java.util.function.Function;

/**
 * This class expands the capability of, and is directly modeled after, Guava's Preconditions
 * class.
 * <p>
 * Here is an excerpt from Guava's documentation:
 * <p>
 * Static convenience methods that help a method or constructor check whether it was invoked
 * correctly (that is, whether its <i>preconditions</i> were met). If the precondition is not met,
 * the Preconditions method throws an unchecked exception of a specified type, which helps the
 * method in which the exception was thrown communicate that its caller has made a mistake.
 */
public class Preconditions {

    /**
     * Verify that neither this Iterable, nor any of its elements, are null.
     *
     * @param items An iterable of items
     *
     * @throws NullPointerException if items is null or any specific element in items is null.
     */
    public static void checkNoNullElement(Iterable<?> items) {
        checkNotNull(items);
        for (Object item : items) {
            checkNotNull(item);
        }
    }

    public static void checkNoNullElement(Object[] items) {
        checkNotNull(items);
        for (Object item : items) {
            checkNotNull(item);
        }
    }

    public static <T> void checkAllTrue(Iterable<T> items, Function<T, Boolean> rule) {
        requireNonNull(items);
        requireNonNull(rule);

        for (T item : items) {
            checkArgument(rule.apply(item));
        }
    }

    public static <T> void checkAllFalse(Iterable<T> items, Function<T, Boolean> rule) {
        requireNonNull(items);
        requireNonNull(rule);
        for (T item : items) {
            checkArgument(!rule.apply(item));
        }
    }

    /**
     * Verify that applying a function to every item in an Iterable always produces the same output.
     * For example, if you have a class "Shape" with a method "getColor" you can use this method to
     * verify that all the Shapes have the same color using "checkAllMatch(listOfShapes,
     * Shape::getColor)"
     * <p>
     * BE AWARE: This method relies on {@code <U>}'s equals() method
     *
     * @param <T>       The starting data type (e.g. Shape)
     * @param <U>       The type obtained by applying a function (e.g. Color)
     * @param items     A collection of data (e.g. {@code List<Shape>)}
     * @param extractor A value extractor (e.g. Shape::getColor)
     *
     * @throws IllegalArgumentException when the mapped values do not map to exactly 1 output
     *                                  value.
     */
    public static <T, U> void checkAllMatch(Iterable<T> items, Function<T, U> extractor) {
        requireNonNull(items);
        requireNonNull(extractor);

        long count = stream(items).map(extractor).distinct().count();

        checkArgument(count == 1);
    }
}
