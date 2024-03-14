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

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * A DataCleaner manipulates input data and produces a "cleaned" version of each input.
 * <p>
 * The DataCleaner interface is designed to cleanly support both manipulating data (i.e. rounding
 * numbers to 2 significant figures) and filtering data (i.e. removing all negative numbers). The
 * flexibility allows for creating, and applying, complete data cleaning pipelines in a single
 * step.
 * <p>
 * A DataCleaner returns an Optional to encourage users to address the possibility that all data is
 * filtered out.
 */
@FunctionalInterface
public interface DataCleaner<T> {

    /** @return a DataCleaner that always returns an Empty Optional. */
    static DataCleaner suppressAll() {
        return (obj) -> {
            return Optional.empty();
        };
    }

    /**
     * @param data A single piece of input data.
     *
     * @return A "cleaned" version of the input, or an empty Optional.
     */
    Optional<T> clean(T data);

    /**
     * Apply this DataCleaner to every piece of data in the collection, the return the non-empty
     * results.
     *
     * @param dataset A collection of data
     *
     * @return A list of cleaned data (empty results are excluded)
     */
    default ArrayList<T> clean(Collection<T> dataset) {

        // apply this DataCleaner to each entry and collect the non-empty results
        return dataset.stream()
                .map((T item) -> clean(item))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toCollection(ArrayList::new));
    }
}
