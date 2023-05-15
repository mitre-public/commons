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
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.Time.theDuration;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@FunctionalInterface
public interface HasTime {

    public Instant time();

    default long timeAsEpochMs() {
        return time().toEpochMilli();
    }

    /** Verifies the input Instant is  {@code nonNull and>= 0ms} from the epoch. */
    static void validate(Instant time) {
        checkState(nonNull(time), "time() cannot return null");
        checkState(time.toEpochMilli() > 0L, "time() must return positive epochMilli");
    }

    /** @return A Positive Duration, no matter the input time. */
    default Duration durationBtw(Instant time) {
        return Duration.between(time(), time).abs();
    }

    /** @return A Positive Duration, no matter the input's time. */
    default Duration durationBtw(HasTime hasTime) {
        return durationBtw(hasTime.time());
    }

    static HasTime wrap(Long epochMills) {
        return () -> Instant.ofEpochMilli(epochMills);
    }

    static HasTime wrap(Instant time) {
        return () -> time;
    }

    public static <P extends HasTime> P nearest(P left, P right, Instant time) {
        Duration leftDelta = Duration.between(left.time(), time).abs();
        Duration rightDelta = Duration.between(right.time(), time).abs();

        return (theDuration(leftDelta).isLessThanOrEqualTo(rightDelta))
            ? left
            : right;
    }

    /**
     * This implementation of binary search avoids a limitation of Collections.binarySearch. Imagine
     * you have a complex class T that implements HasTime. If it is difficult to construct new
     * instances of T you cannot use Collections.binarySearch(List, T, Comparator). You can't use
     * the basic convenience method in java.util because you can't create the "search key object T".
     *
     * <p>This implementation of binary search allows you to (1) replace the search key T with a
     * "searchTime" and (2) skip providing a Comparator because we'll require the input data to be
     * sorted by time.
     *
     * @param itemsSortedByTime A chronologically sorted list of items
     * @param searchTime        The search time
     * @param <T>               Some type T that implements HasTime.  The type does not need to
     *                          implement Comparable.
     *
     * @return If the list contains an item whose "time()" method returns the searchTime it will
     * return the index of that "matching item".  Otherwise, this method will return (-(insertion
     * point) -1).  Where the insertion point is defined as the point at which an item with the
     * "searchTime" would be inserted into the list. See Collections.binarySearch for a nearly
     * identical use-case.
     */
    static <T extends HasTime> int binarySearch(List<? extends T> itemsSortedByTime,
                                                Instant searchTime) {

        requireNonNull(itemsSortedByTime);
        requireNonNull(searchTime);

        return CollectionUtils.binarySearch(itemsSortedByTime, HasTime::time, searchTime);
    }


    /**
     * Find the newest item in the list that occurs at or before the searchTime.
     *
     * @param itemsSortedByTime A chronologically sorted list of items
     * @param searchTime        A time inside the TimeWindow spanning all items in the list
     *
     * @return The newest item in the list that occurs at or before the searchTime.
     */
    static <T extends HasTime> T floor(List<? extends T> itemsSortedByTime, Instant searchTime) {
        requireNonNull(itemsSortedByTime);
        requireNonNull(searchTime);

        Instant start = itemsSortedByTime.get(0).time();
        Instant end = itemsSortedByTime.get(itemsSortedByTime.size() - 1).time();
        TimeWindow window = TimeWindow.of(start, end);

        checkArgument(window.contains(searchTime), "searchTime must be inside spanning TimeWindow");

        int index = binarySearch(itemsSortedByTime, searchTime);

        return (index >= 0)
            ? itemsSortedByTime.get(index)
            : itemsSortedByTime.get(-index - 2);
    }

    /**
     * Find the oldest item in the list that occurs at or after the searchTime.
     *
     * @param itemsSortedByTime A chronologically sorted list of items
     * @param searchTime        A time inside the TimeWindow spanning all items in the list
     *
     * @return Find the oldest item in the list that occurs at or after the searchTime.
     */
    static <T extends HasTime> T ceiling(List<? extends T> itemsSortedByTime, Instant searchTime) {

        requireNonNull(itemsSortedByTime);
        requireNonNull(searchTime);

        Instant start = itemsSortedByTime.get(0).time();
        Instant end = itemsSortedByTime.get(itemsSortedByTime.size() - 1).time();
        TimeWindow window = TimeWindow.of(start, end);

        checkArgument(window.contains(searchTime), "searchTime must be inside spanning TimeWindow");

        int index = binarySearch(itemsSortedByTime, searchTime);

        return (index >= 0)
            ? itemsSortedByTime.get(index)
            : itemsSortedByTime.get(-index - 1);
    }


    /**
     * Find the item in the list whose time is closest to the searchTime (tie goes to oldest item).
     *
     * @param itemsSortedByTime A chronologically sorted list of items
     * @param searchTime        A time inside the TimeWindow spanning all items in the list
     *
     * @return The item in the list whose time is closest to the searchTime.
     */
    static <T extends HasTime> T closest(List<? extends T> itemsSortedByTime, Instant searchTime) {
        requireNonNull(itemsSortedByTime);
        requireNonNull(searchTime);

        Instant start = itemsSortedByTime.get(0).time();
        Instant end = itemsSortedByTime.get(itemsSortedByTime.size() - 1).time();
        TimeWindow window = TimeWindow.of(start, end);

        checkArgument(window.contains(searchTime), "searchTime must be inside spanning TimeWindow");

        int index = binarySearch(itemsSortedByTime, searchTime);

        if (index >= 0) {
            return itemsSortedByTime.get(index);
        }

        int floorIndex = -index - 2;
        int ceilIndex = -index - 1;
        T floor = itemsSortedByTime.get(floorIndex);
        T ceil = itemsSortedByTime.get(ceilIndex);
        Duration floorDelta = Duration.between(floor.time(), searchTime);
        Duration ceilDelta = Duration.between(searchTime, ceil.time());

        if (theDuration(floorDelta).isLessThanOrEqualTo(ceilDelta)) {
            return floor;
        } else {
            return ceil;
        }
    }
}