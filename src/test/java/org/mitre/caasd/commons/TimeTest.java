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

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Time.confirmApproximateTimeOrdering;
import static org.mitre.caasd.commons.Time.confirmStrictTimeOrdering;
import static org.mitre.caasd.commons.Time.confirmTimeOrdering;
import static org.mitre.caasd.commons.Time.latest;
import static org.mitre.caasd.commons.Time.max;
import static org.mitre.caasd.commons.Time.min;
import static org.mitre.caasd.commons.Time.theDuration;
import static org.mitre.caasd.commons.Time.theDurationBtw;
import static org.mitre.caasd.commons.Time.toMap;
import static org.mitre.caasd.commons.Time.verifyYearMonthDayFormat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TimeTest {

    @Test
    public void testGetDecimalDuration() {

        Duration duration = Duration.ofHours(1);

        double TOLERANCE = 0.000001;

        assertEquals(
            1.0,
            Time.getDecimalDuration(duration, ChronoUnit.HOURS),
            TOLERANCE
        );

        assertEquals(
            60.0,
            Time.getDecimalDuration(duration, ChronoUnit.MINUTES),
            TOLERANCE
        );

        assertEquals(
            60.0 * 60.0,
            Time.getDecimalDuration(duration, ChronoUnit.SECONDS),
            TOLERANCE
        );
    }

    @Test
    public void testAverageTime() {

        long TIME_DELTA_IN_MILLISEC = 1256000; //some arbitrary number

        Instant time1 = Instant.EPOCH;
        Instant time2 = time1.plus(Duration.ofMillis(TIME_DELTA_IN_MILLISEC));

        Instant avg12 = Time.averageTime(time1, time2);
        Instant avg21 = Time.averageTime(time2, time1);

        Instant expected = Instant.EPOCH.plus(Duration.ofMillis(TIME_DELTA_IN_MILLISEC / 2));

        assertThat("Input order 1 of 2", avg12, is(expected));
        assertThat("Input order 2 of 2", avg21, is(expected));
    }

    @Test
    public void testAverageTime_nullInputs1() {
        assertThrows(NullPointerException.class,
            () -> Time.averageTime(null, Instant.MIN)
        );
    }

    @Test
    public void testAverageTime_nullInputs2() {
        assertThrows(NullPointerException.class,
            () -> Time.averageTime(Instant.EPOCH, null)
        );
    }

    @Test
    public void testConfirmApproximateTimeOrdering_standardCase() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = currentTime.plusMillis(500);
        Duration maxLag = Duration.of(500, ChronoUnit.MILLIS);

        //does nothing if input is Verified
        assertDoesNotThrow(
            () -> confirmApproximateTimeOrdering(currentTime, futureTime, maxLag)
        );
    }

    @Test
    public void testConfirmApproximateTimeOrdering_farInFutureIsOK() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = currentTime.plus(500, ChronoUnit.DAYS);  //far in future
        Duration maxLag = Duration.of(500, ChronoUnit.MILLIS);  //only allowed to be a bit in the past

        //does nothing if input is Verified
        assertDoesNotThrow(
            () -> Time.confirmApproximateTimeOrdering(currentTime, futureTime, maxLag)
        );
    }

    @Test
    public void testConfirmApproximateTimeOrdering_farInPastFails() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = currentTime.minus(500, ChronoUnit.DAYS);  //far in past
        Duration maxLag = Duration.of(500, ChronoUnit.MILLIS);  //only allowed to be a bit in the past

        //Fails because the futureTime is too far in the past
        assertThrows(IllegalArgumentException.class,
            () -> Time.confirmApproximateTimeOrdering(currentTime, futureTime, maxLag)
        );
    }

    @Test
    public void testConfirmApproximateTimeOrdering_aLittleInPastIsOK() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = currentTime.minus(500, ChronoUnit.NANOS);  //a little bit in past
        Duration maxLag = Duration.of(500, ChronoUnit.MILLIS);  //only allowed to be a bit in the past

        //does nothing if input is Verified
        assertDoesNotThrow(
            () -> Time.confirmApproximateTimeOrdering(currentTime, futureTime, maxLag)
        );
    }

    @Test
    public void tetsConfirmApproximateTimeOrdering_negativeDurationsArentAllowed() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = Instant.EPOCH;
        Duration maxLag = Duration.of(500, ChronoUnit.MILLIS).negated();  //A NEGATIVE DURATION

        //Fails because maxLag is negative
        assertThrows(IllegalArgumentException.class,
            () -> Time.confirmApproximateTimeOrdering(currentTime, futureTime, maxLag)
        );
    }

    @Test
    public void testConfirmStrictTimeOrdering_sameTime() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = Instant.EPOCH;

        //does nothing if input is Verified
        assertDoesNotThrow(
            () -> confirmStrictTimeOrdering(currentTime, futureTime)
        );
    }

    @Test
    public void testConfirmStrictTimeOrdering_passingCase() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = Instant.EPOCH.plusNanos(1L);

        //does nothing if input is Verified
        assertDoesNotThrow(
            () -> confirmStrictTimeOrdering(currentTime, futureTime)
        );
    }

    @Test
    public void testConfirmStrictTimeOrdering_failingCase() {

        Instant currentTime = Instant.EPOCH;
        Instant futureTime = Instant.EPOCH.minusNanos(1L);

        //Fails because the futureTime is actually in the past
        assertThrows(IllegalArgumentException.class,
            () -> Time.confirmStrictTimeOrdering(currentTime, futureTime)
        );
    }

    @Test
    public void confirmTimeOrdering_rejectsOutOfOrder() {
        Instant[] times = new Instant[]{
            Instant.EPOCH,
            Instant.EPOCH.minusMillis(1)
        };

        assertThrows(IllegalArgumentException.class,
            () -> confirmTimeOrdering(times)
        );
    }

    @Test
    public void confirmTimeOrdering_acceptsInOrder() {
        Instant[] times = new Instant[]{
            Instant.EPOCH,
            Instant.EPOCH.plusMillis(100)
        };
        assertDoesNotThrow(
            () -> confirmTimeOrdering(times)
        );
    }

    @Test
    public void confirmTimeOrdering_acceptsDuplicateTimes_whenOrderedCorrectly() {
        Instant[] times = new Instant[]{
            Instant.EPOCH.minusMillis(100),
            Instant.EPOCH,
            Instant.EPOCH,
            Instant.EPOCH.plusMillis(100)
        };
        assertDoesNotThrow(
            () -> confirmTimeOrdering(times)
        );

    }

    @Test
    public void testDurationFormatting_1() {

        //13 hours, 22 minutes, and 15 seconds
        long numSeconds = 13 * 3600 + 22 * 60 + 15;

        Duration dur = Duration.ofSeconds(numSeconds);

        assertEquals(
            "13:22:15",
            Time.asString(dur)
        );
    }

    @Test
    public void testDurationFormatting_2() {

        //2 days, 13 hours, 22 minutes, and 15 seconds
        int SECONDS_PER_DAY = 24 * 60 * 60;
        long numSeconds = 2 * SECONDS_PER_DAY + 13 * 3600 + 22 * 60 + 15;

        Duration dur = Duration.ofSeconds(numSeconds);

        assertEquals(
            "2 days, 13:22:15",
            Time.asString(dur)
        );
    }

    @Test
    public void testUtcDateAsString() {

        assertEquals(
            "1970-01-01",
            Time.utcDateAsString(Instant.EPOCH)
        );

        assertEquals(
            "1970-01-03",
            Time.utcDateAsString(Instant.EPOCH.plus(2, ChronoUnit.DAYS))
        );
    }

    @Test
    public void testEarliest() {

        Instant time0 = Instant.EPOCH;
        Instant time1 = Instant.EPOCH.plusSeconds(1);

        assertEquals(
            time0,
            Time.earliest(time0, time1)
        );

        assertEquals(
            time0,
            Time.earliest(time1, time0)
        );
    }

    @Test
    public void testEarliestVargArgs() {

        Instant time0 = Instant.EPOCH;
        Instant time1 = Instant.EPOCH.plusSeconds(1);
        Instant time2 = Instant.EPOCH.plusSeconds(2);
        Instant time3 = Instant.EPOCH.plusSeconds(3);
        Instant time4 = Instant.EPOCH.plusSeconds(4);

        assertEquals(
            time0,
            Time.earliest(time0, time1, time2)
        );
        assertEquals(
            time0,
            Time.earliest(time1, time0, time4, time3)
        );
        assertEquals(
            time0,
            Time.earliest(time1, time1, time1, time0)
        );
        assertEquals(
            time0,
            Time.earliest(time0)
        );
        assertEquals(
            time0,
            Time.earliest(new Instant[]{time1, time3, time0, time4})
        );
    }

    @Test
    public void testLatest() {

        Instant time0 = Instant.EPOCH;
        Instant time1 = Instant.EPOCH.plusSeconds(1);

        assertEquals(
            time1,
            Time.latest(time0, time1)
        );

        assertEquals(
            time1,
            Time.latest(time1, time0)
        );
    }

    @Test
    public void testLatestVargArgs() {

        Instant time0 = Instant.EPOCH;
        Instant time1 = Instant.EPOCH.minusSeconds(1);
        Instant time2 = Instant.EPOCH.minusSeconds(2);
        Instant time3 = Instant.EPOCH.minusSeconds(3);
        Instant time4 = Instant.EPOCH.minusSeconds(4);

        assertEquals(
            time0,
            Time.latest(time0, time1, time2)
        );
        assertEquals(
            time0,
            Time.latest(time1, time0, time4, time3)
        );
        assertEquals(
            time0,
            Time.latest(time1, time1, time1, time0)
        );
        assertEquals(
            time0,
            Time.latest(time0)
        );
        assertEquals(
            time0,
            Time.latest(new Instant[]{time1, time3, time0, time4})
        );
    }

    @Test
    public void testDurationBtw() {

        Instant time0 = Instant.EPOCH;
        Instant time1 = Instant.EPOCH.minusSeconds(1);
        Instant time2 = Instant.EPOCH.minusSeconds(2);
        Instant time3 = Instant.EPOCH.minusSeconds(3);
        Instant time4 = Instant.EPOCH.minusSeconds(4);

        assertEquals(
            Duration.ofSeconds(2),
            Time.durationBtw(time0, time1, time2)
        );
        assertEquals(
            Duration.ofSeconds(4),
            Time.durationBtw(time1, time0, time4, time3)
        );
        assertEquals(
            Duration.ofSeconds(1),
            Time.durationBtw(time1, time1, time1, time0)
        );
        assertEquals(
            Duration.ofSeconds(0),
            Time.durationBtw(time0)
        );
        assertEquals(
            Duration.ofSeconds(4),
            Time.durationBtw(new Instant[]{time1, time3, time0, time4})
        );
    }

    @Test
    public void testAsZTimeString() {

        Instant time = Instant.EPOCH
            .plus(Duration.ofHours(2))
            .plusSeconds(60 * 33)
            .plusSeconds(2)
            .plusMillis(431);

        assertEquals(
            "02:33:02.431",
            Time.asZTimeString(time)
        );
    }

    @Test
    public void testDurationOf_isLessThan() {
        Duration fiveSec = Duration.ofSeconds(5);
        Duration threeSec = Duration.ofSeconds(3);
        assertFalse(theDuration(fiveSec).isLessThan(threeSec));
        assertTrue(theDuration(threeSec).isLessThan(fiveSec));
        assertFalse(theDuration(fiveSec).isLessThan(fiveSec));
    }

    @Test
    public void testDurationOf_isLessThanOrEqualTo() {
        Duration fiveSec = Duration.ofSeconds(5);
        Duration threeSec = Duration.ofSeconds(3);
        assertFalse(theDuration(fiveSec).isLessThanOrEqualTo(threeSec));
        assertTrue(theDuration(threeSec).isLessThanOrEqualTo(fiveSec));
        assertTrue(theDuration(fiveSec).isLessThanOrEqualTo(fiveSec));
    }

    @Test
    public void testDurationOf_isGreaterThan() {
        Duration fiveSec = Duration.ofSeconds(5);
        Duration threeSec = Duration.ofSeconds(3);
        assertTrue(theDuration(fiveSec).isGreaterThan(threeSec));
        assertFalse(theDuration(threeSec).isGreaterThan(fiveSec));
        assertFalse(theDuration(fiveSec).isGreaterThan(fiveSec));
    }

    @Test
    public void testDurationOf_isGreaterThanOrEqualTo() {
        Duration fiveSec = Duration.ofSeconds(5);
        Duration threeSec = Duration.ofSeconds(3);
        assertTrue(theDuration(fiveSec).isGreaterThanOrEqualTo(threeSec));
        assertFalse(theDuration(threeSec).isGreaterThanOrEqualTo(fiveSec));
        assertTrue(theDuration(fiveSec).isGreaterThanOrEqualTo(fiveSec));
    }

    @Test
    public void testMax_twoDurations() {
        Duration longDur = Duration.ofDays(12);
        Duration smallDur = Duration.ofMinutes(12);

        assertTrue(max(longDur, smallDur) == longDur);
    }

    @Test
    public void testMin_twoDurations() {
        Duration longDur = Duration.ofDays(12);
        Duration smallDur = Duration.ofMinutes(12);

        assertTrue(min(longDur, smallDur) == smallDur);
    }

    @Test
    public void theDurationBetween_varags() {

        Instant time0 = Instant.EPOCH;
        Instant time2 = Instant.EPOCH.minusSeconds(2);
        Instant time4 = Instant.EPOCH.minusSeconds(4);

        assertThat(theDurationBtw(time0, time4).isGreaterThanOrEqualTo(Duration.ofSeconds(4)), is(true));
        assertThat(theDurationBtw(time0, time4).isLessThanOrEqualTo(Duration.ofSeconds(4)), is(true));
        assertThat(theDurationBtw(time4, time0).isGreaterThanOrEqualTo(Duration.ofSeconds(4)), is(true));
        assertThat(theDurationBtw(time4, time0).isLessThanOrEqualTo(Duration.ofSeconds(4)), is(true));

        //adding a middle time does nothing
        assertThat(theDurationBtw(time0, time2, time4).isGreaterThanOrEqualTo(Duration.ofSeconds(4)), is(true));
        assertThat(theDurationBtw(time2, time0, time4).isLessThanOrEqualTo(Duration.ofSeconds(4)), is(true));
        assertThat(theDurationBtw(time4, time0, time2).isGreaterThanOrEqualTo(Duration.ofSeconds(4)), is(true));
        assertThat(theDurationBtw(time4, time2, time0).isLessThanOrEqualTo(Duration.ofSeconds(4)), is(true));
    }

    @Test
    public void verifyYearMonthDayFormat_requireDashes() {
        assertThrows(IllegalArgumentException.class,
            () -> verifyYearMonthDayFormat("19200131")
        );
    }

    @Test
    public void verifyYearMonthDayFormat_passing() {
        verifyYearMonthDayFormat("1920-01-31");
    }

    @Test
    public void verifyYearMonthDayFormat_badDay_low() {
        assertThrows(IllegalArgumentException.class,
            () -> verifyYearMonthDayFormat("2019-12-00")
        );
    }

    @Test
    public void verifyYearMonthDayFormat_badDay_high() {
        assertThrows(IllegalArgumentException.class,
            () -> verifyYearMonthDayFormat("2019-12-32")
        );
    }

    @Test
    public void verifyYearMonthDayFormat_badMonth_low() {
        assertThrows(IllegalArgumentException.class,
            () -> verifyYearMonthDayFormat("2019-00-02")
        );
    }

    @Test
    public void verifyYearMonthDayFormat_badMonth_high() {
        assertThrows(IllegalArgumentException.class,
            () -> verifyYearMonthDayFormat("2019-13-02")
        );
    }

    @Test
    public void latest_hasTime_hasTime() {
        HasTime a = () -> {
            return EPOCH;
        };
        HasTime b = () -> {
            return EPOCH.plusSeconds(10);
        };

        assertThat(latest(a, b), is(b));
        assertThat(latest(b, a), is(b));
    }

    @Test
    public void toMap_collectionOfHasTimes() {
        HasTimeImp a = new HasTimeImp(EPOCH);
        HasTimeImp b = new HasTimeImp(EPOCH.plusMillis(10));
        HasTimeImp c = new HasTimeImp(EPOCH.plusMillis(20));

        Map<Instant, HasTimeImp> map = toMap(newArrayList(a, b, c));

        assertThat(map.size(), is(3));
        assertThat(map.get(EPOCH), is(a));
        assertThat(map.get(EPOCH.plusMillis(10)), is(b));
        assertThat(map.get(EPOCH.plusMillis(20)), is(c));
    }

    static class HasTimeImp implements HasTime {

        Instant time;

        HasTimeImp(Instant time) {
            this.time = time;
        }

        @Override
        public Instant time() {
            return time;
        }
    }
}
