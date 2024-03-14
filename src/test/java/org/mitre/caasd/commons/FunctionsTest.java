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

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.Functions.*;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class FunctionsTest {

    public static void consumerMethod1(Consumer<Integer> ints) {
        ints.accept(5);
    }

    public static void consumerMethod2(Consumer<? super Number> numbers) {
        Integer num = 50;
        numbers.accept(num);
    }

    @Test
    public void testNoOpConsumer() {
        // the NO_OP_CONSUMER should compile no matter where we use it
        assertDoesNotThrow(() -> consumerMethod1(NO_OP_CONSUMER));
        assertDoesNotThrow(() -> consumerMethod2(NO_OP_CONSUMER));
    }

    public static boolean predicateMethod1(Predicate<Integer> filter) {
        return filter.test(5);
    }

    public static boolean predicateMethod2(Predicate<? super Number> filter) {
        return filter.test(20.0);
    }

    @Test
    public void testReusablePredicates() {
        assertTrue(predicateMethod1(ALWAYS_TRUE));
        assertTrue(predicateMethod2(ALWAYS_TRUE));
        assertFalse(predicateMethod1(ALWAYS_FALSE));
        assertFalse(predicateMethod2(ALWAYS_FALSE));
    }
}
