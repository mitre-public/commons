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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A SupplierChain is a "ordered sequence of Suppliers" that finds and supplies an asset by
 * iteratively attempting each supplier in turn until one successfully produces the desired asset.
 *
 * <p>For example, a SupplierChain that provides a secret might reflect the following strategy:
 * "First search the local environment variables for the secret, then search the System properties,
 * then search for a specific file in the user directory, finally search for a specific file on the
 * host machine. If all of these options fail throw an exception.
 *
 * <p>This class takes design inspiration from the "Chain of Responsibility Pattern", our own
 * "CompositeDataCleaner" and "com.amazonaws.auth.AWSCredentialsProviderChain"
 *
 * @param <T> The type of object being delivered by the chain.
 */
public class SupplierChain<T> implements Supplier<T> {

    private final List<Supplier<? super T>> suppliers;

    private Supplier<? super T> highestPriorityWorkingSupplier;

    /**
     * Create a SupplierChain that iteratively attempts each supplier in turn until one successfully
     * produces the desired asset.
     *
     * <p>The first supplier to successfully deliver a non-null response becomes the "sole
     * supplier" from that point onward. Here we assume that once the "highest priority working
     * supplier" is known it does not makes sense to change the configuration OR waste time retrying
     * Suppliers that have already failed to provide the required asset.
     *
     * @param listOfSuppliers A fixed sequence of Suppliers
     */
    public SupplierChain(List<Supplier<? super T>> listOfSuppliers) {
        this.suppliers = newArrayList(listOfSuppliers); // create a defensive copy
    }

    /**
     * Create a SupplierChain that iteratively attempts each supplier in turn until one successfully
     * produces the desired asset.
     *
     * <p>The first supplier to successfully deliver a non-null response becomes the "sole
     * supplier" from that point onward. Here we assume that once the "highest priority working
     * supplier" is known it does not makes sense to change the configuration OR waste time retrying
     * Suppliers that have already failed to provide the required asset.
     *
     * @param suppliers A fixed sequence of Suppliers
     * @param <T>       The type of object being Supplied.
     * @return A new SupplierChain
     */
    @SafeVarargs
    public static <T> SupplierChain<T> of(Supplier<? super T>... suppliers) {
        return new SupplierChain<T>(newArrayList(suppliers));
    }

    @Override
    public T get() {
        if (nonNull(highestPriorityWorkingSupplier)) {
            return (T) highestPriorityWorkingSupplier.get();
        }

        List<String> exceptionMessages = newArrayList();
        for (Supplier<? super T> aSupplier : suppliers) {

            try {
                T result = (T) aSupplier.get();
                if (nonNull(result)) {
                    this.highestPriorityWorkingSupplier =  aSupplier;
                    return result;
                }
            } catch (Exception ex) {
                // Ignore any exceptions and move onto the next supplier
                String message = aSupplier + ": " + ex.getMessage();
                exceptionMessages.add(message);
            }
        }

        throw new IllegalStateException(
            "No Supplier in the SupplierChain provided a non-null result: " + exceptionMessages
        );
    }
}
