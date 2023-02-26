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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PositionTest {

    @Test
    public void rejectsBadLatitudes() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0L, 90.1, 0.0, 100.0));
    }

    @Test
    public void rejectsBadLongitudes() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0L, 0.0, 180.1, 100.0));
    }

    @Test
    public void constructionMakesWhatWeExpect() {
        Position pos = new Position(0L, 1.0, 20.0, 150.0);

        assertThat(pos.time(), is(EPOCH));
        assertThat(pos.latLong(), is(LatLong.of(1.0, 20.0)));
        assertThat(pos.altitude(), is(Distance.ofFeet(150.0)));
        assertThat(pos.hasAltitude(), is(true));
    }

    @Test
    public void constructionMakesWhatWeExpect_2() {
        Position pos = new Position(
            EPOCH,
            LatLong.of(1.0, 20.0),
            Distance.ofFeet(150.0)
        );

        assertThat(pos.time(), is(EPOCH));
        assertThat(pos.latLong(), is(LatLong.of(1.0, 20.0)));
        assertThat(pos.altitude(), is(Distance.ofFeet(150.0)));
        assertThat(pos.hasAltitude(), is(true));
    }

    @Test
    public void altitudeCanBeMissing() {
        Position pos = new Position(EPOCH,LatLong.of(1.0, 20.0));

        assertThat(pos.altitude(), nullValue());
        assertThat(pos.hasAltitude(), is(false));
    }

    @Test
    public void constructionThroughBuilderMakesWhatWeExpect() {
        Position pos = Position.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .build();

        assertThat(pos.time(), is(EPOCH));
        assertThat(pos.latLong(), is(LatLong.of(1.0, 20.0)));
        assertThat(pos.altitude(), is(Distance.ofFeet(150.0)));
        assertThat(pos.hasAltitude(), is(true));
    }

    @Test
    public void constructionThroughBuilderMakesWhatWeExpect_noAltitude() {
        Position pos = Position.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .build();

        assertThat(pos.time(), is(EPOCH));
        assertThat(pos.latLong(), is(LatLong.of(1.0, 20.0)));
        assertThat(pos.altitude(), nullValue());
        assertThat(pos.hasAltitude(), is(false));
    }

    @Test
    public void builderWithSeedClones() {

        Position pos = Position.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .build();

        Position pos2 = Position.builder(pos).build();

        assertThat(pos.equals(pos2), is(true));
    }

    @Test
    public void builderWithSeedClones_removeAltitude() {

        Position pos = Position.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .build();

        Position pos2 = Position.builder(pos)
            .butAltitude(null)
            .build();

        assertThat(pos.time(), is(pos2.time()));
        assertThat(pos.latLong(), is(pos2.latLong()));
        assertThat(pos2.hasAltitude(), is(false));
        assertThat(pos2.altitude(), nullValue());
    }

    @Test
    public void failWhenBuilderSetsLatLongTwice() {
        assertThrows(
            IllegalStateException.class, () -> Position.builder().latLong(0.0, 0.0).latLong(1.0, 1.0)
        );
    }

    @Test
    public void failWhenBuilderSetsLatLongTwice_2() {
        assertThrows(
            IllegalStateException.class,
            () -> Position.builder().latLong(LatLong.of(0.0, 0.0)).latLong(LatLong.of(0.0, 0.0))
        );
    }

    @Test
    public void failWhenBuilderSetsTimeTwice() {
        assertThrows(
            IllegalStateException.class, () -> Position.builder().time(EPOCH).time(EPOCH.plusSeconds(1L))
        );
    }

    @Test
    public void failWhenBuilderSetsAltitudeTwice() {
        assertThrows(
            IllegalStateException.class, () -> Position.builder().altitudeInFeet(150.0).altitudeInFeet(150.0)
        );
    }

    @Test
    public void builderCanOverrideUsingButMethods() {

        Position pos = Position.builder()
            .time(EPOCH)
            .latLong(1.0, 20.0)
            .altitude(Distance.ofFeet(150.0))
            .build();

        Position pos2 = Position.builder(pos).butTime(EPOCH.plusSeconds(1L)).build();

        assertThat(pos2.time(), is(EPOCH.plusSeconds(1L)));
    }
}
