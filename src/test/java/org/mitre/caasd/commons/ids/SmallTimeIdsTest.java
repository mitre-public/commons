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

package org.mitre.caasd.commons.ids;

import static java.lang.Long.toBinaryString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mitre.caasd.commons.ids.TimeIds.directBitsetTimeId;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.compute64BitHash;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.truncateBits;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SmallTimeIdsTest {



    @Test
    public void verifyDirectBitsetTimeId() {

        long manyRandomBits_1 = compute64BitHash("hello");
        long manyRandomBits_2 = compute64BitHash("goodbye");

        assertThat(toBinaryString(manyRandomBits_1), is("1011010010001011111001011010100100110001001110000000110011101000"));
        assertThat(toBinaryString(manyRandomBits_2), is("1000010111100111110001101110100010011101011101001111111011001111"));

        Instant baseTime = Instant.EPOCH;
        Instant nextTime = baseTime.plusMillis(100);

        SmallTimeId id_1 = directBitsetTimeId(baseTime, manyRandomBits_1);
        SmallTimeId id_2 = directBitsetTimeId(baseTime, manyRandomBits_2);
        SmallTimeId id_3 = directBitsetTimeId(nextTime, manyRandomBits_1);
        SmallTimeId id_4 = directBitsetTimeId(nextTime, manyRandomBits_2);

        assertThat(id_1.time(), is(baseTime));
        assertThat(id_2.time(), is(baseTime));
        assertThat(id_3.time(), is(nextTime));
        assertThat(id_4.time(), is(nextTime));

        //aka the last 21 bits of the "manyRandomBits_N" values...
        assertThat(id_1.nonTimeBits(), is(truncateBits(manyRandomBits_1, 21)));
        assertThat(id_2.nonTimeBits(), is(truncateBits(manyRandomBits_2, 21)));
        assertThat(id_3.nonTimeBits(), is(truncateBits(manyRandomBits_1, 21)));
        assertThat(id_4.nonTimeBits(), is(truncateBits(manyRandomBits_2, 21)));

        //aka the last 21 bits of the "manyRandomBits_N" values (but this test is for human readability)
        assertThat(toBinaryString(id_1.nonTimeBits()), is("110000000110011101000"));
        assertThat(toBinaryString(id_2.nonTimeBits()), is("101001111111011001111"));
        assertThat(toBinaryString(id_3.nonTimeBits()), is("110000000110011101000"));
        assertThat(toBinaryString(id_4.nonTimeBits()), is("101001111111011001111"));
    }


}