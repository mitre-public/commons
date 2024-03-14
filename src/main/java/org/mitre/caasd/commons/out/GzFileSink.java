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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.move;
import static java.lang.Runtime.getRuntime;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;
import static org.mitre.caasd.commons.Time.durationBtw;
import static org.mitre.caasd.commons.Time.theDuration;
import static org.mitre.caasd.commons.fileutil.FileUtils.buildGzWriter;
import static org.mitre.caasd.commons.fileutil.FileUtils.makeDirIfMissing;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

/**
 * A GzFileSink copies data it receives to one or more compressed gz files.
 *
 * <p>A GzFileSink is designed to have a long lifespan (aka multiple days) and continue to write data
 * as part of an enduring process. Consequently, an GzFileSink will automatically notice when a
 * target gz file is no longer being written to. When this occurs the GzFileSink will close the pipe
 * to that file.
 *
 * <p>While a gz file is being written to its name will appear like "UNDER_CONSTRUCTION_myArchive.gz"
 * When the gz file is closed for writing its name will become "myArchive.gz" If there is a long
 * pause between writing data to an archive file the "second time" that archive is created it will
 * have "_N" appended to its name, e.g. "myArchive_1.gz"
 *
 * <p> BE AWARE that a GzFileSink starts a non-daemon thread to monitor which gz files are being written
 * to and which gz files need to be closed. This thread will keep the JVM from closing unless it is
 * shutdown by calling {@code close()}.
 *
 * <p>Example usages:
 * <pre>{@code
 * //Steadily create daily gz files from a persistent stream
 * Supplier<MyDataType> source = getDataSource();
 * GzFileSink sink = new GzFileSink(
 *     "archives",
 *     t -> t.toString(), //write the output of toString() to the gz files
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
 * GzFileSink sink = new GzFileSink(
 *     "archives",
 *     t -> t.toString(), //write the output of toString() to the gz files
 *     t -> t.getDate(), //write to a file titled "YYYY-MM-DD.gz"
 *     Duration.ofHours(1)
 * );
 * hugeList.forEach(sink);
 * sink.close();
 * }</pre>
 */
public class GzFileSink<T> implements Consumer<T>, Closeable {

    /* Put at the front of files that are currently open and being written to. */
    static final String IN_PROGRESS_PREFIX = "UNDER_CONSTRUCTION_";

    /**
     * All archive files are placed in a single directory. Subdirectories are not supported because
     * subdirectories encourage writing "many" archive files at once. Due to operating system limits
     * on the number of File handles open at once this is a fragile pattern and actively
     * discouraged.
     */
    private final String outputDir;

    private final Function<T, String> fileNamer;

    /**
     * Generates the file name for where each input record will be archived. This naming strategy
     * should not generate too many unique results because it will open one Stream to each file.
     */
    private final Function<T, String> toString;

    /**
     * Open GzStreams and their last access time, this maps MUST stay small.
     */
    private final Map<String, TrackedPrintWriter> openWriters;

    /**
     * How many times a particular target file is "opened for archiving", allows us to ensure we
     * dont write data to a file that was already opened, written to, and closed.
     */
    private final Multiset<String> targetCounts;

    /**
     * A PrintWriter does not add data to a .gz file for this amount of time the destination .gz
     * file will be close.
     */
    private final Duration expirationTime;

    /*
     * This limit is enforced to prevent the Archival process from crashing the host machine by
     * opening too many File Handles.
     */
    private final int maxOpenWriters;

    /**
     * All write operations are routed through a single queue so that the ArchivalFileSink can be
     * thread-safe.
     */
    private final BlockingQueue<T> queue;

    private final ScheduledExecutorService executor;

    private boolean isClosed = false;

