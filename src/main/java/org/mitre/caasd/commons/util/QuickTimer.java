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

import java.util.concurrent.atomic.AtomicLong;

/**
 * A QuickTimer is a simple, but powerful, convenience class designed to support taking coherent
 * timing measurements in simple single threaded applications as well as multi-threaded
 * applications.
 * <p>
 * A QuickTimer has two core methods: <code>tic()</code> and <code>toc(AtomicLong)</code>. <br> --
 * The <code>tic()</code> method begins the stopwatch. <br> -- The <code>toc(AtomicLong)</code>
 * method increments the provided AtomicLong by the amount of time that has passed since last
 * calling tic().
 * <p>
 * This code example measures 3 separate stages of a single threaded computation:
 * <pre>
 * {@code
 * //prepare a QuickTimer and 3 required measurements
 * QuickTime timer = new QuickTimer();
 * AtomicLong timeOfStageOne = new AtomicLong();
 * AtomicLong timeOfStageOneAndTwo = new AtomicLong();
 * AtomicLong timeOfStageThree = new AtomicLong();
 *
 * timer.tic(); //start the timer
 *
 * computeStageOne();
 * timer.toc(timeOfStageOne);
 *
 * computeStageTwo();
 * timer.toc(timeOfStageOneAndTwo);
 *
 * timer.tic(); //reset the timer
 * computeStageThree();
 * timer.toc(timeOfStageThree);
 *
 * //print a few timing messages
 * QuickTimer.printTimingMessage("Stage 1 took", timeOfStageOne);
 * QuickTimer.printTimingMessage("Stage 1 and 2 together took", timeOfStageOneAndTwo);
 * QuickTimer.printTimingMessage("Stage 3 took", timeOfStageThree);
 * }
 * </pre> The code example below computes the aggregate time of a computation that occurs across
 * multiple threads.
 * <pre>
 * {@code
 * //these AtomicLongs permits thread-safe timing measurements
 * static AtomicLong stageOneComputationTime = new AtomicLong();
 * static AtomicLong stageTwoComputationTime = new AtomicLong();
 *
 * class DemoThread {
 *
 * 	public void run() {
 * 		QuickTimer qt = new QuickTimer(); //notice, this thread has its own timer
 * 		qt.tic();
 * 		executeStageOne();
 * 		qt.toc(stageOneComputationTime);
 * 		qt.tic();
 * 		executeStageTwo();
 * 		qt.toc(stageTwoComputationTime);
 *    }
 * }
 *
 * public static void main(String[] args) {
 * 	(new DemoThread()).run();
 * 	(new DemoThread()).run();
 * 	(new DemoThread()).run();
 * 	(new DemoThread()).run();
 * 	(new DemoThread()).run();
 *
 * 	long A_LONG_TIME = 100_000_000L;
 * 	Thread.sleep(A_LONG_TIME);
 * 	QuickTimer.printTimingMessage("Stage One's total compute time", stageOneComputationTime);
 * 	QuickTimer.printTimingMessage("Stage Two total compute time", stageTwoComputationTime);
 *
 * }
 *
 * }
 *
 * </pre>
 */
public class QuickTimer {

    private long savedNanoTime;

    /**
     * Mark a start time (in nanoseconds).
     */
    public void tic() {
        this.savedNanoTime = System.nanoTime();
    }

    /**
     * Compute the time (in nanoseconds) that has elapsed since calling "tic()", add this time to
     * the provided AtomicLong
     *
     * @param incrementMe This AtomicLong is increased by calling this method.
     */
    public void toc(AtomicLong incrementMe) {

        long endTime = System.nanoTime();
        long elapsedTime = endTime - savedNanoTime;

        incrementMe.addAndGet(elapsedTime);
    }

    /**
     * This convenience method prints a timing message to System.out. The message printed is =
     * "MESSAGE: TIME_IN_SECONDS (sec)"
     *
     * @param msg           A String prefix (which usually explains what the measurement means)
     * @param timeInNanoSec A time measurement in nanoseconds
     */
    public static void printTimingMessage(String msg, AtomicLong timeInNanoSec) {

        long timeInSeconds = timeInNanoSec.longValue() / 1_000_000_000;

        System.out.println(msg + ": " + timeInSeconds + " (sec)");
    }
}
