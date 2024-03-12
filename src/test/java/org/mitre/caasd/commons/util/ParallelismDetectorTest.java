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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class ParallelismDetectorTest {

    //This class's public facing "doSingleThreadedWork" method should NEVER be using in parallel
    //  We use a ParallelismDetector object to detect parallelism
    static class SingleThreadedAccessOnly {

        ParallelismDetector parallelismDetector = new ParallelismDetector();

        public void doSingleThreadedWork() {
            parallelismDetector.run(() -> theFragileWork());
        }

        private void theFragileWork() {
            //waste some time doing something, encourage thread to pause midstep
            long sum = 0;
            for (int i = 0; i < 10_000; i++) {
                for (int j = 0; j < 10_000; j++) {
                    sum += i;
                }
                Thread.yield();
            }
        }
    }

    @Test
    public void demonstrateParallelismDetectionAndExceptionThrowing() throws InterruptedException {

        SingleThreadedAccessOnly delicateFlower = new SingleThreadedAccessOnly();
        CountDownLatch latch = new CountDownLatch(1);

        //This thread will start a long-duration task that "has the lease" on delicateFlower
        Thread t1 = new Thread(
            () -> {
                latch.countDown();
                delicateFlower.doSingleThreadedWork();
            }
        );
        t1.start();
        latch.await();

        //Now, when this 2nd thread begins operating on the same delicateFlower it gets a ConcurrentModificationException
        assertThrows(
            ConcurrentModificationException.class,
            () -> delicateFlower.doSingleThreadedWork()
        );
    }

    @Test
    public void detectorKnowsWhenItsWorking_andWillThrowConcurrentModificationException()
        throws InterruptedException {

        CountDownLatch pausesRunMe = new CountDownLatch(1);
        CountDownLatch pausesMainThread = new CountDownLatch(1);

        Runnable task = () -> {
            try {
                //this countdown permits the main thread can proceed after this task is active
                pausesMainThread.countDown();

                //pause this "task" so the main thread has a moment to notice "task" is still actively processing
                pausesRunMe.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        ParallelismDetector detector = new ParallelismDetector();

        //Start a 2nd Thread (beyond this test execution thread) that launches work inside a NoParallelism task wrapper...
        Thread t = new Thread(() -> detector.run(task));
        t.start();

        //Make sure this "main test execution thread" pauses to give time for the thread we just launched to get going...
        pausesMainThread.await();  //stops blocking when "pausesMainThread.countDown();" is called

        //At this point the NoParallelism object has a Runnable "inside it" that hasn't completed...

        // "isMidExecution()" should let us know
        assertThat(detector.isMidExecution(), is(true));
        // AND we should get a ConcurrentModificationException if we launch any new work
        assertThrows(ConcurrentModificationException.class, () -> detector.run(() -> {int x = 1 + 2;}));

        //allow Thread "t" to exit normally by unblocking the Runnable "task"
        pausesRunMe.countDown();
    }


    @Test
    public void happyPath_runnables() {
        //A single-thread can push a handful of Runnable through a single detector and everything works fine

        ParallelismDetector detector = new ParallelismDetector();

        //these Runnable do nothing
        detector.run(() -> {int x = 1 + 2;});
        detector.run(() -> {int x = 2 + 3;});
        detector.run(() -> {int x = 4 + 5;});

        assertThat(detector.isMidExecution(), is(false));
    }


    @Test
    public void happyPath_callable() throws Exception {
        //A single-thread can push a handful of Callables through a single detector and everything works fine

        ParallelismDetector detector = new ParallelismDetector();

        detector.call(() -> "I did nothing 1");
        detector.call(() -> "I did nothing 2");
        detector.call(() -> "I did nothing 3");

        assertThat(detector.isMidExecution(), is(false));
    }


    @Test
    public void aCrashingRunnableDoesNotBrickTheAbilityToProcessJobs() {

        ParallelismDetector detector = new ParallelismDetector();

        try {
            detector.run(() -> {throw new RuntimeException("oh no an Exception!");});
        } catch (RuntimeException rte) {
            assertThat(rte.getMessage().contains("oh no an Exception!"), is(true));
        }

        //the exception doesn't prevent receiving new jobs
        assertThat(detector.isMidExecution(), is(false));

        assertDoesNotThrow(
            () -> detector.run(() -> {int x = 1 + 2;}),
            "Look, the ParallelismDetector object still works"
        );
    }


    @Test
    public void aCrashingCallableDoesNotBrickTheAbilityToProcessJobs() {

        ParallelismDetector detector = new ParallelismDetector();

        try {
            detector.call(() -> {throw new RuntimeException("oh no an Exception!");});
        } catch (Exception rte) {
            assertThat(rte.getMessage().contains("oh no an Exception!"), is(true));
        }

        //the exception doesn't prevent receiving new jobs
        assertThat(detector.isMidExecution(), is(false));

        assertDoesNotThrow(
            () -> detector.run(() -> {int x = 1 + 2;}),
            "Look, the ParallelismDetector object still works");
    }
}