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
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.makeBitMask;
import static org.mitre.caasd.commons.util.BitAndHashingUtils.truncateBits;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;

import org.mitre.caasd.commons.HasTime;

/**
 * A SmallTimeId is a 63-bit unique identifier that embeds a millisecond timestamp.
 *
 * <p>SmallTimeId takes after Twitter's Snowflake ID (see https://en.wikipedia.org/wiki/Snowflake_ID)
 * because both ID systems: (1) Generate 63-bits longs for uniquely identifying objects and (2)
 * Embed a timeStamp within the id. These systems generate their 63-bits ids by combining 40-ish
 * bits from a timestamp and 20-ish bits from elsewhere. Snowflake_ID uses 41 and 22 bits while
 * TimeId uses 42 and 21 bits.
 *
 * <p>Besides uniqueness, SmallTimeId provides 2 additional features. They are Comparable and they
 * can by built while the data a SmallTimeId will be used to identify is STILL changing. In other
 * words, using SmallTimeId does NOT require capturing an entire data stream (like a completed
 * flight track) AND THEN building the identifying SmallTimeId by hashing the contents of the
 * completed data stream. Rather, SmallTimeId supports providing a unique id at the earliest
 * possible moment (e.g. once a new data stream is identified).
 *
 * <p>Class design goals: Be unique, Be comparable by seed time, Enable VERY compact serialization,
 * Support "backing out" the input timestamp, Delay/Off-load the decision about how to create the
 * 21-bits used to distinguish multiple SmallTimeId that encode the same timestamp. Aka retain the
 * ability to choose between simple stateless SmallTimeId-ing (probably based on hashing) and
 * stateful SmallTimeId-ing (probably based on stateful "ID Factory Layer")
 *
 * <p>SmallTimeId is different from Snowflake_ID because a TimeId does not know/care how the
 * "non-time
 * bits" are generated. Snowflake_ID, on the other hand, has a strong opinion on how the "non-time
 * bits" are generated. Getting a Snowflake_ID requires reaching out to a dedicated ID generation
 * service that ensures "No two Snowflake_IDs will be the same 63-bits" even when multiple
 * Snowflake_IDs have the same millisecond time stamp. This strong "no Id collision guarantee" comes
 * with a non-trivial integration price and performance-rate-limit.
 *
 * <p>SmallTimeId's sole constructor requires the caller to provide the "21 non-time bits". This
 * means
 * the caller/user can use any id-generation strategy they wish so long as that strategy adequately
 * distinguish objects using only the 21 bits of "space" available (Note: 21-bits provides enough
 * space for 2_097_152 unique bit-sequences).
 *
 * <p>Directly using SmallTimeId's constructor is discouraged because: (1) convenient factory
 * methods
 * are available, (2) manually creating the distinguishing 21-bits is tedious and (3) incorporating
 * a factory method in your code will make it far easier to change the id-generation strategy in the
 * future.
 *
 * <p>KEEP IN MIND -- A stateful SmallTimeId Factory (like IdFactoryShard) can make MUCH better
 * use of the available bitspace than any ID-ing strategy that relies on hashing or randomization. A
 * stateful ID Factory can systematically increment the SmallTimeId bits and avoid the pitfalls of a
 * birthday attack (see https://en.wikipedia.org/wiki/Birthday_attack). Therefore, it makes sense to
 * avoid the regular constructor and use a "factory method" layer. The existence of this layer will
 * simplify changing the ID generation strategy should future needs require it.
 *
 * <p>Factory methods are provided for common use cases like hash-based ID construction and simple
 * indexing. Note, opting for deterministic ID generation GAINS reproducibility, independence from a
 * dedicated "ID generation service", and easily parallelism but it COSTS having a chance for id
 * collisions while drastically reducing the effectively useful bitspace (birthday attacks are
 * legit)
 */
public class SmallTimeId implements Comparable<SmallTimeId>, HasTime, Serializable {

    private static final long serialVersionUID = 6470927123705680470L;

    /** Number of bits extracted from a timestamp's epochMills long. */
    private static final int NUM_BITS_FOR_TIMESTAMP = 42;

    /** Number of bits available to distinguish different TimeIds that encode the same timestamp. */
    static final int NUM_BITS_FOR_DISTINGUISHING_ITEMS = 21;

