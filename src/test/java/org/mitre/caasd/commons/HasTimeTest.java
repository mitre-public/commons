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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.HasTime.nearest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import org.mitre.caasd.commons.TimeTest.HasTimeImp;

import org.junit.jupiter.api.Test;

public class HasTimeTest {

    @Test
    public void validateRejectsNull() {
        assertThrows(IllegalStateException.class, () -> HasTime.validate(null));
    }

    @Test
    public void validateRejectsInstantsAtEpoch() {
        assertThrows(IllegalStateException.class, () -> HasTime.validate(EPOCH));
    }

    @Test
    public void validateRejectsInstantsBeforeEpoch() {
        assertThrows(IllegalStateException.class, () -> HasTime.validate(EPOCH.minusMillis(1)));
    }

    @Test
    public void validateAcceptsInstantsAfterEpoch() {
        HasTime.validate(EPOCH.plusMillis(1)); //passes validation, no other side effect
    }

    @Test
    public void durationBtw_correct_alwaysPositiveNoMatterOrder() {
        HasTimeImp a = new HasTimeImp(EPOCH);
        HasTimeImp b = new HasTimeImp(EPOCH.plusSeconds(10));

        assertThat(a.durationBtw(b), is(Duration.ofSeconds(10)));
        assertThat(b.durationBtw(a), is(Duration.ofSeconds(10)));
    }

    @Test
    public void testNearest() {
        HasTimeImp a = new HasTimeImp(EPOCH);
        HasTimeImp b = new HasTimeImp(EPOCH.plusSeconds(10));

        assertThat(nearest(a, b, EPOCH.plusSeconds(1)), is(a));
        assertThat(nearest(a, b, EPOCH.plusSeconds(9)), is(b));
        assertThat(nearest(a, b, EPOCH.plusSeconds(5)), is(a));
    }

    @Test
    public void binarySearchMatchesGiveCorrectIndex() {

        ArrayList<PojoWithTime> list = newArrayList(
            new PojoWithTime("a", EPOCH),
            new PojoWithTime("b", EPOCH.plusSeconds(10)),
            new PojoWithTime("c", EPOCH.plusSeconds(20))
        );

        assertThat(HasTime.binarySearch(list, EPOCH), is(0));
        assertThat(HasTime.binarySearch(list, EPOCH.plusSeconds(10)), is(1));
        assertThat(HasTime.binarySearch(list, EPOCH.plusSeconds(20)), is(2));
    }

    @Test
    public void binarySearchMissesGiveCorrectIndex() {

        ArrayList<PojoWithTime> list = newArrayList(
            new PojoWithTime("a", EPOCH),
            new PojoWithTime("b", EPOCH.plusSeconds(10)),
            new PojoWithTime("c", EPOCH.plusSeconds(20))
        );

        //output int = (-(insertion point) -1)
        assertThat(HasTime.binarySearch(list, EPOCH.minusSeconds(1)), is(-1));  //insert before item 0
        assertThat(HasTime.binarySearch(list, EPOCH.plusSeconds(1)), is(-2)); //insert between 0 and 1
        assertThat(HasTime.binarySearch(list, EPOCH.plusSeconds(11)), is(-3)); //insert between 1 and 2
        assertThat(HasTime.binarySearch(list, EPOCH.plusSeconds(21)), is(-4)); //insert after 2
    }

    @Test
    public void floorGivesCorrectItem() {
        PojoWithTime a = new PojoWithTime("a", EPOCH);
        PojoWithTime b = new PojoWithTime("b", EPOCH.plusSeconds(10));
        PojoWithTime c = new PojoWithTime("c", EPOCH.plusSeconds(20));
        PojoWithTime d = new PojoWithTime("d", EPOCH.plusSeconds(30));

        ArrayList<PojoWithTime> list = newArrayList(a, b, c, d);

        assertThat(HasTime.floor(list, EPOCH), is(a));
        assertThat(HasTime.floor(list, EPOCH.plusSeconds(10)), is(b));
        assertThat(HasTime.floor(list, EPOCH.plusSeconds(20)), is(c));
        assertThat(HasTime.floor(list, EPOCH.plusSeconds(30)), is(d));

        assertThat(HasTime.floor(list, EPOCH.plusSeconds(5)), is(a));
        assertThat(HasTime.floor(list, EPOCH.plusSeconds(15)), is(b));
        assertThat(HasTime.floor(list, EPOCH.plusSeconds(25)), is(c));
    }

