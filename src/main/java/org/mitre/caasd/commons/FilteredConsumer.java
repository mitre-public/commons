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

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A FilteredConsumer decorates any Consumer with a filtering stage provided by a Predicate.
 *
 * <p>In other words, the wrapped Consumer only receives input items that have already by "passed"
 * the filter. Inputs that do no pass the filter are ignored.
 *
 * @param <T> The type of the wrapped Consumer
 */
public class FilteredConsumer<T> implements Consumer<T>, Serializable {

    private static final long serialVersionUID = 7232976316826633043L;

    private final Predicate<T> filter;

    private final Consumer<T> uponVerification;

    public FilteredConsumer(Predicate<T> filter, Consumer<T> uponVerification) {
        this.uponVerification = uponVerification;
        this.filter = filter;
    }

    /**
     * Accept and input item, pass it to the wrapped consumer if the input passes the filtering
     * step.
     *
     * @param item An input that passed to the wrapped consumer if and only if the input first
     *             passed the Predicate filter provided at construction.
     */
    @Override
    public void accept(T item) {
        if (filter.test(item)) {
            uponVerification.accept(item);
        }
    }
}
