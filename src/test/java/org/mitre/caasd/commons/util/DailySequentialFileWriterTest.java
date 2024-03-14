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

import java.io.File;
import java.time.ZoneId;

import org.mitre.caasd.commons.fileutil.FileUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.jupiter.api.Test;

public class DailySequentialFileWriterTest {

    @Test
    public void testCreateWarningFile() throws Exception {

        File directory = new File("testDir");
        String message = "simple message that should be written";

        DailySequentialFileWriter writer = new DailySequentialFileWriter(directory.getName());
        writer.warn(message);

        File[] files = directory.listFiles();

        assertThat("Should have created 1 file", files.length, is(1));
        File actual = files[0];

        assertThat(
                "Heading should include WARNING",
                Files.readLines(actual, Charsets.UTF_8).get(0).contains("WARNING"));
        assertThat(
                "Message must be included",
                Files.readLines(actual, Charsets.UTF_8).get(1).contains(message));

        FileUtils.forceDelete(actual);
        FileUtils.deleteDirectory(directory);
    }

    @Test
    public void testCreateWarningFileWithMultiLineMessage() throws Exception {

        File directory = new File("testDir");
        String message =
                "simple message that should be written\n" + "plus some more details here, potentially going into\n"
                        + "a long paragraph describing what happened, why we might think\n"
                        + "it could have happened, etc., even though no one may read this...";

        DailySequentialFileWriter writer = new DailySequentialFileWriter(directory.getName());
        writer.warn(message);

        File[] files = directory.listFiles();

        assertThat("Should have created 1 file", files.length == 1);
        File actual = files[0];

        assertThat(
                "Message should be tabbed.",
                Files.readLines(actual, Charsets.UTF_8).get(4).contains("\tit could have happened"));

        FileUtils.forceDelete(actual);
        FileUtils.deleteDirectory(directory);
    }

    @Test
    public void testCreateWarningFileWithException() throws Exception {

        File directory = new File("testDirErrors");
        Exception ex = new RuntimeException("Something went wrong!");
        int maxStackTraceDepthToPrint = 4;

        DailySequentialFileWriter writer =
                new DailySequentialFileWriter(directory.getName(), ZoneId.of("UTC-5"), maxStackTraceDepthToPrint);
        writer.handle(ex);

        File[] files = directory.listFiles();

        assertThat("Should have created 1 file", files.length == 1);
        File actual = files[0];

        assertThat(
                "Heading should include ERROR",
                Files.readLines(actual, Charsets.UTF_8).get(0).contains("ERROR"));
        assertThat(
                "Message must be included",
                Files.readLines(actual, Charsets.UTF_8).get(1).contains("RuntimeException"));
        // stack trace depth (4) + header (1) + footer (1) + the ellipsis (1)
        assertThat(
                "Stack trace should be truncated",
                Files.readLines(actual, Charsets.UTF_8).size() == maxStackTraceDepthToPrint + 1 + 1 + 1);

        FileUtils.forceDelete(actual);
        FileUtils.deleteDirectory(directory);
    }

    @Test
    public void testCreateWarningFileWithExceptionAndMessage() throws Exception {

        File directory = new File("testDir");
        String message = "Simple message that should be written";
        Exception ex = new RuntimeException("Something went wrong!");

        DailySequentialFileWriter writer = new DailySequentialFileWriter(directory.getName());
        writer.handle(message, ex);

        File[] files = directory.listFiles();

        assertThat("Should have created 1 file", files.length == 1);
        File actual = files[0];

        assertThat(
                "Heading should include ERROR",
                Files.readLines(actual, Charsets.UTF_8).get(0).contains("ERROR"));
        assertThat(
                "Message must be included",
                Files.readLines(actual, Charsets.UTF_8).get(1).contains(message));
        assertThat(
                "Exception must be included",
                Files.readLines(actual, Charsets.UTF_8).get(1).contains("Something went wrong!"));

        FileUtils.forceDelete(actual);
        FileUtils.deleteDirectory(directory);
    }
}
