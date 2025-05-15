package org.mitre.caasd.commons.lambda;

import java.util.concurrent.Callable;

/**
 * A CheckedCallable is similar to a {@link Callable} EXCEPT it throws a checked exception.
 * <p>
 * Unfortunately, CheckedCallables can obfuscate code because they require using try-catch blocks.
 * This class and the convenience functions in {@link Uncheck}, allow you to improve the readability
 * of some code (assuming you are willing to demote all checked exceptions to RuntimeExceptions)
 * <p>
 * For example:
 *
 * <pre>{@code
 *     //code WITHOUT these utilities -- is harder to read and write.
 *     try {
 *        return myThrowingCallable();
 *     } catch (AnnoyingCheckedException ex) {
 *        throw DemotedException.demote(ex);
 *     }})
 *
 *
 *     //code WITH these utilities -- is easier to read and write.
 *     return Uncheck.call(() -> myThrowingCallable());
 * }</pre>
 */
@FunctionalInterface
public interface CheckedCallable<V> {

    V call() throws Exception;
}
