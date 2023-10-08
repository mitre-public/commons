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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.mitre.caasd.commons.collect.CenterPointSelectors.maxOfRandomSamples;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mitre.caasd.commons.Pair;

import com.google.common.collect.Lists;

/**
 * A MetricTree is a Map-like data-structure designed to efficiently support k-nearest-neighbor
 * (kNN) searches, range searches, as well as regular Map style put/get operations. To support kNN
 * searches a MetricTree requires a DistanceMetric that defines the distance between any two keys.
 * The DistanceMetric should define a Metric Space (in the strict algebraic sense) in which the
 * following four assumptions are true:
 * <p>
 * {@code
 *    (1) d(x,y) >= 0
 *    (2) d(x,y) = d(y,x)
 *    (3) d(x,z) <= d(x,y) + d(y,z)
 *    (4) d(x , y ) = 0 if and only
 *    if x = y (optional)
 * }
 * <p>
 * A MetricTree is loosely based on the MTree introduced by Paolo Ciaccia, Marco Patella, and Pavel
 * Zezula in 1997 in the paper "M-tree An Efficient Access Method for Similarity Search in Metric
 * Spaces" from the Proceedings of the 23rd VLDB Conference.
 * <p>
 * A MetricTree is a binary search tree in which each node in the tree owns a multi-dimensional
 * sphere. That sphere's radius is large enough to ensure that every Key beneath that node is
 * located inside that node's sphere. Therefore, the root node of an MetricTree has a large radius
 * because every Key contained in the MetricTree must fit inside its sphere. Sub-trees are
 * associated with smaller spheres. Each sphere has a centerpoint and radius whose values are used
 * to route put/get requests, kNN searches, and range searches.
 * <p>
 * When Keys are first added to a MetricTree they are placed inside a "Sphere of Points".
 * Eventually, that Sphere will need to be split because it will have too many entries to search
 * quickly. When this occurs the original "Sphere of Points" becomes a "Sphere of Spheres" that owns
 * 2 newly created "Sphere of Points". Together the 2 new "Sphere of Points" will contain all the
 * Key+Value pairs that the original overfilled "Sphere of Points" contained. The centerpoints of
 * the 2 new "Sphere of Points" are selected to reduce the overlapping volume between the 2 new
 * spheres. Reducing this shared volume reduces the number of spheres a search must visit.
 * <p>
 * When Key+Value pairs are removed from a MetricTree they are removed, however, the fact that the
 * key was present in the MetricTree may leave a permanent imprint on the MetricTree. This occurs
 * when the Key was selected as the centerpoint for a "Sphere of Points". In this case the Key is
 * still used to route get/put queries even though the Key is no longer associated with a Key+Value
 * pair. Any insertion of a Key can permanently reduce the query routing efficency of an MetricTree.
 * This occurs when the key insertion forces a Sphere to increase its radius (which will not shrink
 * upon key removal).
 * <p>
 * It is important to know that MetricTrees have no automatic balancing mechanism. Therefore
 * inserting Key+Value pairs where the Keys vary in some predictable way is likely to produce a tree
 * that is unbalanced. In principal, tree balance could be maintained by rotating the Sphere nodes
 * similar to how AVL and Red-Black trees rotate nodes to maintain balance. The makeBalancedCopy()
 * and rebalance() methods are provided to combat tree imbalance, but these method are expensive and
 * should be used sparingly if possible. These methods rebuild the tree from scratch by adding the
 * existing Key + Value pairs (which may exhibit some amount of imbalance) back into the tree in a
 * random order (which will limit the tree's imbalance IMMEDIATELY AFTER the rebuild).
 * <p>
 * A MetricTree is "fast" because the Binary Tree structure discussed above is paired with a
 * standard HashMap that contains all of the Key + Value Pairs. This means replacing the value
 * associated with a "preexisting" key, removing a Key + Value pair, and getting a key does not
 * require a tree traversal or computing the DistanceMetric.
 *
 * @param <K> The Keys, these keys are stored in HashMaps, so their hashcode() and equals() methods
 *            must be defined correctly.
 * @param <V> The Values
 */
