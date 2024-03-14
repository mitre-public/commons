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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

/**
 * A collection of convenience collection partitioners based on predicates.
 * <p>
 * In general these work in one of two ways:
 * <p>
 * 1) They take a {@link Predicate}, splitting the collection in order into sub-collections based on
 * whether the predicate evaluates to the same thing for successive elements.
 * <p>
 * E.g Given a collection [a,b,c,d,e] that evaluates to [true, true, false, false, true], this would
 * produce a nested set of collections [[a,b],[c,d],[e]]
 * <p>
 * 2) They take a {@link BiPredicate}, splitting the collection in order into sub-collections based
 * on whether the predicate evaluates to true on successive elements. Or to reword this, it groups
 * subsequent elements when the predicate evaluates to true, and splits subsequent elements where it
 * evaluates to false.
 * <p>
 * E.g Given a collection [a,a,b,c,b,b] and using equality as the BiPredicate this would produce a
 * nested set of collections [[a,a],[b],[c],[b,b]]
 */
public class Partitioners {

    /**
     * Split a Collection anytime the output of applying the Predicate to an element produced a new
     * result (ie go from true-to-false OR false-to-true). For example, the input list {-2, -1, 0,
     * 1, 2, 1, 0, -1, -2} can be split into the sequence of Lists: {{-2, -1}, {0, 1, 2, 1, 0}, {-1,
     * -2}} using the predicate {@literal (i -> i >= 0)}.
     *
     * @param data A list that will be split into multiple sub-lists
     * @param rule Adds a new output list whenever applying this predicate changes result.
     *
     * @return a Partitioning of the input data
     */
    public static <T> List<List<T>> splitOnChange(List<T> data, Predicate<T> rule) {
        return data.stream().collect(newListCollector(rule));
    }

    /**
     * This Streams API Collector will split a streamed Collection into a series of Lists where each
     * List contains the "span of elements" that all had the same predicate evaluation. For example,
     * the input list {-2, -1, 0, 1, 2, 1, 0, -1, -2} can be split into the sequence of Lists: {{-2,
     * -1}, {0, 1, 2, 1, 0}, {-1, -2}} using the predicate {@literal (i -> i >= 0)}.
     */
    public static <T> Collector<T, List<List<T>>, List<List<T>>> newListCollector(Predicate<T> pred) {
        return newListCollector(Function.identity(), pred);
    }

    public static <T, U> Collector<T, List<List<T>>, List<U>> newListCollector(
            Function<List<T>, U> partitionFinisher, Predicate<T> pred) {
        return newListCollector(partitionFinisher, toBiPredicate(pred));
    }

    /**
     * Split a Collection anytime the output of applying the BiPredicate to consecutive elements
     * yields false. For example, the input list {a, a, b, c, b, b} can be split into the sequence
     * of Lists: {{a, a} {b}, {c}, {b, b}} using the predicate
     * {@literal (list, e) -> e.equals(list.get(0))}
     *
     * @param data A list that will be split into multiple sub-lists
     * @param rule Creates a new output list whenever applying this predicate yields false.
     */
    public static <T> List<List<T>> splitOnPairwiseChange(List<T> data, BiPredicate<List<T>, T> rule) {
        return data.stream().collect(newListCollector(rule));
    }

    /**
     * This Streams API Collector will split a streamed Collection into a series of Lists where each
     * new List is created when the pair of elements fed to the BiPredicate yield false. For
     * example, the input list {a, a, b, c, b, b} can be split into the sequence of Lists: {{a, a}
     * {b}, {c}, {b, b}} using the predicate {@literal ((i,j) -> i == j).}
     */
    public static <T> Collector<T, List<List<T>>, List<List<T>>> newListCollector(BiPredicate<List<T>, T> pred) {
        return newListCollector(Function.identity(), pred);
    }

    /**
     * Collects elements into partitioned lists based on the provided {@link BiPredicate} applied to
     * the extracted comp element.
     *
     * @param partitionFinisher - function to transform the final list of partitioned lists of
     *                          elements into another form.
     * @param pred              - predicate to use to decide when to split a new element off into a
     *                          different partition
     */
    public static <T, U> Collector<T, List<List<T>>, List<U>> newListCollector(
            Function<List<T>, U> partitionFinisher, BiPredicate<List<T>, T> pred) {

        requireNonNull(partitionFinisher);
        requireNonNull(pred);

        return new ListPartitioner<>(
                partitionFinisher, predicatedConsumer(pred, ArrayList::new, ls -> ls.get(ls.size() - 1)));
    }

    public static <T extends Comparable<? super T>>
            Collector<T, TreeSet<TreeSet<T>>, TreeSet<TreeSet<T>>> newTreeSetCollector(Predicate<T> pred) {
        return newTreeSetCollector(Comparator.comparing(TreeSet::first), Function.identity(), pred);
    }

    public static <T extends Comparable<? super T>, U extends Comparable<? super U>>
            Collector<T, TreeSet<TreeSet<T>>, TreeSet<U>> newTreeSetCollector(
                    Function<TreeSet<T>, U> partitionFinisher, Predicate<T> pred) {
        return newTreeSetCollector(Comparator.naturalOrder(), partitionFinisher, pred);
    }

