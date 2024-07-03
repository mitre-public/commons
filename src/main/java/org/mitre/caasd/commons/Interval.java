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
import static com.google.common.collect.Lists.newArrayList;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

/**
 * Simple serializable interval class.
 * <p>
 * The interval is closed-open. [t1, t2).
 */
public class Interval implements Serializable, Comparable<Interval> {

    public static final Long SECOND = 1000L;
    public static final Long MINUTE = SECOND * 60;
    public static final Long HOUR = MINUTE * 60;
    public static final Long DAY = HOUR * 24;
    public static final Long WEEK = DAY * 7;
    public static final Long YEAR = DAY * 365;

    private final Long start_time;
    private final Long end_time;

    @Deprecated
    public Interval() {
        this.start_time = null;
        this.end_time = null;
    }

    public Interval(Instant start, Instant end) {
        requireNonNull(start);
        requireNonNull(end);
        this.start_time = start.equals(Instant.MIN) ? Long.MIN_VALUE : start.toEpochMilli();
        this.end_time = end.equals(Instant.MAX) ? Long.MAX_VALUE : end.toEpochMilli();

        checkArgument(start_time <= end_time, "Cannot create Interval with start time after end time");
    }

    public Interval(OffsetDateTime start, OffsetDateTime end) {
        this(start.toInstant(), end.toInstant());
    }

