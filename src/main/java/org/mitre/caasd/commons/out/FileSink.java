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
import static org.mitre.caasd.commons.fileutil.FileUtils.makeDirIfMissing;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.util.function.Consumer;

import org.mitre.caasd.commons.Functions.ToStringFunction;
import org.mitre.caasd.commons.fileutil.FileUtils;

/**
 * This Consumer writes every String it accepts to a File on the local file system.
 *
 * <p>This is similar to {@link JsonFileSink} only a FileSink takes String input rather than
 * JsonWritable input.
 */
public class FileSink implements Consumer<String> {

    // @todo -- Consider adding the sub-directory Strategy that JsonFileSink supports.

    /** The root directory where files documenting each event are placed. */
    private final String outputDirectory;

    /** Generates a file name (not extension) per datum */
    private final ToStringFunction<String> fileNamer;

    /**
     * Create an {@link FileSink} that publishes messages to a File in the given directory. The name
     * of the target file is determined by the {@code fileNamer} strategy.
     *
     * @param outputDirectory The output directory of choice
     * @param fileNamer       The function for naming the target file.
     */
    public FileSink(String outputDirectory, ToStringFunction<String> fileNamer) {
        this.outputDirectory = outputDirectory;
        this.fileNamer = checkNotNull(fileNamer, "The file-naming function cannot be null");
    }

    @Override
    public void accept(String message) {

        makeDirIfMissing(outputDirectory);
        String fileName = fileNamer.apply(message);
        String fullFileName = outputDirectory + File.separator + fileName + ".txt";

        try {
            // every message gets a new line (just like System.out.println(msg))
            FileUtils.appendToFile(fullFileName, message + "\n");
        } catch (Exception ex) {
            throw demote("Error writing message to FileSink, fileName = " + fileName, ex);
        }
    }
}
