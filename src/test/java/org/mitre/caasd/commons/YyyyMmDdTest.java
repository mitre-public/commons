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
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.Instant.EPOCH;
import static java.util.Collections.EMPTY_LIST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.YyyyMmDd.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

public class YyyyMmDdTest {

    @Test
    public void yyyyMmDd_fromInstant() {
        YyyyMmDd date = new YyyyMmDd(Instant.EPOCH);

        assertThat(date.date(), is("1970-01-01"));
    }

    @Test
    public void yyyyMmDd_equality() {

        YyyyMmDd date1 = new YyyyMmDd(Instant.EPOCH);
        YyyyMmDd date2 = new YyyyMmDd(Instant.EPOCH.plusSeconds(1000));

        assertThat(date1, is(date2));
        assertThat(date1 == date2, is(false));
    }

    @Test
    public void toString_givesReadableResult() {

        YyyyMmDd date = new YyyyMmDd(Instant.EPOCH);

        assertThat(date.toString(), is("1970-01-01"));
    }

    @Test
    public void oldestOf_worksWhenGivenGoodInput() {

        YyyyMmDd epoch = new YyyyMmDd(EPOCH);
        YyyyMmDd epochPlusDay = new YyyyMmDd(EPOCH.plus(Duration.ofHours(24)));
        YyyyMmDd epochPlus4Days = new YyyyMmDd(EPOCH.plus(Duration.ofHours(96)));

        List<YyyyMmDd> list = newArrayList(epoch, epochPlusDay, epochPlus4Days);

        assertThat(oldestOf(list), is(epoch));
    }

    @Test
    public void oldestOf_returnsNullWhenGivenEmptyCollection() {
        assertThat(oldestOf(EMPTY_LIST), nullValue());
    }

    @Test
    public void newestOf_worksWhenGivenGoodInput() {

        YyyyMmDd epoch = new YyyyMmDd(EPOCH);
        YyyyMmDd epochPlusDay = new YyyyMmDd(EPOCH.plus(Duration.ofHours(24)));
        YyyyMmDd epochPlus4Days = new YyyyMmDd(EPOCH.plus(Duration.ofHours(96)));

        List<YyyyMmDd> list = newArrayList(epoch, epochPlusDay, epochPlus4Days);

        assertThat(newestOf(list), is(epochPlus4Days));
    }

    @Test
    public void newestOf_returnsNullWhenGivenEmptyCollection() {
        assertThat(newestOf(EMPTY_LIST), nullValue());
    }

    @Test
    public void dayOfWeek() {
        YyyyMmDd sunday = new YyyyMmDd("2021-07-04");
        YyyyMmDd monday = new YyyyMmDd("2021-07-05");

        assertThat(sunday.dayOfWeek(), is(SUNDAY));
        assertThat(monday.dayOfWeek(), is(MONDAY));
    }

    @Test
    public void tomorrowAndYesterday() {
        YyyyMmDd randomDay = new YyyyMmDd("2019-06-22");
        assertThat(randomDay.tomorrow(), is(new YyyyMmDd("2019-06-23")));
        assertThat(randomDay.yesterday(), is(new YyyyMmDd("2019-06-21")));
        assertThat(randomDay.tomorrow().yesterday(), is(randomDay));
    }

    @Test
    public void asInstant() {
        YyyyMmDd date = new YyyyMmDd("2021-07-04");
        Instant time = date.asInstant();

        // converting back to a YyyyMmDd produces the same date
        assertThat(YyyyMmDd.from(time), is(date));

        // subtracting a tiny amount of time rolls back to the prior days
        assertThat(YyyyMmDd.from(time.minusMillis(1)), is(date.yesterday()));
    }

    @Test
    public void verifyYearMonthDayFormat_requireDashes() {
        assertThrows(IllegalArgumentException.class, () -> verifyYearMonthDayFormat("19200131"));
    }

    @Test
    public void verifyYearMonthDayFormat_passing() {
        verifyYearMonthDayFormat("1920-01-31");
    }

    @Test
    public void verifyYearMonthDayFormat_badDay_low() {
        assertThrows(IllegalArgumentException.class, () -> verifyYearMonthDayFormat("2019-12-00"));
    }

    @Test
    public void verifyYearMonthDayFormat_badDay_high() {
        assertThrows(IllegalArgumentException.class, () -> verifyYearMonthDayFormat("2019-12-32"));
    }

    @Test
    public void verifyYearMonthDayFormat_badMonth_low() {
        assertThrows(IllegalArgumentException.class, () -> verifyYearMonthDayFormat("2019-00-02"));
    }

    @Test
    public void verifyYearMonthDayFormat_badMonth_high() {
        assertThrows(IllegalArgumentException.class, () -> verifyYearMonthDayFormat("2019-13-02"));
    }

    @Test
    void plusTimeAmount() {
        YyyyMmDd date = new YyyyMmDd("2021-07-04");

        assertThat(date.plus(Duration.ofSeconds(2)), is(date));
        assertThat(date.plus(Duration.ofSeconds(24 * 60 * 60 - 1)), is(date));
        assertThat(date.plus(Duration.ofHours(24)), is(date.tomorrow()));
    }

    @Test
    void minusTimeAmount() {
        YyyyMmDd date = new YyyyMmDd("2021-07-04");

        assertThat(date.minus(Duration.ofSeconds(2)), is(date.yesterday()));
        assertThat(date.minus(Duration.ofSeconds(24 * 60 * 60 - 1)), is(date.yesterday()));
        assertThat(date.minus(Duration.ofHours(24)), is(date.yesterday()));
    }
}
