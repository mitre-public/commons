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

package org.mitre.caasd.commons;

import static java.time.Instant.EPOCH;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class IntervalTest {

    @Test
    public void testIntervalTruncation() {
        Interval x = new Interval(LocalDate.of(2016, 01, 01), LocalDate.of(2016, 02, 01));

        Interval y = x.extend(new Interval(LocalDate.of(2016, 02, 01), LocalDate.of(2016, 03, 01)));
        assertTrue(y.equals(new Interval(LocalDate.of(2016, 01, 01), LocalDate.of(2016, 03, 01))));

        Interval z = y.truncateTo(new Interval(LocalDate.of(2016, 02, 15), LocalDate.MAX));
        assertTrue(z.equals(new Interval(LocalDate.of(2016, 02, 15), LocalDate.of(2016, 03, 01))));
    }

    @Test
    public void testMaxIntervals() {
        Interval fromLocal = new Interval(LocalDate.MIN, LocalDate.MAX);
        Interval fromInstant = new Interval(Instant.MIN, Instant.MAX);

        //long outputs are the same
        assertTrue(fromLocal.startEpoch().equals(fromInstant.startEpoch()));
        assertTrue(fromLocal.endEpoch().equals(fromInstant.endEpoch()));

        //Instant outputs are the same
        assertTrue(fromLocal.start().equals(fromInstant.start()));
        assertTrue(fromLocal.end().equals(fromInstant.end()));

        //values are correct
        assertTrue(fromInstant.start().equals(Instant.MIN));
        assertTrue(fromInstant.end().equals(Instant.MAX));
    }

    @Test
    public void testIntervalOverlapContains() {
        Interval i1 = new Interval(EPOCH, EPOCH.plusMillis(1000L));
        Interval i2 = new Interval(EPOCH, EPOCH.plusMillis(1000L));

        assertTrue(i1.contains(i2));
        assertTrue(i1.overlaps(i2));
        assertFalse(i1.contains(10000L));
        assertTrue(i1.contains(0L));

        Interval i3 = new Interval(EPOCH.plusMillis(10001L), EPOCH.plusMillis(20000L));
        Interval i4 = new Interval(EPOCH.plusMillis(5000L), EPOCH.plusMillis(15000L));

        assertTrue(i3.overlaps(i4));
        assertFalse(i3.contains(i4));

        Interval i5 = new Interval(EPOCH.plusMillis(10000L), EPOCH.plusMillis(20000L));
        Interval i6 = new Interval(EPOCH.plusMillis(20000L), EPOCH.plusMillis(25000L));

        assertFalse(i5.overlaps(i6));
    }

    @Test
    public void testIntervalDates() {
        Interval itv = new Interval(EPOCH.plus(2L, DAYS), EPOCH.plus(10L, DAYS));
        Set<Long> dates = LongStream.range(2L, 10L)
            .map(l -> l * Interval.DAY)
            .mapToObj(l -> l)
            .collect(Collectors.toSet());

        assertTrue(itv.listDates().size() == 8);
        assertTrue(Sets.difference(dates, Interval.datesBetween(itv.start(), itv.end()).map(Instant::toEpochMilli).collect(Collectors.toSet())).isEmpty());
    }

    @Test
    public void testIntervalTimesBetween() {
        Interval itv = new Interval(EPOCH.plus(1, DAYS), EPOCH.plus(10, DAYS));

        Set<Instant> times = LongStream.range(0, 18)
            .mapToObj(l -> EPOCH.plus(Duration.ofHours(12 * l + 24)))
            .collect(Collectors.toSet());

        Set<Instant> itvTimes = Interval.timesBetween(itv.start(), itv.end(), Duration.ofHours(12))
            .collect(Collectors.toSet());

        assertTrue(Sets.intersection(times, itvTimes).size() == times.size());
    }

    @Test
    public void testComplementOf() {
        Interval base = new Interval(EPOCH.plusMillis(0L), EPOCH.plusMillis(10L));

        List<Interval> itvs = Arrays.asList(
            new Interval(EPOCH.plusMillis(0L), EPOCH.plusMillis(2L)),
            new Interval(EPOCH.plusMillis(2L), EPOCH.plusMillis(3L)),
            new Interval(EPOCH.plusMillis(6L), EPOCH.plusMillis(8L))
        );

        Set<Interval> cmpTrue = new HashSet<>(Arrays.asList(
            new Interval(EPOCH.plusMillis(3L), EPOCH.plusMillis(6L)),
            new Interval(EPOCH.plusMillis(8L), EPOCH.plusMillis(10L))
        ));

        Collection<Interval> complement = Interval.complementOf(base, itvs);

        assertTrue(complement.size() == 2);
        assertTrue(Sets.intersection(cmpTrue, new HashSet<>(complement)).size() == 2);
    }

    @Test
    public void testMerge() {
        Interval i1 = new Interval(EPOCH.plusMillis(0L), EPOCH.plusMillis(3L));
        Interval i2 = new Interval(EPOCH.plusMillis(3L), EPOCH.plusMillis(6L));
        Interval i3 = new Interval(EPOCH.plusMillis(7L), EPOCH.plusMillis(10L));

        NavigableSet<Interval> itvs = Interval.merge(Arrays.asList(i1, i2, i3));

        assertThat(itvs.size(), is(2));
        assertThat(itvs.first().endEpoch(), is(6L));

        Interval i4 = new Interval(EPOCH.plusMillis(8L), EPOCH.plusMillis(12L));
        itvs = Interval.merge(Arrays.asList(i1, i3, i4));
        assertThat(itvs.size(), is(2));
        assertThat(itvs.last().startEpoch(), is(7L));
        assertThat(itvs.last().endEpoch(), is(12L));
    }

    @Test
    public void testEmptyIntervals() {
        Interval emptyInterval = Interval.empty();
        assertTrue(emptyInterval.isEmpty());
        assertThat(
            "Extension of two empty intervals should still be empty",
            emptyInterval.extend(new Interval(EPOCH.plusMillis(10L), EPOCH.plusMillis(10L))).isEmpty()
        );
        assertTrue(emptyInterval.extend(null, null).isEmpty());

        Interval interval = new Interval(EPOCH.plusMillis(10L), EPOCH.plusMillis(11L));
        assertThat(
            "Extending an interval to cover an empty interval should be a no op",
            interval.extend(emptyInterval),
            is(interval)
        );
        assertThat(interval, is(emptyInterval.extend(interval)));
    }
}
