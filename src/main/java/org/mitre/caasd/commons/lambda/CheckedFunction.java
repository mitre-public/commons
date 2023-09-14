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

import java.util.function.Function;

import org.mitre.caasd.commons.util.DemotedException;

/**
 * Extension of the {@link Function} interface for a checked lambda
 * function
 */
@FunctionalInterface
public interface CheckedFunction<S, T> {

    T apply(S t) throws Exception;

    /**
     * Demote the {@link FunctionalInterface} that throws an {@link Exception} to a
     * {@link Function}
     */
    static <S, T> Function<S, T> demote(CheckedFunction<S, T> func) {
        return x -> {
            try {
                return func.apply(x);
            } catch (RuntimeException e) {
                // pass runtime exceptions
                throw e;
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }
}