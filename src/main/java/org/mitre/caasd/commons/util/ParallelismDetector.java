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

import java.util.ConcurrentModificationException;
import java.util.concurrent.Callable;

/**
 * ParallelismDetector verifies every {@link Runnable} or {@link Callable} it executes is executed
 * without parallelism. Using a ParallelismDetector instance is a more performant and more
 * informative alternative to adding "synchronized" to a method signature.
 *
 * <p>Imagine you have a performance-critical method that is not thread-safe. You need to ensure
 * the method is never used unsafely in a multi-threaded setting BUT you do not want to absorb the
 * performance penalty of adding "synchronized" to the method's signature. Instead, you want to rely
 * on software design (and a correct implementation!) to guarantee ONLY single-threaded access ever
 * occurs. In this case, using a ParallelismDetector can help verify the correctness of your design
 * and implementation.
 *
 * <p>Using a ParallelismDetector enables faster executing code because we know adding the
 * "synchronized" keyword to a method's signature comes with a significant performance penalty.
 *
 * <p>ParallelismDetector provides MORE INFORMATION about single-threaded usage than just
 * adding the synchronized keyword on a method. ParallelismDetector detects parallel execution
 * attempts and throws Exceptions when they occur. This means a ParallelismDetector can help verify
 * the correctness of an implementation that relies on software design, and not the on
 * "synchronized" keyword, to prevent multi-threaded access.
 *
 * <p>A ParallelismDetector object throws ConcurrentModificationException when multiple Runnable or
 * Callables are executing at the same time. This allows your code to guarantee that an Exception
 * WILL BE thrown if incorrect (and probably unintended) parallelism is detected. Thus, if your code
 * does not produce ConcurrentModificationExceptions then it is not being executed by multiple
 * threads at the same time.
 *
 * <p>Note: This class is generalizing the strategy from several Collection Iterator
 * implementations where unwanted multi-thread access is not "prevented with synchronized" but
 * "detected and flagged with an Exception".
 *
 * <p>Warning: This class is optimized for speed. It does not use Atomic variables, the volatile
 * keyword, or the synchronized keyword. Therefore, the "parallelism detection strategy" is not 100%
 * guaranteed to catch parallelism EVERY time. A ParallelismDetector makes a "best effort" attempt
 * to detect parallelism and throw ConcurrentModificationExceptions. The same implementation
 * strategy is used in the standard Java Collections. The underlying assumption is that rarely used
 * method should just add synchronized to the method signature because the 1-time performance
 * penalty is irrelevant.  However, frequently used methods --that are incorrectly called in
 * multi-thread code-- can use a faster less-robust parallelism detection approach because this
 * mistaken parallelism occurs numerous times and the less-robust detection approach has many
 * opportunities to trigger.
 *
 * <p>Note: This class was written to allow us to inject an "implementation correctness proof" into
 * an existing multi-thread, highly partitioned, data processing pipe-line in which each data
 * partition should only be touched by a single thread at any moment in time.
 *
 * <p> Here is an example of using two ParallelismDetectors to detect and prevent parallel
 * execution btw some methods but not others.
 *
 * <pre>{@code
 * public class ExampleUsage {
 *
 *     private final ParallelismDetector parallelismDetector_1and2 = new ParallelismDetector();
 *     private final ParallelismDetector parallelismDetector_3only = new ParallelismDetector();
 *
 *     public void method1() {
 *         parallelismDetector_1and2.run(
 *             () -> I cannot co-occur with other executions of method1 or method2 ...
 *         );
 *     }
 *
 *     public Item method2() {
 *         return parallelismDetector_1and2.call(
 *             () -> I cannot co-occur with other executions of method1 or method2 ...
 *         );
 *     }
 *
 *     public void method3() {
 *         parallelismDetector_3only.run(
 *             () -> I CAN co-occur with executions of method1 and method2, but I cannot co-occur with other executions of method3
 *         );
 *     }
 * }
 * }</pre>
 */
public class ParallelismDetector {

    private boolean isMidExecution = false;

    /**
     * Execute the provided task in the current thread.  Ensure any other Threads that calls
     * "run(Runnable)" or "call(Callable)" on this instance of ParallelismDetector will fail with a
     * ConcurrentModificationException.
     *
     * @param task An item of work that must be executing without parallelism.
     */
    public void run(Runnable task) {

        throwIfMidExecution();
        isMidExecution = true;

        try {
            task.run();
        } catch (Exception ex) {
            isMidExecution = false;
            throw ex;
        }

        isMidExecution = false;
    }

    /**
     * Execute the provided task in the current thread.  Ensure any other Threads that calls
     * "run(Runnable)" or "call(Callable)" on this instance of ParallelismDetector will fail with a
     * ConcurrentModificationException.
     *
     * @param task An item of work that must be executing without parallelism.
     */
    public <T> T call(Callable<T> task) throws Exception {

        throwIfMidExecution();
        isMidExecution = true;
        T output;

        try {
            output = task.call();
        } catch (Exception ex) {
            isMidExecution = false;
            throw ex;
        }

        isMidExecution = false;

        return output;
    }

    private void throwIfMidExecution() {
        if (isMidExecution) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * @return True if a thread is currently executing in this ParallelismDetector's "run(Runnable)"
     *     or "call(Callable)" methods.
     */
    public boolean isMidExecution() {
        return isMidExecution;
    }
}
