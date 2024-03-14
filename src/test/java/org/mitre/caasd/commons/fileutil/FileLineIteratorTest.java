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

package org.mitre.caasd.commons.fileutil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.mitre.caasd.commons.util.DemotedException;

import org.junit.jupiter.api.Test;

public class FileLineIteratorTest {

    @Test
    public void canIterateOverFileContents() {
        File testFile = getResourceFile("org/mitre/caasd/commons/fileutil/textFile.txt");
        FileLineIterator iter = new FileLineIterator(testFile);

        assertThat(iter.next(), is("line 1"));
        assertThat(iter.next(), is("line 2"));
        assertThat(iter.hasNext(), is(false));
    }

    private FileLineIterator makeIterator() {
        File testFile = getResourceFile("org/mitre/caasd/commons/fileutil/textFile.txt");
        return new FileLineIterator(testFile);
    }

    @Test
    public void iteratorHasNextBecomesFalse() {

        FileLineIterator iter = makeIterator();
        assertThat(iter.hasNext(), is(true));
        iter.next();
        assertThat(iter.hasNext(), is(true));
        iter.next();
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void tooManyNextCallGeneratesException() {
        FileLineIterator iter = makeIterator();
        iter.next();
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void iteratorDoesNotSupportRemove() {

        FileLineIterator iter = makeIterator();
        iter.next();

        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void canCloseIterator() {
        FileLineIterator iter = makeIterator();
        iter.next();

        assertDoesNotThrow(() -> iter.close());
    }

    @Test
    public void closedIteratorsGiveNoData() throws IOException {
        FileLineIterator iter = makeIterator();
        iter.next();
        iter.close();
        assertThat(iter.hasNext(), is(false));

        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void handleEmptyFilesCorrectly() {

        FileLineIterator iter =
                new FileLineIterator(getResourceFile("org/mitre/caasd/commons/fileutil/emptyTextFile.txt"));

        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void canHandleMissingFilesGracefully() {
        try {
            FileLineIterator iter = new FileLineIterator("thisFileDoesNotExist.missing");
            fail("exception should have been thrown");
        } catch (DemotedException de) {
            assertThat(de.getCause(), instanceOf(FileNotFoundException.class));
        }
    }

    @Test
    public void canOpenAndReadGzFile() {
        File testFile = getResourceFile("org/mitre/caasd/commons/fileutil/twoLinesHelloGoodbye.txt.gz");
        FileLineIterator iter = new FileLineIterator(testFile);

        assertThat(iter.next(), is("Hello"));
        assertThat(iter.next(), is("Goodbye"));
        assertThat(iter.hasNext(), is(false));
    }
}
