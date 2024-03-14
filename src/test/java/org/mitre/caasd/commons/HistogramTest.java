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

package org.mitre.caasd.commons;

import static java.lang.Math.PI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class HistogramTest {

    @Test
    public void testConstructor() {
        Histogram hist = new Histogram(0, 10, 5, new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        assertEquals(0, hist.min(), 0.000001);
        assertEquals(10, hist.max(), 0.000001);
        assertEquals(5, hist.numColumns(), 0.000001);
        assertArrayEquals(new int[] {1, 2, 2, 2, 3}, hist.counts());
        assertArrayEquals(new double[] {0.1, 0.2, 0.2, 0.2, 0.3}, hist.percentages(), 0.000001);
    }

    @Test
    public void testConstructor_MinMustBeLessThanMax() {
        // Fails because the min is larger than the max
        assertThrows(
                IllegalArgumentException.class,
                () -> new Histogram(10, 1, 10, new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
    }

    @Test
    public void testConstructor_requireAtLeastOneColumn() {
        // Fail because the number of columns is less than 1
        assertThrows(
                IllegalArgumentException.class,
                () -> new Histogram(10, 1, 0, new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
    }

    @Test
    public void testConstructor_CorrectCounts() {
        Histogram hist = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});

        assertArrayEquals(new int[] {3, 0, 0, 0, 1}, hist.counts());
    }

    @Test
    public void testConstructor_DataAboveMaxIsNotLost_PercentagesAreCorrect() {
        Histogram hist = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99, 105});

        assertArrayEquals(new int[] {3, 0, 0, 0, 2}, hist.counts());
        assertArrayEquals(new double[] {0.6, 0, 0, 0, 0.4}, hist.percentages(), 0.000001);
    }

    @Test
    public void testConstructor_DataBelowMinIsNotLost_PercentagesAreCorrect() {
        Histogram hist = new Histogram(0.0, 100.0, 5, new double[] {-1, 11, 12, 13, 99});

        assertArrayEquals(new int[] {4, 0, 0, 0, 1}, hist.counts());
        assertArrayEquals(new double[] {0.8, 0, 0, 0, 0.2}, hist.percentages(), 0.000001);
    }

    @Test
    public void testEmdRequiresSameNumberOfColumns() {
        Histogram hist1 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});
        Histogram hist2 = new Histogram(0.0, 100.0, 10, new double[] {11, 12, 13, 99});

        // Fails because the histograms do not have the same number of columns
        assertThrows(IllegalArgumentException.class, () -> hist1.earthMoverDistanceTo(hist2));
    }

    @Test
    public void testEmdRequiresSameMaximumValue() {
        Histogram hist1 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});
        Histogram hist2 = new Histogram(0.0, 110.0, 5, new double[] {11, 12, 13, 99});

        // Fails because the histograms do not have the same maximum values
        assertThrows(IllegalArgumentException.class, () -> hist1.earthMoverDistanceTo(hist2));
    }

    @Test
    public void testEmdRequiresSameMinimumValues() {
        Histogram hist1 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});
        Histogram hist2 = new Histogram(10.0, 100.0, 5, new double[] {11, 12, 13, 99});

        // Fails because the histograms do not have the same minimum values
        assertThrows(IllegalArgumentException.class, () -> hist1.earthMoverDistanceTo(hist2));
    }

    /* Make a histogram with 10 equal column -- Then add an extra 11th value to break the pattern. */
    private Histogram testHistogram(double extraValue) {
        return new Histogram(0, 100, 10, new double[] {5, 15, 25, 35, 45, 55, 65, 75, 85, 95, extraValue});
    }

    @Test
    public void testEmd_MovingEarthFarIsMoreExpensive() {

        // Creates 10 copies of the same histogram but with one extra value
        Histogram hist0 = testHistogram(5);
        Histogram hist1 = testHistogram(15);
        Histogram hist2 = testHistogram(25);
        Histogram hist3 = testHistogram(35);
        Histogram hist4 = testHistogram(45);
        Histogram hist5 = testHistogram(55);
        Histogram hist6 = testHistogram(65);
        Histogram hist7 = testHistogram(75);
        Histogram hist8 = testHistogram(85);
        Histogram hist9 = testHistogram(95);

        // Compute the distance between all these histograms and the first one
        double distance0to0 = hist0.earthMoverDistanceTo(hist0);
        double distance0to1 = hist0.earthMoverDistanceTo(hist1);
        double distance0to2 = hist0.earthMoverDistanceTo(hist2);
        double distance0to3 = hist0.earthMoverDistanceTo(hist3);
        double distance0to4 = hist0.earthMoverDistanceTo(hist4);
        double distance0to5 = hist0.earthMoverDistanceTo(hist5);
        double distance0to6 = hist0.earthMoverDistanceTo(hist6);
        double distance0to7 = hist0.earthMoverDistanceTo(hist7);
        double distance0to8 = hist0.earthMoverDistanceTo(hist8);
        double distance0to9 = hist0.earthMoverDistanceTo(hist9);

        // All of these distances are strictly sorted and the distance grows as the extra value gets further away
        assertTrue(distance0to0 < distance0to1);
        assertTrue(distance0to1 < distance0to2);
        assertTrue(distance0to2 < distance0to3);
        assertTrue(distance0to3 < distance0to4);
        assertTrue(distance0to4 < distance0to5);
        assertTrue(distance0to5 < distance0to6);
        assertTrue(distance0to6 < distance0to7);
        assertTrue(distance0to7 < distance0to8);
        assertTrue(distance0to8 < distance0to9);
    }

    @Test
    public void testEmd_distanceToYourselfIsZero() {

        Histogram hist0 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});
        Histogram hist1 = new Histogram(0.0, 100.0, 5, new double[] {11, 97, 98, 99});
        Histogram hist2 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 14});
        Histogram hist3 = new Histogram(0.0, 100.0, 5, new double[] {96, 97, 98, 99});

        // distance to the same histogram is zero
        assertEquals(0, hist0.earthMoverDistanceTo(hist0), .000001);
        assertEquals(0, hist1.earthMoverDistanceTo(hist1), .000001);
        assertEquals(0, hist2.earthMoverDistanceTo(hist2), .000001);
        assertEquals(0, hist3.earthMoverDistanceTo(hist3), .000001);
    }

    @Test
    public void testEmd_IsSymmetric() {

        // 75% in the first bin
        Histogram hist0 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});

        // 75% in the last bin
        Histogram hist1 = new Histogram(0.0, 100.0, 5, new double[] {11, 97, 98, 99});

        // 100% in the first bin
        Histogram hist2 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 14});

        // 100% in the last bin
        Histogram hist3 = new Histogram(0.0, 100.0, 5, new double[] {96, 97, 98, 99});

        // distance to the same histogram is zero
        assertEquals(0, hist0.earthMoverDistanceTo(hist0), .000001);
        assertEquals(0, hist1.earthMoverDistanceTo(hist1), .000001);
        assertEquals(0, hist2.earthMoverDistanceTo(hist2), .000001);
        assertEquals(0, hist3.earthMoverDistanceTo(hist3), .000001);

        // These are mirror images of each other. The distances should be the same
        assertEquals(hist2.earthMoverDistanceTo(hist0), hist0.earthMoverDistanceTo(hist2), .00001);
        assertEquals(hist3.earthMoverDistanceTo(hist1), hist1.earthMoverDistanceTo(hist3), .00001);
    }

    @Test
    public void testEmd_DistanceCannotBeNegative() {
        Histogram hist0 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 99});
        Histogram hist1 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 98, 99});
        Histogram hist2 = new Histogram(0.0, 100.0, 5, new double[] {11, 12, 13, 14});

        // all distances are non-negative (regardless of direction)
        assertTrue(hist0.earthMoverDistanceTo(hist1) >= 0);
        assertTrue(hist1.earthMoverDistanceTo(hist0) >= 0);
        assertTrue(hist0.earthMoverDistanceTo(hist2) >= 0);
        assertTrue(hist2.earthMoverDistanceTo(hist0) >= 0);
        assertTrue(hist1.earthMoverDistanceTo(hist2) >= 0);
        assertTrue(hist2.earthMoverDistanceTo(hist1) >= 0);
    }

    @Test
    public void testConstructionViaBuilder() {
        Histogram hist = Histogram.builder()
                .min(0)
                .max(100)
                .numColumns(5)
                .fromRawData(new double[] {11, 12, 13, 99})
                .build();

        assertEquals(0, hist.min(), 0.000001);
        assertEquals(100, hist.max(), 0.000001);
        assertEquals(5, hist.numColumns(), 0.000001);
        assertArrayEquals(new int[] {3, 0, 0, 0, 1}, hist.counts());
        assertArrayEquals(new double[] {.75, 0, 0, 0, .25}, hist.percentages(), 0.000001);
    }

    @Test
    public void testBuilderFromCollection() {
        // Here is an arbitrary data set (that could be any Type T)
        Collection<Double> randomNumbers = sampleShiftedNormalDistribution(new Random(17), 0.0);
        Function<Double, Double> scorer = Double::doubleValue;

        Histogram hist = Histogram.builder()
                .min(-4)
                .max(4)
                .numColumns(40)
                .fromCollection(randomNumbers, scorer) // Testing this line
                .build();

        assertEquals(-4, hist.min(), 0.000001);
        assertEquals(4, hist.max(), 0.000001);
        assertEquals(40, hist.numColumns(), 0.000001);
        assertEquals(1, Arrays.stream(hist.percentages()).sum(), 0.00001);
        assertEquals(0, hist.counts()[0]);
        assertEquals(0, hist.counts()[39]);
        assertTrue(hist.counts()[19] + hist.counts()[20] > 100);
        assertEquals(1000, IntStream.of(hist.counts()).sum());
    }

    private Collection<Double> sampleShiftedNormalDistribution(Random rng, double mean) {
        Collection<Double> randomNumbers = IntStream.range(0, 1000)
                .mapToDouble(x -> rng.nextGaussian() + mean)
                .boxed()
                .collect(Collectors.toList());
        return randomNumbers;
    }

    @Test
    public void testBuilder_RandomEmdDistribution() {
        Random rng = new Random(17);

        Collection<Double> normalMean0 = sampleShiftedNormalDistribution(rng, 0);

        Collection<Double> normalMean2 = sampleShiftedNormalDistribution(rng, 2);

        Collection<Double> normalMean4 = sampleShiftedNormalDistribution(rng, 4);

        Histogram hist0 = Histogram.builder()
                .min(-10)
                .max(10)
                .numColumns(40)
                .fromCollection(normalMean0, Double::doubleValue)
                .build();

        Histogram hist2 = Histogram.builder()
                .min(-10)
                .max(10)
                .numColumns(40)
                .fromCollection(normalMean2, Double::doubleValue)
                .build();

        Histogram hist4 = Histogram.builder()
                .min(-10)
                .max(10)
                .numColumns(40)
                .fromCollection(normalMean4, Double::doubleValue)
                .build();

        assertTrue(
                hist0.earthMoverDistanceTo(hist2) < hist0.earthMoverDistanceTo(hist4),
                "The distance between the sample with mean 0 and the sample with mean 2 is less than "
                        + "the distance between theh sampel with mean 0 and the sample with mean 4");
    }

    @Test
    public void toStringReflectDigitAccuracy() {

        Histogram hist = Histogram.builder()
                .min(PI)
                .max(10)
                .numColumns(4)
                .fromRawData(new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                .build();

        // width = 6.858407346410207 =  (10-PI)
        // step = 1.714601836602552
        // ceiling of 0 = 4.856194490192345
        // ceiling of 1 = 6.570796326794897
        // ceiling of 2 = 8.28539816339745
        // ceiling of 3 = 10.0
        assertThat(hist.toString(5), containsString("-[3.14159-4.85619]"));
        assertThat(hist.toString(5), containsString("[4.85619-6.57080]"));
        assertThat(hist.toString(5), containsString("[6.57080-8.28540]"));
        assertThat(hist.toString(5), containsString("[8.28540-10.00000]+"));

        assertThat(hist.toString(2), containsString("-[3.14-4.86]"));
        assertThat(hist.toString(2), containsString("[4.86-6.57]"));
        assertThat(hist.toString(2), containsString("[6.57-8.29]"));
        assertThat(hist.toString(2), containsString("[8.29-10.00]+"));
    }
}