    public static <T extends Comparable<? super T>, U>
            Collector<T, TreeSet<TreeSet<T>>, TreeSet<U>> newTreeSetCollector(
                    Comparator<U> comparator, Function<TreeSet<T>, U> partitionFinisher, Predicate<T> pred) {
        return newTreeSetCollector(comparator, partitionFinisher, toBiPredicate(pred));
    }

    public static <T extends Comparable<? super T>>
            Collector<T, TreeSet<TreeSet<T>>, TreeSet<TreeSet<T>>> newTreeSetCollector(
                    BiPredicate<TreeSet<T>, T> pred) {
        return newTreeSetCollector(Comparator.comparing(TreeSet::first), Function.identity(), pred);
    }

    public static <T extends Comparable<? super T>, U extends Comparable<? super U>>
            Collector<T, TreeSet<TreeSet<T>>, TreeSet<U>> newTreeSetCollector(
                    Function<TreeSet<T>, U> partitionFinisher, BiPredicate<TreeSet<T>, T> pred) {
        return newTreeSetCollector(Comparator.naturalOrder(), partitionFinisher, pred);
    }

    /**
     * Collects elements into partitioned lists based on the provided {@link BiPredicate} applied to
     * the extracted comp element.
     *
     * @param partitionFinisher - function to transform the final list of partitioned lists of
     *                          elements into another form.
     * @param pred              - predicate to use to decide when to split a new element off into a
     *                          different partition
     */
    public static <T extends Comparable<? super T>, U>
            Collector<T, TreeSet<TreeSet<T>>, TreeSet<U>> newTreeSetCollector(
                    Comparator<U> comparator,
                    Function<TreeSet<T>, U> partitionFinisher,
                    BiPredicate<TreeSet<T>, T> pred) {
        requireNonNull(partitionFinisher);
        requireNonNull(pred);

        return new TreeSetPartitioner<>(
                comparator, partitionFinisher, predicatedConsumer(pred, TreeSet::new, TreeSet::last));
    }

    private static <T, C extends Collection<T>> BiPredicate<C, T> toBiPredicate(Predicate<T> predicate) {
        return (col, t2) -> predicate.test(col.iterator().next()) == predicate.test(t2);
    }

    /**
     * Takes a BiPredicate splitter as well as a boolean on whether to use the first or last element
     * of the grouping being evaluated for partitioning when the split is being checked.
     * <p>
     * E.g. Use firstLast=false when you want to split a group when points are >30s apart, while you
     * would use firstLast=true when the you want to split a group of points who's first and last
     * points are over 5 minutes apart (i.e. 5min subsegments).
     */
    private static <T, C extends Collection<T>, CC extends Collection<C>> BiConsumer<CC, T> predicatedConsumer(
            BiPredicate<C, T> pred, Supplier<C> supplier, Function<CC, C> lastCol) {
        return (x, y) -> {
            if (x.isEmpty()) {
                C col = supplier.get();
                col.add(y);
                x.add(col);
                return;
            }

            C last = lastCol.apply(x);
            if (pred.negate().test(last, y)) {
                C col = supplier.get();
                col.add(y);
                x.add(col);
            } else {
                last.add(y);
            }
        };
    }

    private static final class ListPartitioner<T, U> implements Collector<T, List<List<T>>, List<U>> {

        private final Function<List<T>, U> finisher;
        private final BiConsumer<List<List<T>>, T> accumulator;

        public ListPartitioner(Function<List<T>, U> finisher, BiConsumer<List<List<T>>, T> accumulator) {
            this.finisher = requireNonNull(finisher);
            this.accumulator = requireNonNull(accumulator);
        }

        @Override
        public Supplier<List<List<T>>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<List<T>>, T> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<List<List<T>>> combiner() {
            return (r1, r2) -> {
                r1.addAll(r2);
                return r1;
            };
        }

        @Override
        public Function<List<List<T>>, List<U>> finisher() {
            return l -> l.stream().map(finisher).collect(Collectors.toList());
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Sets.newHashSet(Characteristics.CONCURRENT);
        }
    }

    private static final class TreeSetPartitioner<T extends Comparable<? super T>, U>
            implements Collector<T, TreeSet<TreeSet<T>>, TreeSet<U>> {

        private final Comparator<U> comparator;
        private final Function<TreeSet<T>, U> finisher;
        private final BiConsumer<TreeSet<TreeSet<T>>, T> accumulator;

        public TreeSetPartitioner(
                Comparator<U> comparator,
                Function<TreeSet<T>, U> finisher,
                BiConsumer<TreeSet<TreeSet<T>>, T> accumulator) {
            this.comparator = requireNonNull(comparator);
            this.finisher = requireNonNull(finisher);
            this.accumulator = requireNonNull(accumulator);
        }

        @Override
        public Supplier<TreeSet<TreeSet<T>>> supplier() {
            return () -> new TreeSet<>(Comparator.comparing(TreeSet::first));
        }

        @Override
        public BiConsumer<TreeSet<TreeSet<T>>, T> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<TreeSet<TreeSet<T>>> combiner() {
            return (r1, r2) -> {
                r1.addAll(r2);
                return r1;
            };
        }

        @Override
        public Function<TreeSet<TreeSet<T>>, TreeSet<U>> finisher() {
            return s -> s.stream().map(finisher).collect(Collectors.toCollection(() -> new TreeSet<>(comparator)));
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Sets.newHashSet(Characteristics.CONCURRENT);
        }
    }
}
