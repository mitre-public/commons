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
import static java.lang.Math.sqrt;
import static java.time.Instant.EPOCH;
import static java.time.Instant.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mitre.caasd.commons.ids.TimeId.newId;
import static org.mitre.caasd.commons.ids.TimeId.newIdFor;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.makeBitMask;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Time;

import com.google.common.math.StatsAccumulator;

class TimeIdTest {

    @Test
    public void constructorEmbedsTime() {
        Instant now = now();

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

        Instant now = now();

        TimeId id = new TimeId(EPOCH);
        TimeId id_1 = new TimeId(EPOCH.plusMillis(1L));
        TimeId idNow = new TimeId(now);

        assertThat(TimeId.fromString(id.toString()), is(id));
        assertThat(TimeId.fromString(id_1.toString()), is(id_1));
        assertThat(TimeId.fromString(idNow.toString()), is(idNow));
    }

    @Test
    public void bytesMethodAndConstructorAreConsistent() {

        Instant now = now();

        TimeId idNow = new TimeId(now);  //use the most straight forward constructor
        byte[] byteEncoding = idNow.bytes();
        TimeId fromBytes = new TimeId(byteEncoding);  //build a new ID directly from the bytes

        assertThat(idNow.time(), is(fromBytes.time()));
        assertArrayEquals(idNow.bytes(), fromBytes.bytes());
    }

    @Test
    public void base64Encoding() {

        Instant now = now();

        TimeId idNow = new TimeId(now);  //use the most straight forward constructor

        TimeId fromBase64Str = TimeId.fromBase64(idNow.asBase64());

        assertThat(idNow, is(fromBase64Str));
        assertThat(idNow.time(), is(fromBase64Str.time()));
        assertArrayEquals(idNow.bytes(), fromBase64Str.bytes());
    }

    @Test
    public void bulkToStringFromStringCycles() {

        int n = 100;
        for (int i = 0; i < n; i++) {
            TimeId id = newId();
            TimeId rebuilt = TimeId.fromString(id.toString());

            assertThat(id, is(rebuilt));
        }
    }

    @Test
    public void base64Encoding_22charsLong() {
        TimeId id = TimeId.newId();
        assertThat(id.asBase64().length(), is(22));
    }