    /**
     * Creates an interval covering the specified date.
     */
    public Interval(LocalDate startDateInclusive, LocalDate endDateInclusive) {
        this(
                startDateInclusive.equals(LocalDate.MIN)
                        ? Instant.MIN
                        : startDateInclusive.atStartOfDay().toInstant(ZoneOffset.UTC),
                endDateInclusive.equals(LocalDate.MAX)
                        ? Instant.MAX
                        : endDateInclusive.plus(1, DAYS).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public boolean isEmpty() {
        return !start().isBefore(end());
    }

    public Long startEpoch() {
        return start_time;
    }

    public Long endEpoch() {
        return end_time;
    }

    public Instant start() {
        return startEpoch().equals(Long.MIN_VALUE) ? Instant.MIN : Instant.ofEpochMilli(startEpoch());
    }

    public Instant end() {
        return endEpoch().equals(Long.MAX_VALUE) ? Instant.MAX : Instant.ofEpochMilli(endEpoch());
    }

    public Instant getStartDay() {
        return start().truncatedTo(ChronoUnit.DAYS);
    }

    public Instant getEndDay() {
        return end().truncatedTo(ChronoUnit.DAYS);
    }

    public Long getMidpointEpoch() {
        return (start_time + end_time) / 2;
    }

    public Duration duration() {
        return Duration.between(start(), end());
    }

    /**
     * Returns whether the interval contains the specified time, behavior is closed-open on
     * startEpoch-end.
     */
    public boolean contains(Long time) {
        return start_time <= time && end_time > time;
    }

    public boolean contains(Instant time) {
        return contains(time.toEpochMilli());
    }

    /**
     * Interval containment check, the entire given interval must fall within this interval to
     * return true. Need this to be inclusive on boundaries to get reasonable results. I.e. is
     * start=start and end &lt; end OR startEpoch &gt; startEpoch and end=end containment should
     * return true.
     */
    public boolean contains(Interval i) {
        return start_time <= i.startEpoch() && end_time >= i.endEpoch(); // Assuming [,) for each >= is reasonable...
    }

    public boolean contains(LocalDate dt) {
        return contains(new Interval(dt, dt));
    }

    public boolean containsClosed(Instant tau) {
        return contains(tau) || end().equals(tau);
    }

    /**
     * Returns true if the given interval overlaps (exclusive on boundary times) with this
     * interval.
     */
    public boolean overlaps(Interval i) {
        return start().isBefore(i.end()) && end().isAfter(i.start());
    }

    public boolean overlaps(LocalDate dt) {
        return overlaps(new Interval(dt, dt));
    }

    public Interval withBuffer(Integer daysBefore, Integer daysAfter) {
        if (daysBefore != null && daysAfter != null) {
            return new Interval(start().plus(daysBefore, DAYS), end().plus(daysAfter, DAYS));
        } else {
            return this;
        }
    }

    public Interval shiftForward(Duration d) {
        return new Interval(start().plus(d), end().plus(d));
    }

    public Interval shiftBackward(Duration d) {
        return new Interval(start().minus(d), end().minus(d));
    }

    public Interval extend(Interval i) {
        return extend(i.start(), i.end());
    }

    public Interval extend(Instant s, Instant e) {
        if (this.isEmpty() && s != null && e != null) {
            return new Interval(s, e);
        }
        if (s != null && s.equals(e)) {
            return new Interval(this.start(), this.end());
        }

        Instant start = s == null || s.isAfter(start()) ? start() : s;
        Instant end = e == null || e.isBefore(end()) ? end() : e;
        return new Interval(start, end);
    }

    public Interval extendBy(Duration d) {
        return new Interval(start(), end().plus(d));
    }

    public Interval extendTo(Instant time) {
        return new Interval(start(), time.isAfter(end()) ? time : end());
    }

    public Interval truncateTo(Interval int1) {
        Instant start = start().isBefore(int1.start()) ? int1.start() : start();
        Instant end = end().isAfter(int1.end()) ? int1.end() : end();
        if (start.isAfter(end)) {
            start = end;
        }
        return new Interval(start, end);
    }

    /**
     * @return time zone-less dates that intersect the interval
     */
    public List<LocalDate> listDates() {
        return datesBetween(start(), end())
                .map(i -> i.atZone(ZoneId.of("GMT")).toLocalDate())
                .collect(Collectors.toList());
    }

    public Stream<Instant> timesBetween(Duration width) {
        return timesBetween(start(), end(), width);
    }

    public <P extends Comparable<? super P>> NavigableSet<P> filter(NavigableSet<P> vals, Function<P, Instant> conv) {
        return vals.stream()
                .filter(val -> this.contains(conv.apply(val)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.start_time);
        hash = 17 * hash + Objects.hashCode(this.end_time);
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
        final Interval other = (Interval) obj;
        if (!Objects.equals(this.start_time, other.start_time)) {
            return false;
        }
        if (!Objects.equals(this.end_time, other.end_time)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Interval o) {
        int comp = start_time.compareTo(o.startEpoch());
        return comp != 0 ? comp : end_time.compareTo(o.endEpoch());
    }

    public static Long seconds(int num) {
        return Interval.SECOND * num;
    }

    public static Interval covering(Collection<LocalDate> dates) {
        requireNonNull(dates);
        checkArgument(!dates.isEmpty(), "No covering for an empty date set.");
        return new Interval(
                dates.stream().min(LocalDate::compareTo).get(),
                dates.stream().max(LocalDate::compareTo).get());
    }

    public static Interval covering(LocalDate... dates) {
        return Interval.covering(Arrays.asList(dates));
    }

    public static Interval parseInstants(String start, String end) {
        return new Interval(Instant.parse(start), Instant.parse(end));
    }

    public static Interval parseLocalDates(String startInclusive, String endInclusive) {
        return new Interval(LocalDate.parse(startInclusive), LocalDate.parse(endInclusive));
    }

    /**
     * Returns a collection of intervals at the given time of day and day of the week covering the
     * specified date range. Where the days of the week integers are those from {@link Calendar} :
     * {@link DayOfWeek}.
     */
    public static Collection<Interval> fromRange(
            Long startTOD, Long endTOD, List<Integer> daysOfWeek, Long startDate, Long endDate) {
        Collection<Interval> itvs = new HashSet<>();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        long startDay = startDate - (startDate % DAY);
        long endDay = endDate + (DAY - (endDate % DAY));

        for (long day = startDay; day < endDay; day += DAY) {
            c.setTime(Date.from(Instant.ofEpochMilli(day)));
            if (daysOfWeek.contains(c.get(Calendar.DAY_OF_WEEK))) {

                Interval itrvl = new Interval(Instant.ofEpochMilli(day + startTOD), Instant.ofEpochMilli(day + endTOD));

                itvs.add(itrvl);
            }
        }
        return itvs;
    }

    /**
     * Returns a stream of the instant day floors between the startEpoch and end instants.
     */
    public static Stream<Instant> datesBetween(Instant start, Instant end) {
        return timesBetween(start, end, Duration.ofMillis(DAY));
    }

    /**
     * Returns a stream of "rounded times" occurring btw the two Instants.
     *
     * @param start An instant that usually occurs BEFORE the first Instant in the stream. When
     *              start is a perfect multiple of the width Duration then start will be the first
     *              value in the output steam (e.g., if the duration width = 5 minutes and start =
     *              12:05:00.000 then the start Instant will appear in the output stream)
     * @param end   An instant that always occurs AFTER the last Instant in the stream.
     *
     * @return A stream of "Rounded Instants" that come between the start and end. For example, if
     *     start = 13:52:45 and end = 15:14:45 (82 min apart) timesBetween(start, end,
     *     Duration.ofMinutes(15)) will return a Stream containing: {14:00:00, 14:15:00, 14:30:00,
     *     14:45:00, and 15:00:00}
     *     <p>
     *     Returns a stream of the epoch floors at the given width between the two dates. I.e.
     *     returns time bins of size width between start and end.
     */
    public static Stream<Instant> timesBetween(Instant start, Instant end, Duration width) {
        if ((end.toEpochMilli() - start.toEpochMilli()) / width.toMillis() > 99999) {
            throw new IllegalArgumentException("Too many sub-intervals to enumerate");
        }

        ArrayList<Instant> times = newArrayList();
        long startTime = start.toEpochMilli() - (start.toEpochMilli() % width.toMillis());
        long endTime = end.toEpochMilli();
        long step = width.toMillis();

        for (long time = startTime; time < endTime; time += step) {
            if (time >= start.toEpochMilli()) {
                times.add(Instant.ofEpochMilli(time));
            }
        }
        return times.stream();
    }

    /**
     * Returns whether the specified collection of intervals are disjoint from one another.
     */
    public static boolean areDisjoint(Collection<Interval> intervals) {
        List<Interval> itvs = intervals.stream().sorted().collect(Collectors.toList());
        return IntStream.range(1, itvs.size())
                        .filter(i -> itvs.get(i - 1).overlaps(itvs.get(i)))
                        .count()
                == 0;
    }

    /**
     * Returns a complementary subset of intervals within an outer interval to the given input
     * collection. There are faster ways to do this but meh.
     */
    public static Collection<Interval> complementOf(Interval full, Collection<Interval> subintervals) {
        if (!areDisjoint(subintervals)) {
            /* For now if the intervals aren't disjoint we just don't allow taking the complement.
             * We can add functionality to combine overlapping intervals in a collection later. */
            throw new RuntimeException(
                    "Taking the complement of a collection of overlapping intervals is currently unsupported. Intervals were: "
                            + subintervals.stream().map(Object::toString).collect(Collectors.joining("\n")));
        }

        NavigableSet<Instant> times = subintervals.stream()
                .flatMap(i -> Stream.of(i.start(), i.end()))
                .collect(Collectors.toCollection(TreeSet::new));
        times.add(full.start());
        times.add(full.end());

        List<Instant> dts = new ArrayList<>(times);
        NavigableSet<Interval> fullCover = IntStream.range(1, dts.size())
                .mapToObj(i -> new Interval(dts.get(i - 1), dts.get(i)))
                .collect(Collectors.toCollection(TreeSet::new));

        return new HashSet<>(Sets.difference(fullCover, new TreeSet<>(subintervals)));
    }

    /**
     * Merges sequential intervals with shared end/startEpoch times into a single interval.
     */
    public static NavigableSet<Interval> merge(Collection<Interval> itvs) {
        List<Interval> sorted = itvs.stream().sorted().collect(Collectors.toList());
        NavigableSet<Interval> merged = new TreeSet<>();
        Interval open = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            Interval current = sorted.get(i);
            if (open.end().equals(current.start()) || open.overlaps(current)) {
                open = open.extend(current);
            } else {
                merged.add(open);
                open = current;
            }
        }
        merged.add(open);
        return merged;
    }

    public static Interval empty() {
        return new Interval(Instant.EPOCH, Instant.EPOCH);
    }

    public static Interval max() {
        return new Interval(Instant.MIN, Instant.MAX);
    }

    public String toString() {
        return start_time.toString() + " " + end_time.toString();
    }

    public String toString(boolean printInstants) {
        return printInstants ? start() + " - " + end() : toString();
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    /**
     * Returns a simplified string representation of this {@link Interval}.
     * <p>
     * A {@link LocalDate} string is returned for single UTC dates, a string of two {@link
     * LocalDate} s is returned when both startEpoch and end fall on UTC midnight, otherwise a
     * string of two {@link Instant}s is returned.
     * <p>
     * Examples:
     * <ul>
     * <li>UTC-midnight-aligned single-day interval: 2020-11-20</li>
     * <li>UTC-midnight-aligned multi-day interval: 2020-11-20 - 2020-11-23</li>
     * <li>Other intervals: 2020-11-20T01:02:03Z - 2020-11-23T04:05:06Z</li>
     * </ul>
     */
    public String toSimpleString() {
        String start = DATE_TIME_FORMAT.format(start());
        String end = DATE_TIME_FORMAT.format(end());
        if (start().equals(getStartDay()) && end().equals(getEndDay())) {
            start = DATE_FORMAT.format(start().atOffset(ZoneOffset.UTC).toLocalDate());
            end = DATE_FORMAT.format(end().atOffset(ZoneOffset.UTC).toLocalDate());
            if (duration().toDays() == 1) {
                end = null;
            }
        }
        return Stream.of(start, end).filter(Objects::nonNull).collect(Collectors.joining(" - "));
    }
}
