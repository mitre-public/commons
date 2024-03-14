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

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Math.hypot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MetricSetTest {

    @Test
    public void testGetClosest() {

        MetricSet<Point> mTree = metricTreeWithTestData();

        Point searchKey = new Point(0, 0);
        SetSearchResult<Point> result = mTree.getClosest(searchKey);
        DistanceMetric<Point> metric = mTree.metric();

        // confirm all Points in the tree are at least this far away..
        for (Point point : mTree.keySet()) {
            double dist = metric.distanceBtw(searchKey, point);
            assertTrue(dist >= result.distance());
        }
    }

    @Test
    public void testNullKey_getClosest() {

        MetricSet<Point> mTree = emptyMetricSet();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.getClosest(null));
    }

    @Test
    public void testNullKey_getNClosest() {

        MetricSet<Point> mTree = emptyMetricSet();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.getNClosest(null, 5));
    }

    @Test
    public void testNullKey_remove() {

        MetricSet<Point> mTree = emptyMetricSet();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.remove(null));
    }

    @Test
    public void testNullKey_getAllWithinRange() {

        MetricSet<Point> mTree = emptyMetricSet();

        // null keys are not allowed
        assertThrows(IllegalArgumentException.class, () -> mTree.getAllWithinRange(null, 5.0));
    }

    @Test
    public void testNegativeK_getNClosest() {

        MetricSet<Point> mTree = emptyMetricSet();
        Point p = new Point(0, 0);

        // k cannot be negative
        assertThrows(IllegalArgumentException.class, () -> mTree.getNClosest(p, -5));
    }

    @Test
    public void testKEqualsZero_getNClosest() {

        MetricSet<Point> mTree = emptyMetricSet();
        Point p = new Point(0, 0);

        // k cannot be 0
        assertThrows(IllegalArgumentException.class, () -> mTree.getNClosest(p, 0));
    }

    @Test
    public void testNegativeRange() {

        MetricSet<Point> mTree = emptyMetricSet();
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
    public void testSearchesOnEmpty() {
        MetricSet<Point> mTree = emptyMetricSet();
        Point searchKey = new Point(0, 0);

        Collection<SetSearchResult<Point>> knnResults = mTree.getNClosest(searchKey, 4);

        assertNotNull(knnResults);
        assertTrue(knnResults.isEmpty());

        Collection<SetSearchResult<Point>> rangeResults = mTree.getAllWithinRange(searchKey, 10.0);

        assertNotNull(rangeResults);
        assertTrue(rangeResults.isEmpty());
    }

    @Test
    public void removingAnElementReturnsCorrectBoolean() {

        MetricSet<Point> mTree = emptyMetricSet();
        Point searchKey = new Point(0, 0);
        Point aDifferentKey = new Point(1, 1);

        mTree.add(searchKey);

        assertThat(mTree.remove(aDifferentKey), is(false));
        assertThat(mTree.remove(searchKey), is(true));
    }

    public MetricSet<Point> emptyMetricSet() {
        return new MetricSet<>(new DistanceMetric<Point>() {

            @Override
            public double distanceBtw(Point item1, Point item2) {
                return Math.hypot(Math.abs(item1.getX() - item2.getX()), Math.abs(item1.getY() - item2.getY()));
            }
        });
    }

    /** @return A MetricSet containing a fixed collection of data. */
    private MetricSet<Point> metricTreeWithTestData() {

        MetricSet<Point> set = emptyMetricSet();
        set.addAll(testSet());

        return set;
    }

    private static final int SIZE_OF_TEST_COLLECTION = 3000;

    /**
     * @return - A standard Map containing a fixed collection of test data.
     */
    private Set<Point> testSet() {

        Set<Point> hashSet = newHashSet();

        List<Point> points = points();

        for (int i = 0; i < SIZE_OF_TEST_COLLECTION; i++) {
            hashSet.add(points.get(i));
        }

        return hashSet;
    }

    private List<Point> points() {

        Set<Point> points = new HashSet<>(SIZE_OF_TEST_COLLECTION);

        Random rng = new Random(17L);

        while (points.size() < SIZE_OF_TEST_COLLECTION) {
            points.add(new Point(rng.nextInt(1000), rng.nextInt(1000)));
        }

        return new ArrayList<>(points);
    }

    @Test
    public void constructorRequiresADistanceMetric() {

        // Should not be able to build a MetricSet with a null DistanceMetric
        assertThrows(NullPointerException.class, () -> new MetricSet<>(null));
    }

    @Test
    public void addingNullIsNotAllowed() {
        MetricSet<Point> tree = emptyMetricSet();

        // Should not be able to put using \"null\" as a key
        assertThrows(NullPointerException.class, () -> tree.add(null));
    }

    @Test
    public void testDuplicateAdds() {
        MetricSet<Point> tree = emptyMetricSet();

        boolean beforeAnyAdds = tree.add(new Point(0, 0));
        assertThat(beforeAnyAdds, is(true));

        boolean afterDuplicateAdds = tree.add(new Point(0, 0));
        assertThat(afterDuplicateAdds, is(false));

        boolean afterSecondDuplicateAdd = tree.add(new Point(0, 0));
        assertThat(afterSecondDuplicateAdd, is(false));

        assertThat(tree.size(), is(1));
    }

    @Test
    public void testDifferentPutInput() {

        MetricSet<Point> tree = emptyMetricSet();

        tree.add(new Point(1, 0));
        tree.add(new Point(2, 0));
        tree.add(new Point(3, 0));

        assertThat(tree.size(), is(3));
    }

    @Test
    public void testIsEmpty() {

        MetricSet<Point> tree = emptyMetricSet();

        assertThat(tree.isEmpty(), is(true));

        tree.add(new Point(1, 0));

        assertThat(tree.isEmpty(), is(false));
    }

    @Test
    public void testClear() {
        MetricSet<Point> tree = metricTreeWithTestData();

        assertThat(tree.size(), is(SIZE_OF_TEST_COLLECTION));

        tree.clear();

        assertThat(tree.size(), is(0));
        assertThat(tree.isEmpty(), is(true));
    }

    @Test
    public void testContainsKeyAndRemove() {

        MetricSet<Point> tree = metricTreeWithTestData();

        Set<Point> set = testSet();

        for (Point point : set) {

            // "The test tree should contain this key"
            assertThat(tree.contains(point), is(true));

            boolean hadImpact = tree.remove(point);

            assertThat(hadImpact, is(true));
            assertThat(tree.contains(point), is(false));
        }
    }

    @Test
    public void testEntrySet() {
        MetricSet<Point> testTree = metricTreeWithTestData();

        confirmExactMatchWithTestData(testTree);
    }

    @Test
    public void testMakeBalancedCopy() {
        MetricSet<Point> testTree = metricTreeWithTestData();

        confirmExactMatchWithTestData(testTree.makeBalancedCopy());
    }

    @Test
    public void testRebalance() {

        MetricSet<Point> testSet = metricTreeWithTestData();

        testSet.rebalance();

        confirmExactMatchWithTestData(testSet);
    }

    private void confirmExactMatchWithTestData(MetricSet<Point> metricSet) {
        Set<Point> set = testSet();

        for (Point point : set) {

            assertThat("the testTree should contain all keys from the testMap", metricSet.contains(point), is(true));

            boolean hadImpact = metricSet.remove(point);

            assertThat("the testTree contents should match the testMap contents", hadImpact, is(true));

            assertThat(metricSet.contains(point), is(false));
        }

        assertThat("The testTree should be empty by now", metricSet.isEmpty(), is(true));
    }

    @Test
    public void testSearchResultAccuracy() {

        MetricSet<Point> testTree = metricTreeWithTestData();
        Set<Point> testData = testSet();
        Point testKey = new Point(1, 2);

        assertThat(
                "The input tree, and data it should contain, must have the same size",
                testTree.size(),
                is(testData.size()));

        ArrayList<SetSearchResult<Point>> allResults = exhautivelySearch(testTree, testData, testKey);

        int N = 100;
        List<SetSearchResult<Point>> kNNSearchResults = testTree.getNClosest(testKey, N);
        assertEquals(N, kNNSearchResults.size());
        verifySearchResults(allResults, kNNSearchResults, 1000);

        double MAX_DIST = 50;
        List<SetSearchResult<Point>> rangeSearchResults = testTree.getAllWithinRange(testKey, MAX_DIST);
        verifySearchResults(allResults, rangeSearchResults, MAX_DIST);
    }

    private <K> ArrayList<SetSearchResult<K>> exhautivelySearch(MetricSet<K> testTree, Set<K> testData, K testKey) {

        ArrayList<SetSearchResult<K>> results = new ArrayList<>(testTree.size());

        DistanceMetric<K> metric = testTree.metric();

        for (K key : testData) {
            results.add(new SetSearchResult<>(key, metric.distanceBtw(key, testKey)));
        }

        Collections.sort(results);

        return results;
    }

    private <K> void verifySearchResults(
            ArrayList<SetSearchResult<K>> allResults, List<SetSearchResult<K>> searchResults, double maxDist) {

        // confirm everything in "search results" is within "all results"
        for (SetSearchResult<K> searchResult : searchResults) {
            assertTrue(allResults.contains(searchResult));
        }

        // confirm everything in "search results" has a distance less than "maxDistance"
        for (SetSearchResult<K> searchResult : searchResults) {
            assertTrue(searchResult.distance() <= maxDist);
        }

        // compute the furthest item in the search results
        double maxSearchDist = 0;
        for (SetSearchResult<K> result : searchResults) {
            maxSearchDist = Math.max(maxSearchDist, result.distance());
        }

        // find all the results that were excluded from "search results"
        ArrayList<SetSearchResult<K>> excluded = new ArrayList<>(allResults);
        excluded.removeIf((result) -> searchResults.contains(result));

        // confirm everything in the "excluded results" is further away
        for (SetSearchResult<K> result : excluded) {
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

        MetricSet<Key> set = new MetricSet<>(metric);

        int n = 500;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                set.add(new Key(i, j));
            }
        }

        List<SetSearchResult<Key>> inRange = set.getAllWithinRange(new Key(1_000, 1_000), 20);

        assertThat(inRange, hasSize(n * n));
    }
}
