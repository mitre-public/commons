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
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.mitre.caasd.commons.Functions.ToStringFunction;
import org.mitre.caasd.commons.fileutil.FileUtils;

/**
 * A PrintStreamSink converts each incoming data record to a String and forwards it as new line of
 * text printed to a PrintSteam.
 *
 * <p>Some planned uses include: (1) bridging the ARIA and CDA/TDP codebases by piping output from
 * the ARIA code-base to System.out where it can be received and archived by the CDA code, (2)
 * piping output directly to a single flat file.
 *
 * <p>Output published via a PrintStream is usually aggregated in some way for bulk analysis.
 */
public class PrintStreamSink<T> implements OutputSink<T> {

    private final ToStringFunction<T> stringConverter;

    private final PrintStream stream;

    /**
     * Create an PrintStreamSink that will apply a custom ToStringFunction conversion to each
     * incoming item and send it to the a PrintStream.
     *
     * @param formatter Converts an item of type T to a String
     * @param stream    The destination PrintStream
     */
    public PrintStreamSink(ToStringFunction<T> formatter, PrintStream stream) {
        this.stringConverter = checkNotNull(formatter);
        this.stream = checkNotNull(stream);
    }

    /**
     * Writes data records (received via {@code accept(T)}) to {@code eventRecord.txt} in the
     * specified directory. If the directory does not exist, it is created before writing.
     *
     * <p>Remember to close the stream to free resources when finished.
     */
    public static <T> PrintStreamSink<T> writeRecordToFlatFile(String outputDirectory, ToStringFunction<T> formatter) {

        FileUtils.makeDirIfMissing(outputDirectory);
        File outputFile = new File(outputDirectory, "eventRecords.txt");

        /*
         * This code produces a "Unreleased Resource" Finding in FAA code scans. This finding is a
         * false positive because the whole purpose of this code is to open a log-running pipe to an
         * output file that will continually recieve output data.
         */
        try {
            return new PrintStreamSink<>(formatter, new PrintStream(outputFile));
        } catch (FileNotFoundException fnfe) {
            throw demote(fnfe);
        }
    }

    @Override
    public void accept(T item) {
        String converted = stringConverter.apply(item);
        stream.println(converted);
    }

    /** Close the output stream. */
    @Override
    public void close() {
        stream.close();
    }
}