public class MetricTree<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_SPHERE_SIZE = 50;

    /** This controls how SPHERE_OF_POINTS are split. */
    private final CenterPointSelector<K> centerPointSelector;

    /** The distance metric governing the space of Keys (K). */
    private final DistanceMetric<K> metric;

    /** The root of this tree.  */
    private Sphere rootSphere;

    /** Used to judge the efficiency of different centerPointSelectors. */
    private int sphereCount = 0;

    /** How many items a "Sphere of Points" can contain until that Sphere needs to be split. */
    private final int MAX_INNER_SPHERE_SIZE;

    /**
     * The globalMap is used to quickly replace values associated with preexisting Keys and remove
     * values associated with preexisting keys. Basically, speed up "getClosest(K key)" and "put(K
     * key, V value)" calls when the key has already "in" the dataset.
     */
    private HashMap<K, SphereAssignment<K, V>> globalHashMap = new HashMap<>();

    /**
     * Create a new MetricTree that uses the default values for the maxSphereSize and
     * centerPointSelector.
     *
     * @param metric A DistanceMetric that can measure the distance between two Keys.
     */
    public MetricTree(DistanceMetric<K> metric) {
        this(metric, DEFAULT_SPHERE_SIZE);
    }

    public MetricTree(DistanceMetric<K> metric, int maxSphereSize) {
        this(metric, maxSphereSize, maxOfRandomSamples());
    }

    public MetricTree(DistanceMetric<K> metric, int maxSphereSize, CenterPointSelector<K> selector) {
        checkNotNull(metric, "The input DistanceMetric cannot be null");
        checkArgument(maxSphereSize >= 4, "The maxSphereSize must be at least 4, it was: " + maxSphereSize);
        checkNotNull(selector, "The CenterPointSelector cannot be null");

        this.metric = metric;
        this.centerPointSelector = selector;
        this.MAX_INNER_SPHERE_SIZE = maxSphereSize;
    }

    public final DistanceMetric<K> metric() {
        return this.metric;
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously
     * contained a mapping for the key, the old value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with key, or null if there was no prior mapping for the
     *     key. (A null return can also indicate that the map previously associated null with key.)
     */
    public V put(K key, V value) {

        if (key == null) {
            throw new NullPointerException("Null Keys are not permited because they cannot be "
                + "placed in the metric space");
        }

        //delay building root until now because we don't have a key for the centerPoint until
        //the first use of put(K key)
        if (this.rootSphere == null) {
            this.rootSphere = new Sphere(key);
        }

        //do a "short-circuit" put when our global HashMap has already seen the Key provided
        if (globalHashMap.containsKey(key)) {
            SphereAssignment<K, V> sa = globalHashMap.get(key);
            sa.sphere().entries.put(key, value); //update the "local" entry
            V prior = sa.updateValue(value); //update the "global" entry
            return prior;
        }

        V prior = rootSphere.put(key, value);

        assert (prior == null) : "The prior should always be null because globalMap.containsKey was false";

        return prior;
    }

    /**
     * Convenience call put(entry.getKey(), entry.getValue())
     *
     * @param map A Map of valid input.
     */
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @return The number of entries in this Map.
     */
    public int size() {
        return this.globalHashMap.size();
    }

    public boolean isEmpty() {
        return this.globalHashMap.isEmpty();
    }

    public boolean containsKey(K key) {
        return this.globalHashMap.containsKey(key);
    }

    /**
     * Search for an exact key match.
     *
     * @param exactKey The exact key being searched for. Null is not permitted.
     *
     * @return The value paired with the exact key or null.
     */
    public V get(K exactKey) {
        checkArgument(nonNull(exactKey));

        SphereAssignment<K, V> sa = globalHashMap.get(exactKey);

        return (sa != null)
            ? sa.value()
            : null;
    }

    /**
     * Perform a kNN search where k = 1.
     *
     * @param searchKey The point-in-space from which the closest entry is found
     *
     * @return The Key/Value Result with the minimum distance to the search key
     */
    public SearchResult<K, V> getClosest(K searchKey) {

        if (globalHashMap.containsKey(searchKey)) {
            SphereAssignment<K, V> sa = globalHashMap.get(searchKey);
            return new SearchResult<>(searchKey, sa.value(), 0.0); //distance must be zero because we have an exact key match
        }

        Collection<SearchResult<K, V>> results = getNClosest(searchKey, 1);

        ArrayList<SearchResult<K, V>> list = new ArrayList<>(results);
        return list.get(0);
    }

    /**
     * Perform a kNN search with arbitrary k.
     *
     * @param searchKey The point-in-space from which the closest entries are found
     * @param n         The number of entries to search for
     *
     * @return A collection of n Key/Value Results with the smallest distances to the search key
     */
    public List<SearchResult<K, V>> getNClosest(K searchKey, int n) {

        checkArgument(nonNull(searchKey));

        if (n < 1) {
            throw new IllegalArgumentException("n must be at least 1");
        }

        //nothing to retrieve...
        if (this.isEmpty()) {
            return Collections.emptyList();
        }

        Search<K, V> q = new Search<>(searchKey, n, metric);
        q.startQuery(rootSphere);

        ArrayList<SearchResult<K, V>> list = new ArrayList<>(q.results());
        Collections.sort(list);

        return list;
    }

    /**
     * @param searchKey The point-in-space from which the closest entries are found
     * @param range     The distance below which all entries are included in the output.
     *
     * @return A Result for all keys within this range of the key.
     */
    public List<SearchResult<K, V>> getAllWithinRange(K searchKey, double range) {

        checkArgument(nonNull(searchKey));

        if (range <= 0) {
            throw new IllegalArgumentException("The range must be strictly positive " + range);
        }

        //nothing to retrieve...
        if (this.isEmpty()) {
            return Collections.emptyList();
        }

        Search<K, V> q = new Search<>(searchKey, metric, range);
        q.startQuery(rootSphere);

        ArrayList<SearchResult<K, V>> list = new ArrayList<>(q.results());
        Collections.sort(list);

        return list;
    }

    /**
     * Remove the value associated with a particular Key. IMPORTANT: A reference to a Key may remain
     * if that Key was selected as a "routing" Key. This will not have an effect on the correct
     * operation of this data structure, but it may cause some Keys to remain ineligible for garbage
     * collection.
     *
     * @param exactKey Cannot be null
     *
     * @return The Value paired with the exact key (or null if no match is found).
     */
    public V remove(K exactKey) {

        checkArgument(nonNull(exactKey));

        SphereAssignment<K, V> sa = globalHashMap.remove(exactKey);

        if (sa != null) {
            V priorValue = sa.sphere().remove(exactKey);

            if (priorValue != sa.value()) {
                throw new AssertionError("the value found in the globalMap should match "
                    + "the value found in the tree structure");
            }

            return priorValue;
        } else {
            return null;
        }
    }

    public void clear() {
        this.rootSphere = null;
        this.globalHashMap = new HashMap<>();
        this.sphereCount = 0;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return rootSphere.entrySet();
    }

    public Set<K> keySet() {
        return this.globalHashMap.keySet();
    }

    /**
     * @return The number of different spheres used to contain this data. This number can be used
     *     to: (1) evaluate the quality of the CenterPointSelector being used and (2) detect when
     *     the tree is unbalanced.
     */
    public int sphereCount() {
        return this.sphereCount;
    }

    /**
     * Build an entirely new version of this MetricTree. The newly built MetricTree should be
     * relatively well balanced because the Key+Value pairs from "this" MetricTree are inserted into
     * the "new" MetricTree in random order.
     *
     * @return A new version of this MetricTree that should be well balanced
     */
    public MetricTree<K, V> makeBalancedCopy() {

        List<Map.Entry<K, V>> listOfEntries = Lists.newArrayList(this.entrySet());

        Collections.shuffle(listOfEntries);

        MetricTree<K, V> newMap = new MetricTree<>(metric);
        for (Map.Entry<K, V> entry : listOfEntries) {
            newMap.put(entry.getKey(), entry.getValue());
        }

        if (this.size() != newMap.size()) {
            throw new AssertionError("The rebalancing process changed the number of entries");
        }

        return newMap;
    }

    /**
     * Rebuild this MetricTree using makeBalancedCopy().
     */
    public void rebalance() {

        MetricTree<K, V> newMap = makeBalancedCopy();
        this.rootSphere = newMap.rootSphere;
        this.globalHashMap = newMap.globalHashMap;
        this.sphereCount = newMap.sphereCount;
    }

    private enum SphereState {

        /**
         * A SPHERE_OF_POINTS contain a HashMap of key, value pairs. A SPHERE_OF_POINTS is
         * essentially a leaf node of a FastMetricTree
         */
        SPHERE_OF_POINTS,
        /**
         * A SPHERE_OF_SPHERES contains 2 other spheres. A SPHERE_OF_SPHERES is essentially an inner
         * node of a FastMetricTree.
         */
        SPHERE_OF_SPHERES
    }

    /**
     * A Sphere represents a Spherical Region in the Metric Space defined by the distance metric
     * provided during construction. The radius of a sphere is increased so that it always all
     * Spheres contain every Key that can be found either directly inside it or inside one of its
     * child Sphere.
     */
    class Sphere implements Serializable {

        final K centerPoint;

        private double radius;

        private SphereState type;

        /**
         * This Map is used when SphereType == SPHERE_OF_POINTS.
         */
        private Map<K, V> entries;

        /**
         * This pair is used when SphereType == SPHERE_OF_SPHERES.
         */
        private Pair<Sphere, Sphere> childSpheres;

        /**
         * Create a SphereOfPoints centered around this key.
         */
        Sphere(K key) {
            this.type = SphereState.SPHERE_OF_POINTS;
            this.centerPoint = key;
            this.entries = new HashMap<>();
            this.childSpheres = null;
            sphereCount++;
        }

        double radius() {
            return this.radius;
        }

        boolean isSphereOfPoints() {
            return this.type == SphereState.SPHERE_OF_POINTS;
        }

        boolean isSphereOfSpheres() {
            return this.type == SphereState.SPHERE_OF_SPHERES;
        }

        Set<Map.Entry<K, V>> points() {
            return entries.entrySet();
        }

        Pair<Sphere, Sphere> children() {
            return childSpheres;
        }

        V put(K key, V value) {

            if (isFull()) {
                split();
            }

            //update radius if necessary
            this.radius = Math.max(
                radius,
                verifiedDistance(this.centerPoint, key));

            if (isSphereOfPoints()) {
                globalHashMap.put(key, new SphereAssignment<>(this, value));
                return this.entries.put(key, value);
            } else if (isSphereOfSpheres()) {
                Sphere child = findClosestChildSphere(key);
                return child.put(key, value);
            } else {
                throw new AssertionError("Should never get here, all SphereTypes covered");
            }
        }

        private void split() {
            Pair<Sphere, Sphere> newNodes = this.splitSphereOfPoints();

            //"promote" this SPHERE_OF_POINTS to a SPHERE_OF_SPHERES
            //null out the list of entries
            //use the a pair of Spheres in its place
            this.type = SphereState.SPHERE_OF_SPHERES;
            this.entries = null;
            this.childSpheres = newNodes;
        }

        V remove(K key) {
            if (this.isSphereOfPoints()) {
                return this.entries.remove(key);
            } else {
                throw new AssertionError(
                    "Should never get here.  "
                        + "This should only be called on \"Sphere of Points\"");
            }
        }

        private boolean isFull() {
            return (this.type == SphereState.SPHERE_OF_POINTS) && (this.entries.size() >= MAX_INNER_SPHERE_SIZE);
        }

        /**
         * @return - The child whose centerPoint is closest to the piece of data we are inserting.
         */
        private Sphere findClosestChildSphere(K key) {

            double firstDist = verifiedDistance(key, this.childSpheres.first().centerPoint);
            double secondDist = verifiedDistance(key, this.childSpheres.second().centerPoint);

            if (firstDist < secondDist) {
                return childSpheres.first();
            } else {
                return childSpheres.second();
            }
        }

        /**
         * Split a SphereOfPoints into two SphereOfPoints.
         */
        private Pair<Sphere, Sphere> splitSphereOfPoints() {

            if (this.type != SphereState.SPHERE_OF_POINTS) {
                throw new IllegalStateException("Only SPHERE_OF_POINTS should be split");
            }

            Pair<K, K> centers = pickCentersForNewSpheres();

            Sphere part1 = new Sphere(centers.first());
            Sphere part2 = new Sphere(centers.second());

            moveEntriesToChildren(part1, part2);

            return new Pair<>(part1, part2);
        }

        private Pair<K, K> pickCentersForNewSpheres() {
            return centerPointSelector.selectNewCenterPoints(
                new ArrayList<>(entries.keySet()),
                metric);
        }

        /**
         * Move the entries from this Sphere to the new Sphere.
         */
        private void moveEntriesToChildren(Sphere part1, Sphere part2) {
            //push the contents of this.children to either part1 or part2

            boolean tieBreaker = false;
            for (Map.Entry<K, V> entry : entries.entrySet()) {
                addToBestOf(part1, part2, entry, tieBreaker);
                tieBreaker = !tieBreaker; //alternate the tiebreaker
            }
        }

        /**
         * Add a Key-Value pair from a parent node to one of the child nodes
         *
         * @param node1      The left child
         * @param node2      The right child
         * @param entry      The entry that needs to be placed in one of the child nodes
         * @param tieBreaker A tiebreaker to use in the event that an entry is equidistant from both
         *                   child nodes. This is important if many keys in the tree have the same
         *                   location. Without the tiebreaker behavior a StackOverflow can occur.
         */
        private void addToBestOf(Sphere node1, Sphere node2, Map.Entry<K, V> entry, boolean tieBreaker) {

            double distanceTo1 = verifiedDistance(entry.getKey(), node1.centerPoint);
            double distanceTo2 = verifiedDistance(entry.getKey(), node2.centerPoint);

            Sphere bestSphere = null;

            if (distanceTo1 == distanceTo2) {
                //use the tiebreaker when distances are equal
                bestSphere = (tieBreaker) ? node1 : node2;
            } else if (distanceTo1 < distanceTo2) {
                bestSphere = node1;
            } else {
                bestSphere = node2;
            }

            bestSphere.put(entry.getKey(), entry.getValue());
        }

        Set<Map.Entry<K, V>> entrySet() {

            if (this.isSphereOfPoints()) {
                return this.entries.entrySet();
            }

            if (this.isSphereOfSpheres()) {

                HashSet<Map.Entry<K, V>> set = new HashSet<>();
                set.addAll(childSpheres.first().entrySet());
                set.addAll(childSpheres.second().entrySet());

                return set;
            }

            throw new AssertionError("Should never get here, all SphereTypes covered");
        }
    }

    /**
     * A SphereAssignment contains a reference to a VALUE and the Sphere that contains that Value.
     * SphereAssignments permit the fast removal of VALUEs from MetricTree because we can go
     * directly to the Sphere that needs to be altered.
     *
     * @param <KEY>
     * @param <VALUE>
     */
    static class SphereAssignment<KEY, VALUE> {

        private final MetricTree<KEY, VALUE>.Sphere currentSphere;

        private VALUE value;

        SphereAssignment(MetricTree<KEY, VALUE>.Sphere sphere, VALUE value) {
            this.currentSphere = sphere;
            this.value = value;
        }

        MetricTree<KEY, VALUE>.Sphere sphere() {
            return this.currentSphere;
        }

        VALUE value() {
            return this.value;
        }

        VALUE updateValue(VALUE newValue) {
            VALUE oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }
    }

    /*
     * Use the distance metric, but provide fail-fast notification when feasible.
     */
    private double verifiedDistance(K k1, K k2) {
        double dist = metric.distanceBtw(k1, k2);

        if (Double.isNaN(dist)) {
            throw new IllegalStateException("A distance measurement was NaN.");
        }
        if (dist < 0) {
            throw new IllegalStateException("A negative distance measurement was observed.");
        }
        return dist;
    }
}
