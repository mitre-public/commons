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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.*;

import org.junit.jupiter.api.Test;

class BitAndHashingUtilsTest {

    private static String showHashInBinary(String str) {
        long hash = compute64BitHash(str);
        return Long.toBinaryString(hash);
    }

    // exists to make tests more readable...
    private static String inBinary(long val) {
        return Long.toBinaryString(val);
    }

    @Test
    public void hashingToLongWorksAsExpected() {
        // notice, we have good bit dispersion because we are working with a decent hash function
        assertThat(showHashInBinary("a"), is("1011001101000101010000100110010110110110110111110111010111100011"));
        assertThat(showHashInBinary("aa"), is("1001111010000111111111111011101000010101100000000110010111000"));
        assertThat(showHashInBinary("b"), is("1010001110110010011000000010000101011110110010001111000100010110"));
        assertThat(showHashInBinary("bb"), is("1000111110000001000111010000100100010110100001011111000010"));
        assertThat(showHashInBinary("c"), is("1001111010100001000110001010100111100000110010110111101100101000"));
        assertThat(showHashInBinary("cc"), is("1011101011010110101101010010011001000100101010000000110101101100"));
        assertThat(showHashInBinary("d"), is("11111000001010111001000110001110010101011111011001101000100001"));
        assertThat(showHashInBinary("dd"), is("1110101110011111101111010000111110111010000110001111111101001110"));
    }

    @Test
    public void nRandomBits_bitsAreAlwaysOnRightSide() {

        String gibberish = "abcde";

        long allBits = compute64BitHash(gibberish);
        long mask = makeBitMask(5);
        long nBits = nRandomBitsFrom(5, gibberish);

        assertThat(inBinary(allBits), is("1100001000101111010001100110001111100101010011100000010011010100"));
        assertThat(inBinary(mask), is("11111"));
        assertThat(inBinary(nBits), is("10100"));

        assertThat(mask, is(31L)); // 1111 in binary = 15
        assertThat(allBits, is(not(nBits)));
        assertThat(nBits, is(allBits & mask));
    }

    @Test
    public void bitmasksAreCorrect() {

        long oneBit = makeBitMask(1);
        long twoBits = makeBitMask(2);
        long threeBits = makeBitMask(3);

        assertThat(inBinary(oneBit), is("1"));
        assertThat(inBinary(twoBits), is("11"));
        assertThat(inBinary(threeBits), is("111"));
    }
}
