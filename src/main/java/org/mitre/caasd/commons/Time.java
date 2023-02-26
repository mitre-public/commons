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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newTreeMap;
import static java.lang.Integer.parseInt;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Convenience methods for Time related computations.
 */
public class Time {

    private Time() {
        //Time Objects are not allowed
    }

    public static <H extends HasTime> Comparator<H> compareByTime() {
        return (H o1, H o2) -> o1.time().compareTo(o2.time());
    }

    public static double getDecimalDuration(Duration dt, TemporalUnit unit) {
        return ((double) dt.toMillis()) / unit.getDuration().toMillis();
    }

    public static Instant averageTime(Instant time1, Instant time2) {

        checkNotNull(time1, "The first input to Time.averageTime(Instant, Instant) was null");
        checkNotNull(time2, "The second input to Time.averageTime(Instant, Instant) was null");

        long timeLong1 = time1.toEpochMilli();
        long timeLong2 = time2.toEpochMilli();
        long avgLong = (timeLong1 + timeLong2) / 2L;
        return Instant.ofEpochMilli(avgLong);
    }

    /**
     * Confirm an Instant in time is in "approximate time order" (i.e. a candidate time is not "too
     * far" in the past with respect to a reference Instant).
     *
     * @param referenceTime The "correct" or "current" moment in time
     * @param futureTime    The "future" moment in time
     * @param maxInputLag   The maximum amount of time that the "future" time can lag behind the
     *                      reference time
     *
     * @throws IllegalStateException if the futureTime Instant is too far in the past.
     */
    public static void confirmApproximateTimeOrdering(Instant referenceTime, Instant futureTime, Duration maxInputLag) {

        checkNotNull(referenceTime, "referenceTime cannot be null");
        checkNotNull(futureTime, "futureTime cannot be null");
        checkArgument(!maxInputLag.isNegative(), "Negative input lags are prohibitied for clarity");

        Duration timeDelta = Duration.between(referenceTime, futureTime);

        checkArgument(
            timeDelta.toMillis() >= -maxInputLag.toMillis(),
            "The futureTime cannot occur \"far\" in the past."
                + "\n  referenceTime: " + referenceTime.toEpochMilli()
                + "\n  futureTime: " + futureTime.toEpochMilli()
                + "\n  timeDelta: " + timeDelta.toMillis() + " milliseconds"
                + "\n  maxInputLag: " + maxInputLag.toMillis() + " milliseconds"
        );
    }

    /**
     * Confirm an Instant in time is in "strict time order" (i.e. a candidate time is not in the
     * past with respect to a reference Instant).
     *
     * @param referenceTime The "correct" or "current" moment in time
     * @param futureTime    The "future" moment in time
     *
     * @throws IllegalStateException if the futureTime Instant is too far in the past.
     */
    public static void confirmStrictTimeOrdering(Instant referenceTime, Instant futureTime) {
        confirmApproximateTimeOrdering(referenceTime, futureTime, Duration.ZERO);
    }

    /**
     * Confirms all the Instants in the array are in chronological order from oldest to newest.
     *
     * @param times An array of times.
     */
    public static void confirmTimeOrdering(Instant[] times) {
        //cannot be out of order with 0 or 1 entry
        if (times.length < 2) {
            return;
        }

        for (int i = 1; i < times.length; i++) {
            Instant prior = times[i - 1];
            Instant current = times[i];
            checkArgument(prior.isBefore(current) || prior.equals(current));
        }
    }

    /**
     * @param times One or more Instants
     *
     * @return The smallest possible TimeWindow that includes all input times.
     */
    public static TimeWindow enclosingTimeWindow(Instant... times) {

        return TimeWindow.of(
            earliest(times),
            latest(times)
        );
    }

    /**
     * @param times One or more Instants
     *
     * @return The Duration of time that separates the earliest input time from the latest input
     *     time
     */
    public static Duration durationBtw(Instant... times) {
        return enclosingTimeWindow(times).duration();
    }

    /**
     * Convert a duration of time to a String.
     *
     * @param duration A duration of time
     *
     * @return A String like "13:22:15" or "2 days, 13:22:15"
     */
    public static String asString(Duration duration) {

        long numDays = duration.toDays();
        long numHours = duration.toHours() % 24;
        long numMinutes = duration.toMinutes() % 60;
        long numSeconds = duration.getSeconds() % 60;

        String output = String.format("%d:%02d:%02d", numHours, numMinutes, numSeconds);

        if (numDays > 0L) {
            return numDays + " days, " + output;
        } else {
            return output;
        }
    }

