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

import static com.google.common.base.Preconditions.checkArgument;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.makeBitMask;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.mitre.caasd.commons.HasTime;

/**
 * A TimeId is a 128-bit unique identifier that combines key features of java.util.UUID and
 * java.time.Instant.
 *
 * <p>The TimeId bits are: {{42 time bits}} + {{86 pseudo-random bits}}
 *
 * <p>TimeId takes after Twitter's Snowflake ID (see https://en.wikipedia.org/wiki/Snowflake_ID)
 * and UUID (see https://en.wikipedia.org/wiki/Universally_unique_identifier). TimeId is similar to
 * Snowflake_ID because both systems embed a timestamp that ensures the id's are mostly sorted by
 * time. TimeId is similar to UUID because they both allocate enough bits to storing pseudo-random
 * bits for the probability of a hash-collision to be vanishingly small.
 *
 * <p>For example, if you create 100 Million TimeId's that all embed the same timestamp (to the
 * millisecond resolution) the probability of a hash collision is only 6.462e-11. This means there
 * is virtually zero chance for TimeId's to collide if they are generated correctly (i.e. you don't
 * use two random number generators with the same seed).
 *
 * <p>TimeId's 86-bit allocation for pseudo-random bits permits 2^86 =
 * 77,371,252,455,336,267,181,195,264 possible unique hashes.
 *
 * <p>Besides uniqueness, TimeId provides 2 additional features. They are Comparable and they
 * can be built while the data an ID will be used to identify is STILL changing. In other words,
 * using TimeId does NOT require capturing an entire data stream (like a completed flight track) AND
 * THEN building the identifying TimeId by hashing the contents of the completed data stream.
 * Rather, TimeId supports providing a unique id at the earliest possible moment (e.g. once a new
 * data stream is identified).
 *
 * <p>Class design goals: Be unique, Be comparable by seed time, Enable compact serialization,
 * Support "backing out" the input timestamp, Solve the "ensure ID uniqueness" problem by having
 * enough bits so that hash collision are not a practical concern, and finally Eliminate the need
 * for "coordinating and configuring stateful ID Factories (see IdFactoryShard)"
 */
public class TimeId implements Comparable<TimeId>, HasTime, Serializable {

    private static final long serialVersionUID = 3013947926990530136L;

    /** The RNG that creates random bytes. In a holder class to defer initialization until needed. */
    private static class Holder {
        static final SecureRandom RNG = new SecureRandom();
    }

    /** TimeIds are encoded as Base64 without padding (get the Encoder exactly once). */
    private static final Base64.Encoder BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** Number of bits extracted from a timestamp's epochMills long. */
    private static final int NUM_TIMESTAMP_BITS = 42;

    /** Number of pseudo-random bits within the "mostSigBits" long. */
    private static final int NUM_RAND_BITS_ON_LEFT = 22;

    /** A bitmask to help isolate the correct number of bits from an epochMills. */
    private static final long TIME_BIT_MASK = makeBitMask(NUM_TIMESTAMP_BITS);

    /** A bitmask to help isolate 21 pseudo-random bits from a long. */
    private static final long NON_TIME_BIT_MASK = makeBitMask(NUM_RAND_BITS_ON_LEFT);

    /** The 1st 64 bits of this id (42 time bits are on the left, 22 random bits on the right). */
    private final long leftBits;

    /** The 2nd 64 bits of this id (all bits are random) (there are the least significant bits). */
    private final long rightBits;

    /**
     * Create a new TimeId that references the provided timestamp. This TimeId stores 42-bits
     * of a timestamp and 86-bits of pseudo-randomness to (A) ensure the IDs are sortable by time
     * and (B) ensure no two IDs are the same because there 86-bits of randomness will be
     * different.
     *
     * @param time The epochMills of this Instant get embedded in the resulting TimeId
     */
    public TimeId(Instant time) {
        //Inspired by java.util.UUID's implementation of randomUUID()
        SecureRandom ng = Holder.RNG;

        long timeBasedBits = TIME_BIT_MASK & time.toEpochMilli(); //isolate 42 bits from epochMills
        long nonTimeBits = NON_TIME_BIT_MASK & ng.nextLong(); //isolate 22 pseudo-random bits
        this.leftBits = timeBasedBits << NUM_RAND_BITS_ON_LEFT | nonTimeBits;

        this.rightBits = ng.nextLong(); //64 pseudo-random bits
    }

