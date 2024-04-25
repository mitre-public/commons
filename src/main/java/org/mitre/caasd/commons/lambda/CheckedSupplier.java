package org.mitre.caasd.commons.lambda;

import java.util.function.Supplier;

/**
 * A CheckedSupplier is similar to a {@link Supplier} EXCEPT it throws a checked exception.
 * <p>
 * Unfortunately, a CheckedSupplier obfuscates code because they require using try-catch blocks.
 * This class and the convenience functions in {@link Uncheck}, allow you to improve the readability
 * of code (assuming you are willing to demote all checked exceptions to RuntimeExceptions).
 * <p>
 * For example:
 *
 * <pre>{@code
 *     //code WITHOUT these utilities -- is harder to read and write
 *     Stream<String> fileLines = null;
 *     try {
 *         fileLines = java.nio.lines(pathThFile);
 *     } catch (IOException ex) {
 *         throw DemotedException.demote(ex);
 *     }
 *     List<String> subset = fileLines.stream()
 *         .filter(str -> str.contains("abcde"))
 *         .toList();
 *
 *
 *     //code WITH these utilities -- is easier to read and write
 *     Stream<String> fileLines = Uncheck.supplier(() -> java.nio.lines(pathThFile));
 *     List<String> subset = fileLines.stream()
 *         .filter(str -> str.contains("abcde"))
 *         .toList();
 * }</pre>
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

    T get() throws Exception;
}
