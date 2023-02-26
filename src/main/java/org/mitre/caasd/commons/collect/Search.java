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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.PriorityQueue;

import org.mitre.caasd.commons.Pair;

/**
 * A Search iterates through a MetricTree collects Key+Value Pairs that are close to the "search
 * key".
 * <p>
 * Search objects can perform a "k-nearest neighbors" or "all neighbors within range" search. Both
 * of these search types require providing "search Key".
 * <p>
 * This class is package private because it is an implementation detail of the MetricTree class.
 *
 * @param <KEY>   The "Key" class is used to measure distance between two objects
 * @param <VALUE> The "Value" class
 */
class Search<KEY, VALUE> {

    private enum SearchType {
        K_NEAREST_NEIGHBORS,
        RANGE;
    }

    private final DistanceMetric<KEY> metric;

    private final SearchType type;

    private final KEY searchKey;

    private final int maxNumResults; //only used for kNN searches

    private final double fixedRadius; //only used for range searches

    private final PriorityQueue<SearchResult<KEY, VALUE>> queue;

    /**
     * Create a kNN search query.
     *
     * @param searchKey     Search for this
     * @param maxNumResults The "k" in k-Nearest-Neighbors
     * @param metric        The distance metric used to determine how far objects are
     */
    Search(KEY searchKey, int maxNumResults, DistanceMetric<KEY> metric) {
        this.metric = metric;
        this.type = SearchType.K_NEAREST_NEIGHBORS;
        this.searchKey = searchKey;
        this.maxNumResults = maxNumResults;
        this.fixedRadius = Double.POSITIVE_INFINITY;
        this.queue = new PriorityQueue<>();
    }

    /**
     * Create a range query that returns all entries within range
     *
     * @param searchKey Search for this
     * @param metric    The distance metric used to determine how far objects are
     * @param range     Include results within this distance
     */
    Search(KEY searchKey, DistanceMetric<KEY> metric, double range) {
        this.metric = metric;
        this.type = SearchType.RANGE;
        this.searchKey = searchKey;
        this.maxNumResults = Integer.MAX_VALUE;
        this.fixedRadius = range;
        this.queue = new PriorityQueue<>();
    }

    /*
     * Note: This search process cannot be written as a recursive search. Searching recursivly can
     * produce a StackoverflowError when the underlying tree is deeper than the JVM's internal stack
     */
    void startQuery(MetricTree<KEY, VALUE>.Sphere root) {

        Deque<MetricTree<KEY, VALUE>.Sphere> stackOfNodesToSearch = new ArrayDeque<>();
        stackOfNodesToSearch.push(root);

        while (!stackOfNodesToSearch.isEmpty()) {

            MetricTree<KEY, VALUE>.Sphere currentNode = stackOfNodesToSearch.pop();

            //ignore this node (and all its sub-trees) when it cannot improve the current result
            if (!this.overlapsWith(currentNode)) {
                continue;
            }

            if (currentNode.isSphereOfPoints()) {
                ingestSphereOfPoints(currentNode);
            } else {

                Pair<MetricTree<KEY, VALUE>.Sphere, MetricTree<KEY, VALUE>.Sphere> childSpheres = currentNode.children();

                double firstDist = metric.distanceBtw(
                    searchKey,
                    (KEY) childSpheres.first().centerPoint);

                double secondDist = metric.distanceBtw(
                    searchKey,
                    (KEY) childSpheres.second().centerPoint);

                /*
                 * Submit the closest sphere second to reduce work (because this increases the
                 * chance we can skip items in the sphere that are further away).
                 */
                if (firstDist < secondDist) {
                    stackOfNodesToSearch.push(childSpheres.second());
                    stackOfNodesToSearch.push(childSpheres.first()); //will be popped first
                } else {
                    stackOfNodesToSearch.push(childSpheres.first());
                    stackOfNodesToSearch.push(childSpheres.second()); //will be popped second
                }
            }
        }
    }

    private void ingestSphereOfPoints(MetricTree<KEY, VALUE>.Sphere inputSphere) {

        for (Map.Entry<KEY, VALUE> entry : inputSphere.points()) {

            SearchResult<KEY, VALUE> r = new SearchResult<>(entry.getKey(),
                entry.getValue(),
                metric.distanceBtw(searchKey, entry.getKey()));

            if (r.distance <= this.radius()) {

                this.queue.offer(r);

                //enforce the "k" in kNN search
                if (queue.size() > this.maxNumResults) {
                    //if too big, remove the worst result
                    queue.poll();
                }
            }
        }
    }

    /** @return True when the "query sphere" and this sphere overlap. */
    private boolean overlapsWith(MetricTree<KEY, VALUE>.Sphere s) {

        double distance = metric.distanceBtw((KEY) s.centerPoint, this.searchKey);
        double overlap = s.radius() + this.radius() - distance;

        return (overlap >= 0);
    }

    /**
     * @return The "inclusion radius" based on the type of query being executed and the quality of
     *     the current results (so we can avoid processing spheres that cannot contain better
     *     results)
     */
    private double radius() {

        if (type == SearchType.K_NEAREST_NEIGHBORS) {
            if (queue.size() < maxNumResults) {
                //radius is still large because we haven't found "k" results yet
                return Double.POSITIVE_INFINITY;
            } else {
                return queue.peek().distance; //must beat this to improve
            }
        } else if (type == SearchType.RANGE) {
            return this.fixedRadius;  //includes everything within this radius
        } else {
            throw new AssertionError("Should never get here");
        }
    }

    Collection<SearchResult<KEY, VALUE>> results() {
        return queue;
    }
}