    /**
     * Convert an Instant to a String with the format: HH:mm:ss.SSS
     *
     * @param instant A moment in time
     *
     * @return The above moment in time written with the given format. For example,
     *     Instant.EPOCH.plusSeconds(20).plusMilli(431) = "00:00:20.431"
     */
    public static String asZTimeString(Instant instant) {

        DateTimeFormatter Z_TIME_FORMATTER = DateTimeFormatter.ofPattern(
            "HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

        return Z_TIME_FORMATTER.format(instant);
    }

    /**
     * Convert an Instant to a String that contains the date and time.
     *
     * @param instant A moment in time
     *
     * @return The above moment in 12/31/69 7:00 PM
     */
    public static String asString(Instant instant) {

        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }

    /**
     * Generate the date for "right now". This date is set using the local system time.
     *
     * @return A String like "2017-03-27" (i.e. yyyy-mm-dd)
     */
    public static String todaysDateAsString() {
        /*
         * Note: I would have preferred to have this method accept an Instant as input. I was
         * prevented from doing this because Instants do not have time-zones, thus they don't map
         * cleanly into an exact Date. Consequently, I wrote the method that did exactly what I
         * needed (got me today's date in the format I wanted for logging purposes)
         */
        return (LocalDateTime.now()).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static final String DATE_FORMAT_ERROR = "Date not specified as YYYY-MM-DD format";

    /** Throw an IllegalArgumentException if the input String is not in the YYYY-MM-DD format. */
    public static void verifyYearMonthDayFormat(String date) {
        checkNotNull(date);
        checkArgument(date.length() == 10);
        checkArgument(Character.isDigit(date.charAt(0)), DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(1)), DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(2)), DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(3)), DATE_FORMAT_ERROR);
        checkArgument(date.charAt(4) == '-', DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(5)), DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(6)), DATE_FORMAT_ERROR);
        checkArgument(date.charAt(7) == '-', DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(8)), DATE_FORMAT_ERROR);
        checkArgument(Character.isDigit(date.charAt(9)), DATE_FORMAT_ERROR);

        //require 01 through 12 for month
        int month = parseInt(date.substring(5, 7));
        checkArgument(1 <= month && month <= 12, "The month " + month + " is not valid");

        //require 01 through 31 for day
        int day = parseInt(date.substring(8, 10));
        checkArgument(1 <= day && day <= 31, "The day " + day + " is not valid");
    }

    /**
     * Generate the date for a specific instant in time (assuming the UTC timezone).
     *
     * @param instant An instant in time. The date of this instant, in the UTC timezone, is
     *                provided.
     *
     * @return A String like "2017-03-27" (i.e. yyyy-mm-dd)
     */
    public static String utcDateAsString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }

    /**
     * @param time0 An instant in time.
     * @param time1 An instant in time.
     *
     * @return The earliest of these two times (the 1st argument is returned in the event of a tie)
     */
    public static Instant earliest(Instant time0, Instant time1) {
        return (time0.isBefore(time1))
            ? time0
            : time1;
    }

    /**
     * @param times One or more Instants
     *
     * @return The earliest of these times.
     */
    public static Instant earliest(Instant... times) {
        Instant earliest = times[0];
        for (int i = 1; i < times.length; i++) {
            earliest = earliest(earliest, times[i]);
        }
        return earliest;
    }

    /**
     * @param time0 An instant in time.
     * @param time1 An instant in time.
     *
     * @return The latest of these two times (the 1st argument is returned in the event of a tie)
     */
    public static Instant latest(Instant time0, Instant time1) {
        return (time0.isAfter(time1))
            ? time0
            : time1;
    }

    /**
     * @param times One or more Instants
     *
     * @return The earliest of these times.
     */
    public static Instant latest(Instant... times) {
        Instant latest = times[0];
        for (int i = 1; i < times.length; i++) {
            latest = latest(latest, times[i]);
        }
        return latest;
    }

    /**
     * @param h1 Something with a time.
     * @param h2 Something with a time.
     *
     * @return The latest occurring of these two inputs (the 1st argument is returned in the event
     *     of a tie)
     */
    public static HasTime latest(HasTime h1, HasTime h2) {
        return (h1.time().isAfter(h2.time()))
            ? h1
            : h2;
    }

    static <H extends HasTime> NavigableMap<Instant, H> toMap(Collection<H> items) {

        TreeMap<Instant, H> map = newTreeMap();
        items.stream().forEach(item -> map.put(item.time(), item));

        checkArgument(
            items.size() == map.size(),
            "Two items in the input collection had the same time() value"
        );

        return map;
    }

    public static Duration max(Duration duration1, Duration duration2) {
        return (theDuration(duration1).isGreaterThanOrEqualTo(duration2))
            ? duration1
            : duration2;
    }

    public static Duration min(Duration duration1, Duration duration2) {
        return (theDuration(duration1).isGreaterThanOrEqualTo(duration2))
            ? duration2
            : duration1;
    }

    /**
     * This method permits code like
     * <code>if(theDurationBtw(time1, time2).isLessThan(maxAllowableDuration))</code>
     *
     * @param times One or more Instants
     *
     * @return A LiterateDurationof time that separates the earliest input time from the latest
     *     input time.
     */
    public static LiterateDuration theDurationBtw(Instant... times) {
        return new LiterateDuration(durationBtw(times));
    }

    /**
     * @param duration A Duration
     *
     * @return A LiterateDuration object which helps write literate boolean expressions that compare
     *     two Durations against each other.
     */
    public static LiterateDuration theDuration(Duration duration) {
        checkNotNull(duration);
        return new LiterateDuration(duration);
    }

    /**
     * This helper class enables literate code like
     * <code>theDuration(variable).isLessThan(maxAllowableDuration)</code>
     */
    public static class LiterateDuration {

        Duration firstDuration;

        private LiterateDuration(Duration dur) {
            this.firstDuration = checkNotNull(dur);
        }

        public boolean isLessThan(Duration secondDuration) {
            return firstDuration.toMillis() < secondDuration.toMillis();
        }

        public boolean isLessThanOrEqualTo(Duration secondDuration) {
            return firstDuration.toMillis() <= secondDuration.toMillis();
        }

        public boolean isGreaterThan(Duration secondDuration) {
            return firstDuration.toMillis() > secondDuration.toMillis();
        }

        public boolean isGreaterThanOrEqualTo(Duration secondDuration) {
            return firstDuration.toMillis() >= secondDuration.toMillis();
        }
    }
}
