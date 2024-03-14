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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

public class SequentialfileWriterTest {

    @Test
    public void testCreateWarningFile() {
        /*
         * Ensure that the warning file is made correctly. Specifically, confirm that (1) a new
         * directory can be made if necessary and (2) that the warning file itself is written to the
         * target directory.
         */

        String directory = "testDir";
        String message = "simpleMessage";

        SequentialFileWriter writer = new SequentialFileWriter(directory);
        writer.warn(message);

        File wasThisfileMade = new File(directory + File.separator + "warning_0.txt");

        assertTrue(wasThisfileMade.exists());

        wasThisfileMade.delete();
        File dirAsFile = new File(directory);
        dirAsFile.delete();
    }

    @Test
    public void testCreateWarningFileWithException() {
        /*
         * Ensure that the warning file is made correctly. Specifically, confirm that (1) a new
         * directory can be made if necessary and (2) that the warning file itself is written to the
         * target directory.
         */

        String directory = "testDir2";
        String message = "simpleMessage";
        Exception ex = new RuntimeException("hello");

        SequentialFileWriter writer = new SequentialFileWriter(directory);
        writer.handle(message, ex);

        File wasThisfileMade = new File(directory + File.separator + "error_0.txt");

        assertTrue(wasThisfileMade.exists());

        wasThisfileMade.delete();
        File dirAsFile = new File(directory);
        dirAsFile.delete();
    }

    @Test
    public void willNotFailOnNullMessage() {
        /*
         * Ensure that the warning file is made correctly even though a null message is
         * provided.Confirm that (1) a new directory can be made if necessary and (2) that the
         * warning file itself is written to the target directory.
         */

        String directory = "testDir3";
        String nullMessage = null;
        Exception ex = new RuntimeException("hello");

        SequentialFileWriter writer = new SequentialFileWriter(directory);
        writer.handle(nullMessage, ex);

        File wasThisfileMade = new File(directory + File.separator + "error_0.txt");

        assertThat(wasThisfileMade.exists(), is(true));

        wasThisfileMade.delete();
        File dirAsFile = new File(directory);
        dirAsFile.delete();
    }
}
