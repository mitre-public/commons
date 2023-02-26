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

import java.util.concurrent.CountDownLatch;

/**
 * An AwaitableTask is a simple wrapper that adds a CountDownLatch.countDown() call to a Runnable.
 * The purpose of this decorator is to enable the easy execution of parallel tasks.
 */
public class AwaitableTask implements Runnable {

    public final Runnable runMe;

    public final CountDownLatch latch;

    /**
     * An AwaitableTask is a Decorator that decrements a CountDownLatch when it finishes running the
     * provided Runnable
     *
     * @param latch A latch that is triggered after the Runnable is complete
     * @param runMe Work to be completed before the latch is triggered
     */
    public AwaitableTask(CountDownLatch latch, Runnable runMe) {
        this.latch = latch;
        this.runMe = runMe;
    }

    /**
     * Run the runnable, then call countDown on the latch.
     */
    @Override
    public void run() {
        runMe.run();
        latch.countDown();
    }
}
