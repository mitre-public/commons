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

package org.mitre.caasd.commons.math.locationfit;

import static java.time.Instant.EPOCH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.TimeWindow;

public class GaussianWindowTest {

    @Test
    public void weightsAreNormalDist() {

        Duration windowSize = Duration.ofMinutes(1); //aka 6 standard deviations

        GaussianWindow gw = new GaussianWindow(windowSize);

        double plus6 = gw.computeGaussianWeight(EPOCH, EPOCH.plus(windowSize));
        double minus6 = gw.computeGaussianWeight(EPOCH, EPOCH.minus(windowSize));

        double plus3 = gw.computeGaussianWeight(EPOCH, EPOCH.plus(windowSize.dividedBy(2)));
        double minus3 = gw.computeGaussianWeight(EPOCH, EPOCH.minus(windowSize.dividedBy(2)));

        double plus1 = gw.computeGaussianWeight(EPOCH, EPOCH.plus(windowSize.dividedBy(6)));
        double minus1 = gw.computeGaussianWeight(EPOCH, EPOCH.minus(windowSize.dividedBy(6)));

        assertThat(
            "Weight for +6 standard deviations is essentially 0",
            minus6 < 0.000001,
            is(true)
        );
        assertThat(
            "Weight for -6 standard deviations is essentially 0",
            plus6 < 0.000001,
            is(true)
        );

        //0.011108996538242 = Math.exp(-(3.0 * 3.0) / 2.0) = Expected Weight at +/1 3 Standard Dev
        assertThat(
            "+3.0 standard deviations is 1-2%",
            0.01 < plus3 && plus3 < 0.02,
            is(true)
        );
        assertThat(
            "-3.0 standard deviations is 1-2%",
            0.01 < minus3 && minus3 < 0.02,
            is(true)
        );

        //0.606530659712633 = Math.exp(-(1.0 * 1.0) / 2.0) = Expected Weight at +/- 1 Standard Dev
        assertThat(
            "+1.0 standard deviations is .6065",
            0.60 < plus1 && plus1 < 0.61,
            is(true)
        );
        assertThat(
            "-1.0 standard deviations is .6065",
            0.60 < minus1 && minus1 < 0.61,
            is(true)
        );
    }

    @Test
    public void sigmaIsOneSixthWindowSize() {

        Duration windowSize = Duration.ofMinutes(2);

        GaussianWindow gw = new GaussianWindow(windowSize);

        Duration sigma = gw.sigma();

        assertThat(sigma.toMillis() * 6, is(windowSize.toMillis()));
    }

    @Test
    public void onDemandWindowsAreCorrectSizeAndLocation() {

        Duration windowSize = Duration.ofMinutes(2);

        GaussianWindow gw = new GaussianWindow(windowSize);

        //build an "onDemand TimeWindow we can use to filter data"
        TimeWindow epochWindow = gw.windowCenteredAt(EPOCH);

        assertThat(epochWindow.duration(), is(windowSize));
        assertThat(epochWindow.start(), is(EPOCH.minus(Duration.ofMinutes(1))));
        assertThat(epochWindow.end(), is(EPOCH.plus(Duration.ofMinutes(1))));
    }

    @Test
    public void onDemandWindowsExcludeDataBeyondThreeSigma() {

        GaussianWindow gw = new GaussianWindow(Duration.ofMinutes(2));

        Duration sigma = gw.sigma();

        TimeWindow epochWindow = gw.windowCenteredAt(EPOCH);

        Instant justInside = EPOCH.plus(sigma.multipliedBy(3));
        Instant justOutside = EPOCH.plus(sigma.multipliedBy(3)).plusSeconds(1);

        assertThat(epochWindow.contains(justInside), is(true));
        assertThat(epochWindow.contains(justOutside), is(false));
    }

}
