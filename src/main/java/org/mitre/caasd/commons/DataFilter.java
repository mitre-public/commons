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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A DataFilter is a DataCleaner that applies a Predicate. Inputs that pass the predicate "pass
 * through" when the <code>clean(T input)</code> method is called. Inputs that do not pass the
 * predicate will either be ignored or forwarded to a downstream Consumer.
 * <p>
 * The key benefit of a DataFilter is that it provides a natural seam in the event that you need to
 * capture inputs that were screened out by the filter.
 *
 * @param <T> The data type being cleaned
 */
public class DataFilter<T> implements DataCleaner<T> {

    private final Predicate<T> filter;

    private final Consumer<T> onRemoval;

    /**
     * Create a DataFilter that does nothing with inputs that are filtered out by the predicate.
     *
     * @param filter This Predicate is applied to all input
     */
    public DataFilter(Predicate<T> filter) {
        this(filter, NO_OP_CONSUMER);
    }

    /**
     * Create a DataFilter that applies a predicate and forwards all inputs that failed the
     * predicate's test to a downstream consumer.
     *
     * @param filter    This Predicate is applied to all input
     * @param onRemoval A Consumer that will receive all inputs that did not pass the predicate's
     *                  test
     */
    public DataFilter(Predicate<T> filter, Consumer<T> onRemoval) {
        this.filter = checkNotNull(filter);
        this.onRemoval = checkNotNull(onRemoval);
    }

    /**
     * @param input An instance of T
     *
     * @return An Optional that contains the input (when predicate passed) or an empty Optional
     *     (when the predicate failed)
     */
    @Override
    public Optional<T> clean(T input) {

        boolean filterAccepts = filter.test(input);

        if (!filterAccepts) {
            onRemoval.accept(input);
        }

        return (filterAccepts) ? Optional.of(input) : Optional.empty();
    }
}