    @Test
    void floorRejectsOutOfRange() {

        PojoWithTime a = new PojoWithTime("a", EPOCH);
        PojoWithTime b = new PojoWithTime("b", EPOCH.plusSeconds(10));
        PojoWithTime c = new PojoWithTime("c", EPOCH.plusSeconds(20));
        PojoWithTime d = new PojoWithTime("d", EPOCH.plusSeconds(30));

        ArrayList<PojoWithTime> list = newArrayList(a, b, c, d);

        assertThrows(
            IllegalArgumentException.class,
            () -> HasTime.floor(list, EPOCH.minusSeconds(1))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> HasTime.floor(list, EPOCH.plusSeconds(31))
        );
    }

    @Test
    public void ceilingGivesCorrectItem() {
        PojoWithTime a = new PojoWithTime("a", EPOCH);
        PojoWithTime b = new PojoWithTime("b", EPOCH.plusSeconds(10));
        PojoWithTime c = new PojoWithTime("c", EPOCH.plusSeconds(20));
        PojoWithTime d = new PojoWithTime("d", EPOCH.plusSeconds(30));

        ArrayList<PojoWithTime> list = newArrayList(a, b, c, d);

        assertThat(HasTime.ceiling(list, EPOCH), is(a));
        assertThat(HasTime.ceiling(list, EPOCH.plusSeconds(10)), is(b));
        assertThat(HasTime.ceiling(list, EPOCH.plusSeconds(20)), is(c));
        assertThat(HasTime.ceiling(list, EPOCH.plusSeconds(30)), is(d));

        assertThat(HasTime.ceiling(list, EPOCH.plusSeconds(5)), is(b));
        assertThat(HasTime.ceiling(list, EPOCH.plusSeconds(15)), is(c));
        assertThat(HasTime.ceiling(list, EPOCH.plusSeconds(25)), is(d));
    }

    @Test
    void ceilingRejectsOutOfRange() {

        PojoWithTime a = new PojoWithTime("a", EPOCH);
        PojoWithTime b = new PojoWithTime("b", EPOCH.plusSeconds(10));
        PojoWithTime c = new PojoWithTime("c", EPOCH.plusSeconds(20));
        PojoWithTime d = new PojoWithTime("d", EPOCH.plusSeconds(30));

        ArrayList<PojoWithTime> list = newArrayList(a, b, c, d);

        assertThrows(
            IllegalArgumentException.class,
            () -> HasTime.ceiling(list, EPOCH.minusSeconds(1))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> HasTime.ceiling(list, EPOCH.plusSeconds(31))
        );
    }


    @Test
    public void closestGivesCorrectItem() {
        PojoWithTime a = new PojoWithTime("a", EPOCH);
        PojoWithTime b = new PojoWithTime("b", EPOCH.plusSeconds(10));
        PojoWithTime c = new PojoWithTime("c", EPOCH.plusSeconds(20));
        PojoWithTime d = new PojoWithTime("d", EPOCH.plusSeconds(30));

        ArrayList<PojoWithTime> list = newArrayList(a, b, c, d);

        assertThat(HasTime.closest(list, EPOCH), is(a));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(10)), is(b));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(20)), is(c));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(30)), is(d));

        assertThat(HasTime.closest(list, EPOCH.plusSeconds(1)), is(a));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(5)), is(a));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(9)), is(b));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(11)), is(b));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(15)), is(b));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(19)), is(c));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(21)), is(c));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(25)), is(c));
        assertThat(HasTime.closest(list, EPOCH.plusSeconds(29)), is(d));
    }


    @Test
    void closestRejectsOutOfRange() {

        PojoWithTime a = new PojoWithTime("a", EPOCH);
        PojoWithTime b = new PojoWithTime("b", EPOCH.plusSeconds(10));
        PojoWithTime c = new PojoWithTime("c", EPOCH.plusSeconds(20));
        PojoWithTime d = new PojoWithTime("d", EPOCH.plusSeconds(30));

        ArrayList<PojoWithTime> list = newArrayList(a, b, c, d);

        assertThrows(
            IllegalArgumentException.class,
            () -> HasTime.closest(list, EPOCH.minusSeconds(1))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> HasTime.closest(list, EPOCH.plusSeconds(31))
        );
    }


    /**
     * This class does not implement Comparable BUT we can sort instance of PojoWithTime by its time
     * value.  If we do this we may want to binary search a collection of these PojoWithTime for a
     * specific time value.
     */
    static class PojoWithTime implements HasTime {

        final String name;

        final Instant time;

        PojoWithTime(String name, Instant time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public Instant time() {
            return time;
        }
    }
}
