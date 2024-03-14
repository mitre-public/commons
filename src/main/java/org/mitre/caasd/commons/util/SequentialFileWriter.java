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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.mitre.caasd.commons.fileutil.FileUtils.appendToFile;
import static org.mitre.caasd.commons.util.DemotedException.demote;
import static org.mitre.caasd.commons.util.Exceptions.stackTraceOf;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.mitre.caasd.commons.fileutil.FileUtils;

/**
 * A SequentialFileWriter creates sequentially numbered text files in a specific target directory.
 * <p>
 * Each numbered text file will contain either a warning message or the stack trace of a Throwable.
 * <p>
 * SequentialFileWriter implements the ErrorHandler interface so that it can be used as a convenient
 * way to organize warnings and error messages.
 */
public class SequentialFileWriter implements ExceptionHandler {

    // this threadsafe counter ensures files are logged in different files..
    private final AtomicInteger fileCounter = new AtomicInteger(0);

    private final String targetDirectory;

    /**
     * Create a WarningWriter that write files to System.getProperty("user.dir").
     */
    public SequentialFileWriter() {
        this(System.getProperty("user.dir"));
    }

    /**
     * Create a WarningWriter that will write files to the specified directory.
     *
     * @param directory Where file will be written.
     */
    public SequentialFileWriter(String directory) {
        checkNotNull(directory);
        this.targetDirectory = directory.endsWith(File.separator) ? directory : directory + File.separator;
    }

    /**
     * Create a new numbered file containing this message
     *
     * @param filePrefix The new file is named: "filePrefix_NUMBER.txt"
     * @param message    The contents of the file
     */
    public void write(String filePrefix, String message) {
        File newFile = createNumberedFile(filePrefix);
        appendSilently(newFile, message);
    }

    /**
     * Create a warning file containing the provided message.
     *
     * @param message A Message that will be included in the Warning file.
     */
    @Override
    public void warn(String message) {

        File warningFile = createNumberedFile("warning");

        /*
         * Note, create the entire warning message and write to the file exactly once. This is more
         * efficient AND prevents an issue in which sometimes only a portion of the warning message
         * got written to the target file
         */
        String completeMessage = authorWarningMessage(message);
        appendSilently(warningFile, completeMessage);

        System.out.println("\n\n**** WARNING WRITTEN TO :: " + warningFile.getName() + " ****\n\n");
    }

    /**
     * Create a new error file containing the provided message as well as the stack trace of the
     * provided Exception.
     *
     * @param message A Message that will be included in the Warning file.
     * @param traceMe The source of a stack trace and message that will be included in the Warning
     *                File.
     */
    @Override
    public void handle(String message, Exception traceMe) {

        File errorFile = createNumberedFile("error");

        /*
         * Note, create the entire error message and write to the file exactly once. This is more
         * efficient AND prevents an issue in which sometimes only a portion of the error message
         * got written to the target file
         */
        String completeMessage = authorErrorMessage(message, traceMe);
        appendSilently(errorFile, completeMessage);

        System.out.println("\n\n**** STACK TRACE WRITTEN TO :: " + errorFile.getName() + " ****\n\n");
    }

    /**
     * @param prefix The first word in the file name. For example "warning" or "error"
     *
     * @return A new File (in the target directory) with a name like "warning_0.txt" or
     *     "error_0.txt"
     */
    private File createNumberedFile(String prefix) {

        FileUtils.makeDirIfMissing(targetDirectory);

        int fileNumber = fileCounter.getAndIncrement();

        String fileName = prefix + "_" + fileNumber + ".txt";

        return new File(targetDirectory + fileName);
    }

    private void appendSilently(File targetFile, String content) {
        try {
            appendToFile(targetFile, content);
        } catch (Exception ioe) {
            throw demote(ioe);
        }
    }

    /** Create the entire contents of a warning message. */
    private String authorWarningMessage(String message) {

        String msg = nonNull(message) ? message : "NO WARNING MESSAGE";

        return timestampStr() + "\n" + msg + "\n";
    }

    /** Create the entire contents of a error message. */
    private String authorErrorMessage(String message, Throwable traceMe) {

        String msg = nonNull(message) ? message : "NO ERROR MESSAGE";

        return new StringBuilder()
                .append(timestampStr() + "\n")
                .append(msg + "\n")
                .append(stackTraceOf(traceMe) + "\n")
                .toString();
    }

    private static String timestampStr() {

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.US)
                .withZone(ZoneId.systemDefault());

        return "Time: " + formatter.format(Instant.now());
    }
}
