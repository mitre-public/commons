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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An ErrorCatchingTask is a Decorator that passes Exceptions thrown by a Runnable's run() method to
 * a dedicated ExceptionHandler. Any Errors thrown by the Runnable are caught, provided to the
 * ErrorHandlingPolicy, and then rethrown.
 * <p>
 * ErrorCatchingTask is intended to:
 * <p>
 * (1) Make it easy to combine pre-built "Error Handling logic" and pre-built "Task logic" into a
 * single "Task with well understood error handling".
 * <p>
 * (2) Provide a better way to handle Exceptions and Errors that cause confusing behavior in
 * ExecutorServices. The issue with ExecutorServices is that the Runnable objects executed within
 * them can fail silently unless you (A) use an exception trapping mechanism (like this class) or
 * you (B) actively inspect the {@code Future} objects returned when the Runnables were added to the
 * Executor in the first place. Manually inspecting these {@code Futures} is a MUCH more complicated
 * approach because properly interacting with them is in itself a multi-threaded problem that can
 * lead to unintended thread deadlocks.
 */
public class ErrorCatchingTask implements Runnable {

    // the Exit code provided when an Error causes the ErrorHandlingPolicy to call System.exit(int)
    private static final int EXIT_ERROR_CODE = 123_456_789;

    private final Runnable runMe;

    private final ExceptionHandler exceptionHandler;

    private final ErrorHandlingPolicy errorHandler;

    /**
     * Create an ErrorCatchingTask that adds exception handling to the submitted Runnable.
     *
     * @param runMe            A Runnable that will be run
     * @param exceptionHandler An ExceptionHandler that handles any exceptions thrown by runMe
     * @param errorHandler     A block of code that is called when Errors are encountered.
     */
    public ErrorCatchingTask(Runnable runMe, ExceptionHandler exceptionHandler, ErrorHandlingPolicy errorHandler) {
        this.runMe = checkNotNull(runMe, "The input Runnable cannot be null");
        this.exceptionHandler = checkNotNull(exceptionHandler, "The ErrorHandler cannot be null");
        this.errorHandler = errorHandler;
    }

    /**
     * Create an ErrorCatchingTask that adds a specific ExceptionHandler to a Runnable and ensure
     * THE JVM IS KILLED anytime an Error is encountered. This strict ErrorHandlingPolicy is the
     * default policy because we want to ensure that the "easy way to build an ErrorCatchingTask" is
     * also the "easy way to ensure no Error goes unnoticed" (even when this ErrorCatchingTask is
     * submitted to an ExecutorService)
     *
     * @param runMe            A Runnable that will be run
     * @param exceptionHandler An ExceptionHandler that handles any exceptions thrown by runMe
     */
    public ErrorCatchingTask(Runnable runMe, ExceptionHandler exceptionHandler) {
        this(runMe, exceptionHandler, killJvmOnError(EXIT_ERROR_CODE));
    }

    @Override
    public void run() {
        try {
            runMe.run();
        } catch (Exception ex) {
            exceptionHandler.handle(ex);
        } catch (Error err) {
            errorHandler.handleError(err);
            throw err;
        }
    }

    /**
     * @param errorCode The error code supplied when killing the JVM
     *
     * @return An ErrorHandlingPolicy that prints a stackTrace and exits the JVM via
     *     System.exit(errorCode)
     */
    public static ErrorHandlingPolicy killJvmOnError(int errorCode) {
        return (error) -> {
            System.err.println("Killing JVM from ErrorCatchingTask due to unhandled Error");
            error.printStackTrace();
            System.exit(errorCode);
        };
    }

    /** @return An ErrorHandlingPolicy that does no extra work, the Error is merely rethrown. */
    public static ErrorHandlingPolicy ignoreAndRethrow() {
        return error -> {};
    }

    @FunctionalInterface
    public interface ErrorHandlingPolicy {

        void handleError(Error error);
    }
}
