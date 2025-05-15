package org.mitre.caasd.commons.lambda;

/**
 * A CheckedRunnable is similar to a {@link Runnable} EXCEPT it throws a checked exception.
 * <p>
 * Unfortunately, CheckedRunnables can obfuscate code because they require using try-catch blocks.
 * This class and the convenience functions in {@link Uncheck}, allow you to improve the readability
 * of some code (assuming you are willing to demote all checked exceptions to RuntimeExceptions)
 * <p>
 * For example:
 *
 * <pre>{@code
 *     //code WITHOUT these utilities -- is harder to read and write.
 *     try {
 *        myThrowingFunction();
 *     } catch (AnnoyingCheckedException ex) {
 *        throw DemotedException.demote(ex);
 *     }})
 *
 *
 *     //code WITH these utilities -- is easier to read and write.
 *     Uncheck.run(() -> myThrowingFunction());
 * }</pre>
 */
@FunctionalInterface
public interface CheckedRunnable {

    void run() throws Exception;
}
