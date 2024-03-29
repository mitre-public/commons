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

import java.time.Instant;

import org.mitre.caasd.commons.ids.IdFactoryShard.CountKeeper;

@SuppressWarnings("UnstableApiUsage")
public class TimeIds {

    /**
     * An IdFactory creates a unique ID that does not reflect any information about the item the ID
     * will be used to identify.
     */
    @FunctionalInterface
    public interface IdFactory<T> {
        T generateIdFor(Instant time);
    }

    /**
     * Create an IdFactory configured to use all the ID bit space. This method is suitable for
     * non-distributed applications where we KNOW the SmallTimeId's generated by this IdFactory will
     * not be co-mingled with SmallTimeId's generated by a different IdFactory.
     *
     * @param counter The strategy object that saves the "Instant counts"
     * @return An IdFactory that ensures every ID it produces falls within its (comprehensive)
     * "partition" of the ID bitspace
     */
    public IdFactory<SmallTimeId> soloFactory(CountKeeper counter) {
        return factoryTeamMember(0, 1, counter);
    }

    /**
     * Create an IdFactory configured to use one N-th of the ID bit space. This method is suitable
     * for distributed or multi-threaded applications where we KNOW the SmallTimeId's generated by
     * this IdFactory WILL be co-mingled with SmallTimeId's generated by other IdFactories.
     *
     * @param shardIndex     The index of this IdFactory returned by this call (i.e. i)
     * @param totalNumShards The number of IdFactories that must coordinate (i.e. N)
     * @param counter        The strategy object that saves the "Instant counts"
     * @return An IdFactory that ensures every ID it produces falls within its "partition" of the ID
     * bitspace
     */
    public IdFactory<SmallTimeId> factoryTeamMember(int shardIndex, int totalNumShards, CountKeeper counter) {
        return new IdFactoryShard(shardIndex, totalNumShards, counter);
    }

    /**
     * Convenience method for creating one IdFactory in a "Team of IdFactories" for times when
     * exactly one IdFactoryShard is assigned to every member of an Enum.
     *
     * @param memberOfEnum A member of the Enum class
     * @param counter      The strategy object that saves the "Instant counts"
     * @param <T>          An Enum class
     * @return A new IdFactoryShard configured to "match" the ordinal and enum size. In other words,
     * you can create a "complete Factory Shard team" by calling this factory method once for each
     * member of the enum
     */
    public <T extends Enum<T>> IdFactory<SmallTimeId> factoryEnumTeamMember(T memberOfEnum, CountKeeper counter) {

        int enumElementIndex = memberOfEnum.ordinal();
        int sizeOfEnumSpace = memberOfEnum.getClass().getEnumConstants().length;

        return new IdFactoryShard(enumElementIndex, sizeOfEnumSpace, counter);
    }

    /**
     * Create a SmallTimeId by combining a timeStamp with 21-bits dedicated solely to distinguishing
     * two SmallTimeId that reflect the same timeStamp. This function relies on the caller to
     * provide bits that properly utilize the entire available bitspace to prevent ID collisions.
     *
     * @param time The time encoded into the SmallTimeId (up to millisecond accuracy)
     * @param bits The lowest order 21-bits of this long are retained.  Other bits are ignored.
     * @return A new SmallTimeId that builds the "idBits" by concatenated epochMilli bits with these
     * additional provided bits. The full 63 bit TimeId SmallTimeId be {{42 time-based bits}} + {{21
     * non-time bits}}
     */
    public static SmallTimeId directBitsetTimeId(Instant time, long bits) {
        return new SmallTimeId(time, bits);
    }
}
