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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A TwoWayConsumer "splits" incoming data between two separate destinations by applying a Predicate
 * and passing the incoming data to "Consumer A" when a predicate is true and passing the data to
 * "Consumer B" when the predicate is false.
 */
public class TwoWayConsumer<T> implements Consumer<T> {

    private final Predicate<T> splitCriteria;

    private final Consumer<T> whenTrue;

    private final Consumer<T> whenFalse;

    /**
     * Create a Consumer that redirects input two either the "whenTrue" or "whenFalse" Consumer
     * based on a predicate's result.
     *
     * @param rule      A Predicate that directs input to one of the two provided Consumers
     * @param whenTrue  The Consumer that receives the input item when the predicate is true
     * @param whenFalse The Consumer that receives the input item when the predicate is false
     */
    public TwoWayConsumer(Predicate<T> rule, Consumer<T> whenTrue, Consumer<T> whenFalse) {
        checkNotNull(rule);
        checkNotNull(whenTrue);
        checkNotNull(whenFalse);
        this.splitCriteria = rule;
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
    }

    @Override
    public void accept(T t) {
        if (splitCriteria.test(t)) {
            whenTrue.accept(t);
        } else {
            whenFalse.accept(t);
        }
    }
}
