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

import static java.util.Objects.isNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Exceptions {

    /**
     * Captures and returns the result of {@code ex.printStackTrace()}
     *
     * @param ex Any Throwable
     *
     * @return The stack trace as a String
     */
    public static String stackTraceOf(Throwable ex) {
        if (isNull(ex)) {
            return "Exception is null: no stack trace available";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
