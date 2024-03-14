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
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.ids.IdFactoryShard.*;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class IdFactoryShardTest {

    @Test
    public void testNumBitsRequiredFor() {

        assertThrows(
            IllegalArgumentException.class,
            () -> numBitsRequiredFor(0)
        );

        assertAll(
            () -> assertThat(numBitsRequiredFor(1), is(0)),
            () -> assertThat(numBitsRequiredFor(2), is(1)),
            () -> assertThat(numBitsRequiredFor(3), is(2)),
            () -> assertThat(numBitsRequiredFor(4), is(2)),
            () -> assertThat(numBitsRequiredFor(8), is(3)),
            () -> assertThat(numBitsRequiredFor(16), is(4)),
            () -> assertThat(numBitsRequiredFor(32), is(5))
        );
    }

    @Test
    public void testBitCountsAndItemLimit_1shard() {
        IdFactoryShard factory = new IdFactoryShard(0, 1);

        assertThat(factory.shardIndex(), is(0));
        assertThat(factory.numBitsToStoreShardIndex(), is(0));
        assertThat(factory.numBitsForItemDistinction(), is(21));
        assertThat(factory.limitTimeIdsPerEpochMills(), is(2_097_152));
    }

    @Test
    public void testBitCountsAndItemLimit_2shards() {
        IdFactoryShard factory = new IdFactoryShard(0, 2);

        assertThat(factory.shardIndex(), is(0));
        assertThat(factory.numBitsToStoreShardIndex(), is(1));
        assertThat(factory.numBitsForItemDistinction(), is(20));
        assertThat(factory.limitTimeIdsPerEpochMills(), is(1_048_576));
    }

    @Test
    public void testBitCountsAndItemLimit_3shards() {
        IdFactoryShard factory = new IdFactoryShard(2, 3);

        assertThat(factory.shardIndex(), is(2));
        assertThat(factory.numBitsToStoreShardIndex(), is(2));
        assertThat(factory.numBitsForItemDistinction(), is(19));
        assertThat(factory.limitTimeIdsPerEpochMills(), is(524_288));
    }

    @Test
    public void testBitCountsAndItemLimit_4shards() {
        IdFactoryShard factory = new IdFactoryShard(0, 4);

        assertThat(factory.shardIndex(), is(0));
        assertThat(factory.numBitsToStoreShardIndex(), is(2));
        assertThat(factory.numBitsForItemDistinction(), is(19));
        assertThat(factory.limitTimeIdsPerEpochMills(), is(524_288));
    }

    @Test
    public void limitOnShardIndexIsEnforced() {
        //if you only have 1 shard the max "shardIndex" is zero
        assertDoesNotThrow(() -> new IdFactoryShard(0, 1));
        assertThrows(
            IllegalArgumentException.class,
            () -> new IdFactoryShard(1, 1)
        );

        //if you only have 5 shard the max "shardIndex" is 4
        assertDoesNotThrow(() -> new IdFactoryShard(4, 5));
        assertThrows(
            IllegalArgumentException.class,
            () -> new IdFactoryShard(5, 5)
        );
    }

    @Test
    public void testLimitPerInstant_1MillionShards_shardIndex0() {
        IdFactoryShard factory = new IdFactoryShard(0, 1_048_576);

        SmallTimeId id0 = factory.generateIdFor(EPOCH);
        SmallTimeId id1 = factory.generateIdFor(EPOCH);

        assertThat(toBinaryString(id0.nonTimeBits()), is("0")); //21 bits of 00000...0
        assertThat(toBinaryString(id1.nonTimeBits()), is("1")); //21 bits of 00000...1
        assertThat(id0.nonTimeBits(), is(0L));
        assertThat(id1.nonTimeBits(), is(1L));

        //trying to get a 3rd TimeId while referencing the same EpochMill will fail because you only had 1 bit to work with
        assertThrows(
            NoSuchElementException.class,
            () -> factory.generateIdFor(EPOCH)
        );
    }

    @Test
    public void testLimitPerInstant_1MillionShards_shardIndex1_048_575() {
        IdFactoryShard factory = new IdFactoryShard(1_048_575, 1_048_576);

        assertThat(factory.numBitsForItemDistinction(), is(1));

        SmallTimeId id0 = factory.generateIdFor(EPOCH); //power of 2 - 2
        SmallTimeId id1 = factory.generateIdFor(EPOCH); //power of 2 - 1

        assertThat(toBinaryString(id0.nonTimeBits()), is("111111111111111111110"));
        assertThat(toBinaryString(id1.nonTimeBits()), is("111111111111111111111"));

        assertThat(id0.nonTimeBits(), is((long)(2_097_152 - 2)));
        assertThat(id1.nonTimeBits(), is((long)(2_097_152 - 1)));

        //trying to get a 3rd TimeId while referencing the same EpochMill will fail because you only had 1 bit to work with
        assertThrows(
            NoSuchElementException.class,
            () -> factory.generateIdFor(EPOCH)
        );
    }

    @Test
    public void testLimitPerInstant_500kShards() {
        //524_288 = 2^19
        IdFactoryShard factory = new IdFactoryShard(0, 524_288);

        assertThat(factory.numBitsForItemDistinction(), is(2));

        SmallTimeId id0 = factory.generateIdFor(EPOCH);
        SmallTimeId id1 = factory.generateIdFor(EPOCH);
        SmallTimeId id2 = factory.generateIdFor(EPOCH);
        SmallTimeId id3 = factory.generateIdFor(EPOCH);

        assertThat(toBinaryString(id0.nonTimeBits()), is("0"));
        assertThat(toBinaryString(id1.nonTimeBits()), is("1"));
        assertThat(toBinaryString(id2.nonTimeBits()), is("10"));
        assertThat(toBinaryString(id3.nonTimeBits()), is("11"));
        assertThat(id0.nonTimeBits(), is(0L));
        assertThat(id1.nonTimeBits(), is(1L));
        assertThat(id2.nonTimeBits(), is(2L));
        assertThat(id3.nonTimeBits(), is(3L));

        //trying to get a 5th TimeId while referencing the same EpochMill will fail because you only had 2 bit to work with
        assertThrows(
            NoSuchElementException.class,
            () -> factory.generateIdFor(EPOCH)
        );
    }


    @Test
    public void multipleFactoriesCombineToCoverTheBitSpace() {
        //Together, these for factories should create 2_097_152 unique TimeIds that map to EPOCH
        IdFactoryShard factory0 = new IdFactoryShard(0, 4);
        IdFactoryShard factory1 = new IdFactoryShard(1, 4);
        IdFactoryShard factory2 = new IdFactoryShard(2, 4);
        IdFactoryShard factory3 = new IdFactoryShard(3, 4);

        TreeSet<SmallTimeId> idsThemselves = new TreeSet<>();
        TreeSet<Long> idBitsets = new TreeSet<>();
        Instant sharedTimeStamp = EPOCH;

        for (int i = 0; i < 524_288; i++) {
            //Every factory makes a TimeId
            SmallTimeId id0 = factory0.generateIdFor(sharedTimeStamp);
            SmallTimeId id1 = factory1.generateIdFor(sharedTimeStamp);
            SmallTimeId id2 = factory2.generateIdFor(sharedTimeStamp);
            SmallTimeId id3 = factory3.generateIdFor(sharedTimeStamp);

            //Save the TimeId
            idsThemselves.add(id0);
            idsThemselves.add(id1);
            idsThemselves.add(id2);
            idsThemselves.add(id3);

            //Save the full bitsets
            idBitsets.add(id0.id());
            idBitsets.add(id1.id());
            idBitsets.add(id2.id());
            idBitsets.add(id3.id());

        }

        //All TimeIds and Bitset Longs were unique! ....
        assertThat(idsThemselves.size(), is(2_097_152));
        assertThat(idBitsets.size(), is(2_097_152));

        //all 4 factories will fail if they ask for one more TimeId using the "sharedTimeStamp" that was just fully allocated
        assertThrows(NoSuchElementException.class, () -> factory0.generateIdFor(sharedTimeStamp));
        assertThrows(NoSuchElementException.class, () -> factory1.generateIdFor(sharedTimeStamp));
        assertThrows(NoSuchElementException.class, () -> factory2.generateIdFor(sharedTimeStamp));
        assertThrows(NoSuchElementException.class, () -> factory3.generateIdFor(sharedTimeStamp));

        //BUT all 4 factories have no problem getting a TimeId that hasn't been allocated
        assertDoesNotThrow(() -> factory0.generateIdFor(sharedTimeStamp.plusMillis(1)));
        assertDoesNotThrow(() -> factory1.generateIdFor(sharedTimeStamp.plusMillis(1)));
        assertDoesNotThrow(() -> factory2.generateIdFor(sharedTimeStamp.plusMillis(1)));
        assertDoesNotThrow(() -> factory3.generateIdFor(sharedTimeStamp.plusMillis(1)));
    }

    @Test
    public void inMemoryCounterWorks() {

        CountKeeper counter = inMemoryCounter();

        assertThat(counter.nextCountFor(EPOCH), is(0));
        assertThat(counter.nextCountFor(EPOCH), is(1));

        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(0));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(1));
    }

    @Test
    public void limitedMemoryCounterWorks_happyPath() {

        CountKeeper counter = limitedMemoryCounter(4);

        assertThat(counter.nextCountFor(EPOCH), is(0));
        assertThat(counter.nextCountFor(EPOCH), is(1));

        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(0));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(1));
    }

    @Test
    public void limitedMemoryCounterWorks_evictionPath() {

        CountKeeper counter = limitedMemoryCounter(3);

        assertThat(counter.nextCountFor(EPOCH.plusMillis(0)), is(0));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(0));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(2)), is(0));

        assertThat(counter.nextCountFor(EPOCH.plusMillis(0)), is(1));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(1));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(2)), is(1));

        assertThat(counter.nextCountFor(EPOCH.plusMillis(0)), is(2));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(1)), is(2));
        assertThat(counter.nextCountFor(EPOCH.plusMillis(2)), is(2));

        //trigger the eviction of the oldest "time counter" by requesting a counter for a 4th unique timestamp..
        assertThat(counter.nextCountFor(EPOCH.plusSeconds(1)), is(0));

        assertThrows(IllegalStateException.class, () -> counter.nextCountFor(EPOCH));
    }

    enum SimpleEnum {
        CASE_A, CASE_B, CASE_C, CASE_D
    }

    @Test
    public void enumStaticFactoryWorksWithEnum() {

        IdFactoryShard factory_A = IdFactoryShard.newFactoryShardFor(SimpleEnum.CASE_A);
        IdFactoryShard factory_B = IdFactoryShard.newFactoryShardFor(SimpleEnum.CASE_B);
        IdFactoryShard factory_C = IdFactoryShard.newFactoryShardFor(SimpleEnum.CASE_C);
        IdFactoryShard factory_D = IdFactoryShard.newFactoryShardFor(SimpleEnum.CASE_D);

        assertThat(factory_A.shardIndex(), is(0));
        assertThat(factory_A.numShardsInTeam(), is(4));
        assertThat(factory_A.numBitsToStoreShardIndex(), is(2));

        assertThat(factory_B.shardIndex(), is(1));
        assertThat(factory_B.numShardsInTeam(), is(4));
        assertThat(factory_B.numBitsToStoreShardIndex(), is(2));

        assertThat(factory_C.shardIndex(), is(2));
        assertThat(factory_C.numShardsInTeam(), is(4));
        assertThat(factory_C.numBitsToStoreShardIndex(), is(2));

        assertThat(factory_D.shardIndex(), is(3));
        assertThat(factory_D.numShardsInTeam(), is(4));
        assertThat(factory_D.numBitsToStoreShardIndex(), is(2));
    }

}