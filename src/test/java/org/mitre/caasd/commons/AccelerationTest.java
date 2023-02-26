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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AccelerationTest {

    @Test
    public void perSecondNormalizationIsCorrect() {

        Speed input = Speed.ofKnots(10.0);

        Acceleration accel = Acceleration.of(input, Duration.ofSeconds(2L));

        assertThat(accel.speedDeltaPerSecond(), is(Speed.ofKnots(5.0)));
    }

    @Test
    public void timesChangesMagnitude() {
        Speed base = Speed.ofKnots(1.0);
        Acceleration accel = Acceleration.of(base, Duration.ofSeconds(1L));
        assertThat(accel.times(2.0), is(Acceleration.of(Speed.ofKnots(2.0), Duration.ofSeconds(1L))));
    }

    @Test
    public void lessThan_isCorrect() {

        Acceleration accel0 = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration accel1 = Acceleration.of(Speed.ofKnots(0.9), Duration.ofSeconds(1L));

        assertThat(accel1.isLessThan(accel0), is(true));
        assertThat(accel0.isLessThan(accel1), is(false));
        assertThat(accel0.isLessThan(accel0), is(false));
    }

    @Test
    public void lessThanOrEqualTo_isCorrect() {

        Acceleration accel0 = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration accel1 = Acceleration.of(Speed.ofKnots(0.9), Duration.ofSeconds(1L));

        assertThat(accel1.isLessThanOrEqualTo(accel0), is(true));
        assertThat(accel0.isLessThanOrEqualTo(accel1), is(false));
        assertThat(accel0.isLessThanOrEqualTo(accel0), is(true));
    }

    @Test
    public void greaterThan_isCorrect() {

        Acceleration accel0 = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration accel1 = Acceleration.of(Speed.ofKnots(0.9), Duration.ofSeconds(1L));

        assertThat(accel1.isGreaterThan(accel0), is(false));
        assertThat(accel0.isGreaterThan(accel1), is(true));
        assertThat(accel0.isGreaterThan(accel0), is(false));
    }

    @Test
    public void greaterThanOrEqualTo_isCorrect() {

        Acceleration accel0 = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration accel1 = Acceleration.of(Speed.ofKnots(0.9), Duration.ofSeconds(1L));

        assertThat(accel1.isGreaterThanOrEqualTo(accel0), is(false));
        assertThat(accel0.isGreaterThanOrEqualTo(accel1), is(true));
        assertThat(accel0.isGreaterThanOrEqualTo(accel0), is(true));
    }

    @Test
    public void signTestorsAreCorrect() {

        Acceleration posAccelertation = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration negAcceleration = Acceleration.of(Speed.ofKnots(-1.0), Duration.ofSeconds(1L));
        Acceleration zeroAccel = Acceleration.of(Speed.ZERO, Duration.ofMinutes(20));

        assertThat(posAccelertation.isPositive(), is(true));
        assertThat(posAccelertation.isNegative(), is(false));
        assertThat(posAccelertation.isZero(), is(false));

        assertThat(negAcceleration.isPositive(), is(false));
        assertThat(negAcceleration.isNegative(), is(true));
        assertThat(negAcceleration.isZero(), is(false));

        assertThat(zeroAccel.isPositive(), is(false));
        assertThat(zeroAccel.isNegative(), is(false));
        assertThat(zeroAccel.isZero(), is(true));
    }

    @Test
    public void abs_Works() {

        Acceleration posAccelertation = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration negAcceleration = Acceleration.of(Speed.ofKnots(-1.0), Duration.ofSeconds(1L));

        assertThat(posAccelertation.abs(), is(posAccelertation));
        assertThat(negAcceleration.abs(), is(not(negAcceleration)));
        assertThat(negAcceleration.abs(), is(Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L))));
    }

    @Test
    public void equalsIsGood() {

        Acceleration accel0 = Acceleration.of(Speed.ofKnots(1.0), Duration.ofSeconds(1L));
        Acceleration accel1 = Acceleration.of(Speed.ofKnots(2.0), Duration.ofSeconds(2L));

        assertThat(accel0.equals(accel1), is(true));
        assertThat(accel0.hashCode(), is(accel1.hashCode()));
    }

}