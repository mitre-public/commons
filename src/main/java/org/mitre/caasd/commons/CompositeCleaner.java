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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Optional;

/**
 * A CompositeCleaner combines multiple DataCleaner into a single DataCleaner.
 *
 * @param <T> The type of data being cleaned.
 */
public class CompositeCleaner<T> implements DataCleaner<T> {

    private final List<DataCleaner<? super T>> cleaners;

    /**
     * Create a DataCleaner that applies each of these DataCleaners in sequence.
     *
     * @param cleaners A fixed sequence of DataCleaners
     */
    public CompositeCleaner(List<DataCleaner<? super T>> cleaners) {
        this.cleaners = newArrayList(cleaners); // create a defensive copy
    }

    /**
     * Create a DataCleaner out of an ordered sequence of DataCleaner
     *
     * @param cleaners An ordered listed of DataCleaners
     * @param <T>      The type of data being cleaned.
     *
     * @return A new CompositeCleaner
     */
    @SafeVarargs
    public static <T> CompositeCleaner<T> of(DataCleaner<? super T>... cleaners) {
        return new CompositeCleaner<T>(newArrayList(cleaners));
    }

    /**
     * @param dataItem A piece of data that needs cleaning.
     *
     * @return The result of applying each of the wrapped DataCleaners in order
     */
    @Override
    public Optional<T> clean(T dataItem) {
        if (cleaners.isEmpty()) {
            return Optional.of(dataItem);
        }

        T current = dataItem;
        Optional<T> lastResult = null;

        for (DataCleaner smoother : cleaners) {

            lastResult = smoother.clean(current);

            if (lastResult.isPresent()) {
                current = lastResult.get();
            } else {
                return Optional.empty();
            }
        }

        return lastResult;
    }
}