    /** @return a unique TimeId that references the provided timestamp. */
    public static TimeId newIdFor(Instant time) {
        return new TimeId(time);
    }

    /** @return a unique TimeId that references Instant.now() as its internal timestamp. */
    public static TimeId newId() {
        return new TimeId(now());
    }

    /**
     * Directly build a TimeId from 16 bytes (useful when working with serialization layers).
     *
     * @param leftBits  The most significant bits of the {@code TimeId}
     * @param rightBits The least significant bits of the {@code TimeId}
     */
    private TimeId(long leftBits, long rightBits) {
        this.leftBits = leftBits;
        this.rightBits = rightBits;
    }

    /**
     * Directly build a TimeId from 16 bytes (useful when working with serialization layers).
     *
     * @param exactly16Bytes The bytes used to encode a TimeId
     */
    public TimeId(byte[] exactly16Bytes) {
        requireNonNull(exactly16Bytes);
        checkArgument(exactly16Bytes.length == 16, "Must use exactly 16 bytes");
        long bigBits = 0;  //e.g. most significant bits
        long smallBits = 0;  //e.g. least significant bits
        for (int i = 0; i < 8; i++) {
            bigBits = (bigBits << 8) | (exactly16Bytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            smallBits = (smallBits << 8) | (exactly16Bytes[i] & 0xff);
        }
        this.leftBits = bigBits;
        this.rightBits = smallBits;
    }

    /** @return The 16 bytes in this TimeId. */
    public byte[] bytes() {
        return ByteBuffer.allocate(16)
            .putLong(leftBits)
            .putLong(rightBits)
            .array();
    }

    /**
     * @return The 16 bytes of this TimeId THAT HAVE BEEN MASKED to only include the random bits.
     *     Thus, the first 42 bits of the byte[] are ALWAYS zero and the last 86 bits in the byte[]
     *     are random.  This means the first 5 bytes of the resulting byte[] can be "thrown out" or
     *     "deleted" if saving 5 bytes per TimeId is worthwhile.
     */
    public byte[] randomBytes() {

        /*
         * Implementation note:  We choose to return 16 bytes, KNOWING 5 entire bytes will be empty.
         * We made this choice because it means:
         *
         * (A) Now, the output of "bytes()" and "randomBytes()" are only different by a simple bit
         * masking operation and
         *
         * (B) Now, the base-64 encoding of "bytes()" and "randomBytes()" yields two String that are
         * obviously related (e.g., "YpmEcX3VEmPQ35Kic756OQ" and "AAAAAAAVEmPQ35Kic756OQ").  Notice,
         * the first 7 chars of the 2nd String are all "A"s and the last 15 chars of these String
         * are identical.  IF! we redacted the 5 empty bytes from this method's output then these
         * base-64 encoded String would not be so similar.
         */

        //Isolate just the 22 pseudo-random bits within the "left bits" (drops 42 bits, or 5 full bytes)
        long randomBits = leftBits & NON_TIME_BIT_MASK;

        return ByteBuffer.allocate(16)
            .putLong(randomBits)
            .putLong(rightBits)
            .array();
    }

    private static final long SIXTY_THREE_BIT_MASK = Long.parseLong("7fffffffffffffff", 16);

    /**
     * Converts 63 random bits in this TimeId to a uniform random variable. This method supports
     * randomized data sampling algorithms for cases when every piece of data is labeled with a
     * TimeId.
     * <p>
     * Warning: DO NOT rely on this value for hashing. The double returned here is derived from only
     * 63 random bits. Thus, the probability of hash-collision will increase.
     *
     * @return A uniform random number between 0 and 1, based on 63 random bits
     */
    public double asUniformRand() {

        //Isolate 63 pseudo-random bits within the "right bits"
        long randomBits = rightBits & SIXTY_THREE_BIT_MASK;
        long maxPossibleValue = SIXTY_THREE_BIT_MASK;

        return (double) randomBits / (double) maxPossibleValue;
    }

    /**
     * @return The {@code bytes()} of this ID encoded as unpadded Base64 String (e.g.
     *     "YpnxRaZ-usc_8KDibYzccw"). The returned String will be 22 characters.  The first 7 chars
     *     encode the epochMills of the source instant, the last 15 char encode the 86 bits of
     *     randomness. Note: Base64 encodes 6 bits per char, so we get lucky that a "42 time bits"
     *     can be isolated so cleanly to just the first 7 chars of this encoding.
     */
    public String asBase64() {
        return BASE_64_ENCODER.encodeToString(bytes());
    }

    /**
     * @return The component of the "asBase64()" output that corresponds to the 86-bits of
     *     randomness. This maps to characters 7-22 of the "asBase64()" output (i.e., the last 15
     *     characters from the 22 character base-64 encoding)
     */
    public String rngBitsAsBase64() {
        return asBase64().substring(7);
    }

    /** @return A new TimeId by parsing the binary data represented within a Base64 String. */
    public static TimeId fromBase64(String str) {
        requireNonNull(str);

        byte[] exactly16Bytes = Base64.getUrlDecoder().decode(str);

        return new TimeId(exactly16Bytes);
    }

    /**
     * @return A 22-character String encoding of this 128-bit id (e.g. "YpnxRaZ-usc_8KDibYzccw").
     *     This is the base-64 "unpadded", "url-safe" encoding of the 128-bit id. Note, base-64
     *     encodes 6 bits per char. So we get lucky! The first 42 bits (i.e. all the time bits) of
     *     this TimeId become the first 7 characters of this String.  The last 86 bits (i.e. all the
     *     random bits) of this TimeId become the last 15 characters of this String.
     */
    public String toString() {

        //some samples toStrings
        // YpnxRaaF_h64-ogTLrRE_g
        // YpnxRaaE500hOUgD4eO1dw
        // YpnxRaaLGRd3W__ogPSekw

        return asBase64();
    }

    /** @return A new TimeId by parsing the binary data represented within a Base64 String. */
    public static TimeId fromString(String str) {
        requireNonNull(str);
        return fromBase64(str);
    }

    /**
     * @return A 32-character hex String embedding this 128-bit id (e.g.
     *     "609c2cf98dc9fa21d9633a14f800bbb6").  Upside: This encoding only uses standard hex chars.
     *      Downsides:  This encoding requires 32 chars AND the 11th char mixes bits from the
     *     "time portion" and "random portion" of the TimeId (thus you can't split the String to
     *     isolate JUST the random bits or JUST the time bits)
     */
    public String asHexString() {
        return String.format("%016x", leftBits) + String.format("%016x", rightBits);
    }

    /**
     * Construct a TimeId from the hex string representation given by asHexString().
     *
     * @param hexStr A string that specifies a TimeId
     *
     * @return A TimeId with the internal bits configured to match the input hex value
     */
    public static TimeId fromHexString(String hexStr) {
        requireNonNull(hexStr);
        checkArgument(hexStr.length() == 32, "A 32 character hex string is expected");

        // Using BigInteger because decoding hex Strings with "java.lang.Long" code is failing ??
        //
        // For example, when long = -7974281278593573608 and hexString = "9155a96d3a61dd18"
        // Long.decode(hex, 16) and Long.parseLong(hex, 16) will Fail.
        BigInteger left = new BigInteger(hexStr.substring(0, 16), 16);
        BigInteger right = new BigInteger(hexStr.substring(16, 32), 16);

        return new TimeId(left.longValue(), right.longValue());
    }

    @Override
    public Instant time() {
        return Instant.ofEpochMilli(leftBits >> NUM_RAND_BITS_ON_LEFT);
    }

    @Override
    public long timeAsEpochMs() {
        //override because it's wasteful to construct the Instant object we don't need.
        return leftBits >> NUM_RAND_BITS_ON_LEFT;
    }

    /** @return A hash code value for this {@code TimeId} */
    public int hashCode() {
        //implementation from java.util.UUID
        long hilo = leftBits ^ rightBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    public boolean equals(Object obj) {
        //implementation from java.util.UUID
        if ((null == obj) || (obj.getClass() != TimeId.class)) {
            return false;
        }
        TimeId id = (TimeId) obj;
        return (leftBits == id.leftBits &&
            rightBits == id.rightBits);
    }

    public int compareTo(TimeId val) {
        // Reusing the compareTo implementation from java.util.UUID

        // The ordering is intentionally set up so that the ids
        // can simply be numerically compared as two numbers
        return (this.leftBits < val.leftBits ? -1 :
            (this.leftBits > val.leftBits ? 1 :
                (this.rightBits < val.rightBits ? -1 :
                    (this.rightBits > val.rightBits ? 1 :
                        0))));
    }
}