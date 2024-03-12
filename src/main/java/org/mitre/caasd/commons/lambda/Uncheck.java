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

import java.util.function.*;
import java.util.stream.Stream;

import org.mitre.caasd.commons.util.DemotedException;

/**
 * Collection of convenience methods to shorten the syntax when used in
 * {@link Stream} operators:
 *
 * <pre>{@code
 * stream
 *    .filter(Uncheck.pred(x -> checkedTest(x)))
 *    .map(Uncheck.func(x -> checkedFn(x)))
 *    .toList();
 * }</pre>
 */
public class Uncheck {

    /**
     * Demote a {@link CheckedFunction} that throws an {@link Exception} into a plain
     * {@link Function}.  This method is for simplifying stream processing pipelines.
     *
     * @param func A function that throws a checked exception
     * @param <S>  The input type
     * @param <T>  The output type
     *
     * @return A plain Function that may emit DemotedExceptions
     * @throws DemotedException when the original CheckedFunction would have thrown a checked
     *                          Exception
     */
    public static <S, T> Function<S, T> func(CheckedFunction<S, T> func) {
        return x -> {
            try {
                return func.apply(x);
            } catch (RuntimeException e) {
                // pass runtime exceptions, they cannot be demoted
                throw e;
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }

    /**
     * Demote a {@link CheckedPredicate} that throws an {@link Exception} into a plain
     * {@link Predicate}.  This method is for simplifying stream processing pipelines.
     *
     * @param pred A predicate that throws a checked exception
     * @param <T>  The generic type
     *
     * @return A plain Predicate that may emit DemotedExceptions
     * @throws DemotedException when the original CheckedPredicate would have thrown a checked
     *                          Exception
     */
    public static <T> Predicate<T> pred(CheckedPredicate<T> pred) {
        return x -> {
            try {
                return pred.test(x);
            } catch (RuntimeException e) {
                // pass runtime exceptions, they cannot be demoted
                throw e;
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }

    /**
     * Demote a {@link CheckedConsumer} that throws an {@link Exception} into a plain
     * {@link Consumer}.  This method is for simplifying stream processing pipelines.
     *
     * @param consumer A consumer that throws a checked exception
     * @param <T>  The generic type
     *
     * @return A plain Consumer that may emit DemotedExceptions
     * @throws DemotedException when the original CheckedConsumer would have thrown a checked
     *                          Exception
     */
    public static <T> Consumer<T> consumer(CheckedConsumer<T> consumer) {
        return x -> {
            try {
                consumer.accept(x);
            } catch (RuntimeException e) {
                // pass runtime exceptions, they cannot be demoted
                throw e;
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }

    /**
     * Demote a {@link CheckedBiFunction} that throws an {@link Exception} into a plain
     * {@link BiFunction}.  This method is for simplifying stream processing pipelines.
     *
     * @param biFunc A BiFunction that throws a checked exception
     *
     * @return A plain BiFunction that may emit DemotedExceptions
     * @throws DemotedException when the original CheckedPredicate would have thrown a checked
     *                          Exception
     */
    public static <T, U, R> BiFunction<T, U, R> biFunc(CheckedBiFunction<T, U, R> biFunc) {
        return (t, u) -> {
            try {
                return biFunc.apply(t, u);
            } catch (RuntimeException e) {
                // pass runtime exceptions, they cannot be demoted
                throw e;
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }

    /**
     * Demote a {@link CheckedBinaryOperator} that throws an {@link Exception} into a plain
     * {@link BinaryOperator}.  This method is for simplifying stream processing pipelines.
     *
     * @param binaryOperator A BinaryOperator that throws a checked exception
     *
     * @return A plain BinaryOperator that may emit DemotedExceptions
     * @throws DemotedException when the original CheckedBinaryOperator would have thrown a checked
     *                          Exception
     */
    public static <T> BinaryOperator<T> biOp(CheckedBinaryOperator<T> binaryOperator) {
        return (t, u) -> {
            try {
                return binaryOperator.apply(t, u);
            } catch (RuntimeException e) {
                // pass runtime exceptions, they cannot be demoted
                throw e;
            } catch (Exception e) {
                throw DemotedException.demote(e);
            }
        };
    }
}
