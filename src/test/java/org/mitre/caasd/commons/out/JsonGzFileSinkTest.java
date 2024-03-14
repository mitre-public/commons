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

package org.mitre.caasd.commons.out;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.caasd.commons.fileutil.FileUtils.gzFileLines;
import static org.mitre.caasd.commons.out.JsonGzFileSink.IN_PROGRESS_PREFIX;

import java.io.File;
import java.time.Duration;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JsonGzFileSinkTest {

    @TempDir
    public File tempDir;

    @Test
    public void canWriteMultipleThingsToSameFile() throws Exception {

        File expectFile = new File(tempDir, "aTargetFile.gz");
        File expectInProgressFile = new File(tempDir, IN_PROGRESS_PREFIX + "aTargetFile.gz");

        // none of the files exist yet....
        assertThat(expectFile.exists(), is(false));
        assertThat(expectInProgressFile.exists(), is(false));

        JsonGzFileSink<JsonableData> sink =
                new JsonGzFileSink<>(tempDir.toString(), (item) -> "aTargetFile", Duration.ofHours(1));

        JsonableData item1 = new JsonableData("one", 1);
        JsonableData item2 = new JsonableData("two", 2);
        JsonableData item3 = new JsonableData("three", 3);
        JsonableData item4 = new JsonableData("four", 4);

        sink.accept(item1);
        sink.accept(item2);
        sink.accept(item3);
        sink.accept(item4);

        // we manually drain the data because the automatic process occurs once a second, no need to wait
        sink.drainQueueAndWriteRecords();

        // after the drain, the "IN_PROGRESS_PREFIX" file exists, but the "DONE" file doesn't
        assertThat(expectInProgressFile.exists(), is(true));
        assertThat(expectFile.exists(), is(false));
        assertThat(sink.numOpenWriters(), is(1));

        sink.close();

        // after the close, the "IN_PROGRESS_PREFIX" file no longers exists, but the "DONE" file appears
        assertThat(expectInProgressFile.exists(), is(false));
        assertThat(expectFile.exists(), is(true));
        assertThat(sink.numOpenWriters(), is(0));

        // Now we inspect the archive file to ensure it contains everything we need.
        List<String> lines = gzFileLines(expectFile);

        assertThat(lines.contains(item1.asJson()), is(true));
        assertThat(lines.contains(item2.asJson()), is(true));
        assertThat(lines.contains(item3.asJson()), is(true));
        assertThat(lines.contains(item4.asJson()), is(true));
    }

    @Test
    public void canWriteThingsToMultipleFiles() throws Exception {

        File expectFileA = new File(tempDir, "a.gz");
        File expectFileB = new File(tempDir, "b.gz");

        assertThat(expectFileA.exists(), is(false));
        assertThat(expectFileB.exists(), is(false));

        JsonGzFileSink<JsonableData> sink =
                new JsonGzFileSink<>(tempDir.toString(), (item) -> item.key, Duration.ofHours(1));

        JsonableData item1 = new JsonableData("a", 1);
        JsonableData item2 = new JsonableData("a", 2);
        JsonableData item3 = new JsonableData("b", 3);
        JsonableData item4 = new JsonableData("b", 4);

        sink.accept(item1);
        sink.accept(item2);
        sink.accept(item3);
        sink.accept(item4);

        sink.drainQueueAndWriteRecords();
        assertThat(sink.numOpenWriters(), is(2));
        sink.close();
        assertThat(sink.numOpenWriters(), is(0));

        assertThat(expectFileA.exists(), is(true));
        assertThat(expectFileB.exists(), is(true));

        List<String> linesA = gzFileLines(expectFileA);
        List<String> linesB = gzFileLines(expectFileB);

        assertThat(linesA.contains(item1.asJson()), is(true));
        assertThat(linesA.contains(item2.asJson()), is(true));
        assertThat(linesB.contains(item3.asJson()), is(true));
        assertThat(linesB.contains(item4.asJson()), is(true));
    }

    @Test
    public void targetFileNameCounterAppearsOnSecondOpen() throws Exception {
        /*
         * Say you write data to XYZ.gz, eventually that file is closed. Then you try to add more
         * data to XYZ.gz, this should produce a new file name with an number added on eg XYZ_1.gz
         */
        File expectFile1 = new File(tempDir, "aTargetFile.gz");
        File expectFile2 = new File(tempDir, "aTargetFile_1.gz");

        assertThat(expectFile1.exists(), is(false));
        assertThat(expectFile2.exists(), is(false));

        JsonGzFileSink<JsonableData> sink =
                new JsonGzFileSink<>(tempDir.toString(), (item) -> "aTargetFile", Duration.ofHours(1));

        JsonableData item1 = new JsonableData("one", 1);
        JsonableData item2 = new JsonableData("two", 2);
        JsonableData item3 = new JsonableData("three", 3);
        JsonableData item4 = new JsonableData("four", 4);

        // this data should appear in "aTargetFile.gz"
        sink.accept(item1);
        sink.accept(item2);
        sink.flushAndCloseCurrentFiles();

        assertThat(expectFile1.exists(), is(true));

        List<String> lines1 = gzFileLines(expectFile1);

        assertThat(lines1.contains(item1.asJson()), is(true));
        assertThat(lines1.contains(item2.asJson()), is(true));

        // this data should appear in "aTargetFile_1.gz"
        sink.accept(item3);
        sink.accept(item4);
        sink.close();

        assertThat(expectFile2.exists(), is(true));

        List<String> lines2 = gzFileLines(expectFile2);

        assertThat(lines2.contains(item3.asJson()), is(true));
        assertThat(lines2.contains(item4.asJson()), is(true));
    }

    @Disabled // skipping because this test has a 5 second wait
    @Test
    public void backgroundThreadFinalizesArchivesRecords() throws Exception {

        File expectFile = new File(tempDir, "aTargetFile.gz");

        assertThat(expectFile.exists(), is(false));

        // Any data published to this sink should be fully flushed and closed within 2 or 3 seconds
        JsonGzFileSink<JsonableData> sink =
                new JsonGzFileSink<>(tempDir.toString(), (item) -> "aTargetFile", Duration.ofSeconds(2));

        JsonableData item1 = new JsonableData("one", 1);
        JsonableData item2 = new JsonableData("two", 2);
        JsonableData item3 = new JsonableData("three", 3);
        JsonableData item4 = new JsonableData("four", 4);

        sink.accept(item1);
        sink.accept(item2);
        sink.accept(item3);
        sink.accept(item4);

        // wait..
        Thread.sleep(5_000);

        assertThat(expectFile.exists(), is(true));

        List<String> lines = gzFileLines(expectFile);

        assertThat(lines.contains(item1.asJson()), is(true));
        assertThat(lines.contains(item2.asJson()), is(true));
        assertThat(lines.contains(item3.asJson()), is(true));
        assertThat(lines.contains(item4.asJson()), is(true));
    }

    static class JsonableData implements JsonWritable {

        public final String key;

        public final int importantValue;

        public JsonableData(String key, int value) {
            this.key = key;
            this.importantValue = value;
        }

        @Override
        public String asJson() {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(this);
        }
    }
}
