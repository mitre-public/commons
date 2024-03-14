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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.util.ErrorCatchingTask.ignoreAndRethrow;
import static org.mitre.caasd.commons.util.ErrorCatchingTask.killJvmOnError;

import org.mitre.caasd.commons.util.ErrorCatchingTask.ErrorHandlingPolicy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ErrorCatchingTaskTest {

    static class BasicErrorHandler implements ExceptionHandler {

        Throwable caught = null;

        @Override
        public void warn(String message) {
            throw new UnsupportedOperationException(
                    "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void handle(Exception ex) {
            caught = ex;
        }

        @Override
        public void handle(String message, Exception ex) {
            throw new UnsupportedOperationException(
                    "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
        }
    }

    @Test
    public void exceptionsAreCaught() {

        BasicErrorHandler errorHandler = new BasicErrorHandler();

        ErrorCatchingTask ect = new ErrorCatchingTask(
                () -> {
                    throw new ArrayIndexOutOfBoundsException();
                },
                errorHandler);
        ect.run();

        assertThat(errorHandler.caught, notNullValue());
    }

    @Test
    public void errorsAreRethrown() {

        ErrorCatchingTask protectedTask = new ErrorCatchingTask(
                () -> {
                    causeStackOverflowError();
                },
                new SequentialFileWriter(),
                new NoticeError());

        assertThrows(StackOverflowError.class, () -> protectedTask.run());
    }

    @Test
    public void errorsArePassedToErrorHandlingPolicy() {

        NoticeError errorHandler = new NoticeError();

        ErrorCatchingTask protectedTask = new ErrorCatchingTask(
                () -> {
                    causeStackOverflowError();
                },
                new SequentialFileWriter(),
                errorHandler);

        assertThat(errorHandler.gotError, is(false));

        try {
            protectedTask.run();
            fail("Should not get here because the protectedTask should throw an Error");
        } catch (Error er) {
            // the error did escape -- but the errorHandler saw it
            assertThat(errorHandler.gotError, is(true));
        }
    }

    @Test
    public void ignoreAndRethrowPolicyIsAvailable() {
        ErrorHandlingPolicy policy = ignoreAndRethrow();

        assertDoesNotThrow(() -> policy.handleError(new StackOverflowError()));
    }

    @Disabled // because killing the JVM can't be part of your standard test workflow
    @Test
    public void killJvmPolicyIsAvailable() {
        ErrorHandlingPolicy policy = killJvmOnError(17);

        assertDoesNotThrow(() -> policy.handleError(new StackOverflowError()));
    }

    static class NoticeError implements ErrorHandlingPolicy {

        boolean gotError = false;

        @Override
        public void handleError(Error error) {
            gotError = true;
        }
    }

    public static void causeStackOverflowError() {
        causeStackOverflowError();
    }
}
