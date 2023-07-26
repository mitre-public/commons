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

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.Speed.Unit.FEET_PER_MINUTE;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class KineticPositionTest {

    @Test
    public void constructorMakesWhatWeExpect() {
        KineticPosition pos = new KineticPosition(
            EPOCH,
            LatLong.of(1.0, 20.0),
            Distance.ofFeet(150.0),
            Speed.of(7, FEET_PER_MINUTE),
            Course.ofDegrees(12),
            1.0,
            Speed.of(42, KNOTS),
            Acceleration.of(Speed.ofKnots(22))
        );

        assertThat(pos.time(), is(EPOCH));
        assertThat(pos.latLong(), is(LatLong.of(1.0, 20.0)));
        assertThat(pos.altitude(), is(Distance.ofFeet(150.0)));
        assertThat(pos.climbRate(), is(Speed.of(7.0, FEET_PER_MINUTE)));
        assertThat(pos.course(), is(Course.ofDegrees(12)));
        assertThat(pos.turnRate(), is(1.0));
        assertThat(pos.speed(), is(Speed.of(42.0, KNOTS)));
        assertThat(pos.acceleration().speedDeltaPerSecond().inKnots(), is(22.0));
    }

    @Test
    public void constructionThroughBuilderMakesWhatWeExpect() {
        KineticPosition pos = KineticPosition.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .climbRate(Speed.of(7, FEET_PER_MINUTE))
            .course(Course.ofDegrees(12))
            .turnRate(1.0)
            .speed(Speed.of(42, KNOTS))
            .acceleration(Acceleration.of(Speed.ofKnots(22)))
            .build();

        assertThat(pos.time(), is(EPOCH));
        assertThat(pos.latLong(), is(LatLong.of(1.0, 20.0)));
        assertThat(pos.altitude(), is(Distance.ofFeet(150.0)));
        assertThat(pos.climbRate(), is(Speed.of(7.0, FEET_PER_MINUTE)));
        assertThat(pos.course(), is(Course.ofDegrees(12)));
        assertThat(pos.turnRate(), is(1.0));
        assertThat(pos.speed(), is(Speed.of(42.0, KNOTS)));
        assertThat(pos.acceleration().speedDeltaPerSecond().inKnots(), is(22.0));
    }

    @Test
    public void builderWithSeedClones() {

        KineticPosition pos = KineticPosition.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .climbRate(Speed.of(7, FEET_PER_MINUTE))
            .course(Course.ofDegrees(12))
            .turnRate(1.0)
            .speed(Speed.of(42, KNOTS))
            .acceleration(Acceleration.of(Speed.ofKnots(22.0)))
            .build();

        KineticPosition pos2 = KineticPosition.builder(pos).build();

        assertThat(pos.equals(pos2), is(true));
        assertThat(pos.hashCode(), is(pos2.hashCode()));
    }

    @Test
    public void failWhenBuilderSetsLatLongTwice() {
        assertThrows(
            IllegalStateException.class, () -> KineticPosition.builder().latLong(0.0, 0.0).latLong(1.0, 1.0)
        );
    }

    @Test
    public void failWhenBuilderSetsLatLongTwice_2() {
        assertThrows(
            IllegalStateException.class,
            () -> KineticPosition.builder().latLong(LatLong.of(0.0, 0.0)).latLong(LatLong.of(0.0, 0.0))
        );
    }

    @Test
    public void failWhenBuilderSetsTimeTwice() {
        assertThrows(
            IllegalStateException.class, () -> KineticPosition.builder().time(EPOCH).time(EPOCH.plusSeconds(1L))
        );
    }

    @Test
    public void failWhenBuilderSetsAltitudeTwice() {
        assertThrows(
            IllegalStateException.class, () -> KineticPosition.builder().altitude(Distance.ofFeet(150.0)).altitude(Distance.ofFeet(150.0))
        );
    }

    @Test
    public void turnRateSpeedAndturnRadiusAgree() {

        KineticPosition pos = new KineticPosition(
            EPOCH,
            LatLong.of(1.0, 20.0),
            Distance.ofFeet(150.0),
            Speed.of(7, FEET_PER_MINUTE),
            Course.ofDegrees(12),
            1.0,
            Speed.of(42, KNOTS),
            Acceleration.of(Speed.ofKnots(22))
        );

        //turning 1 degree per second will require 360 seconds to travel in a circle...
        assertThat(pos.turnRate(), is(1.0));
        assertThat(pos.speed(), is(Speed.of(42.0, KNOTS)));

        Distance circumference = pos.speed().times(Duration.ofSeconds(360));
        Distance radius = circumference.times(1.0 / (2.0 * Math.PI));

        assertThat(pos.turnRadius(), is(radius));
    }

    @Test
    public void turnRadiusDoesNotFailWhenTurnRateIsZero() {

        KineticPosition pos = new KineticPosition(
            EPOCH,
            LatLong.of(1.0, 20.0),
            Distance.ofFeet(150.0),
            Speed.of(7, FEET_PER_MINUTE),
            Course.ofDegrees(12),
            0.0, //TURN RATE IS ZERO
            Speed.of(42, KNOTS),
            Acceleration.of(Speed.ofKnots(22))
        );

        assertThat(pos.turnRadius().inNauticalMiles(), is(Double.POSITIVE_INFINITY));
    }

    @Test
    public void negativeTurnRatesProduceNegativeTurnRadius() {

        KineticPosition position1 = new KineticPosition(
            EPOCH,
            LatLong.of(1.0, 20.0),
            Distance.ofFeet(150.0),
            Speed.of(7, FEET_PER_MINUTE),
            Course.ofDegrees(12),
            -1.0,
            Speed.of(42, KNOTS),
            Acceleration.of(Speed.ofKnots(22))
        );

        KineticPosition position2 = KineticPosition.builder(position1)
            .butTurnRate(1.0)
            .build();

        assertThat(position1.turnRadius().isNegative(), is(true));
        assertThat(position1.turnRadius().abs(), is(position2.turnRadius()));
    }

    @Test
    public void toBytesFromBytes_doesNotChangeData() {

        KineticPosition pos = new KineticPosition(
            EPOCH,
            LatLong.of(1.0, 20.0),
            Distance.ofFeet(150.0),
            Speed.of(7, FEET_PER_MINUTE),
            Course.ofDegrees(12),
            0.0, //TURN RATE IS ZERO
            Speed.of(42, KNOTS),
            Acceleration.of(Speed.ofKnots(22))
        );

        byte[] bytes = pos.toBytes();

        KineticPosition pos_2 = KineticPosition.fromBytes(bytes);

        assertThat(pos, is(pos_2));
        assertThat(bytes, is(pos_2.toBytes()));
    }

    @Test
    public void toBase64FromBase64_doesNotChangeData() {

        KineticPosition pos = KineticPosition.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .climbRate(Speed.of(7, FEET_PER_MINUTE))
            .course(Course.ofDegrees(12))
            .turnRate(1.0)
            .speed(Speed.of(42, KNOTS))
            .acceleration(Acceleration.of(Speed.ofKnots(22)))
            .build();

        String base64 = pos.toBase64();

        KineticPosition pos_2 = KineticPosition.fromBase64(base64);

        assertThat(pos, is(pos_2));
        assertThat(base64, is(pos_2.toBase64()));
    }

    @Test
    public void toBase64_yields96CharString() {

        KineticPosition pos = KineticPosition.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .climbRate(Speed.of(7, FEET_PER_MINUTE))
            .course(Course.ofDegrees(12))
            .turnRate(1.0)
            .speed(Speed.of(42, KNOTS))
            .acceleration(Acceleration.of(Speed.ofKnots(22)))
            .build();

        String base65Str = pos.toBase64();

        assertThat(base65Str.length(), is(96));
    }

}
