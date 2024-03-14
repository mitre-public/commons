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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.mitre.caasd.commons.fileutil.FileUtils;

/**
 * A {@code SequentialFileWriter} creates sequentially numbered text files in a specific target
 * directory, based on the day's date. Each file will contain warning message(s) and/or the stack
 * trace(s) of Throwable(s) that occurred on the corresponding day. The messages are appended until
 * the day is over, at which point a new file is create to collect the day's warnings/stack traces.
 * <p>
 * The contents of the file may look something like the following:
 * <pre> Contents of log_for_2019-02-04.txt:
 * ## WARNING @ 11:05:36 EST  ##
 * simple message that should be written
 * - - - - - - - - - - - - - - - - - -
 * ## WARNING @ 11:05:36 EST  ##
 * simple message that should be written
 * plus some more details here, potentially going into
 * a long paragraph describing what happened, why we might think
 * it could have happened, etc...
 * - - - - - - - - - - - - - - - - - -
 * ## ERROR @ 11:53:12 EST  ##
 * java.lang.RuntimeException: Something went wrong!
 * at org.mitre.caasd.commons.util.DailySequentialFileWriterTest.testCreateWarningFileWithException(DailySequentialFileWriterTest.java:67)
 * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 * ...
 * - - - - - - - - - - - - - - - - - -
 * </pre>
 *
 * @see SequentialFileWriter
 */
public class DailySequentialFileWriter implements ExceptionHandler {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("America/New_York");
    private static final String NEW_LINE_CHAR = System.lineSeparator();
    private static final String FOOTER = NEW_LINE_CHAR + "- - - - - - - - - - - - - - - - - -" + NEW_LINE_CHAR;
    private static final int MAX_STACK_TRACE_DEPTH = 8;

    private final String targetDirectory;
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter timeFormatter;
    private final int maxStackTraceDepthToPrint;

    /**
     * Create a {@link DailySequentialFileWriter} that writes files to the specified directory,
     * using Eastern Time as the time zone for file creation, and a maximum of {@link
     * #MAX_STACK_TRACE_DEPTH} lines in the printing of stack traces (when applicable).
     */
    public DailySequentialFileWriter(String targetDirectory) {
        this(targetDirectory, DEFAULT_ZONE_ID, MAX_STACK_TRACE_DEPTH);
    }

    /**
     * Create a {@link DailySequentialFileWriter} that writes files to the specified directory,
     * using the specified time zone for file creation, and a maximum of {@link
     * #MAX_STACK_TRACE_DEPTH} lines in the printing of stack traces (when applicable).
     */
    public DailySequentialFileWriter(String targetDirectory, ZoneId zoneId) {
        this(targetDirectory, zoneId, MAX_STACK_TRACE_DEPTH);
    }

    /**
     * Create a {@link DailySequentialFileWriter} that writes files to the specified directory,
     * using the specified time zone for file creation, and the specified max lines when printing
     * stack traces.
     */
    public DailySequentialFileWriter(String targetDirectory, ZoneId zoneId, int maxStackTraceDepthToPrint) {

        this.targetDirectory = checkNotNull(targetDirectory);
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId);
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(zoneId);
        this.maxStackTraceDepthToPrint = maxStackTraceDepthToPrint;

        FileUtils.makeDirIfMissing(targetDirectory);
    }

    @Override
    public void warn(String message) {

        Instant now = Instant.now();
        StringBuilder sb = new StringBuilder();
        sb.append(header("WARNING", now));
        sb.append(indent(message));
        sb.append(FOOTER);

        writeTo(currentFile(now), sb.toString());
    }

    @Override
    public void handle(String message, Exception traceMe) {

        Instant now = Instant.now();
        StringBuilder sb = new StringBuilder();
        sb.append(header("ERROR", now));
        if (nonNull(message)) {
            sb.append(indent(message));
        }
        sb.append(indent(truncatedStackTrace(traceMe)));
        sb.append(FOOTER);

        writeTo(currentFile(now), sb.toString());
    }

    private String header(String type, Instant now) {
        return "## " + type + " @ " + timeFormatter.format(now) + "  ##" + NEW_LINE_CHAR;
    }

    private String indent(String message) {

        // \R matches any type of line separator
        // (https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
        return "\t" + String.join(NEW_LINE_CHAR + "\t", message.split("\\R"));
    }

    private File currentFile(Instant now) {

        String filename = "log_for_" + dateFormatter.format(now) + ".txt";
        return new File(targetDirectory, filename);
    }

    /**
     * This is synchronized so that multiple threads don't have their messages mashed up when
     * writing to the current day's file.
     */
    private synchronized void writeTo(File file, String message) {

        try {
            FileUtils.appendToFile(file, message);
        } catch (Exception e) {
            System.out.println("Failed to write the following message to: " + file.getName());
            System.out.println(message);
            System.out.println("\nThe following exception was thrown: \n" + e.getMessage());
            System.out.println("\n\n");
        }
    }

    private String truncatedStackTrace(Exception traceMe) {

        String[] stackTrace = stackTraceLines(traceMe);

        if (stackTrace.length == 0) {
            return "";
        }

        String truncated = stackTrace.length < maxStackTraceDepthToPrint
                ? String.join(NEW_LINE_CHAR, stackTrace)
                : String.join(NEW_LINE_CHAR, Arrays.asList(stackTrace).subList(0, maxStackTraceDepthToPrint));

        return truncated + NEW_LINE_CHAR + "...";
    }

    private String[] stackTraceLines(Exception traceMe) {

        StringWriter sw = new StringWriter();
        traceMe.printStackTrace(new PrintWriter(sw));
        return sw.toString().split("\\R");
    }
}
