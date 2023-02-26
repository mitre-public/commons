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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

public class DemotedExceptionTest {

    @Test
    public void canAccessCause() {
        FileNotFoundException fnfe1 = new FileNotFoundException();
        DemotedException de = new DemotedException(fnfe1);
        assertThat(de.getCause(), is(fnfe1));

        FileNotFoundException fnfe2 = new FileNotFoundException();
        DemotedException de2 = new DemotedException("message", fnfe2);
        assertThat(de2.getMessage(), is("message"));
        assertThat(de2.getCause(), is(fnfe2));
    }

    @Test
    public void cannotMakeDemotedExceptionFromRuntimeException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DemotedException(new ArrayIndexOutOfBoundsException())
        );
    }

    @Test
    public void cannotMakeDemotedExceptionFromRuntimeException_2() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DemotedException("message", new ArrayIndexOutOfBoundsException())
        );
    }

    @Test
    public void cannotDemoteARuntimeException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> demote(new ArrayIndexOutOfBoundsException())
        );
    }

    @Test
    public void cannotDemoteARuntimeException_2() {
        assertThrows(
            IllegalArgumentException.class,
            () -> demote("message", new ArrayIndexOutOfBoundsException())
        );
    }

    @Test
    public void canDemoteCheckedExceptions() {
        FileNotFoundException fnfe1 = new FileNotFoundException();
        DemotedException de = demote(fnfe1);
        assertThat(de.getCause(), is(fnfe1));
    }

    @Test
    public void canDemoteCheckedExceptions_2() {
        FileNotFoundException fnfe1 = new FileNotFoundException();
        DemotedException de = demote("message", fnfe1);
        assertThat(de.getCause(), is(fnfe1));
        assertThat(de.getMessage(), is("message"));
    }
}
