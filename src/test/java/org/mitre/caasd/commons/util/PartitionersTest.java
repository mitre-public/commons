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

package org.mitre.caasd.commons.util;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mitre.caasd.commons.util.Partitioners.*;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class PartitionersTest {

    @Test
    public void splitOnChange_positiveNegative() {

        List<Integer> list = newArrayList(-2, -1, 0, 1, 2, 1, 0, -1, -2);

        List<List<Integer>> result = splitOnChange(list, i -> i >= 0);

        assertThat(result, hasSize(3));
        assertThat(result.get(0), hasSize(2));
        assertThat(result.get(0).get(0), is(-2));
        assertThat(result.get(0).get(1), is(-1));

        assertThat(result.get(1), hasSize(5));
        assertThat(result.get(1).get(0), is(0));
        assertThat(result.get(1).get(1), is(1));
        assertThat(result.get(1).get(2), is(2));
        assertThat(result.get(1).get(3), is(1));
        assertThat(result.get(1).get(4), is(0));

        assertThat(result.get(2), hasSize(2));
        assertThat(result.get(2).get(0), is(-1));
        assertThat(result.get(2).get(1), is(-2));
    }

    @Test
    public void testPartitioners_List1() {
        List<Integer> ls = Arrays.asList(1, 1, 6, 6, 2, 4);

        List<List<Integer>> result = ls.stream().collect(newListCollector(i -> i > 5));

        assertThat(result, hasSize(3));
        assertEquals(1, result.get(0).get(0).intValue());
        assertEquals(6, result.get(1).get(0).intValue());
        assertEquals(2, result.get(2).get(0).intValue());
        assertEquals(4, result.get(2).get(1).intValue());
    }

    @Test
    public void splitOnPairwiseChange_positiveNegative() {

        List<String> list = newArrayList("a", "a", "b", "c", "b", "b");

        List<List<String>> result = splitOnPairwiseChange(list, (l, s2) -> s2.equals(l.get(0)));

        assertThat(result, hasSize(4));
        assertThat(result.get(0), hasSize(2));
        assertThat(result.get(0).get(0), is("a"));
        assertThat(result.get(0).get(1), is("a"));

        assertThat(result.get(1), hasSize(1));
        assertThat(result.get(1).get(0), is("b"));

        assertThat(result.get(2), hasSize(1));
        assertThat(result.get(2).get(0), is("c"));

        assertThat(result.get(3), hasSize(2));
        assertThat(result.get(3).get(0), is("b"));
        assertThat(result.get(3).get(1), is("b"));
    }

    @Test
    public void testPartitioners_List2() {
        List<String> ls = Arrays.asList("a", "a", "b", "c", "b", "b");

        List<List<String>> res = ls.stream().collect(newListCollector((l, s2) -> s2.equals(l.get(0))));

        assertEquals(4, res.size());
        assertEquals("a", res.get(0).get(0));
        assertEquals("b", res.get(1).get(0));
        assertEquals("c", res.get(2).get(0));
        assertEquals("b", res.get(3).get(0));
    }

    @Test
    public void testPartitioners_List3() {
        List<String> ls = Arrays.asList("a", "aa", "aaa", "b", "bb", "bbb");

        List<List<String>> res = ls.stream()
                .collect(newListCollector((s1, s2) -> EditDistance.similarity(s1.get(s1.size() - 1), s2) < 2));

        assertEquals(1, res.size());
        assertEquals(6, res.get(0).size());
    }

    @Test
    public void testPartitioners_Navigable1() {
        TreeSet<Integer> ts = new TreeSet<>(Arrays.asList(2, 4, 5, 7, 8));

        TreeSet<TreeSet<Integer>> res = ts.stream().collect(newTreeSetCollector(i -> i % 2 == 0));

        assertEquals(3, res.size());
        assertEquals(2, res.first().first().intValue());
        assertEquals(4, res.first().last().intValue());
        assertEquals(8, res.last().last().intValue());
    }

    @Test
    public void testPartitioners_Navigable2() {
        TreeSet<Integer> ts = new TreeSet<>(Arrays.asList(1, 2, 4, 5, 8, 9));

        TreeSet<TreeSet<Integer>> res = ts.stream().collect(newTreeSetCollector((x, y) -> y - x.last() < 2));

        assertEquals(3, res.size());
        assertEquals(1, res.first().first().intValue());
        assertEquals(2, res.first().last().intValue());
        assertEquals(8, res.last().first().intValue());
    }

    @Test
    public void testPartitioners_Navigable3() {
        TreeSet<Integer> ts = new TreeSet<>(Arrays.asList(1, 2, 5, 6, 7, 9));

        TreeSet<TreeSet<Integer>> res = ts.stream().collect(newTreeSetCollector((x, y) -> y - x.first() < 5));

        assertEquals(2, res.size());
        assertEquals(1, res.first().first().intValue());
        assertEquals(5, res.first().last().intValue());
        assertEquals(6, res.last().first().intValue());
    }
}
