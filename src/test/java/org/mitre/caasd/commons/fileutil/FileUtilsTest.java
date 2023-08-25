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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.fileutil.FileUtils.deserialize;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class FileUtilsTest {

    private static final String USER = System.getProperty("user.dir");

    private static final String GZIPPED_FILE = USER + "/src/test/resources/oneTrack.txt.gz";
    private static final String UNZIPPED_FILE = USER + "/src/test/resources/oneTrack.txt";

    @Test
    public void testIsGZipFile() throws Exception {
        assertTrue(FileUtils.isGZipFile(new File(GZIPPED_FILE)));
        assertFalse(FileUtils.isGZipFile(new File(UNZIPPED_FILE)));
    }

    @Test
    public void testWriteToNewGzFile() throws Exception {

        String fileName = "doesThisFileGetWrittenProperly.gz";
        String fileContents = "this text goes inside the file";

        assertThat(
            "The file should not exist at the beginning of the test",
            new File(fileName).exists(), is(false)
        );

        FileUtils.writeToNewGzFile(fileName, fileContents);

        assertThat("The file should exist now", new File(fileName).exists());

        BufferedReader reader = FileUtils.createReaderFor(new File(fileName));

        assertThat(
            "The file should have the expected contents",
            reader.readLine().equals(fileContents)
        );
        assertThat(
            "That should have been the only line",
            reader.readLine() == null
        );

        reader.close();

        new File(fileName).delete(); //clean up
    }

    @Test
    public void testDeserializeViaInputStream() {
        //This file can be found in the resource folder
        String TEST_FILE = "serializedArrayListOfTenDoubles.ser";
        InputStream stream = getClass().getResourceAsStream(TEST_FILE);

        Object obj = FileUtils.deserialize(stream);
        ArrayList<Double> list = (ArrayList<Double>) obj;

        assertEquals(10, list.size());

        double TOL = 0.000001;
        for (int i = 0; i < list.size(); i++) {
            assertEquals(i * 1.0, list.get(i), TOL);
        }
    }

    @Test
    public void testDeserializeFile_missingFile() {

        File file = new File("ThisFileDoesnotExist.txt");

        //should throw an IllegalArgumentException because the file didn't exist
        assertThrows(IllegalArgumentException.class,
            () -> deserialize(file)
        );
    }

    @Test
    public void testGetResourceFile() throws Exception {
        //we cannot just use: "findMe.txt" because this method isn't based off a particular class
        File file = getResourceFile("org/mitre/caasd/commons/fileutil/findMe.txt");

        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(1, lines.size());
        assertEquals("hello", lines.get(0));
    }

    @Test
    public void testGetResourceFile_missingResource() {
        //"Should fail because the request resource doesnt exist
        assertThrows(IllegalArgumentException.class,
            () -> getResourceFile("thisFileDoesntExist.missing")
        );
    }

    @Test
    public void getPropertiesRejectsFilesWithDuplicateKeys() {

        File rejectFile = getResourceFile("org/mitre/caasd/commons/fileutil/rejectThisPropertiesFile.props");

        assertTrue(rejectFile.exists());

        assertThrows(IllegalArgumentException.class,
            () -> FileUtils.getProperties(rejectFile)
        );
    }

    @Test
    public void getPropertiesWorksWhenDuplicateKeysAreInComments() throws Exception {

        File acceptFile = getResourceFile("org/mitre/caasd/commons/fileutil/acceptThisPropertiesFile.props");

        assertTrue(acceptFile.exists());

        Properties props = FileUtils.getProperties(acceptFile);

        assertThat(props.getProperty("key1"), is("goodValue"));
    }
}
