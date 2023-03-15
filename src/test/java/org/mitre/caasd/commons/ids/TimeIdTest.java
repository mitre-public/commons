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

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mitre.caasd.commons.ids.TimeId.newId;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Time;

class TimeIdTest {

    @Test
    public void constructorEmbedsTime() {
        Instant now = Instant.now();

        TimeId id = new TimeId(EPOCH.minusMillis(1L));
        TimeId id_1 = new TimeId(EPOCH.plusMillis(1L));
        TimeId idNow = new TimeId(now);

        assertThat(id.time(), is(EPOCH.minusMillis(1L)));
        assertThat(id.timeAsEpochMs(), is(-1L));

        assertThat(id_1.time(), is(EPOCH.plusMillis(1L)));
        assertThat(id_1.timeAsEpochMs(), is(1L));

        //The Instant are not perfect copies because the nanosecond part of the Instant is not carried over
        assertThat(idNow.time(), is(not(now)));

        //But the "epochMilliSeconds" are the same
        assertThat(idNow.timeAsEpochMs(), is(now.toEpochMilli()));
    }

    @Test
    public void fromStringParserYieldsEquivalentIds() {

        Instant now = Instant.now();

        TimeId id = new TimeId(EPOCH);
        TimeId id_1 = new TimeId(EPOCH.plusMillis(1L));
        TimeId idNow = new TimeId(now);

        assertThat(TimeId.fromString(id.toString()), is(id));
        assertThat(TimeId.fromString(id_1.toString()), is(id_1));
        assertThat(TimeId.fromString(idNow.toString()), is(idNow));
    }

    @Test
    public void bytesMethodAndConstructorAreConsistent() {

        Instant now = Instant.now();

        TimeId idNow = new TimeId(now);  //use the most straight forward constructor
        byte[] byteEncoding = idNow.bytes();
        TimeId fromBytes = new TimeId(byteEncoding);  //build a new ID directly from the bytes

        assertThat(idNow.time(), is(fromBytes.time()));
        assertArrayEquals(idNow.bytes(), fromBytes.bytes());
    }

    @Test
    public void base64Encoding() {

        Instant now = Instant.now();

        TimeId idNow = new TimeId(now);  //use the most straight forward constructor

        TimeId fromBase64Str = TimeId.fromBase64(idNow.asBase64());

        assertThat(idNow, is(fromBase64Str));
        assertThat(idNow.time(), is(fromBase64Str.time()));
        assertArrayEquals(idNow.bytes(), fromBase64Str.bytes());

        for (int i = 0; i < 1000; i++) {
            System.out.println(newId().asBase64());
        }
    }

    @Test
    public void orderingMatchesHasTime() {

        TimeId old = new TimeId(EPOCH.minusMillis(1L));
        TimeId lessOld = new TimeId(EPOCH.plusMillis(1L));
        TimeId another = newId();

        ArrayList<TimeId> byIdCompare = newArrayList(old, lessOld, another);
        ArrayList<TimeId> byHasTime = newArrayList(another, old, lessOld);

        Collections.sort(byIdCompare);
        Collections.sort(byHasTime, Time.compareByTime());

        assertThat(byIdCompare.get(0), is(byHasTime.get(0)));
        assertThat(byIdCompare.get(1), is(byHasTime.get(1)));
        assertThat(byIdCompare.get(2), is(byHasTime.get(2)));
    }

    @Test
    public void cyclicalLongParsing() {
        /*
         * This test isolates the hex parsing component of "toString()" and "fromString(String)"
         * The implementation was harder than you'd expect because the java.lang.Long parser
         * wasn't working for 64 random bits (when those bits defined a negative long)
         */
        SecureRandom rng = new SecureRandom();

        for (int i = 0; i < 100; i++) {
            long randomLong = rng.nextLong();

            String encoding = String.format("%016x", randomLong);
            long decoding = (new BigInteger(encoding, 16)).longValue();

            assertThat(randomLong, is(decoding));
        }
    }

}