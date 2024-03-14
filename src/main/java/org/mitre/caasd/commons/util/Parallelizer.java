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
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Parallelizer is simplifies parallelizing a batch of independent tasks.
 * <p>
 * A Parallelizer properly starts and shuts down an ExecutorService that executes tasks in parallel.
 * It also handles the CountDownLatch that is used to ensure that all jobs have completed.
 * <p>
 * This class is provided so that parallelizing simple jobs can be done without using
 * java.util.concurrent classes or dealing with any of the annoying error catching code that is
 * required when moving from a single-threaded context to a multi-threaded context and then back to
 * a single-threaded context.
 * <p>
 * Another core benefit of a Parallelizer is that exceptions thrown while doing the "work" (i.e.
 * from the provided Runnables) can be handled in isolation and prevented from bringing down the
 * entire parallel computation.
 * <p>
 * This class also has better parallel performance than Java 1.8's Streams API. The parallelStream()
 * is not as good as it could be at ensuring that all cores are being utilized at all times.
 * Additionally, the parallelStream() approach does not allow for a configurable number of
 * cores/threads to be used.
 */
public class Parallelizer {

    /**
     * This executor will do the work in parallel.
     */
    private final ExecutorService service;

    private final ExceptionHandler errorHandler;

    /**
     * This flag prevents the "one time use" Parallelizer from being used twice.
     */
    private boolean isSpent = false;

    /**
     * Create a Parallelizer that will use a fixed number of thread to process jobs in parallel. The
     * {@link SequentialFileWriter} is used to handle exceptions.
     *
     * @param numThreads The number of threads the inner ExecutorService will use to process jobs.
     */
    public Parallelizer(int numThreads) {
        this(numThreads, new SequentialFileWriter());
    }

    /**
     * Create a Parallelizer that will use a fixed number of thread to process jobs in parallel and
     * will use the provided {@link ExceptionHandler}.
     *
     * @param numThreads       The number of threads the inner ExecutorService will use to process
     *                         jobs.
     * @param exceptionHandler The mechanism for handling exceptions.
     */
    public Parallelizer(int numThreads, ExceptionHandler exceptionHandler) {
        this.service = Executors.newFixedThreadPool(numThreads);
        this.errorHandler = exceptionHandler;
    }

    /**
     * This method blocks until the "run()" method associated with all inputs job is complete.
     *
     * @param jobs A batch of work.
     */
    public void doWorkInParallel(Runnable[] jobs) {

        checkState(!this.isSpent, "This Parallelizer is spent -- it cannot be reused");
        checkNotNull(jobs, "Cannot submit a null array of Runnable objects");

        for (Runnable job : jobs) {
            checkNotNull(job, "The input array of Runnable objects cannot contain a null");
        }

        CountDownLatch latch = new CountDownLatch(jobs.length);

        /*
         * Wrap each job in a pair of Wrappers,
         *
         * The AwaitableTask wrapper trips the CountDownLatch and the ErrorCatchingTask ensures all
         * errors are properly handled (because the ExecutorService will swallow exceptions)
         */
        for (Runnable job : jobs) {

            Runnable wrappedJob = new AwaitableTask(latch, new ErrorCatchingTask(job, errorHandler));

            service.submit(wrappedJob);
        }

        waitForJobsToFinish(latch);

        this.isSpent = true;

        service.shutdown();
    }

    /**
     * This method blocks until the "run()" method associated with each job is complete.
     *
     * @param jobs A batch of work.
     */
    public void doWorkInParallel(Collection<? extends Runnable> jobs) {
        doWorkInParallel(jobs.toArray(new Runnable[0]));
    }

    private void waitForJobsToFinish(CountDownLatch latch) {
        try {
            latch.await();
        } catch (Exception ex) {
            errorHandler.handle(ex);
        }
    }
}