    private static final long BIT_MASK_FOR_DISTINGUISHING_BITS =
        makeBitMask(NUM_BITS_FOR_DISTINGUISHING_ITEMS);

    /** bitset = {{bits from epochMills}} + {{bits to distinguish IDs with same timestamp}} */
    private final long idBits;


    /**
     * A SmallTimeId is a stable GLOBALLY unique identifier for a piece of data. There are important
     * technical nuances to making SmallTimeId that will not collide with each other. BUT! this
     * constructor does nothing to enforce or encourage those technical nuances. Consequently, it is
     * almost always better to use a Factory (see IdFactoryShard) or a static factory method.
     *
     * @param time          An Instant used to "timestamp" this SmallTimeId (up to ms are used)
     * @param twentyOneBits These bits are concatenated with the "epoch time bits" to distinguish
     *                      the collection of different objects that are seeded with the same
     *                      epochMillis time value. Only the right most 21 bits will be used. These
     *                      bits are typically created by truncated the result of a hashFunction or
     *                      using a "counter service" that ensures uniqueness for all the TimeIds
     *                      generated from the same input time Instant.
     */
    public SmallTimeId(Instant time, long twentyOneBits) {

        long timeBasedBits = truncateBits(time.toEpochMilli(), NUM_BITS_FOR_TIMESTAMP);
        long distinguishingBits = truncateBits(twentyOneBits, NUM_BITS_FOR_DISTINGUISHING_ITEMS);

        //shift the 42 bits...of time value... then add in 21 bits pseudo randomness
        this.idBits = (timeBasedBits << NUM_BITS_FOR_DISTINGUISHING_ITEMS) | distinguishingBits;
    }

    public Instant time() {
        return Instant.ofEpochMilli(idBits >> NUM_BITS_FOR_DISTINGUISHING_ITEMS);
    }

    @Override
    public long timeAsEpochMs() {
        //override because it's wasteful to construct the Instant object we don't need.
        return idBits >> NUM_BITS_FOR_DISTINGUISHING_ITEMS;
    }

    /** @return the full 63 bit TimeId as a long (i.e. {{42 time-based bits}} + {{21 non-time bits}} */
    public long id() {
        return this.idBits;
    }

    /** @return A 16-character hex String embedding this 64-bit id (e.g. "609c2cf98dc9fa21"). */
    public String toString() {
        return String.format("%016x", idBits);
    }

    /**
     * Construct a SmallTimeId from the hex string representation given by toString().
     *
     * @param hexStr A hexadecimal String like "609c2cf98dc9fa21" (notice the missing of "0x")
     * @return A new SmallTimeId.
     */
    public static SmallTimeId fromString(String hexStr) {
        requireNonNull(hexStr);
        checkArgument(hexStr.length() == 16, "Exactly 16 hexadecimal chars are required");
        long bits = new BigInteger(hexStr, 16).longValue();
        Instant time = Instant.ofEpochMilli(bits >> NUM_BITS_FOR_DISTINGUISHING_ITEMS);

        return new SmallTimeId(time, bits);
    }

    /**
     * @return The 21 "non-time bits" within the id. These bits are used to distinguish multiple
     * SmallTimeId that reference the same seed Instant. These are also the 21 lowest order bits in
     * the id.
     */
    public long nonTimeBits() {
        return idBits & BIT_MASK_FOR_DISTINGUISHING_BITS;
    }

    @Override
    public int compareTo(SmallTimeId other) {
        return Long.compare(idBits, other.idBits);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SmallTimeId timeId = (SmallTimeId) o;

        return idBits == timeId.idBits;
    }

    //longs are 64-bits, but hashcodes are only 32-bit ints...so we'll need a mask.
    private static final long HASH_MASK = makeBitMask(32);

    @Override
    public int hashCode() {
        /*
         * THIS IS A CUSTOM HASHCODE IMPLEMENTATION! I'M USING THE LOWEST ORDER 32 because: (a)
         * the goal of the lower "non-time" bits is to avoid collision and (b) the highest
         * "epochTime bits" will show very little bit dispersion, so we don't want to utilize them
         * in the hash function.
         */
        return (int) (HASH_MASK & idBits);
    }
}