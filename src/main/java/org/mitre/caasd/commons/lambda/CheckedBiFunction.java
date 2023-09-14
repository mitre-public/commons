/*
 *    Copyright 2023 The MITRE Corporation
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

package org.mitre.caasd.commons.lambda;

import java.util.function.BiFunction;

import org.mitre.caasd.commons.util.DemotedException;

/**
 * Extension of the {@link BiFunction} interface for a checked lambda function
 */
@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {

    R apply(T t, U u) throws Exception;

    /**
     * Demote the {@link FunctionalInterface} that throws an {@link Exception} to a
     * {@link BiFunction}
     */
    static <T, U, R> BiFunction<T, U, R> demote(CheckedBiFunction<T, U, R> func) {
        return (t, u) -> {
            try {
                return func.apply(t, u);
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }
}