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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.mitre.caasd.commons.Functions.ToStringFunction;
import org.mitre.caasd.commons.util.ExceptionHandler;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test/Demo: Show how the JsonFileSink can easily (1) accept different classes that implement
 * JsonWritable and (2) how the filenaming scheme can be easily configured to work with a particular
 * input class.
 */
public class JsonFileSinkTest {

    private static final Logger log = LoggerFactory.getLogger(JsonFileSinkTest.class);

    @TempDir
    public File tempDir;

    static class SampleClassThatContainsNamingInformation implements JsonWritable {

        String fileNamePrefix;
        double recordValue;

        public SampleClassThatContainsNamingInformation(String fileName, double recordValue) {
            this.fileNamePrefix = fileName;
            this.recordValue = recordValue;
        }
    }

    static class ClassSpecificFileNamer implements ToStringFunction<SampleClassThatContainsNamingInformation> {

        @Override
        public String apply(SampleClassThatContainsNamingInformation record) {
            // cherry pick some value from the type we expect...in this case, the fileNamePrefix itself
            return record.fileNamePrefix;
        }
    }

    static class FlatDirectory<T extends JsonWritable> implements Function<T, Path> {

        @Override
        public Path apply(T instance) {
            // empty path will functionally trigger flat-directory storage
            return Paths.get("");
        }
    }

    static class SubdirectoryByFilename implements Function<SampleClassThatContainsNamingInformation, Path> {

        @Override
        public Path apply(SampleClassThatContainsNamingInformation sampleClassThatContainsNamingInformation) {
            // just use the filename prefix for something
            return Paths.get(sampleClassThatContainsNamingInformation.fileNamePrefix);
        }
    }

    static class SubdirectoryByValueThenFilename implements Function<SampleClassThatContainsNamingInformation, Path> {

        @Override
        public Path apply(SampleClassThatContainsNamingInformation sampleClassThatContainsNamingInformation) {
            // just use the filename prefix for something
            return Paths.get(
                    Double.toString(sampleClassThatContainsNamingInformation.recordValue),
                    sampleClassThatContainsNamingInformation.fileNamePrefix);
        }
    }

    @Test
    public void showHowAPerRecordSubdirectoryCanBeSpecified() {

        File expectFile_A = new File(tempDir, new File("A", "A.json").toString());
        File expectFile_B = new File(tempDir, new File("B", "B.json").toString());

        // neither file exists....
        assertThat(expectFile_A.exists(), is(false));
        assertThat(expectFile_B.exists(), is(false));

        JsonFileSink jfd =
                newJsonFileDestination(tempDir.toString(), new ClassSpecificFileNamer(), new SubdirectoryByFilename());

        jfd.accept(new SampleClassThatContainsNamingInformation("A", 123.0));
        jfd.accept(new SampleClassThatContainsNamingInformation("B", 345.0));

        // but now they do
        assertThat(expectFile_A.exists(), is(true));
        assertThat(expectFile_B.exists(), is(true));
    }

    @Test
    public void showHowAPerRecordMultipleLevelSubdirectoryCanBeSpecified() throws IOException {

        File expectFile_A =
                tempDir.toPath().resolve("123.0").resolve("A").resolve("A.json").toFile();
        File expectFile_B =
                tempDir.toPath().resolve("345.0").resolve("B").resolve("B.json").toFile();

        // neither file exists....
        assertThat(expectFile_A.exists(), is(false));
        assertThat(expectFile_B.exists(), is(false));

        JsonFileSink jfd = newJsonFileDestination(
                tempDir.toString(), new ClassSpecificFileNamer(), new SubdirectoryByValueThenFilename());

        jfd.accept(new SampleClassThatContainsNamingInformation("A", 123.0));
        jfd.accept(new SampleClassThatContainsNamingInformation("B", 345.0));

        // but now they do
        assertThat(expectFile_A.exists(), is(true));
        assertThat(expectFile_B.exists(), is(true));
    }

