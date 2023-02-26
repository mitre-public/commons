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

import static java.time.Instant.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.compute64BitHash;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.makeBitMask;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class SmallTimeIdTest {

    @Test
    public void timeEpochMillsAre41bits() {

        long epochMills = now().toEpochMilli();
        long mask = makeBitMask(41);

        assertThat(epochMills, is(mask & epochMills));
    }

    @Test
    public void timeEpochMillsAre42bits_in20Years() {

        long epochMillsInFuture = now().plus(20 * 365, ChronoUnit.DAYS).toEpochMilli();

        long mask_41 = makeBitMask(41);
        long mask_42 = makeBitMask(42);

        //these 2 assertions use bitwise AND
        assertThat(epochMillsInFuture, is(not(mask_41 &
            epochMillsInFuture))); //41 bits CANNOT store an epochMillis for the semi-distant future
        assertThat(epochMillsInFuture, is(mask_42 &
            epochMillsInFuture)); //42 bits CAN store an epochMillis for the semi-distant future
    }

    @Test
    public void basicConstructorUse() {

        long someNonTimeBits = Long.parseLong("1010101011101", 2);
        Instant baseTime = Instant.now();

        SmallTimeId timeId = new SmallTimeId(baseTime, someNonTimeBits);

        assertThat(timeId.nonTimeBits(), is(someNonTimeBits));
        assertThat(timeId.timeAsEpochMs(), is(baseTime.toEpochMilli()));

        //the "base timestamp" is encoded in bits [63-22]
        //the "non time bits" are encoded in bits [21-1]
        //Therefore, a bit shift operation and a bitwise OR should generate our id
        long expectedID = (timeId.timeAsEpochMs() << 21) | someNonTimeBits;

        assertThat(timeId.id(), is(expectedID));
    }

    @Test
    public void bulkConstructionWithOrdering() {
        /* Verify a bunch of TimeIds encoding the SAME epoch time have a stable ordering based on low-order bits. */

        Instant baseTime = Instant.now();
        int N = 100;

        //Build this list and immediately sort it...
        List<SmallTimeId> ids = IntStream.range(0, N)
            .mapToObj(i -> new SmallTimeId(baseTime, compute64BitHash(Integer.toString(i))))
            .sorted()
            .collect(Collectors.toList());

        //this time we don't sort the list at build time...
        List<SmallTimeId> ids_round2 = IntStream.range(0, N)
            .mapToObj(i -> new SmallTimeId(baseTime, compute64BitHash(Integer.toString(i))))
            .collect(Collectors.toList());

        //Instead we will throw in an extra shuffle just for fun and then sort...
        Collections.shuffle(ids_round2);
        Collections.sort(ids_round2);

        //Since both Lists encode the same source data their post-sort order should be identical
        for (int i = 0; i < N; i++) {
            assertThat(ids.get(i), is(ids_round2.get(i)));
            assertThat(ids.get(i).id(), is(ids_round2.get(i).id()));
            assertThat(ids.get(i).time(), is(ids_round2.get(i).time()));
            assertThat(ids.get(i).nonTimeBits(), is(ids_round2.get(i).nonTimeBits()));
        }
    }

    @Test
    public void hexEncodingAndDecoding() {

        long someNonTimeBits = Long.parseLong("1010101011101", 2);

        SmallTimeId timeId = new SmallTimeId(Instant.now(), someNonTimeBits);
        SmallTimeId fromHex = SmallTimeId.fromString(timeId.toString());

        assertThat(timeId, is(fromHex));
    }
}