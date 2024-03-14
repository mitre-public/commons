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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.caasd.commons.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.caasd.commons.util.Exceptions.stackTraceOf;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class ExceptionsTest {

    @Test
    public void stackTraceString_producesGoodResult() {

        IndexOutOfBoundsException someException = new IndexOutOfBoundsException("hello");

        // catch the result of `someException.printStackTrace()` so we
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        someException.printStackTrace(new PrintStream(os));
        String actualTrace = os.toString();

        assertThat(stackTraceOf(someException), is(actualTrace));
    }
}