    @Test
    public void showHowFilesCanBeExtractedFromInputToFlatDirectory() {

        File expectFile_A = new File(tempDir, "A.json");
        File expectFile_B = new File(tempDir, "B.json");

        // neither file exists....
        assertThat(expectFile_A.exists(), is(false));
        assertThat(expectFile_B.exists(), is(false));

        JsonFileSink jfd =
                newJsonFileDestination(tempDir.toString(), new ClassSpecificFileNamer(), new FlatDirectory());

        jfd.accept(new SampleClassThatContainsNamingInformation("A", 123.0));
        jfd.accept(new SampleClassThatContainsNamingInformation("B", 345.0));

        // but now they do
        assertThat(expectFile_A.exists(), is(true));
        assertThat(expectFile_B.exists(), is(true));
    }

    @Test
    public void showHowFileNamesCanBeSequentiallyGenerated() throws IOException {

        File expectFile_1 = new File(tempDir, "789.json");
        File expectFile_2 = new File(tempDir, "790.json");

        // neither file exists....
        assertThat(expectFile_1.exists(), is(false));
        assertThat(expectFile_2.exists(), is(false));

        JsonFileSink jfd = newJsonFileDestination(tempDir.toString(), new NumericFileNamer(), new FlatDirectory());

        jfd.accept(new SampleClassNamedUsingExternalRules(123.0));
        jfd.accept(new SampleClassNamedUsingExternalRules(345.0));

        // but now they do
        assertThat(expectFile_1.exists(), is(true));
        assertThat(expectFile_2.exists(), is(true));
    }

    /**
     * @return a {@link File} named for calling method under {@link JsonFileSinkTest} class simple
     *     name
     * @throws IOException
     */
    private File newNonExistentFileUnderTemporaryFolder() {
        String outerMethod = new Throwable().getStackTrace()[1].getMethodName();
        return new File(tempDir, outerMethod);
    }

    private <T> JsonFileSink newJsonFileDestination(
            String outputDirectory, ToStringFunction<T> fileNamer, Function<T, Path> subdirectory) {
        return new JsonFileSink(outputDirectory, fileNamer, subdirectory, new VoidExceptionHandler());
    }

    // Files written from these inputs will be numbered numerically
    static class SampleClassNamedUsingExternalRules implements JsonWritable {

        double recordValue;

        public SampleClassNamedUsingExternalRules(double recordValue) {
            this.recordValue = recordValue;
        }
    }

    static class NumericFileNamer implements ToStringFunction<SampleClassNamedUsingExternalRules> {

        int nextNumber = 789; //

        @Override
        public String apply(SampleClassNamedUsingExternalRules record) {
            return Integer.toString(nextNumber++);
        }
    }

    public static class VoidExceptionHandler implements ExceptionHandler {

        @Override
        public void warn(String s) {
            // do nothing
        }

        @Override
        public void handle(String s, Exception e) {
            // do nothing
        }
    }

    public static IsDirectory isDirectory() {
        return new IsDirectory();
    }

    public static class IsDirectory extends TypeSafeMatcher<File> {

        @Override
        protected boolean matchesSafely(File candidate) {
            return candidate.isDirectory();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an existing directory");
        }
    }

    public static class FauxLogEntry implements JsonWritable {

        String logType;
        String msg;

        FauxLogEntry(String logType, String msg) {
            this.logType = logType;
            this.msg = msg;
        }
    }

    @Test
    public void multipleItemsMustGoToSameFile() throws IOException {

        File expectFile_1 = new File(tempDir, "type1.json");
        File expectFile_2 = new File(tempDir, "type2.json");

        // neither file exists....
        assertThat(expectFile_1.exists(), is(false));
        assertThat(expectFile_2.exists(), is(false));

        JsonFileSink<FauxLogEntry> jfd = new JsonFileSink<>(tempDir.toString(), (FauxLogEntry fle) -> {
            return fle.logType;
        });

        jfd.accept(new FauxLogEntry("type1", "a"));
        jfd.accept(new FauxLogEntry("type1", "b"));
        jfd.accept(new FauxLogEntry("type2", "c"));

        // but now they do
        assertThat(expectFile_1.exists(), is(true));
        assertThat(expectFile_2.exists(), is(true));

        // The file with 2 entries is twice as big as the file with one entry
        // i.e. When using a JsonFileSink to write log data the log data must aggregate, not overwrite.
        assertThat(expectFile_1.length() == 2 * expectFile_2.length(), is(true));
    }
}
