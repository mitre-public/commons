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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Collection of convenience methods to shorten the syntax when used in
 * {@link Stream} operators:
 * 
 * stream
 * .filter(Uncheck.pred(x -> checkedTest(x)))
 * .map(Uncheck.func(x -> checkedFn(x)))
 */
public class Uncheck {

    /**
     * Demote the {@link CheckedFunction} to a {@link BiFunction}
     */
    public static <S, T> Function<S, T> func(CheckedFunction<S, T> func) {
        return CheckedFunction.demote(func);
    }

    /**
     * Demote the {@link CheckedPredicate} to a {@link Predicate}
     */
    public static <S, T> Predicate<T> pred(CheckedPredicate<T> func) {
        return CheckedPredicate.demote(func);
    }

    /**
     * Demote the {@link CheckedBiFunction} to a {@link CheckedBiFunction}
     */
    public static <T, U, R> BiFunction<T, U, R> bifunc(CheckedBiFunction<T, U, R> func) {
        return CheckedBiFunction.demote(func);
    }

    /**
     * Demote the {@link CheckedBinaryOperator} to a {@link BinaryOperator}
     */
    public static <T> BinaryOperator<T> biop(CheckedBinaryOperator<T> func) {
        return CheckedBinaryOperator.demote(func);
    }

}
