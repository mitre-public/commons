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
import static com.google.common.collect.Lists.newLinkedList;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.caasd.commons.CollectionUtils.zip;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.HasTimeTest.PojoWithTime;


public class CollectionUtilsTest {


    @Test
    public void binarySearchMatchesGiveCorrectIndex_arrayList() {

        ArrayList<PojoWithTime> list = newArrayList(
            new PojoWithTime("a", EPOCH),
            new PojoWithTime("b", EPOCH.plusSeconds(10)),
            new PojoWithTime("c", EPOCH.plusSeconds(20))
        );

        Function<PojoWithTime, Instant> timeGetter = PojoWithTime::time;

        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH), is(0));
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(10)), is(1));
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(20)), is(2));
    }

    @Test
    public void binarySearchMissesGiveCorrectIndex_arrayList() {

        ArrayList<PojoWithTime> list = newArrayList(
            new PojoWithTime("a", EPOCH),
            new PojoWithTime("b", EPOCH.plusSeconds(10)),
            new PojoWithTime("c", EPOCH.plusSeconds(20))
        );

        Function<PojoWithTime, Instant> timeGetter = PojoWithTime::time;

        //output int = (-(insertion point) -1)
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.minusSeconds(1)), is(-1));  //insert before item 0
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(1)), is(-2)); //insert between 0 and 1
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(11)), is(-3)); //insert between 1 and 2
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(21)), is(-4)); //insert after 2
    }


    @Test
    public void binarySearchMatchesGiveCorrectIndex_linkedList() {

        LinkedList<PojoWithTime> list = newLinkedList();
        list.add(new PojoWithTime("a", EPOCH));
        list.add(new PojoWithTime("b", EPOCH.plusSeconds(10)));
        list.add(new PojoWithTime("c", EPOCH.plusSeconds(20)));

        Function<PojoWithTime, Instant> timeGetter = PojoWithTime::time;

        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH), is(0));
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(10)), is(1));
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(20)), is(2));
    }

    @Test
    public void binarySearchMissesGiveCorrectIndex_linkedList() {

        LinkedList<PojoWithTime> list = newLinkedList();
        list.add(new PojoWithTime("a", EPOCH));
        list.add(new PojoWithTime("b", EPOCH.plusSeconds(10)));
        list.add(new PojoWithTime("c", EPOCH.plusSeconds(20)));

        Function<PojoWithTime, Instant> timeGetter = PojoWithTime::time;

        //output int = (-(insertion point) -1)
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.minusSeconds(1)), is(-1));  //insert before item 0
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(1)), is(-2)); //insert between 0 and 1
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(11)), is(-3)); //insert between 1 and 2
        assertThat(CollectionUtils.binarySearch(list, timeGetter, EPOCH.plusSeconds(21)), is(-4)); //insert after 2
    }


    @Test
    public void zip_onPairs_happyPath() {
        List<String> text = newArrayList("one", "two", "three");
        List<Integer> numbers = newArrayList(1, 2, 3);

        Iterator<Pair<String, Integer>> pairIter = zip(text, numbers);

        Pair<String, Integer> a = pairIter.next();
        assertThat(a.first(), is("one"));
        assertThat(a.second(), is(1));

        Pair<String, Integer> b = pairIter.next();
        assertThat(b.first(), is("two"));
        assertThat(b.second(), is(2));

        Pair<String, Integer> c = pairIter.next();
        assertThat(c.first(), is("three"));
        assertThat(c.second(), is(3));

        assertThat(pairIter.hasNext(), is(false));
    }

    @Test
    public void zip_onPairs_unequalCollectionInputs() {
        List<String> text = newArrayList("one");
        List<Integer> numbers = newArrayList(1, 2, 3);

        Iterator<Pair<String, Integer>> pairIter = zip(text, numbers);

        Pair<String, Integer> a = pairIter.next();
        assertThat(a.first(), is("one"));
        assertThat(a.second(), is(1));

        assertThat(pairIter.hasNext(), is(false));
    }

    @Test
    public void zip_onTriples_happyPath() {
        List<String> text = newArrayList("one", "two", "three");
        List<Integer> numbers = newArrayList(1, 2, 3);
        List<String> moreText = newArrayList("a", "b", "c");

        Iterator<Triple<String, Integer, String>> tripIter = zip(text, numbers, moreText);

        Triple<String, Integer, String> a = tripIter.next();
        assertThat(a.first(), is("one"));
        assertThat(a.second(), is(1));
        assertThat(a.third(), is("a"));

        Triple<String, Integer, String> b = tripIter.next();
        assertThat(b.first(), is("two"));
        assertThat(b.second(), is(2));
        assertThat(b.third(), is("b"));

        Triple<String, Integer, String> c = tripIter.next();
        assertThat(c.first(), is("three"));
        assertThat(c.second(), is(3));
        assertThat(c.third(), is("c"));

        assertThat(tripIter.hasNext(), is(false));
    }

    @Test
    public void zip_onTrips_unequalCollectionInputs() {

        List<String> text = newArrayList("one", "two", "three");
        List<Integer> numbers = newArrayList(1, 2, 3);
        List<String> moreText = newArrayList("a");

        Iterator<Triple<String, Integer, String>> tripIter = zip(text, numbers, moreText);

        Triple<String, Integer, String> a = tripIter.next();
        assertThat(a.first(), is("one"));
        assertThat(a.second(), is(1));
        assertThat(a.third(), is("a"));

        assertThat(tripIter.hasNext(), is(false));
    }

}
