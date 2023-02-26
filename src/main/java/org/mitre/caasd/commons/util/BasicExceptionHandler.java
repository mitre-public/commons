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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.mitre.caasd.commons.Pair;

/**
 * A BasicExceptionHandler catches and retains parameters passed when calling the ExceptionHandler
 * methods. This implementation of ExceptionHandler is designed to be useful for writing Unit
 * tests.
 */
public class BasicExceptionHandler implements ExceptionHandler {

    private final List<String> warnings = newArrayList();

    private final List<Pair<String, Exception>> labeledExceptions = newArrayList();

    @Override
    public void warn(String message) {
        warnings.add(message);
    }

    /** @return The number of warnings the ExceptionHandler has received. */
    public int numWarnings() {
        return warnings.size();
    }

    /** @return A List of all String input parameters received via calls to warn(String). */
    public List<String> warnings() {
        return warnings;
    }

    @Override
    public void handle(String message, Exception ex) {
        labeledExceptions.add(Pair.of(message, ex));
    }

    /** @return The number of Exceptions the ExceptionHandler has handled. */
    public int numExceptions() {
        return labeledExceptions.size();
    }

    /**
     * @return A List of all String+Exception input parameters received via calls to
     *     handle(String,Exception).
     */
    public List<Pair<String, Exception>> exceptions() {
        return labeledExceptions;
    }
}
