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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.mitre.caasd.commons.util.ExceptionHandler;

import org.junit.jupiter.api.Test;

public class ExceptionCatchingCleanerTest {

    static class TestDataCleaner implements DataCleaner<Double> {

        @Override
        public Optional<Double> clean(Double data) {
            if (data.equals(0.0)) {
                throw new IllegalArgumentException("Zero is not allowed");
            }

            return Optional.of(data + 1.0);
        }
    }

    static class TestExceptionHandler implements ExceptionHandler {

        private String lastMessage;
        private Throwable lastException;

        @Override
        public void warn(String message) {
            throw new AssertionError("should not be called");
        }

        @Override
        public void handle(Exception ex) {
            throw new AssertionError("should not be called");
        }

        @Override
        public void handle(String message, Exception ex) {
            this.lastMessage = message;
            this.lastException = ex;
        }
    }

    @Test
    public void testErrorCatching() {

        TestExceptionHandler errorHandler = new TestExceptionHandler();

        ExceptionCatchingCleaner<Double> instance =
                new ExceptionCatchingCleaner(new TestDataCleaner(), d -> d.toString(), errorHandler);

        Double input1 = 5.0;
        Optional<Double> output1 = instance.clean(input1);

        assertThat("The Cleaner generated an output", output1.isPresent(), is(true));
        assertThat("The Cleaner was applied because the number increase", output1.get(), is(6.0));

        Double input2 = 0.0;
        Optional<Double> output2 = instance.clean(input2);
        assertThat("The TestDataCleaner should remove zeros", output2.isPresent(), is(false));
        assertThat("We have a history of the failing input", errorHandler.lastMessage, is("0.0"));
        assertThat(
                "We have the exact exception", errorHandler.lastException, instanceOf(IllegalArgumentException.class));
    }
}
