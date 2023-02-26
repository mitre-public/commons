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

import static java.util.Objects.nonNull;

import java.util.concurrent.ExecutorService;

/**
 * ShutdownHook is a convenience class that makes it easier to properly shutdown a java program that
 * contains an ExecutorService.
 * <p>
 * A ShutdownHook prevents a program that contains an ExecutorService from hanging when the user
 * presses "Control + C". A program with an ExecutorService can hang because the ExecutorService
 * contains a non-daemon thread that keeps the JVM from fully shutting down when "Control + C"
 * signal is given. A ShutdownHook receives the "Control + C" signal from the Runtime and tells its
 * ExecutorService it needs to shutdown.
 * <p>
 * Note: Shutdown hooks will be executed if the user pressed "Control + C" but not if the user
 * presses "Control + Z"
 */
public class ShutdownHook extends Thread {

    final ExecutorService exec;

    final Runnable shutdownWork;

    public static void addShutdownHookFor(ExecutorService exec) {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(exec));
    }

    public ShutdownHook(ExecutorService exec, Runnable shutdownWork) {
        this.exec = exec;
        this.shutdownWork = shutdownWork;
    }

    public ShutdownHook(ExecutorService exec) {
        this(exec, null);
    }

    @Override
    public void run() {
        System.out.println("\nLaunching shutdown hook.");
        System.out.print("\nShutting down an ExecutorService.");

        if (nonNull(shutdownWork)) {
            shutdownWork.run();
        }
        exec.shutdownNow();
    }
}