    /**
     * @param outputDir      The directory where the .gz files are written
     * @param toString       Converts input items to Strings that are written to the target gz
     *                       file.
     * @param fileNamer      Generates a target file name for each input record. This strategy
     *                       should yield relatively few unique file names. If too many file names
     *                       are generated you'll open too many file handles and kill performance or
     *                       just crash.
     * @param expirationTime How long between writing pieces of data until a .gz file is closed
     */
    public GzFileSink(
            String outputDir, Function<T, String> toString, Function<T, String> fileNamer, Duration expirationTime) {
        this.outputDir = requireNonNull(outputDir);
        this.toString = requireNonNull(toString);
        this.fileNamer = requireNonNull(fileNamer);
        this.expirationTime = requireNonNull(expirationTime);
        // prevent writing to too many .gz files at once
        this.maxOpenWriters = 100;
        this.openWriters = new TreeMap<>();
        this.targetCounts = TreeMultiset.create();
        // assume a backup of 5000 records means the data creation process is overwhelming the archival process
        this.queue = new ArrayBlockingQueue<>(5000);
        this.executor = buildExecutor();

        scheduleStreamCloser();
        scheduleDataWriting();
    }

    /**
     * Schedule an ongoing task that closes stale GzStreams.
     */
    private void scheduleStreamCloser() {

        executor.scheduleWithFixedDelay(this::closeStaleStreamTargets, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Schedule an ongoing task that writes queued GzStreams.
     */
    private void scheduleDataWriting() {

        executor.scheduleWithFixedDelay(this::drainQueueAndWriteRecords, 0, 1, TimeUnit.SECONDS);
    }

    private void closeStaleStreamTargets() {
        staleOutputTargets().stream().forEach(this::closeAndRemoveWriter);
    }

    private List<String> staleOutputTargets() {

        List<String> staleStreamTargets = openWriters.entrySet().stream()
                .filter(entry -> entry.getValue().isStale(expirationTime))
                .map(Entry::getKey)
                .collect(toList());

        return staleStreamTargets;
    }

    /**
     * Queue up an inputRecord for eventual emission to a gz file. This record will be used to
     * determine which gz file it should be archived in. The record will then be converted to JSON
     * can appended to the appropriate gz file.
     */
    @Override
    public void accept(T inputRecord) {
        checkState(!isClosed, "Cannot add data to a closed GzFileSink");
        /*
         * This method will start blocking when the queue is full. this prevents the "data creation
         * process" from outpacing the "data archving process".
         */
        putInBuffer(inputRecord);
    }

    public int numOpenWriters() {
        return this.openWriters.size();
    }

    /**
     * Records are placed in a fixed capacity queue so that (1) this ArchivalFileSink is threadsafe
     * and (2) the data creation process does not overwhelm the data archival process
     */
    private void putInBuffer(T inputRecord) {
        try {
            queue.put(inputRecord);
        } catch (InterruptedException ex) {
            // A data-providing thread was Interupted while waiting to put an inputRecord into an overflowing queue.
            throw demote("Thread interrupted while waiting to add item to maxxed out writeQueue", ex);
        }
    }

    public synchronized void drainQueueAndWriteRecords() {

        ArrayList<T> recordsToPublish = newArrayList();

        queue.drainTo(recordsToPublish);

        for (T t : recordsToPublish) {
            sendToGzFile(t);
        }
    }

    private void sendToGzFile(T inputRecord) {

        String targetFilename = fileNamer.apply(inputRecord);

        TrackedPrintWriter targetStream = (openWriters.containsKey(targetFilename))
                ? openWriters.get(targetFilename)
                : newGzStreamFor(targetFilename);

        targetStream.write(toString.apply(inputRecord) + "\n");
        targetStream.flush();
    }

    private TrackedPrintWriter newGzStreamFor(String filename) {

        checkState(
                openWriters.size() <= maxOpenWriters,
                "Cannot open new gz file because " + openWriters.size()
                        + " streams are already open.  Could calling flushAndCloseCurrentFiles() help?");

        makeDirIfMissing(outputDir);

        try {
            int count = targetCounts.count(filename);

            String actualFilename = (count > 0)
                    ? IN_PROGRESS_PREFIX + filename + "_" + count + ".gz"
                    : IN_PROGRESS_PREFIX + filename + ".gz";

            File target = new File(outputDir + File.separator + actualFilename);

            TrackedPrintWriter tpw = new TrackedPrintWriter(target);

            openWriters.put(filename, tpw);
            targetCounts.add(filename);
            return tpw;
        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    /**
     * Prevents any additional input, flushes all waiting records, closees all .gz files being
     * written to, and shuts down the internal monitoring and data writing threads.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {

        isClosed = true;
        executor.shutdownNow();

        drainQueueAndWriteRecords();
        closeAllWriters();
    }

    private void closeAllWriters() {

        List<String> closeKeys = newArrayList(openWriters.keySet());

        for (String targetToClose : closeKeys) {
            closeAndRemoveWriter(targetToClose);
        }
    }

    /**
     * Similar to {@code close()}.
     *
     * <p>Flushes all waiting records and closes all .gz files being written to, and shuts down the
     * internal monitoring and data writing threads.
     *
     * <p>DOES NOT prevents any additional input. DOES NOT shuts down the internal monitoring and data
     * writing threads
     */
    public void flushAndCloseCurrentFiles() {
        drainQueueAndWriteRecords();
        closeAllWriters();
    }

    private void closeAndRemoveWriter(String targetFile) {
        try {
            TrackedPrintWriter closeMe = openWriters.remove(targetFile);
            closeMe.close();

            // rename file to let people know its done building...
            File hasInProgressName = closeMe.targetFile();
            File afterRename = new File(
                    outputDir + File.separator + hasInProgressName.getName().substring(IN_PROGRESS_PREFIX.length()));

            move(hasInProgressName, afterRename);

        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    private ScheduledExecutorService buildExecutor() {

        ScheduledExecutorService ses = newScheduledThreadPool(1); // just 1 thread

        /*
         * An ExecutorService can prevent a process from shuting down properly. For example, if the
         * user presses control+C to quit the process it may hang. The line below adds a
         * ShutdownHook that should allow the JVM to exit when a Control+C command is
         * given. Note: a Control+Z command will not allow the JVM to terimate gracefully and will
         * probably hang and leave some resources stranded.
         */
        getRuntime().addShutdownHook(new DrainFlushAndShutdown(this, ses));

        return ses;
    }

    private static class DrainFlushAndShutdown extends Thread {

        final GzFileSink archiver;
        final ExecutorService exec;

        public DrainFlushAndShutdown(GzFileSink archiver, ExecutorService exec) {
            this.archiver = requireNonNull(archiver);
            this.exec = requireNonNull(exec);
        }

        @Override
        public void run() {
            exec.shutdownNow();

            archiver.drainQueueAndWriteRecords();
            try {
                archiver.close();
            } catch (IOException ex) {
                // This class is only executed during the shutdown process, do nothing.
            }
        }
    }

    /**
     * A TrackedPrintWriter combines a PrintWriter with a "last usage" timestamp to make it easy to
     * find "stale PrintWriters" .
     */
    private static class TrackedPrintWriter implements AutoCloseable {

        private final File targetFile;

        private final PrintWriter writer;

        private Instant timeOfLastWrite;

        TrackedPrintWriter(File targetFile) throws IOException {
            this.targetFile = requireNonNull(targetFile);
            this.writer = buildGzWriter(targetFile);
            this.timeOfLastWrite = now();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        void write(String str) {
            timeOfLastWrite = Instant.now();
            writer.write(str);
        }

        void flush() {
            writer.flush();
        }

        Duration timeSinceLastWrite() {
            return durationBtw(now(), timeOfLastWrite);
        }

        boolean isStale(Duration timeLimit) {
            return theDuration(timeSinceLastWrite()).isGreaterThan(timeLimit);
        }

        File targetFile() {
            return targetFile;
        }
    }
}
