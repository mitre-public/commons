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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static org.mitre.caasd.commons.collect.CenterPointSelectors.maxOfRandomSamples;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.mitre.caasd.commons.Pair;

/**
 * A MetricSet is a data-structure designed to efficiently support k-nearest-neighbor (kNN)
 * searches, range searches, as well as regular add/remove operations. To support kNN searches a
 * MetricSet requires a DistanceMetric that defines the distance between any two keys. The
 * DistanceMetric should define a Metric Space (in the strict algebraic sense) in which the
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
 * <p>
 * A MetricSet is loosely based on the MTree introduced by Paolo Ciaccia, Marco Patella, and Pavel
 * Zezula in 1997 in the paper "M-tree An Efficient Access Method for Similarity Search in Metric
 * Spaces" from the Proceedings of the 23rd VLDB Conference.
 * <p>
 * <p>
 * A MetricSet is a binary search tree in which each node in the tree owns a multi-dimensional
 * sphere. That sphere's radius is large enough to ensure that every Key beneath that node is
 * located inside that node's sphere. Therefore, the root node of an MetricSet has a large radius
 * because every Key contained in the MetricSet must fit inside its sphere. Sub-trees are associated
 * with smaller spheres. Each sphere has a centerpoint and radius whose values are used to route
 * adds, kNN searches, and range searches.
 * <p>
 * <p>
 * When Keys are first added to a MetricSet they are placed inside a "Sphere of Points". Eventually,
 * that Sphere will need to be split because it will have too many entries to search quickly. When
 * this occurs the original "Sphere of Points" becomes a "Sphere of Spheres" that owns 2 newly
 * created "Sphere of Points". Together the 2 new "Sphere of Points" will contain all the Key+Value
 * pairs that the original overfilled "Sphere of Points" contained. The centerpoints of the 2 new
 * "Sphere of Points" are selected to reduce the overlapping volume between the 2 new spheres.
 * Reducing this shared volume reduces the number of spheres a search must visit.
 * <p>
 * <p>
 * When Keys are removed from a MetricSet they are correctly removed, however, the fact that the key
 * was present in the MetricSet may leave a permanent imprint on the MetricSet. This occurs when the
 * Key was selected as the centerpoint for a "Sphere of Points". In this case the Key is still used
 * to route add operations and queries even though the Key is no longer contained in the tree. Any
 * insertion of a Key can permanently reduce the query routing efficency of an MetricSet. This
 * occurs when the key insertion forces a Sphere to increase its radius (which will not shrink upon
 * key removal).
 * <p>
 * <p>
 * It is important to know that MetricSets have no automatic balancing mechanism. Therefore
 * inserting Key pairs where the Keys vary in some predictable way is likely to produce a tree that
 * is unbalanced. In principal, tree balance could be maintained by rotating the Sphere nodes
 * similar to how AVL and Red-Black trees rotate nodes to maintain balance. The makeBalancedCopy()
 * and rebalance() methods are provided to combat tree imbalance, but these method are expensive and
 * should be used sparingly if possible. These methods rebuild the tree from scratch by adding the
 * existing Keys (which may exhibit some amount of imbalance) back into the tree in a random order
 * (which will limit the tree's imbalance IMMEDIATELY AFTER the rebuild).
 * <p>
 *
 * @param <K> The Keys, these keys are stored in HashMaps, so their hashcode() and equals() methods
 *            must be defined correctly.
 */
public class MetricSet<K> implements Serializable {

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
    private HashMap<K, Sphere> globalHashMap = new HashMap<>();

    /**
     * Create a new MetricSet that uses the default values for the maxSphereSize and
     * centerPointSelector.
     *
     * @param metric A DistanceMetric that can measure the distance between two Keys.
     */
    public MetricSet(DistanceMetric<K> metric) {
        this(metric, DEFAULT_SPHERE_SIZE);

        TreeMap tm = new TreeMap();
    }

    public MetricSet(DistanceMetric<K> metric, int maxSphereSize) {
        this(metric, maxSphereSize, maxOfRandomSamples());
    }

