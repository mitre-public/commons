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
import static com.google.common.math.DoubleMath.log2;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.ids.SmallTimeId.NUM_BITS_FOR_DISTINGUISHING_ITEMS;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.mitre.caasd.commons.ids.TimeIds.IdFactory;

/**
 * A {@link IdFactoryShard} is one member of a "team of non-communicating factories" that
 * collectively guarantee no SmallTimeIds created by the "factory team" will have colliding id bits.
 * To make this strong guarantee: (A) some bits are allocated for recording the "shard index", (B)
 * each IdFactoryShard retains an incrementing counter for assigning the remaining bits, and (C)
 * there is a strict ceiling to how many SmallTimeIds can be created for any specific input
 * timestamp (because eventually the bitspace will fill up).
 *
 * <p>The SmallTimeIds bits are:
 *
 * <p>{{42 time bits}} + {{n shardIndex bits }} + {{ (21-n) shard-specific counter bits}}
 *
 * <p>A {@link IdFactoryShard} is essentially a Snowflake_IDs implementation (see
 * https://en.wikipedia.org/wiki/Snowflake_ID) that does not include the REST API layer.
 *
 * <p>The implementation differences are: Snowflake_ID uses 41 bits for the timestamp, Snowflake_ID
 * always allocates 10 bits to the machine/shard index, Snowflake_ID is imagined as a remote service
 * (that probably uses its own internal time for the timestamps). On the other hand, TimeIds use an
 * extra bit (42 vs 41) to store the timestamp to be more resilient to future epochMill values. a
 * IdFactoryShard can use fewer than 10 bits for the machine/shard index. Finally, IdFactoryShard is
 * a service that "enriches the timestamps provided" rather than a service that merely provides
 * a "unique timestamps+UUID".
 *
 * <p>Philosophically, {@link IdFactoryShard} is designed to:
 *
 * <p> - 1: Make more efficient use of the available bitspace by avoiding the pitfalls of hashing
 * (see https://en.wikipedia.org/wiki/Birthday_attack. The article shows how hash collisions can
 * become common if the available bit space is too small)
 *
 * <p> - 2: Support non-colliding distributed key-generation (and parallelism) by partitioning the
 * available bitspace. (Proper distributed key generation requires all IdFactoryShards to have with
 * a unique "shardIndex" but a single-shared "totalShardCount").
 */
public class IdFactoryShard implements IdFactory<SmallTimeId> {

    /** The number of bits requires to store the largest possible shardIndex. */
    private final int numBitsForShardNumber;

    /** The number of bits used to distinguish different items with same epochMills. */
    private final int numBitsForItemDistinction;

    /** The shardIndex of this Factory-team member (i.e. the 8 in "shard 8 of 12"). */
    private final int shardIndex;

    /** The size of the "Factory-team" (i.e. the 12 in "shard 8 of 12"). */
    private final int numShardsInTeam;

    /** The maximum number of unique TimeIds that can be made for each unique epoch mills. */
    private final int limitTimeIdsPerEpochMills;

    /**
     * The "obvious goal" of the CountKeeper is to keep track of how often this factory has been
     * asked to generate a TimeId for any particular Instant
     *
     * <p>But, the "REAL GOAL" of CountKeeper is to provide a seam where important but subtle
     * implementation details can be injected by the user. Specifically, the CountKeeper interface
     * allows the user to inject code that controls behavior like "How much memory is allocated to
     * storing the Instant Counts?", "What happens when a nextCountRequest occurs for a VERY old
     * Instant?", and "Should counts be persisted to a durable data storage layer?"
     */
    private final CountKeeper timeCounts;

