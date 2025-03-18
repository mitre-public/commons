package org.mitre.caasd.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

public class LeftMergerTest {

    @Test
    public void testMerge() {
        LeftMerger<MergeMe> merger = leftMerger();

        assertEquals(10, merger.reduce(reduceNone()).size());
        assertEquals(5, merger.reduce(reduceTo5()).size());
    }

    @Test
    public void testMergeViaCollector() {
        LeftMerger<MergeMe> merger = leftMerger();

        assertEquals(10, reduceNone().stream().collect(merger.asCollector()).size());
        assertEquals(5, reduceTo5().stream().collect(merger.asCollector()).size());
    }

    private List<MergeMe> reduceNone() {
        return LongStream.range(0, 10)
                .map(l -> l * 1500L)
                .mapToObj(MergeMe::new)
                .collect(Collectors.toList());
    }

    private List<MergeMe> reduceTo5() {
        return LongStream.range(0, 10).map(l -> l * 501L).mapToObj(MergeMe::new).collect(Collectors.toList());
    }

    private LeftMerger<MergeMe> leftMerger() {
        BiPredicate<MergeMe, MergeMe> pred = (m1, m2) -> m1.name.equals(m2.name) && Math.abs(m1.val - m2.val) <= 1000L;
        BiFunction<MergeMe, MergeMe, MergeMe> mergeLeft = (m1, m2) -> m1;
        return new LeftMerger<>(pred, mergeLeft);
    }

    private static final class MergeMe {

        private final String name;
        private final long val;

        private MergeMe(long val) {
            this("", val);
        }

        private MergeMe(String n, long v) {
            this.name = n;
            this.val = v;
        }
    }
}
