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

import java.util.function.Predicate;

/**
 * A CountingPredicate decorates a Predicate with the ability to track the number of time a
 * Predicate is called, returns true, and returns false.
 */
public class CountingPredicate<T> implements Predicate<T> {

    private final Predicate<T> predicate;

    private long numCalls;

    private long numTrue;

    private long numFalse;

    public CountingPredicate(Predicate<T> pred) {
        checkNotNull(pred);
        this.predicate = pred;
    }

    public static <T> CountingPredicate<T> from(Predicate<T> predicate) {
        return new CountingPredicate<>(predicate);
    }

    @Override
    public boolean test(T t) {
        numCalls++;
        boolean result = predicate.test(t);

        if (result) {
            numTrue++;
        } else {
            numFalse++;
        }
        return result;
    }

    public void resetCounts() {
        numCalls = 0;
        numTrue = 0;
        numFalse = 0;
    }

    /** @return The number of times the "test" method is called. */
    public long count() {
        return numCalls;
    }

    /** @return The number of times the "test" method returned true. */
    public long trueCount() {
        return numTrue;
    }

    /** @return The number of times the "test" method returned false. */
    public long falseCount() {
        return numFalse;
    }
}