    @Test
    public void base64Encoding_first7CharAreTime() {
        Instant time = now();
        TimeId id1 = newIdFor(time);
        TimeId id2 = newIdFor(time);
        TimeId id3 = newIdFor(time);

        //The 1st 7-char of each base64 encoding contain "Time info" THEREFORE, when the time is the same the chars are the same.
        assertThat(id1.asBase64().substring(0, 7), is(id2.asBase64().substring(0, 7)));
        assertThat(id2.asBase64().substring(0, 7), is(id3.asBase64().substring(0, 7)));
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
         * This test isolates the hex parsing component of "toHexString()" and "fromHexString(String)"
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

    @Test
    public void toHexStringEncodingAndParsing() {

        Instant now = now();
        TimeId id = new TimeId(now);

        //e.g. "6299c83dbbf26ab8f01257782fb49a37"
        String hexString = id.asHexString();

        assertThat(hexString.length(), is(32));

        TimeId rebuiltFromHexStr = TimeId.fromHexString(hexString);

        assertThat(id, is(rebuiltFromHexStr));
        assertThat(id.time(), is(rebuiltFromHexStr.time()));
        assertArrayEquals(id.bytes(), rebuiltFromHexStr.bytes());
    }

    @Test
    public void idsCanBeFileNames() {

        //ALLOW ONLY THESE CHARS:  A-Z, a-z, 0-9, '.', '_', and '-'
        // source  https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations
        // section POSIX "Fully portable filenames

        String REGEX_PATTERN = "^[A-Z a-z 0-9 \\- \\_ \\.]{1,255}$";

        for (int i = 0; i < 100; i++) {
            TimeId id = TimeId.newId();
            assertThat(id.toString().matches(REGEX_PATTERN), is(true));
        }
    }

    @Test
    public void randomBitsAndTimeBitsMakeAllBits() {
        // Show that we can make "id.bytes()" from JUST the time data and JUST the random data
        // e.g., the random bits are complete
        // e.g., id.randomBytes() and id.timeAsEpochMs() contain 100% of the data in the TimeId

        TimeId id = newId();

        byte[] randomBits = id.randomBytes();
        byte[] justTimeBits = ByteBuffer.allocate(8)
            .putLong(id.timeAsEpochMs() << 22)
            .array();

        byte[] allBits = id.bytes();

        byte[] manuallyConstructed = new byte[16];  //GOAL -- rebuild "allBits" from randomBits & justTimeBits

        //The "time bits" match the bits we get from "timeId.bytes()"
        for (int j = 0; j < 5; j++) {
            //bits 0-8, 8-16, ... 32-40
            assertThat(justTimeBits[j], is(allBits[j]));
            manuallyConstructed[j] = justTimeBits[j];
        }

        //We can construct the 6th byte (bits 40-48) using timeBits "OR-ed together" with the randomBits
        byte splitByte = (byte) (justTimeBits[5] | randomBits[5]);
        assertThat(splitByte, is(allBits[5]));

        manuallyConstructed[5] = splitByte;

        //The "random bits" match the bits we get from "timeId.bytes()"
        for (int j = 6; j < 16; j++) {
            //bits 48-56, 56-64, ... 120-128
            assertThat(randomBits[j], is(allBits[j]));
            manuallyConstructed[j] = randomBits[j];
        }

        TimeId idFromManualBytes = new TimeId(manuallyConstructed);

        assertThat(id, is(idFromManualBytes));
    }

    @Test
    public void randomBitsAndEncodingMatch() {

        TimeId id = newId();

        //e.g. "YpmLwbo1MNma0swdxsojUQ"
        String fullBase64Encoding = id.asBase64();

        //e.g. "1MNma0swdxsojUQ"  (
        String rngBase64Encoding = id.rngBitsAsBase64();

        assertThat(fullBase64Encoding.length(), is(22));
        assertThat(rngBase64Encoding.length(), is(15));
        assertThat(rngBase64Encoding, is(id.asBase64().substring(7)));

        //e.g. "AAAAAAA1MNma0swdxsojUQ"
        //Manually create the base64 encoding of just the "randomBytes()"
        String base64_fromJustRNG = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(id.randomBytes());

        assertThat(id.rngBitsAsBase64(), is(base64_fromJustRNG.substring(7)));
    }

    @Test
    public void testAsUniformRand_range0to1_andDistributionIsUniform() {

        // This test will RANDOMLY fail 1 in 15787 tries!

        int SAMPLE_SIZE = 10_000;

        StatsAccumulator accumulator = new StatsAccumulator();
        IntStream.range(0, SAMPLE_SIZE)
            .mapToObj(i -> newId())
            .mapToDouble(id -> id.asUniformRand())
            .forEach(rngSample -> accumulator.add(rngSample));

        //Basic Truths, all sample 0-1, correct number of samples...
        assertThat(accumulator.max(), lessThan(1.0));
        assertThat(accumulator.min(), greaterThan(0.0));
        assertThat((int) accumulator.count(), is(SAMPLE_SIZE));

        Double standDev = accumulator.sampleStandardDeviation();

        //SOURCE = https://en.wikipedia.org/wiki/Continuous_uniform_distribution
        double expectedVariance = 1.0 / 12.0;
        double expectedStdDev = sqrt(expectedVariance);
        double expectedMean = .5;

        double stdDev_of_xBar = expectedStdDev / sqrt(SAMPLE_SIZE);

        double zScore = (accumulator.mean() - expectedMean) / stdDev_of_xBar;

        //SOURCE = https://en.wikipedia.org/wiki/68%E2%80%9395%E2%80%9399.7_rule
        // Allowing +- 4 standard deviations or 0.999936657516334 (pass prob)
        //  This test will RANDOMLY fail 1 in 15787 tries
        assertThat(zScore, closeTo(0, 4.0));
    }

    @Test
    public void asUniformIsLossy() {

        long bitMask = makeBitMask(63);

        System.out.println(Long.toHexString(bitMask));

        System.out.println(Long.toBinaryString(bitMask));



        long bitMask2 = Long.parseLong("7fffffffffffffff", 16);
        System.out.println(Long.toBinaryString(bitMask2));


    }
}