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
import java.util.function.Function;

import org.mitre.caasd.commons.util.DemotedException;

/**
 * A CheckedBiFunction is similar to a {@link BiFunction} EXCEPT it throws a checked exception.
 * <p>
 * Unfortunately, CheckedBiFunctions obfuscate stream processing code because they require using
 * try-catch blocks. This class and the convenience functions in {@link Uncheck}, allow you to
 * improve the readability of stream processing pipelines (assuming you are willing to demote all
 * checked exceptions to RuntimeExceptions)
 * <p>
 * For example:
 *
 * <pre>{@code
 *     //code WITHOUT these utilities -- is harder to read and write.
 *
 *     List<String> dataSet = loadData();
 *     List<String> subset = dataSet.stream()
 *         .map(str -> {
 *             try {
 *                 return functionThatThrowsCheckedEx(str);
 *             } catch (Exception ex) {
 *                 throw DemotedException.demote(ex);
 *             }})
 *         .filter(str -> str.length() < 5)
 *         .toList();
 *
 *
 *     //code WITH these utilities -- is easier to read and write.
 *
 *     List<String> dataSet = loadData();
 *     List<String> subset = dataSet.stream()
 *         .map(Uncheck.func(str -> functionThatThrowsCheckedEx(str))
 *         .filter(str -> str.length() < 5)
 *         .toList();
 * }</pre>
 */
@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {

    R apply(T t, U u) throws Exception;
}