package org.mitre.caasd.commons.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.google.common.base.Preconditions;

/**
 * A merge is an object which knows how to combine two objects of the same type into a composite object containing features
 * from both inputs.
 */
public final class LeftMerger<T> {

    /**
     * {@link BiPredicate} to use when determining whether subsequent elements should be merged.
     */
    private final BiPredicate<T, T> mergeable;
    /**
     * {@link BiFunction} to use when merging subsequent mergeable elements into a single object.
     */
    private final BiFunction<T, T, T> mergeLeft;

    public LeftMerger(BiPredicate<T, T> mergeable, BiFunction<T, T, T> mergeLeft) {
        this.mergeable = mergeable;
        this.mergeLeft = mergeLeft;
    }

    public boolean mergeable(T o1, T o2) {
        return mergeable.test(o1, o2);
    }

    public T mergeLeft(T o1, T o2) {
        return mergeLeft.apply(o1, o2);
    }

    /**
     * Takes a collection of {@link LeftMerger}s and allows one to add new elements to a running collection of objects. If the
     * a new object which doesn't meet the {@link #mergeable(Object, Object)} criteria the window is flushed via reducing against
     * {@link #mergeLeft(Object, Object)}. The element is then inserted into the now empty window.
     */
    public List<T> reduce(List<T> mergables) {
        List<T> reduced = new ArrayList<>();
        if (!mergables.isEmpty()) {
            T ele = mergables.get(0);
            for (int i = 1; i < mergables.size(); i++) {
                T next = mergables.get(i);
                if (mergeable(ele, next)) {
                    ele = mergeLeft(ele, next);
                } else {
                    reduced.add(ele);
                    ele = next;
                }
            }
            reduced.add(ele);
        }
        return reduced;
    }

    public boolean nullMergeable(T m1, T m2) {
        Preconditions.checkArgument(!(null == m2 && null == m1));
        return null == m1 || null == m2 || mergeable(m1, m2);
    }

    public T nullableMerge(T m1, T m2) {
        Preconditions.checkArgument(nullMergeable(m1, m2));
        return null == m1 ? m2 : null == m2 ? m1 : mergeLeft(m1, m2);
    }

    /**
     * Returns the given {@link LeftMerger} as a {@link Collector} which can be used to reduce elements at the end of a stream
     * via the mergers {@link #mergeable} and {@link #mergeLeft} functions.
     */
    public Collector<T, List<T>, List<T>> asCollector() {
        return new Collector<T, List<T>, List<T>>() {
            @Override
            public Supplier<List<T>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<T>, T> accumulator() {
                return (list, next) -> {
                    if (list.isEmpty()) {
                        list.add(next);
                    } else {
                        T last = list.get(list.size() - 1);
                        if (mergeable(last, next)) {
                            list.set(list.size() - 1, mergeLeft(last, next));
                        } else {
                            list.add(next);
                        }
                    }
                };
            }

            @Override
            public BinaryOperator<List<T>> combiner() {
                return (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                };
            }

            @Override
            public Function<List<T>, List<T>> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.singleton(Characteristics.IDENTITY_FINISH);
            }
        };
    }
}
