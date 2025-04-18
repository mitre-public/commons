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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * A YyyyMmDd is a simple wrapper for a Date string like "2020-03-22" which happens to match the
 * DateTimeFormatter.ISO_LOCAL_DATE.
 * <p>
 * Even though this class "enforces a format" its goal is to provide a convenient way to "bucket"
 * data that occurs on the same date or day of the week. This need comes up when dealing with
 * time-stamped data that must be summarized by date. For example, "Show me the results from July
 * 4th 2020." and "Show me the average across all Fridays." are questions that can benefit from
 * grouping input data using a common YyyyMmDd.
 * <p>
 * Note: This class IS NOT intended to be used as a timestamp because it does not maintain hours,
 * minute, second, or millisecond data.
 */
public class YyyyMmDd implements Comparable<YyyyMmDd> {

    /** A String with the format "YYYY-MM-DD" (aka DateTimeFormatter.ISO_LOCAL_DATE). */
    private final String date;

    public YyyyMmDd(Instant time) {
        checkNotNull(time);
        this.date = ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(time);
    }

    public static YyyyMmDd today() {
        return new YyyyMmDd(Instant.now());
    }

    /**
     * Parse a String into a YyyyMmDd.
     *
     * @param date A String like "2020-01-12"
     */
    public YyyyMmDd(String date) {
        checkNotNull(date);
        verifyYearMonthDayFormat(date);
        this.date = date;
    }

    public static YyyyMmDd from(Instant time) {
        return new YyyyMmDd(time);
    }

    public static YyyyMmDd of(String date) {
        return new YyyyMmDd(date);
    }

    public String date() {
        return date;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.date);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final YyyyMmDd other = (YyyyMmDd) obj;
        if (!Objects.equals(this.date, other.date)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(YyyyMmDd o) {
        return this.date.compareTo(o.date);
    }

    @Override
    public String toString() {
        return date;
    }

    /** @return A DayOfWeek {e.g. SUNDAY, MONDAY, TUESDAY, ... SATURDAY}. */
    public DayOfWeek dayOfWeek() {
        return DayOfWeek.from(ISO_LOCAL_DATE.parse(date));
    }

    /** @return The first Instant of the day. */
    public Instant asInstant() {
        return Instant.parse(date + "T00:00:00.000Z");
    }

    public YyyyMmDd yesterday() {
        return YyyyMmDd.from(asInstant().minus(Duration.ofDays(1)));
    }

    public YyyyMmDd tomorrow() {
        return YyyyMmDd.from(asInstant().plus(Duration.ofDays(1)));
    }

    public static YyyyMmDd oldestOf(Collection<YyyyMmDd> dates) {
        checkNotNull(dates);
        return dates.stream().min(Comparator.naturalOrder()).orElse(null);
    }

    public static YyyyMmDd newestOf(Collection<YyyyMmDd> dates) {
        checkNotNull(dates);
        return dates.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    /**
     * @return The YyyyMmDd corresponding to adding this time amount to a Timestamp ending with:
     *     T00:00:00.000Z.  In other words, when amount is less than 24 hours the date will not
     *     roll forward.  E.g. this.plus(Duration.ofSeconds(1)) returns the same YyyyMmDd;
     */
    public YyyyMmDd plus(TemporalAmount amount) {
        return YyyyMmDd.from(asInstant().plus(amount));
    }

    /**
     * @return The YyyyMmDd corresponding to subtracting this time amount from a Timestamp ending
     *     with: T00:00:00.000Z.  In other words, when amount is positive this will always
     *     "roll-back" the date.  E.g. this.minus(Duration.ofSeconds(1)) returns this.yesterday();
     */
    public YyyyMmDd minus(TemporalAmount amount) {
        return YyyyMmDd.from(asInstant().minus(amount));
    }

    private static final String DATE_FORMAT_ERROR = "Date not specified as YYYY-MM-DD format";

    /** Throw an IllegalArgumentException if the input String is not in the YYYY-MM-DD format. */
    public static void verifyYearMonthDayFormat(String date) {
        checkNotNull(date);

        try {
            ISO_LOCAL_DATE.parse(date);
        } catch (DateTimeParseException dtpe) {
            throw new IllegalArgumentException("ISO_LOCAL_DATE cannot parse: " + date);
        }
    }
}
