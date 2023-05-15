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

import static com.google.common.base.Preconditions.checkArgument;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

@SuppressWarnings("UnstableApiUsage")
public class BitAndHashingUtils {

    private static final HashFunction HASH_FUNCTION = Hashing.farmHashFingerprint64();
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * Apply a hash function (which may return more than 64 pseudo-random bits) to an input String.
     * Then truncate the returned hash to 64 bits of pseudo-randomness.
     *
     * @param str      An input to the hashing algorithm.
     * @param charset  The Character Set to use when pushing a String through the HashFunction
     * @param hashFunc The hashFunction to use when computing the truncated hash
     * @return 64 bits of the hashing functions output.
     */
    public static long compute64BitHash(String str, Charset charset, HashFunction hashFunc) {
        Hasher hasher = HASH_FUNCTION.newHasher();
        hasher.putString(str, UTF8);
        HashCode hash = hasher.hash();
        return hash.asLong();
    }

    /**
     * This is equivalent to calling compute64BitHash(str, Charset.forName("UTF-8"),
     * Hashing.farmHashFingerprint64())
     *
     * @param str An input to the hashing algorithm.
     * @return 64 bits of the hashing functions output.
     */
    public static long compute64BitHash(String str) {
        return compute64BitHash(str, UTF8, HASH_FUNCTION);
    }

    /**
     * Generate n pseudo-random bits from an input String. The input string is pushed through a
     * hashing function, then the returned hash is truncated to n-bits.
     *
     * @param n   The number of random bits you want {@literal (0 < n <= 64)}
     * @param str The source String
     *
     * @return A set of n pseudo-random bits (only the lowest-order n-bits may be set to 1)
     */
    public static long nRandomBitsFrom(int n, String str) {
        checkArgument(0 < n && n <= 64);
        long hash = compute64BitHash(str);
        return truncateBits(hash, n);
    }

    /** Apply a n-bit mask to a set of bits (retains the right most bits). */
    public static long truncateBits(long bits, int n) {
        checkArgument(0 < n && n <= 64);

        //make and then apply bitMask using the bitwise AND operation --> return the result
        return makeBitMask(n) & bits;
    }

    /** @return A long with the rightmost n-bits set to 1 and all other bets set to 0. */
    public static long makeBitMask(int n) {
        checkArgument(0 < n && n <= 64);
        long mask = 0L;
        for (int i = 0; i < n; i++) {
            mask = mask << 1;
            mask = mask | 1;
        }

        return mask;
    }
}
