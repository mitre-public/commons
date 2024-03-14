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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

public class ParallelizerTest {

    @Test
    public void testDoWorkInParallel() {
        System.out.println("[TEST] Parallelizer.doWorkInParallel(Collection<Runnable>)");

        Runnable r1 = new Runnable() {
            @Override
            public void run() {
                // do nothing
            }
        };
        Runnable r2 = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("Catch me");
            }
        };
        Runnable r3 = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("Catch me too");
            }
        };

        List<Runnable> jobs = new ArrayList<>();
        jobs.add(r1);
        jobs.add(r2);
        jobs.add(r3);

        Parallelizer para = new Parallelizer(2);
        para.doWorkInParallel(jobs);

        // two error files should have been made
        File errorFile0 = new File("error_0.txt");
        assertTrue(errorFile0.exists());
        errorFile0.delete();

        File errorFile1 = new File("error_1.txt");
        assertTrue(errorFile1.exists());
        errorFile1.delete();
    }

    @Test
    public void confirmOneTimeUse() {

        Runnable task1 = new Runnable() {
            @Override
            public void run() {
                // do nothing;
            }
        };
        Runnable task2 = new Runnable() {
            @Override
            public void run() {
                // do nothing;
            }
        };

        List<Runnable> jobs = new ArrayList<>();
        jobs.add(task1);
        jobs.add(task2);

        Parallelizer para = new Parallelizer(2);
        para.doWorkInParallel(jobs);

        try {
            para.doWorkInParallel(jobs);
            fail("Should not work");
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("This Parallelizer is spent -- it cannot be reused"));
        }
    }

    @Test
    public void testUsingCustomExceptionHandler() {

        Runnable r1 = () -> {
            // do nothing
        };
        Runnable r2 = () -> {
            throw new RuntimeException("Catch me");
        };
        Runnable r3 = () -> {
            throw new RuntimeException("Catch me too");
        };
        List<Runnable> jobs = ImmutableList.of(r1, r2, r3);

        CountingExceptionHandler testHandler = new CountingExceptionHandler();
        Parallelizer para = new Parallelizer(2, testHandler);
        para.doWorkInParallel(jobs);

        assertEquals(0, testHandler.numWarnCalls);
        assertEquals(2, testHandler.numHandleCalls); // @todo -- I can sometime fail non-deterministically with
        // testHandler.numHandleCalls = 1)
    }

    /**
     * A simple test double for determining the number of calls to each method.
     */
    private static class CountingExceptionHandler implements ExceptionHandler {

        private int numWarnCalls = 0;
        private int numHandleCalls = 0;

        @Override
        public void warn(String message) {
            numWarnCalls++;
        }

        @Override
        public void handle(String message, Exception ex) {
            numHandleCalls++;
        }
    }
}
