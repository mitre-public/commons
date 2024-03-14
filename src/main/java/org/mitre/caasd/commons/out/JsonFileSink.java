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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.mitre.caasd.commons.Functions.ToStringFunction;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.util.ExceptionHandler;
import org.mitre.caasd.commons.util.SequentialFileWriter;

/**
 * An OutputSink that writes {@link JsonWritable} records as json files placed in a configurable
 * directory.
 */
public class JsonFileSink<T extends JsonWritable> implements OutputSink<T> {

    /** The root directory where files documenting each event are placed. */
    private final String outputDirectory;

    /** Catches {@link Exception}s thrown while writing events to file . */
    private final ExceptionHandler exceptionHandler;

    /** Generates a file name (not extension) per datum */
    private final ToStringFunction<T> fileNamer;

    /** Returns a subdirectory to place the output data relative to {@link #outputDirectory} */
    private final Function<T, Path> subdirectoryStrategy;

    /**
     * Create an {@link JsonFileSink} that publishes json files to the given directory with files
     * named according to the {@code fileNamer} strategy
     *
     * @param outputDirectory The output directory of choice
     * @param fileNamer       The function for naming the file, based on the {@link JsonWritable}
     *                        (not including the file extension b/c this is json)
     */
    public JsonFileSink(String outputDirectory, ToStringFunction<T> fileNamer) {
        this(outputDirectory, fileNamer, noSubdirectories());
    }

    /**
     * Create an {@link JsonFileSink} that publishes json files to the given directory with files
     * named according to the {@code fileNamer} strategy and sub-directories provided by the {@code
     * subDirectory}
     *
     * @param outputDirectory The output directory of choice
     * @param fileNamer       The function for naming the file, based on the {@link JsonWritable}
     *                        (not including the file extension b/c this is json)
     * @param subDir          The function for getting the subdirectory (relative to {@code
     *                        outputDirectory}) of file based on input datum. An empty path will
     *                        result in flat directory
     */
    public JsonFileSink(String outputDirectory, ToStringFunction<T> fileNamer, Function<T, Path> subDir) {
        this(outputDirectory, fileNamer, subDir, new SequentialFileWriter("jsonWritingErrors"));
    }

    /**
     * Create an {@link JsonFileSink} that publishes json files to the given directory with files
     * named according to the {@code fileNamer} strategy and sub-directories provided by the {@code
     * subDirectory}
     *
     * @param outputDirectory  The output directory of choice
     * @param fileNamer        The function for naming the file, based on the {@link JsonWritable}
     *                         (not including the file extension b/c this is json)
     * @param subDir           The function for getting the subdirectory (relative to {@code
     *                         outputDirectory}) of file based on input datum. An empty path will
     *                         result in flat directory
     * @param exceptionHandler Captures and handles exceptions that occur when writing JSON output
     */
    JsonFileSink(
            String outputDirectory,
            ToStringFunction<T> fileNamer,
            Function<T, Path> subDir,
            ExceptionHandler exceptionHandler) {
        this.outputDirectory = outputDirectory;
        this.exceptionHandler = checkNotNull(exceptionHandler);
        this.fileNamer = checkNotNull(fileNamer, "The file-naming function cannot be null");
        this.subdirectoryStrategy = checkNotNull(subDir);
    }

    /**
     * Create two files for this event. The first file contains an EventRecord that summarizes the
     * event. The second file contains the raw track data associated with the event.
     *
     * @param event A CompleteEvent and its raw track data
     */
    @Override
    public void accept(T event) {
        Path subdirectory = this.subdirectoryStrategy.apply(event);

        writeRecordToFile(
                event.asJson() + "\n", // Every record gets sent to a new line (just like System.out)
                subdirectory,
                fileNamer.apply(event));
    }

    private void writeRecordToFile(String json, Path subdirectory, String filePrefix) {
        try {
            Path targetDirectory = Paths.get(outputDirectory).resolve(subdirectory);
            makeDirs(targetDirectory.toFile());
            String fullPath = targetDirectory
                    .resolve(filePrefix + ".json")
                    .normalize()
                    .toAbsolutePath()
                    .toString();

            // we MUST retain the ability to write multiple JsonWritable objects to the same file
            // If you want to support additional behavior add another field to the class that toggles this behavior.
            FileUtils.appendToFile(fullPath, json);

        } catch (Exception ex) {
            exceptionHandler.handle("Error creating a file to contain a " + JsonWritable.class.getSimpleName(), ex);
        }
    }

    /**
     * @param candidate - to create if absent
     *
     * @throws IOException - if absent and unable to create
     */
    static void makeDirs(File candidate) throws IOException {
        candidate.mkdirs();

        if (!candidate.isDirectory()) {
            throw new IOException("Unable to create candidate '" + candidate + "'");
        }
    }

    public static <T> Function<T, Path> noSubdirectories() {
        // cache the return val to try and maintain the blazing fast speed of this rocket ship
        Path empty = Paths.get("");
        return in -> empty;
    }
}