    /**
     * Create one member of a "Team of independent non-communicating factories".
     *
     * @param shardIndex      The index of this "factory team member" (i.e. the "5" in "5 of 12")
     * @param totalShardCount The total number of TimeIdFactoryShard objects that are making keys
     * @param countKeeper     Provides the "count for each Instant". This is a simple task but it
     *                        requires making decisions about in-memory burden, cache-eviction,
     *                        and/or Exception handling policy when counts are requested for
     *                        spurious Instants.
     */
    public IdFactoryShard(int shardIndex, int totalShardCount, CountKeeper countKeeper) {
        checkArgument(shardIndex >= 0, "The shardIndex cannot be negative");
        checkArgument(totalShardCount >= 1, "The totalShardCount must be at least 1");
        checkArgument(numBitsRequiredFor(totalShardCount) < NUM_BITS_FOR_DISTINGUISHING_ITEMS);
        checkArgument(
            shardIndex < totalShardCount,
            "The shardIndex must be smaller than totalShardCount"
        );
        requireNonNull(countKeeper);

        this.shardIndex = shardIndex;
        this.numShardsInTeam = totalShardCount;

        this.numBitsForShardNumber = numBitsRequiredFor(totalShardCount);
        this.numBitsForItemDistinction = NUM_BITS_FOR_DISTINGUISHING_ITEMS - numBitsForShardNumber;
        this.limitTimeIdsPerEpochMills = (int) Math.pow(2.0, numBitsForItemDistinction());
        this.timeCounts = countKeeper;
    }

    /**
     * Create one member of a "Team of independent non-communicating factories".
     *
     * @param shardIndex      The index of this "factory team member" (i.e. the "5" in "5 of 12")
     * @param totalShardCount The total number of TimeIdFactoryShard objects that are making keys
     */
    public IdFactoryShard(int shardIndex, int totalShardCount) {
        this(shardIndex, totalShardCount, inMemoryCounter());
    }

    /**
     * Convenience method for creating a "Factory Team" when every member of an Enum should have
     * exactly one TimeIdFactoryShard assigned to it
     *
     * @param memberOfEnum A member of the Enum class
     * @param <T>          An Enum class
     * @return A new TimeIdFactoryShard configured to "match" the ordinal and enum size. In other
     * words, you can create a "complete Factory Shard team" by calling this factory method once for
     * each member of the enum
     */
    public static <T extends Enum<T>> IdFactoryShard newFactoryShardFor(T memberOfEnum) {
        int enumElementIndex = memberOfEnum.ordinal();
        int sizeOfEnumSpace = memberOfEnum.getClass().getEnumConstants().length;

        return new IdFactoryShard(enumElementIndex, sizeOfEnumSpace);
    }

    /**
     * @return The maximum number of individual TimeIds that can reference the same "epoch
     * millisecond" value AND maintain the promise that every TimeId will have unique id bits.
     */
    public int limitTimeIdsPerEpochMills() {
        return limitTimeIdsPerEpochMills;
    }

    public int numBitsToStoreShardIndex() {
        return numBitsForShardNumber;
    }

    public int numBitsForItemDistinction() {
        return numBitsForItemDistinction;
    }

    public int shardIndex() {
        return shardIndex;
    }

    /** @return The number of Factories this Factory has been configured to interoperate with. */
    public int numShardsInTeam() {
        return numShardsInTeam;
    }

    @Override
    public SmallTimeId generateIdFor(Instant time) {
        return generate(time, timeCounts.nextCountFor(time));
    }

    private SmallTimeId generate(Instant time, int count) {

        if (count >= limitTimeIdsPerEpochMills) {
            throw new NoSuchElementException(
                "Too many requests (" + count + ") for a TimeId at: " + time.toString() + " have "
                    + "been made. Only" + numBitsForItemDistinction + " bits are allocated to "
                    + " store the counter therefore a limit of " + limitTimeIdsPerEpochMills
                    + " is imposed"
            );
        } else {
            long shardBits = ((long) shardIndex) << this.numBitsForItemDistinction;
            long itemBits = (long) count;
            long comboBits = shardBits | itemBits;
            return TimeIds.directBitsetTimeId(time, comboBits);
        }
    }

    /**
     * @param numShards The number of shards that will jointly enumerate the TimeId bitspace.
     * @return The number of bits required to encode the maximum "shardIndex"
     */
    public static int numBitsRequiredFor(int numShards) {
        checkArgument(numShards > 0);

        return (int) Math.ceil(log2(numShards));
    }

