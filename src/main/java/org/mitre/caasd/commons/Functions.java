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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides a useful Functional coding convenience.
 */
public class Functions {

    /**
     * This "no operations" consumer does nothing.
     * <p>
     * This consumer is meant to simplify external code that requires a "downstream Consumer". For
     * example, a filtering mechanism could require one Consumer for the "accepted" items and one
     * Consumer for the "rejected" items. If you plan to ignore the rejected items just use this
     * NO_OP_CONSUMER instead of declaring your own NO_OP_CONSUMER (which usually looks bad when
     * inserted inline)
     */
    public static final Consumer NO_OP_CONSUMER = (obj) -> {
        // this consumer does nothing
    };

    /**
     * This Predicate always returns true.
     * <p>
     * This Predicate is meant to simplify external code that requires a predicate. For example,
     * some constructors and methods will require a Predicate in their signature. When calling these
     * constructors and methods it may be easier (and cleaner) to reference this Predicate rather
     * than write a lambda or Anonymous class (which will be harder to read).
     */
    public static final Predicate ALWAYS_TRUE = (obj) -> {
        return true;
    };

    /**
     * This Predicate always returns false.
     * <p>
     * This Predicate is meant to simplify external code that requires a predicate. For example,
     * some constructors and methods will require a Predicate in their signature. When calling these
     * constructors and methods it may be easier (and cleaner) to reference this Predicate rather
     * than write a lambda or Anonymous class (which will be harder to read).
     */
    public static final Predicate ALWAYS_FALSE = (obj) -> {
        return false;
    };

    @FunctionalInterface
    public interface ToStringFunction<T> extends Function<T, String> {}
}
