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
import static org.hamcrest.Matchers.*;

import java.sql.SQLException;

import org.mitre.caasd.commons.Pair;

import org.junit.jupiter.api.Test;

public class BasicExceptionHandlerTest {

    @Test
    public void testInitialState() {
        BasicExceptionHandler handler = new BasicExceptionHandler();
        assertThat(handler.numWarnings(), is(0));
        assertThat(handler.warnings(), empty());

        assertThat(handler.numExceptions(), is(0));
        assertThat(handler.exceptions(), empty());
    }

    @Test
    public void testExceptionHandling() {

        BasicExceptionHandler handler = new BasicExceptionHandler();

        ArrayIndexOutOfBoundsException ex1 = new ArrayIndexOutOfBoundsException();

        handler.handle("hello", ex1);

        assertThat(handler.numExceptions(), is(1));
        assertThat(handler.exceptions(), hasSize(1));
        assertThat(handler.exceptions().get(0), is(Pair.of("hello", ex1)));

        SQLException ex2 = new SQLException();

        handler.handle(ex2);

        assertThat(handler.numExceptions(), is(2));
        assertThat(handler.exceptions(), hasSize(2));
        assertThat(handler.exceptions().get(1), is(Pair.of("", ex2)));
    }

    @Test
    public void testWarningHandling() {

        BasicExceptionHandler handler = new BasicExceptionHandler();

        handler.warn("first warning");

        assertThat(handler.numWarnings(), is(1));
        assertThat(handler.warnings(), hasSize(1));
        assertThat(handler.warnings().get(0), is("first warning"));

        handler.warn("second warning");

        assertThat(handler.numWarnings(), is(2));
        assertThat(handler.warnings(), hasSize(2));
        assertThat(handler.warnings().get(1), is("second warning"));
    }
}
