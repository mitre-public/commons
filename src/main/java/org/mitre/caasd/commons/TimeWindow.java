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
import static com.google.common.collect.Lists.newArrayList;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A TimeWindow is a fixed window of time that has a well specified beginning and a well specified
 * end.
 * <p>
 * A TimeWindow must represent a non-negative amount of time. In other words, the beginning of a
 * TimeWindow cannot come after the end of a TimeWindow. This requirement exists to prevent
 * unexpected behavior when using TimeWindows in client code.
 * <p>
 * TimeWindows are immutable.
 */
public class TimeWindow implements Serializable {

    private final Instant start;

    private final Instant end;

    /**
     * Create an immutable TimeWindow.
     *
     * @param start A beginning instant in time (cannot be null)(cannot occur after the "end")
     * @param end   A ending instant in time (cannot be null)
     */
    public TimeWindow(Instant start, Instant end) {
        this.start = checkNotNull(start, "The start of the time window cannot be null");
        this.end = checkNotNull(end, "The end of the time window cannot be null");
        checkArgument(
            !start.isAfter(end),
            "The start of a TimeWindow cannot come after the end of a TimeWindow"
        );
    }

    /**
     * @param start A beginning instant in time (cannot be null)(cannot occur after the "end")
     * @param end   A ending instant in time (cannot be null)
     *
     * @return An immutable TimeWindow.
     */
    public static TimeWindow of(Instant start, Instant end) {
        return new TimeWindow(start, end);
    }

    public static <T extends HasTime> TimeWindow enclosingWindow(Collection<T> items) {
        List<T> list = newArrayList(items);
        list.sort(Time.compareByTime());
        T start = list.get(0);
        T end = list.get(list.size() - 1);

        return new TimeWindow(start.time(), end.time());
    }

    /**
     * @param duration The amount of padding to add to the front and back of the TimeWindow
     *
     * @return An expanded TimeWindow {padding + original + padding}
     */
    public TimeWindow pad(Duration duration) {
        return new TimeWindow(start.minus(duration), end.plus(duration));
    }

    /**
     * @return The start of this TimeWindow
     */
    public Instant start() {
        return start;
    }

    /**
     * @return The end of this TimeWindow
     */
    public Instant end() {
        return end;
    }

    /** @deprecated  */
    public Duration length() {
        return duration();
    }

    /** @return This TimeWindow's Duration */
    public Duration duration() {
        return Duration.between(start, end);
    }

    /** @return True when Duration of this TimeWindow is zero. */
    public boolean isEmpty() {
        return !start().isBefore(end());
    }

    /**
     * @param instant An instant in time.
     *
     * @return True if the provided instant is within this time window. Note, the start and end of
     *     this TimeWindow are considered "within" the TimeWindow.
     */
    public boolean contains(Instant instant) {

        boolean afterStart = instant.isAfter(start) || instant.equals(start);
        boolean beforeEnd = instant.isBefore(end) || instant.equals(end);

        return afterStart && beforeEnd;
    }

    public boolean overlapsWith(TimeWindow other) {
        checkNotNull(other);

        Instant startOfOverlap = Time.latest(this.start, other.start);
        Instant endOfOverlap = Time.earliest(this.end, other.end);

        //is only true when these two tracks overlap in time.
        return startOfOverlap.isBefore(endOfOverlap) || startOfOverlap.equals(endOfOverlap);
    }

    public Optional<TimeWindow> getOverlapWith(TimeWindow other) {
        checkNotNull(other);

        Instant startOfOverlap = Time.latest(this.start, other.start);
        Instant endOfOverlap = Time.earliest(this.end, other.end);

        if (startOfOverlap.isBefore(endOfOverlap) || startOfOverlap.equals(endOfOverlap)) {
            return Optional.of(TimeWindow.of(startOfOverlap, endOfOverlap));
        } else {
            return Optional.empty();
        }
    }

    /**
     * What number does the Instant t correspond to if this TimeWindow represents a number line
     * where the start of this TimeWindow = 0 and the end of the TimeWindow = 1? The purpose of this
     * method is to facilitate computing interpolated values in or around this TimeWindow.
     *
     * @param t An Instant to be placed on this TimeWindow's "number line"
     *
     * @return The Instant t "casted to fraction" of this TimeWindow.
     */
    public double toFractionOfRange(Instant t) {
        checkNotNull(t, "The input time t cannot be null");

        long msBetweenStartAndT = Duration.between(start, t).toMillis();
        long msInRange = this.duration().toMillis();

        double castedNumerator = (double) msBetweenStartAndT;
        double castedDenominator = (double) msInRange;

        return castedNumerator / castedDenominator;
    }

    /**
     * Find an Instant within this TimeWindow.
     *
     * @param fraction A number between 0 (inclusive) and 1 (inclusive)
     *
     * @return The appropriate instant within this time window computed to millisecond accuracy.
     */
    public Instant instantWithin(double fraction) {
        checkArgument(fraction >= 0, "Input fraction must be at least 0: " + fraction);
        checkArgument(fraction <= 1, "Input fraction can be no greater than 1: " + fraction);

        long durationAsMillis = this.duration().toMillis();
        long delta = (long) (fraction * durationAsMillis);

        return this.start().plusMillis(delta);
    }

    /**
     * @param timeStep The duration of time between back-to-back Instants supplied by the returned
     *                 Iterator
     *
     * @return An Iterator the iterates over a sequence of Instants that are all within a this
     *     TimeWindow. The returned Iterator starts at "start()"
     */
    public Iterator<Instant> iterator(Duration timeStep) {
        checkNotNull(timeStep, "The timeStep cannot be null");
        checkArgument(!timeStep.isNegative(), "The timeStep cannot be negative");
        checkArgument(!timeStep.isZero(), "The timeStep cannot be zero");
        return new InstantIterator(start, end, timeStep);
    }

    /**
     * @param timeStep The duration of time between back-to-back Instants supplied by the returned
     *                 Iterator
     *
     * @return An ArrayList containing the Instants that appeared in an iteration of this TimeWindow
     * @see TimeWindow#iterator(java.time.Duration)
     */
    public ArrayList<Instant> steppedIteration(Duration timeStep) {
        return newArrayList(iterator(timeStep));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.start);
        hash = 31 * hash + Objects.hashCode(this.end);
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
        final TimeWindow other = (TimeWindow) obj;
        if (!Objects.equals(this.start, other.start)) {
            return false;
        }
        if (!Objects.equals(this.end, other.end)) {
            return false;
        }
        return true;
    }
}