    /**
     * The "REAL GOAL" of CountKeeper is to provide a seam where important but subtle implementation
     * details can be injected by the user. Specifically, the CountKeeper interface allows the user
     * to inject code that controls behavior like "How much memory is allocated to storing the
     * Instant Counts?" and "What happens when a nextCountRequest occurs for a VERY old Instant?".
     */
    public interface CountKeeper {
        int nextCountFor(Instant timestamp);
    }

    /**
     * This CountKeeper NEVER forgets how any Instant it has seen and been asked to count. This
     * means it will eventually cause an OutOfMemoryException if its continually used (within a
     * Factory) to generate an unbounded number of TimeIds.
     */
    public static CountKeeper inMemoryCounter() {
        return new InMemoryCountKeeper();
    }

    /**
     * This CountKeeper retains a limited number of "Instant counters". This requires introducing a
     * new mechanism to enforce the "TimeId bitsets are always unique" guarantee.
     *
     * <p>Consequently, when this CountKeeper "runs out of available Instant counters" it will
     * start evicting the counter for the oldest Instant it has been tracking. From that point on,
     * no count can be provided for an Instant as old as (or older than) the most recently "evicted
     * Instant".
     *
     * <p>The "counter eviction" policy requires this CountKeeper to receive Instant in roughly
     * chronological order from oldest to newest. The resiliency to "out-of-order" timestamps is
     * governed by the number of times tracked (aka the size of the internal TreeMap).
     */
    public static CountKeeper limitedMemoryCounter(int numTimesTracked) {
        return new CappedInMemoryCountKeeper(numTimesTracked);
    }

    /**
     * An InMemoryCountKeeper NEVER forgets how many Instant it has seen. This means an
     * InMemoryCountKeeper will eventually cause an OutOfMemoryException if the ShardedTimeIdFactory
     * it gets embedded in continues being used.
     */
    private static class InMemoryCountKeeper implements CountKeeper {

        private final ConcurrentHashMap<Long, Integer> timeCounts;

        InMemoryCountKeeper() {
            this.timeCounts = new ConcurrentHashMap<>();
        }

        @Override
        public int nextCountFor(Instant timestamp) {
            return timeCounts.merge(
                timestamp.toEpochMilli(), 1,  //when not set:  seed with these values
                Integer::sum    //otherwise:     update existing count with this func
            ) - 1;
        }
    }

    /**
     * A CappedInMemoryCountKeeper retains a limited number of "Instant counters". This means we
     * need to introduce some mechanism to ensure the "TimeId bitsets are always unique" guarantee a
     * still enforceable.
     *
     * <p>Thus, when a CappedInMemoryCountKeeper "runs out of Instant counters" it will start
     * evicting the counter for the oldest Instant it has in memory. From that point on, that
     * "evicted Instant" represents the oldest possible Instant for which a count can be provided.
     */
    private static class CappedInMemoryCountKeeper implements CountKeeper {

        private final TreeMap<Long, Integer> timeCounts;

        private long lastEvictedTime;

        private final int maxSize;

        CappedInMemoryCountKeeper(int maxSize) {
            checkArgument(maxSize >= 1);
            this.maxSize = maxSize;
            this.timeCounts = new TreeMap<>();
            this.lastEvictedTime = Long.MIN_VALUE;
        }

        @Override
        public int nextCountFor(Instant timestamp) {

            if (timestamp.toEpochMilli() <= lastEvictedTime) {
                throw new IllegalStateException(
                    "Cannot generate count for: " + timestamp + ", it occurs too far in the past"
                );
            }

            int count = timeCounts.merge(timestamp.toEpochMilli(), 1, Integer::sum);

            enforceSizeLimit();

            return count - 1;
        }

        private void enforceSizeLimit() {
            if (timeCounts.size() > maxSize) {
                lastEvictedTime = timeCounts.pollFirstEntry().getKey();
            }
        }
    }
}
