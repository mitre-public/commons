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

package org.mitre.caasd.commons.math;

import static java.lang.Math.abs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class ReservoirSamplerTest {


    @Test
    public void constructorWorks() {

        ReservoirSampler<Integer> sampler = new ReservoirSampler(5, new Random(17L));

        assertThat(sampler.k(), is(5));
        assertThat(sampler.numObservations(), is(0));

        sampler.observe(0);
        sampler.observe(1);
        sampler.observe(2);
        sampler.observe(3);
        sampler.observe(4);

        assertThat(sampler.currentSample(), contains(0, 1, 2, 3, 4));
    }

    @Test
    public void samplingIsCorrect() {

        Random rng = new Random(17L); //use a single RNG for all 10k samples

        int NUM_TRIALS = 10_000;

        //for each trial: create a sample of size 10 from the stream the numbers 1 through 20
        int[] sampleCounts = new int[20];

        for (int i = 0; i < NUM_TRIALS; i++) {
            createSampleAndIncrementCounts(sampleCounts, rng);
        }

        //each number 1 through 20 should be in approximately 1/2 the samples (if the sampling is statistically correct)

        //each sampleCounts is a binomial random variable
        //standard deviation = sqrt(n*p*q) = sqrt(10_000 * .5 * .5) = sqrt(2500) = 50

        for (int sampleCount : sampleCounts) {
            //the actual sample count should be within 3 standard deviations (i.e. 150) of 5000
            assertThat(abs(sampleCount - 5000), lessThan(3 * 50));
        }
    }


    private void createSampleAndIncrementCounts(int[] sampleCounts, Random rng) {

        ReservoirSampler sampler = new ReservoirSampler(10, rng);

        sampler.observe(0);
        sampler.observe(1);
        sampler.observe(2);
        sampler.observe(3);
        sampler.observe(4);
        sampler.observe(5);
        sampler.observe(6);
        sampler.observe(7);
        sampler.observe(8);
        sampler.observe(9);
        sampler.observe(10);
        sampler.observe(11);
        sampler.observe(12);
        sampler.observe(13);
        sampler.observe(14);
        sampler.observe(15);
        sampler.observe(16);
        sampler.observe(17);
        sampler.observe(18);
        sampler.observe(19);

        List<Integer> samples = sampler.currentSample();

        for (Integer sample : samples) {
            sampleCounts[sample]++;
        }
    }
}
