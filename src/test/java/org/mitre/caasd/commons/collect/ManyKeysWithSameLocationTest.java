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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * The Bug verified here occurs when a Metric Tree contains too many Keys whose distance from each
 * other is zero.
 * <p>
 * When this occurs a MetricTree can trigger and StackOverflowException because when a leaf node
 * (i.e. a SPHERE_OF_POINTS) gets split ALL of the items in the old leaf node get pushed down to the
 * same (new) leaf node. That new leaf node will then need to be split. etc. etc.
 */
public class ManyKeysWithSameLocationTest {

    Random rng = new Random(17L);

    DistanceMetric<Location> metric = new DistanceMetric<Location>() {
        @Override
        public double distanceBtw(Location item1, Location item2) {
            return Math.hypot(
                Math.abs(item1.x - item2.x),
                Math.abs(item1.y - item2.y)
            );
        }
    };

    int itemCount = 0;

    @Test
    public void testTooManyIdenticalKeys() {

        //PHASE 1 -- create a tree with some inital data
        MetricTree<Location, String> tree = emptyMetricTree();
        HashMap<Location, String> copyOfTreeContent = newHashMap();

        int INITIAL_SIZE = 1000;

        for (int i = 0; i < INITIAL_SIZE; i++) {
            Map.Entry<Location, String> entry = createEntry();
            tree.put(entry.getKey(), entry.getValue());
            copyOfTreeContent.put(entry.getKey(), entry.getValue());
        }

        //confirm the tree is populated..
        assertThat(tree.size(), is(INITIAL_SIZE));

        //PHASE 2 -- Break this tree by adding a bunch of items at coordinate (10,10);
        int NUM_COPIES = 500;
        for (int i = 0; i < NUM_COPIES; i++) {
            Map.Entry<Location, String> entry = createEntryAt(10, 10);
            tree.put(entry.getKey(), entry.getValue());
            copyOfTreeContent.put(entry.getKey(), entry.getValue());
        }

        confirmEqualContent(tree, copyOfTreeContent);
    }

    MetricTree<Location, String> emptyMetricTree() {
        return new MetricTree(metric);
    }

    private Map.Entry<Location, String> createEntry() {

        return new AbstractMap.SimpleEntry<>(
            new Location(rng.nextInt(1000), rng.nextInt(1000)),
            "item_" + itemCount++
        );
    }

    private Map.Entry<Location, String> createEntryAt(int x, int y) {

        return new AbstractMap.SimpleEntry<>(
            new Location(x, y),
            "item_" + itemCount++
        );
    }

    private void confirmEqualContent(MetricTree<Location, String> tree, HashMap<Location, String> copyOfTreeContent) {

        assertThat(
            "Both the tree, and its copy, should have the same size",
            tree.size(), is(copyOfTreeContent.size())
        );

        SetView<Location> keyIntersection = Sets.intersection(
            tree.keySet(),
            copyOfTreeContent.keySet()
        );

        assertThat(
            "If the intersection between these two sets is the same size as one of the orginal sets then the sets are equal",
            tree.size(), is(keyIntersection.size())
        );

        double range = 15.0;

        for (Location location : copyOfTreeContent.keySet()) {
            List<Map.Entry<Location, String>> entriesInRange = findEntriesInRange(
                location,
                copyOfTreeContent,
                range
            );

            List<SearchResult<Location, String>> treeResults = tree.getAllWithinRange(
                location,
                range
            );

            assertThat(
                "Brute force search in the copy should find the same number as efficient search using the metric tree",
                entriesInRange.size(), is(treeResults.size())
            );
        }
    }

    private List<Map.Entry<Location, String>> findEntriesInRange(Location loc, HashMap<Location, String> copyOfTreeContent, double range) {

        List<Map.Entry<Location, String>> output = newArrayList();

        for (Map.Entry<Location, String> entry : copyOfTreeContent.entrySet()) {
            if (metric.distanceBtw(loc, entry.getKey()) <= range) {
                output.add(entry);
            }
        }

        return output;
    }

    /*
     * We aren't using java.awt.Point for the "Key class" because we want unique keys that have a
     * distance of zero between them. The java.awt.Point class's implementation of equals means that
     * two Points are equal when their distance is zero...we don't want this for the purpose of this
     * test.
     */
    static class Location {

        public final int x;
        public final int y;

        Location(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return this.x + ", " + this.y;
        }
    }
}
