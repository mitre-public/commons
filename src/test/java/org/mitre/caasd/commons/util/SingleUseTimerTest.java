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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class SingleUseTimerTest {

    public SingleUseTimerTest() {
    }

    @Test
    public void testDidntCallTicException() {

        SingleUseTimer timer = new SingleUseTimer();
        try {
            timer.toc();
            fail("calling toc with previously calling tic should throw an Exception");
        } catch (IllegalStateException ise) {
            //caught an Exception
        }
    }

    @Test
    public void testDidntCallTicException2() {

        SingleUseTimer timer = new SingleUseTimer();
        try {
            timer.elapsedTime();
            fail("calling elapsedTime with previously calling tic should throw an Exception");
        } catch (IllegalStateException ise) {
            //caught an Exception
        }
    }

    @Test
    public void testCallTicTwiceException() {

        SingleUseTimer timer = new SingleUseTimer();
        timer.tic();
        try {
            timer.tic();
            fail("Should not be able to call tic twice");
        } catch (IllegalStateException ise) {
            //caught the correct exception
        }
    }


    @Test
    public void testCallTocTwiceException() {

        SingleUseTimer timer = new SingleUseTimer();
        timer.tic();
        timer.toc();
        try {
            timer.toc();
            fail("Should not be able to call toc twice");
        } catch (IllegalStateException ise) {
            //caught the correct exception
        }
    }

    @Test
    public void testTimeContinuesToIncreaseIfTocNotCalled() {

        SingleUseTimer timer = new SingleUseTimer();

        timer.tic();
        Duration time1 = timer.elapsedTime();
        wasteTime(100);
        Duration time2 = timer.elapsedTime();
        wasteTime(100);
        Duration time3 = timer.elapsedTime();

        assertTrue(time1.toNanos() < time2.toNanos());
        assertTrue(time2.toNanos() < time3.toNanos());
    }

    @Test
    public void testTimeStopsIncreasingIfTocCalled() {

        SingleUseTimer timer = new SingleUseTimer();

        timer.tic();
        timer.toc();
        Duration time1 = timer.elapsedTime();
        wasteTime(100);
        Duration time2 = timer.elapsedTime();
        wasteTime(100);
        Duration time3 = timer.elapsedTime();

        assertTrue(time1.toNanos() == time2.toNanos());
        assertTrue(time2.toNanos() == time3.toNanos());
    }

    @Test
    public void testTiming1() {

        long MIN_DURATION_IN_MILLISEC = 300;

        SingleUseTimer timer = new SingleUseTimer();
        timer.tic();
        wasteTime(MIN_DURATION_IN_MILLISEC);
        timer.toc();
        Duration timeSpan = timer.elapsedTime();

        assertTrue(timeSpan.toNanos() > 0); // some time elapsed..
        assertTrue(timeSpan.toMillis() < 2 * MIN_DURATION_IN_MILLISEC); //but not much
    }

    private void wasteTime(long millisec) {

        try {
            Thread.sleep(millisec);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
