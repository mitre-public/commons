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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class PrintStreamOutputDestinationTest {

    static class DummyRecord implements JsonWritable {

        public final int anImportantValue;

        public DummyRecord(int i) {
            this.anImportantValue = i;
        }
    }

    @Test
    public void printStreamRecievesOutputOfToStringFunction() {

        // create a PrintStream to print output to.
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outContent, true);

        PrintStreamSink destination =
                new PrintStreamSink<DummyRecord>((record) -> "--" + record.anImportantValue + "--", stream);

        destination.accept(new DummyRecord(123));

        assertThat(outContent.toString(), is("--123--" + System.lineSeparator()));
    }

    @Test
    public void stdOutRecievesOutput() {

        PrintStream oldSystemOut = System.out;

        // redirect System.out to a PrintStream we can inspect
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outContent, true);
        System.setOut(stream);

        DummyRecord record = new DummyRecord(123);

        PrintStreamSink<DummyRecord> destination = new PrintStreamSink<>(t -> t.asJson(), System.out);
        destination.accept(record);

        oldSystemOut.print(record.asJson());

        assertThat(outContent.toString(), is("{\n  \"anImportantValue\": 123\n}" + System.lineSeparator()));

        // reinstate the proper System.out
        System.setOut(oldSystemOut);
    }
}