    public MetricSet(DistanceMetric<K> metric, int maxSphereSize, CenterPointSelector<K> selector) {
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
     * @param key key with which the specified value is to be associated
     *
     * @return the previous value associated with key, or null if there was no prior mapping for the
     *     key. (A null return can also indicate that the map previously associated null with key.)
     */
    public boolean add(K key) {
        checkNotNull(key);

        //delay building root until now because we don't have a key for the centerPoint until
        //the first use of put(K key)
        if (this.rootSphere == null) {
            this.rootSphere = new Sphere(key);
        }

        //do a "short-circuit" put when our global HashMap has already seen the Key provided
        if (globalHashMap.containsKey(key)) {
            return false;
        } else {
            rootSphere.add(key);
            return true;
        }
    }

    public boolean addAll(Collection<K> items) {
        boolean result = false;
        for (K item : items) {
            result |= this.add(item);
        }
        return result;
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

    public boolean contains(K key) {
        return this.globalHashMap.containsKey(key);
    }

    /**
     * Perform a kNN search where k = 1.
     *
     * @param searchKey The point-in-space from which the closest entry is found
     *
     * @return The Key/Value Result with the minimum distance to the search key
     */
    public SetSearchResult<K> getClosest(K searchKey) {

        if (globalHashMap.containsKey(searchKey)) {
            Sphere sa = globalHashMap.get(searchKey);
            return new SetSearchResult<>(searchKey, 0.0); //distance must be zero because we have an exact key match
        }

        Collection<SetSearchResult<K>> results = getNClosest(searchKey, 1);

        ArrayList<SetSearchResult<K>> list = new ArrayList<>(results);
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
    public List<SetSearchResult<K>> getNClosest(K searchKey, int n) {

        checkArgument(nonNull(searchKey));

        if (n < 1) {
            throw new IllegalArgumentException("n must be at least 1");
        }

        //nothing to retrieve...
        if (this.isEmpty()) {
            return Collections.emptyList();
        }

        SetSearch<K> q = new SetSearch<>(searchKey, n, metric);
        q.startQuery(rootSphere);

        ArrayList<SetSearchResult<K>> list = new ArrayList<>(q.results());
        Collections.sort(list);

        return list;
    }

    /**
     * @param searchKey The point-in-space from which the closest entries are found
     * @param range     The distance below which all entries are included in the output.
     *
     * @return A Result for all keys within this range of the key.
     */
    public List<SetSearchResult<K>> getAllWithinRange(K searchKey, double range) {

        checkArgument(nonNull(searchKey));

        if (range <= 0) {
            throw new IllegalArgumentException("The range must be strictly positive " + range);
        }

        //nothing to retrieve...
        if (this.isEmpty()) {
            return Collections.emptyList();
        }

        SetSearch<K> q = new SetSearch<>(searchKey, metric, range);
        q.startQuery(rootSphere);

        ArrayList<SetSearchResult<K>> list = new ArrayList<>(q.results());
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
    public boolean remove(K exactKey) {

        checkArgument(nonNull(exactKey));

        Sphere sa = globalHashMap.remove(exactKey);

        if (sa != null) {
            boolean hadImpact = sa.remove(exactKey);

            if (!hadImpact) {
                throw new AssertionError("Unexpected state, hadImpact should always be true here becuase the key was found in the global map");
            }

            return hadImpact;
        } else {
            return false;
        }
    }

    public void clear() {
        this.rootSphere = null;
        this.globalHashMap = new HashMap<>();
        this.sphereCount = 0;
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
     * Build an entirely new version of this MetricSet. The newly built MetricSet should be
     * relatively well balanced because the Key from "this" MetricSet are inserted into the "new"
     * MetricSet in random order.
     *
     * @return A new version of this MetricSet that should be well balanced
     */
    public MetricSet<K> makeBalancedCopy() {

        List<K> listOfEntries = newArrayList(this.keySet());

        Collections.shuffle(listOfEntries);

        MetricSet<K> newSet = new MetricSet<>(metric);
        newSet.addAll(listOfEntries);

        if (this.size() != newSet.size()) {
            throw new AssertionError("The rebalancing process changed the number of entries");
        }

        return newSet;
    }

    /** Rebuild this MetricSet using makeBalancedCopy(). */
    public void rebalance() {

        MetricSet<K> newMap = makeBalancedCopy();
        this.rootSphere = newMap.rootSphere;
        this.globalHashMap = newMap.globalHashMap;
        this.sphereCount = newMap.sphereCount;
    }

    private enum SphereType {

        /**
         * A SPHERE_OF_POINTS contain a HashSet of key. A SPHERE_OF_POINTS is a leaf node of a
         * MetricSet tree structure
         */
        SPHERE_OF_POINTS,
        /**
         * A SPHERE_OF_SPHERES contains 2 other spheres. A SPHERE_OF_SPHERES is essentially an inner
         * node of a FastMetricTree.
         */
        SPHERE_OF_SPHERES;

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

        private SphereType type;

        /** This Set is used when SphereType == SPHERE_OF_POINTS. */
        private Set<K> entries;

        /** This pair is used when SphereType == SPHERE_OF_SPHERES. */
        private Pair<Sphere, Sphere> childSpheres;

        /** Create a SphereOfPoints centered around this key. */
        Sphere(K key) {
            this.type = SphereType.SPHERE_OF_POINTS;
            this.centerPoint = key;
            this.entries = new HashSet<>();
            this.childSpheres = null;
            sphereCount++;
        }

        double radius() {
            return this.radius;
        }

        boolean isSphereOfPoints() {
            return this.type == SphereType.SPHERE_OF_POINTS;
        }

        boolean isSphereOfSpheres() {
            return this.type == SphereType.SPHERE_OF_SPHERES;
        }

        Set<K> points() {
            return entries;
        }

        Pair<Sphere, Sphere> children() {
            return childSpheres;
        }

        boolean add(K key) {

            if (isFull()) {
                split();
            }

            //update radius if necessary
            this.radius = Math.max(
                radius,
                verifiedDistance(this.centerPoint, key));

            if (isSphereOfPoints()) {
                globalHashMap.put(key, this);
                return this.entries.add(key);
            } else if (isSphereOfSpheres()) {
                Sphere child = findClosestChildSphere(key);
                return child.add(key);
            } else {
                throw new AssertionError("Should never get here, all SphereTypes covered");
            }
        }

        private void split() {
            Pair<Sphere, Sphere> newNodes = this.splitSphereOfPoints();

            //"promote" this SPHERE_OF_POINTS to a SPHERE_OF_SPHERES
            //null out the list of entries
            //use the a pair of Spheres in its place
            this.type = SphereType.SPHERE_OF_SPHERES;
            this.entries = null;
            this.childSpheres = newNodes;
        }

        boolean remove(K key) {
            if (this.isSphereOfPoints()) {
                return this.entries.remove(key);
            } else {
                throw new AssertionError(
                    "Should never get here.  "
                        + "This should only be called on \"Sphere of Points\"");
            }
        }

        private boolean isFull() {
            return (this.type == SphereType.SPHERE_OF_POINTS) && (this.entries.size() >= MAX_INNER_SPHERE_SIZE);
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

        /** Split a SphereOfPoints into two SphereOfPoints. */
        private Pair<Sphere, Sphere> splitSphereOfPoints() {
            checkState(this.type == SphereType.SPHERE_OF_POINTS, "Only SPHERE_OF_POINTS should be split");

            Pair<K, K> centers = pickCentersForNewSpheres();

            Sphere part1 = new Sphere(centers.first());
            Sphere part2 = new Sphere(centers.second());

            moveEntriesToChildren(part1, part2);

            return new Pair<>(part1, part2);
        }

        private Pair<K, K> pickCentersForNewSpheres() {
            return centerPointSelector.selectNewCenterPoints(newArrayList(entries), metric);
        }

        /** Move the entries from this Sphere to the new Sphere. */
        private void moveEntriesToChildren(Sphere part1, Sphere part2) {
            //push the contents of this.children to either part1 or part2

            boolean tieBreaker = false;
            for (K key : entries) {
                addToBestOf(part1, part2, key, tieBreaker);
                tieBreaker = !tieBreaker; //alternate the tiebreaker
            }
        }

        /**
         * Add a Key from a parent node to one of the child nodes
         *
         * @param node1      The left child
         * @param node2      The right child
         * @param entry      The entry that needs to be placed in one of the child nodes
         * @param tieBreaker A tiebreaker to use in the event that an entry is equidistant from both
         *                   child nodes. This is important if many keys in the tree have the same
         *                   location. Without the tiebreaker behavior a StackOverflow can occur.
         */
        private void addToBestOf(Sphere node1, Sphere node2, K entry, boolean tieBreaker) {

            double distanceTo1 = verifiedDistance(entry, node1.centerPoint);
            double distanceTo2 = verifiedDistance(entry, node2.centerPoint);

            Sphere bestSphere = null;

            if (distanceTo1 == distanceTo2) {
                //use the tiebreaker when distances are equal
                bestSphere = (tieBreaker) ? node1 : node2;
            } else if (distanceTo1 < distanceTo2) {
                bestSphere = node1;
            } else {
                bestSphere = node2;
            }

            bestSphere.add(entry);
        }

        Set<K> entries() {

            if (this.isSphereOfPoints()) {
                return this.entries();
            }

            if (this.isSphereOfSpheres()) {

                HashSet<K> set = new HashSet<>();
                set.addAll(childSpheres.first().entries());
                set.addAll(childSpheres.second().entries());

                return set;
            }

            throw new AssertionError("Should never get here, all SphereTypes covered");
        }
    }

    /* Use the distance metric, but provide fail-fast notification if the DistanceMetric fails. */
    private double verifiedDistance(K k1, K k2) {
        double dist = metric.distanceBtw(k1, k2);

        checkState(!Double.isNaN(dist), "A distance measurement was NaN.");
        checkState(dist >= 0, "A negative distance measurement was observed.");
        return dist;
    }
}
