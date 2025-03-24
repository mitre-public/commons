package org.mitre.caasd.commons.math;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.abs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ReservoirSamplerTest {

    @Test
    void demoHappyPath() {
        ReservoirSampler<String> sampler = new ReservoirSampler<>(2);
        sampler.accept("a");
        sampler.accept("b");
        sampler.accept("c");
        sampler.accept("d");
        sampler.accept("e");
        sampler.accept("f");
        sampler.accept("g");

        List<String> letters = newArrayList("a", "b", "c", "d", "e", "f", "g");

        assertThat(sampler.countSeen(), is(7));
        assertThat(sampler.currentSample().size(), is(2));
        assertThat(letters.containsAll(sampler.currentSample()), is(true));
    }

    @Test
    void samplesListCanBeShorterThanSampleSize() {
        ReservoirSampler<String> sampler = new ReservoirSampler<>(7);
        sampler.accept("a");
        sampler.accept("b");

        List<String> samples = sampler.currentSample();

        assertThat(sampler.countSeen(), is(2));
        assertThat(samples.size(), is(2));

        assertThat(sampler.sampleSize(), is(7)); // note the subtle tension
    }

    @Test
    void samplesHaveNoObviousFlaw() {
        // This IS NOT a test of statistical behavior
        // We are looking CLEARLY wrong flaws (e.g. sampling rate = zero when it should be non-zero)

        // In this test we pull 2 random samples from 0-9 -- 100 different times

        List<Integer> numbers = newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        int NUM_TRIALS = 100;
        int SAMPLE_SIZE = 2;
        int[] sampleCounts = new int[10];
        for (int i = 0; i < NUM_TRIALS; i++) {

            // Pull a random sample
            ReservoirSampler<Integer> sampler = new ReservoirSampler<>(SAMPLE_SIZE, new Random(i));
            numbers.forEach(sampler::accept);

            // increment your sample tracking data
            List<Integer> samples = sampler.currentSample();
            samples.forEach((Integer sample) -> sampleCounts[sample]++);
        }

        // sum of all sample counts is correct ...
        int sum = Arrays.stream(sampleCounts).sum();
        assertThat(sum, is(NUM_TRIALS * SAMPLE_SIZE));

        // every number is sampled more than 5 times (expected sample count = 20) (leaving room for random variance)
        Arrays.stream(sampleCounts).forEach(sample -> assertThat(sample > 5, is(true)));
    }

    @Test
    public void samplingIsStatisticallySound() {

        // In this test we pull 10 random samples from 0-19 -- 10_000 different times
        List<Integer> numbers = newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);

        int NUM_TRIALS = 10_000;
        int SAMPLE_SIZE = 10;
        int[] sampleCounts = new int[20];
        Random rng = new Random(17L); // use a single RNG for all 10k samples

        for (int i = 0; i < NUM_TRIALS; i++) {
            ReservoirSampler<Integer> sampler = new ReservoirSampler<>(SAMPLE_SIZE, rng);
            numbers.forEach(sampler::accept);

            // increment your sample tracking data
            List<Integer> samples = sampler.currentSample();
            samples.forEach((Integer sample) -> sampleCounts[sample]++);
        }

        // If: the sampling is statistically correct ...

        // Then: each number 1 through 20 should be in approximately 1/2 the samples
        // And each sampleCount is a binomial random variable with
        // standard deviation = sqrt(n*p*q) = sqrt(10_000 * .5 * .5) = sqrt(2500) = 50

        for (int sampleCount : sampleCounts) {
            // the actual sample count should be within 3 standard deviations (i.e. 150) of 5000
            assertThat(abs(sampleCount - 5000), lessThan(3 * 50));
        }
    }
}
