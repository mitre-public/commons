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

import java.time.Duration;
import java.util.function.Function;

/**
 * A JsonGzFileSink copies data it receives to one or more compressed gz files.
 * <p>
 * A JsonGzFileSink is designed to have a long lifespan (aka multiple days) and continue to write
 * data as part of an enduring process. Consequently, an JsonGzFileSink will automatically notice
 * when a target gz file is no longer being written to. When this occurs the JsonGzFileSink will
 * close the pipe to that file.
 * <p>
 * While a gz file is being written to its name will appear like "UNDER_CONSTRUCTION_myArchive.gz"
 * When the gz file is closed for writing its name will become "myArchive.gz" If there is a long
 * pause between writing data to an archive file the "second time" that archive is created it will
 * have "_N" appended to its name, e.g. "myArchive_1.gz"
 * <p>
 * BE AWARE that a JsonGzFileSink starts a non-daemon thread to monitor which gz files are being
 * written to and which gz files need to be closed. This thread will keep the JVM from closing
 * unless it is shutdown by calling {@code close()}.
 * <p>
 * Example usages:
 * <pre>{@code
 * //Steadily create daily gz files from a persistent stream
 * Supplier<MyDataType> source = getDataSource();
 * JsonGzFileSink sink = new JsonGzFileSink(
 *     "archives",
 *     t -> t.getDate(), //write to a file titled "YYYY-MM-DD.gz"
 *     Duration.ofHours(1)
 * );
 * //this while loop can run for months
 * while(true) {
 *    sink.accept(source.get());
 * }
 * }</pre>
 *
 * <pre>{@code
 * //Divide a large collection into several gz files
 * JsonGzFileSink sink = new JsonGzFileSink(
 *     "archives",
 *     t -> t.getDate(), //write to a file titled "YYYY-MM-DD.gz"
 *     Duration.ofHours(1)
 * );
 * hugeList.forEach(sink);
 * sink.close();
 * }</pre>
 */
public class JsonGzFileSink<T extends JsonWritable> extends GzFileSink<T> implements OutputSink<T> {

    /**
     * @param outputDir      The directory where the .gz files are written
     * @param fileNamer      Generates a target file name for each input record. This strategy
     *                       should yield relatively few unique file names. If too many file names
     *                       are generated you'll open too many file handles and kill performance or
     *                       just crash.
     * @param expirationTime How long between writing pieces of data until a .gz file is closed
     */
    public JsonGzFileSink(String outputDir, Function<T, String> fileNamer, Duration expirationTime) {
        super(outputDir, (item) -> item.asJson(), fileNamer, expirationTime);
    }
}
