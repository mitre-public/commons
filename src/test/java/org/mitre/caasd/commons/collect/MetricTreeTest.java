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

package org.mitre.caasd.commons.collect;

import static java.lang.Math.hypot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MetricTreeTest {

    @Test
    public void testGetClosest() {

        MetricTree<Point, String> mTree = metricTreeWithTestData();

        Point searchKey = new Point(0, 0);
        SearchResult<Point, String> result = mTree.getClosest(searchKey);
        DistanceMetric<Point> metric = mTree.metric();

        // confirm all Points in the tree are at least this far away..
        for (Map.Entry<Point, String> entry : mTree.entrySet()) {
            double dist = metric.distanceBtw(searchKey, entry.getKey());
            assertTrue(dist >= result.distance());
        }
    }

    @Test
    public void testNullKey_getClosest() {

        MetricTree<Point, String> mTree = emptyMetricTree();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.getClosest(null));
    }

    @Test
    public void testNullKey_getNClosest() {

        MetricTree<Point, String> mTree = emptyMetricTree();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.getNClosest(null, 5));
    }

    @Test
    public void testNullKey_remove() {

        MetricTree<Point, String> mTree = emptyMetricTree();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.remove(null));
    }

    @Test
    public void testNullKey_getAllWithinRange() {

        MetricTree<Point, String> mTree = emptyMetricTree();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.getAllWithinRange(null, 5.0));
    }

    @Test
    public void testNegativeK_getNClosest() {

        MetricTree<Point, String> mTree = emptyMetricTree();
        Point p = new Point(0, 0);

        // k cannot be negative
        assertThrows(IllegalArgumentException.class, () -> mTree.getNClosest(p, -5));
    }

    @Test
    public void testKEqualsZero_getNClosest() {

        MetricTree<Point, String> mTree = emptyMetricTree();
        Point p = new Point(0, 0);

        // k cannot be 0
        assertThrows(IllegalArgumentException.class, () -> mTree.getNClosest(p, 0));
    }

    @Test
    public void testNegativeRange() {

        MetricTree<Point, String> mTree = emptyMetricTree();
        Point p = new Point(0, 0);

        // range cannot be negative
        assertThrows(IllegalArgumentException.class, () -> mTree.getAllWithinRange(p, -0.1));
    }

    @Test
    public void testTreeConstructionWithTooSmallSpheres() {
        DistanceMetric<Point> metric =
                (Point p1, Point p2) -> hypot(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));

        // We small usable maxSphereSize is 4
        assertThrows(IllegalArgumentException.class, () -> new MetricTree<>(metric, 3));
    }

    @Test
    public void testNullKey_get() {

        MetricTree<Point, String> mTree = emptyMetricTree();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.get(null));
    }

    @Test
    public void testExactGet() {

        Point KEY = new Point(0, 0);
        String VALUE = "hello";

        MetricTree<Point, String> mTree = emptyMetricTree();
        mTree.put(KEY, VALUE);

        assertEquals(VALUE, mTree.get(KEY));
    }

    @Test
    public void testSearchesOnEmpty() {
        MetricTree<Point, String> mTree = emptyMetricTree();
        Point searchKey = new Point(0, 0);

        Collection<SearchResult<Point, String>> knnResults = mTree.getNClosest(searchKey, 4);

        assertThat(knnResults, notNullValue());
        assertTrue(knnResults.isEmpty());

        Collection<SearchResult<Point, String>> rangeResults = mTree.getAllWithinRange(searchKey, 10.0);

        assertThat(rangeResults, notNullValue());
        assertTrue(rangeResults.isEmpty());
    }

    @Test
    public void testRemoveReturnsNull() {

        MetricTree<Point, String> mTree = emptyMetricTree();
        Point searchKey = new Point(0, 0);
        Point aDifferentKey = new Point(1, 1);

        mTree.put(searchKey, "aValue");
        String value = mTree.remove(aDifferentKey);

        assertNull(value);
    }

    public MetricTree<Point, String> emptyMetricTree() {
        return new MetricTree<>(new DistanceMetric<Point>() {

            @Override
            public double distanceBtw(Point item1, Point item2) {
                return Math.hypot(Math.abs(item1.getX() - item2.getX()), Math.abs(item1.getY() - item2.getY()));
            }
        });
    }

    /**
     * @return - A MetricTree containing a fixed collection of data.
     */
    private MetricTree<Point, String> metricTreeWithTestData() {

        MetricTree<Point, String> tree = emptyMetricTree();

        Map<Point, String> data = testMap();

        for (Map.Entry<Point, String> entry : data.entrySet()) {
            tree.put(entry.getKey(), entry.getValue());
        }

        return tree;
    }

    private static final int SIZE_OF_TEST_COLLECTION = 3000;

    /**
     * @return - A standard Map containing a fixed collection of test data.
     */
    private Map<Point, String> testMap() {

        Map<Point, String> hashMap = new HashMap<>();

        List<Point> points = points();
        List<String> strings = strings();

        for (int i = 0; i < SIZE_OF_TEST_COLLECTION; i++) {
            hashMap.put(points.get(i), strings.get(i));
        }

        return hashMap;
    }

    private List<Point> points() {

        Set<Point> points = new HashSet<>(SIZE_OF_TEST_COLLECTION);

        Random rng = new Random(17L);

        while (points.size() < SIZE_OF_TEST_COLLECTION) {
            points.add(new Point(rng.nextInt(1000), rng.nextInt(1000)));
        }

        return new ArrayList<>(points);
    }

    private List<String> strings() {

        List<String> strings = new ArrayList<>(SIZE_OF_TEST_COLLECTION);

        while (strings.size() < SIZE_OF_TEST_COLLECTION) {
            strings.add("item_" + strings.size());
        }

        return strings;
    }

    @Test
    public void testNullConstructorInput() {
        // Should not be able to build a FastMetricTree with a null DistanceMetric
        assertThrows(NullPointerException.class, () -> new MetricTree<>(null));
    }

    @Test
    public void testNullPutInput() {
        MetricTree<Point, String> tree = emptyMetricTree();

        // Should not be able to put using \"null\" as a key
        assertThrows(NullPointerException.class, () -> tree.put(null, "testString"));
    }

    @Test
    public void testDuplicatePutInput() {
        MetricTree<Point, String> tree = emptyMetricTree();

        String beforeAnyPuts = tree.put(new Point(0, 0), "firstItem");
        assertThat("The 1st call to put should return null", beforeAnyPuts, nullValue());

        String shouldBeFirst = tree.put(new Point(0, 0), "secondItem");
        assertThat("Put should return the value being replaced", shouldBeFirst, is("firstItem"));

        String shouldBeSecond = tree.put(new Point(0, 0), "thirdItem");
        assertThat("Put should return the value being replaced", shouldBeSecond, is("secondItem"));

        assertThat("Repeatedly using the same Key should leave only 1 item in the tree:", tree.size(), is(1));

        MetricTree<Point, String> tree2 = metricTreeWithTestData();
        List<Point> points = points();
        List<String> strings = strings();

        for (int i = 0; i < points.size(); i++) {
            String priorString = tree2.put(points.get(i), "overwritten_" + i);

            assertEquals(priorString, strings.get(i));
        }
    }

    @Test
    public void testDifferentPutInput() {

        MetricTree<Point, String> tree = emptyMetricTree();

        tree.put(new Point(1, 0), "firstItem");
        tree.put(new Point(2, 0), "secondItem");
        tree.put(new Point(3, 0), "thirdItem");

        assertThat("Using different Keys should leave only 3 items in the tree:", tree.size(), is(3));
    }

    @Test
    public void testIsEmpty() {

        MetricTree<Point, String> tree = emptyMetricTree();

        assertThat("A new FastMetricTree should be empty", tree.isEmpty(), is(true));

        tree.put(new Point(1, 0), "firstItem");

        assertThat("After adding an item the tree should not be empty", tree.isEmpty(), is(false));
    }

    @Test
    public void testClear() {
        MetricTree<Point, String> tree = metricTreeWithTestData();

        assertEquals(SIZE_OF_TEST_COLLECTION, tree.size());

        tree.clear();

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
    }

    @Test
    public void testContainsKeyAndRemove() {

        MetricTree<Point, String> tree = metricTreeWithTestData();

        Map<Point, String> map = testMap();

        Random rng = new Random(17L);

        for (Map.Entry<Point, String> entry : map.entrySet()) {

            assertThat("The test tree should contain this key", tree.containsKey(entry.getKey()), is(true));

            if (rng.nextBoolean()) {
                assertThat("The test tree should contain this key", tree.containsKey(entry.getKey()), is(true));
            } else {
                String prior = tree.remove(entry.getKey());

                assertThat(
                        "The test tree should no longer contain this key", tree.containsKey(entry.getKey()), is(false));

                assertEquals(prior, entry.getValue());
            }
        }
    }

    @Test
    public void testEntrySet() {

        MetricTree<Point, String> testTree = metricTreeWithTestData();

        confirmExactMatchWithTestData(testTree);
    }

    @Test
    public void testMakeBalancedCopy() {
        MetricTree<Point, String> testTree = metricTreeWithTestData();

        confirmExactMatchWithTestData(testTree.makeBalancedCopy());
    }

    @Test
    public void testRebalance() {

        MetricTree<Point, String> testTree = metricTreeWithTestData();

        testTree.rebalance();

        confirmExactMatchWithTestData(testTree);
    }

    private void confirmExactMatchWithTestData(MetricTree<Point, String> testTree) {
        Map<Point, String> map = testMap();

        for (Map.Entry<Point, String> entry : map.entrySet()) {

            assertThat(
                    "the testTree should contain all keys from the testMap",
                    testTree.containsKey(entry.getKey()),
                    is(true));

            assertThat(
                    "the testTree contents should match the testMap contents",
                    testTree.get(entry.getKey()),
                    is(entry.getValue()));

            String prior = testTree.remove(entry.getKey());

            assertThat("the testTree contents should match the testMap contents", entry.getValue(), is(prior));

            assertThat(
                    "The testTree should no longer contain this key", testTree.containsKey(entry.getKey()), is(false));
        }

        assertThat("The testTree should be empty by now", testTree.isEmpty(), is(true));
    }

    @Test
    public void testSearchResultAccuracy() {

        MetricTree<Point, String> testTree = metricTreeWithTestData();
        Map<Point, String> testData = testMap();
        Point testKey = new Point(1, 2);

        assertThat(
                "The input tree, and data it should contain, must have the same size",
                testTree.size(),
                is(testData.size()));

        ArrayList<SearchResult<Point, String>> allResults = exhautivelySearch(testTree, testData, testKey);

        int N = 100;
        List<SearchResult<Point, String>> kNNSearchResults = testTree.getNClosest(testKey, N);
        assertEquals(N, kNNSearchResults.size());
        verifySearchResults(allResults, kNNSearchResults, 1000);

        double MAX_DIST = 50;
        List<SearchResult<Point, String>> rangeSearchResults = testTree.getAllWithinRange(testKey, MAX_DIST);
        verifySearchResults(allResults, rangeSearchResults, MAX_DIST);
    }

    private <V, K> ArrayList<SearchResult<K, V>> exhautivelySearch(
            MetricTree<K, V> testTree, Map<K, V> testData, K testKey) {
        ArrayList<SearchResult<K, V>> results = new ArrayList<>(testTree.size());

        DistanceMetric<K> metric = testTree.metric();

        for (Map.Entry<K, V> entry : testData.entrySet()) {
            results.add(
                    new SearchResult<>(entry.getKey(), entry.getValue(), metric.distanceBtw(entry.getKey(), testKey)));
        }

        Collections.sort(results);

        return results;
    }

    private <K, V> void verifySearchResults(
            ArrayList<SearchResult<K, V>> allResults, List<SearchResult<K, V>> searchResults, double maxDist) {

        // confirm everything in "search results" is within "all results"
        for (SearchResult<K, V> searchResult : searchResults) {
            assertTrue(allResults.contains(searchResult));
        }

        // confirm everything in "search results" has a distance less than "maxDistance"
        for (SearchResult<K, V> searchResult : searchResults) {
            assertTrue(searchResult.distance() <= maxDist);
        }

        // compute the furthest item in the search results
        double maxSearchDist = 0;
        for (SearchResult<K, V> result : searchResults) {
            maxSearchDist = Math.max(maxSearchDist, result.distance());
        }

        // find all the results that were excluded from "search results"
        ArrayList<SearchResult<K, V>> excluded = new ArrayList<>(allResults);
        excluded.removeIf(result -> searchResults.contains(result));

        // confirm everything in the "excluded results" is further away
        for (SearchResult<K, V> result : excluded) {
            assertTrue(result.distance() > maxSearchDist);
        }
    }

    @Disabled // because building a MetricSet with 250_000 entries takes too long (about 42 seconds)
    @Test
    public void cannotOverloadWithZeroDistanceKeys() {

        class Key {

            final int x;
            final int y;

            Key(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }

        // This distance metric sucks...but it shouldn't break the data structure
        DistanceMetric<Key> metric = (Key item1, Key item2) -> 1;

        MetricTree<Key, Integer> set = new MetricTree<>(metric);

        int n = 500;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                set.put(new Key(i, j), i * n + j);
            }
        }

        List<SearchResult<Key, Integer>> inRange = set.getAllWithinRange(new Key(1_000, 1_000), 20);

        assertThat(inRange, hasSize(n * n));
    }
}
